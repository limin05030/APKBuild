package com.darkmagic.android.apkbuild.apk

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.darkmagic.android.apkbuild.apk.andResGuard.ResGuard
import com.darkmagic.android.apkbuild.apk.andResGuard.ResGuardExtension
import com.darkmagic.android.apkbuild.apk.config.ApkPublishExtension
import com.darkmagic.android.apkbuild.utils.FileUtils
import com.darkmagic.android.apkbuild.utils.Utils
import com.tencent.mm.resourceproguard.Main
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction


/**
 * <pre>
 * Note:
 *
 * Author: Sens
 * Date  : 2016/12/22</pre>
 */
abstract class ApkPublishTaskImpl extends DefaultTask {

    ApkPublishExtension publishConfig
    ResGuardExtension resGuardOptions
    String publishType
    File logFile

    ApkPublishTaskImpl() {
        outputs.upToDateWhen { false }

        if (!project.plugins.hasPlugin('com.android.application')) {
            throw new GradleException("${ApkPublishExtension.TAG}: Android Application plugin required")
        }

        publishType = getPublishType()
        publishConfig = project.publishConfig
        resGuardOptions = publishConfig.resGuardOption
    }

    abstract String getPublishType()

    @SuppressWarnings("GroovyUnusedDeclaration")
    @TaskAction
    def publish() {
        init()

        AppExtension android = project.android
        android.applicationVariants.all { variant ->
            if ("release".equalsIgnoreCase(variant.name)) {
                if (!publishApk(variant)) {
                    throw new GradleException("${ApkPublishExtension.TAG} FAIL")
                }
            }
        }
    }

    void init() {
        Utils.init(project, publishConfig.logEnable)

        // 执行任务的时候动态指定将打包结果保存到指定文件中，否则直接打印出来
        String logFilePath
        // 获取logFilePath参数指定的值
        if (project.hasProperty("logFilePath")) {
            logFilePath = project.getProperties()["logFilePath"]
        } else {
            Utils.log "logFile:no Property 'logFilePath'"
            logFilePath = null
        }

        if (Utils.isEmpty(logFilePath)) {
            logFile = null
        } else {
            logFile = new File(logFilePath)
        }
        Utils.log "logFile: ${logFile}"
    }

    boolean publishApk(ApplicationVariant variant) {
        Utils.log publishType.capitalize()// Debug | Release

        // 1，获取生成的APK和mapping文件信息
        ApkPublishInfo publishInfo = generateBasePublishInfo(variant)
        if (publishInfo == null) {
            return false
        }

        // 2， 对生成的APK包进行处理
        if (resGuardOptions.enabled) {// 进行资源混淆（包括v1签名）
            if (!ResGuard.compression(project, resGuardOptions, publishInfo)) {
                return false
            }
        }
        // 判断是否替换了Manifest文件，如果替换了则重新进行v1签名
        // 该功能目前已经废弃了，manifestFileIsReplaced始终为假
        else if (publishInfo.manifestFileIsReplaced) {
            StoreFile storeFile = publishInfo.storeFile
            for (ApkInfo apk : publishInfo.apkList) {
                if (apk.splitName == AndroidConfigTool.UNIVERSAL) {
                    continue
                }

                Utils.log "SignApk: ${apk.sourceApkFile}"
                boolean ret = Main.signApk(new File(apk.sourceApkFile), new File(storeFile.storeFile),
                        storeFile.keyPassword, storeFile.keyAlias, storeFile.storePassword,
                        publishConfig.logEnable)
                if (!ret) {
                    Utils.log "sign apk fail:${publishInfo.sourceApkFile}"
                    return false
                }
            }
        }

        // 3，进行v2签名
        if (publishConfig.v2SigningEnabled) {
            for (ApkInfo apk : publishInfo.apkList) {
                if (!signV2(project, new File(apk.sourceApkFile), publishInfo.storeFile)) {
                    Utils.log "sign(V2) apk fail:${apk.sourceApkFile}"
                    return false
                }
            }
        }

        // 4, install
        Utils.log "Install..."
        File outDir
        if (Utils.isEmpty(publishConfig.outDir)) {
            outDir = null
        } else {
            outDir = new File(new File(publishConfig.outDir, "V${publishInfo.versionName}"), publishType.toLowerCase())
        }
        String time = AndroidConfigTool.generateTime()
        String baseName = AndroidConfigTool.generateBaseName(publishInfo.versionName, publishType, publishConfig.channel, time)
        for (ApkInfo apk : publishInfo.apkList) {
            if (!installApkFile(outDir, apk, baseName)) {
                return false
            }
        }
        if (!installMappingFile(outDir, publishInfo, baseName)) {
            return false
        }

        // 5, 这里是为了支持CrashReporter软件，打包完成后将mapping文件复制到CrashReporter的服务器目录下
        if (!Utils.isEmpty(publishConfig.crashReporterProGuard) && !Utils.isEmpty(publishConfig.outDir)) {
            File target = new File(publishConfig.crashReporterProGuard, publishInfo.pkgName)
            target.mkdirs()
            if (Utils.isEmpty(publishConfig.channel)) {
                target = new File(target, "${publishInfo.versionCode}.txt")
            } else {
                target = new File(target, "${publishInfo.versionCode}_${publishConfig.channel.toLowerCase()}.txt")
            }
            target.delete()
            copyFile(new File(publishInfo.sourceMappingFile), target)
        }

        // 复制带符号的so文件
        String soDir
        if (publishType == "debug") {
            soDir = publishConfig.symbolDebugSOPath
        } else {
            soDir = publishConfig.symbolReleaseSOPath
        }
        List<String> soNameList = publishConfig.includeSONameList
        if (!Utils.isEmpty(soDir) && outDir != null && !soNameList.isEmpty()) {
            File symDir = new File(soDir)
            if (!symDir.exists() || !symDir.isDirectory()) {
                Utils.error "The specified so dir($symDir) not exist or not is a directory!!"
                return false
            }

            File[] abiDirs = symDir.listFiles()
            if (abiDirs == null || abiDirs.length == 0) {
                Utils.error "Not found abi dir in the specified so path($symDir)"
                return false
            }

            File sym_so_dir = new File(new File(outDir, "symbol_so"), time)
            if (!sym_so_dir.exists()) {
                sym_so_dir.mkdirs()
            }

            int count = FileUtils.copyFolder(symDir, sym_so_dir, soNameList)
            if (count < abiDirs.length * soNameList.size()) {
                Utils.error "Some .so file not found or copy fail!!"
                return false
            }

            Utils.log "Install symbol so file count: $count"
        }

        // 8，记录/输出结果
        if (logFile == null) {
            // publishConfig.outputs里面的内容将在任务执行结束的时候统一打印出来
            // 见TaskDoneListener
            publishInfo.apkList.each { publishConfig.outputs.add(it.targetApkFile) }

        } else {
            publishInfo.apkList.each { apk ->
                FileUtils.writeFile(logFile, "${apk.targetApkFile}\r\n", true)
            }
        }

        return true
    }

    private boolean installApkFile(File outDir, ApkInfo apk, String baseName) {
        File installDir
        boolean installToLocal
        if (outDir == null) {
            installToLocal = true
            installDir = new File(apk.sourceApkFile).getParent()
        } else {
            installToLocal = false
            installDir = Utils.isEmpty(apk.splitName) ? outDir : new File(outDir, apk.splitName)
            if (!installDir.exists() && !installDir.mkdirs()) {
                Utils.log "Install dir create failure($installDir)"
                return false
            }
        }

        if (Utils.isEmpty(publishConfig.apkPrefix)) {
            publishConfig.apkPrefix = "app"
        }

        File targetApkFile
        File targetResMappingFile
        if (Utils.isEmpty(apk.splitName)) {
            targetApkFile = new File(installDir, "${publishConfig.apkPrefix}_${baseName}.apk")
            targetResMappingFile = new File(installDir, "res_mapping_${baseName}.txt")
        } else {
            targetApkFile = new File(installDir, "${publishConfig.apkPrefix}_${apk.splitName}_${baseName}.apk")
            targetResMappingFile = new File(installDir, "res_mapping_${apk.splitName}_${baseName}.txt")
        }

        File tmp
        if (installToLocal) {
            tmp = new File(apk.sourceApkFile)

            if (!tmp.renameTo(targetApkFile)) {
                Utils.log "APK File rename failure(from=$tmp to=$targetApkFile)"
                return false
            }

            if (!Utils.isEmpty(apk.sourceResMappingFile)) {
                tmp = new File(apk.sourceResMappingFile)
                if (!tmp.renameTo(targetResMappingFile)) {
                    Utils.log "Res Mapping File rename failure(from=$tmp to=$targetResMappingFile)"
                    return false
                }
            }
        } else {
            tmp = new File(apk.sourceApkFile)

            if (!copyFile(tmp, targetApkFile)) {
                Utils.log "APK File copy failure(from=$tmp to=$targetApkFile)"
                return false
            }

            if (!Utils.isEmpty(apk.sourceResMappingFile)) {
                tmp = new File(apk.sourceResMappingFile)
                if (!copyFile(tmp, targetResMappingFile)) {
                    Utils.log "Res Mapping File copy failure(from=$tmp to=$targetResMappingFile)"
                    return false
                }
            }
        }

        apk.targetApkFile = targetApkFile.absolutePath
        apk.targetResMappingFile = targetResMappingFile.absolutePath
        return true
    }

    private static boolean installMappingFile(File outDir, ApkPublishInfo info, String baseName) {
        File installDir
        File targetMappingFile
        File srcMappingFile = new File(info.sourceMappingFile)
        if (outDir == null) {
            installDir = srcMappingFile.getParent()
            targetMappingFile = new File(installDir, "mapping_${baseName}.txt")
            if (!srcMappingFile.renameTo(targetMappingFile)) {
                Utils.log "Mapping File rename failure(from=$srcMappingFile to=$targetMappingFile)"
                return false
            }
        } else {
            targetMappingFile = new File(outDir, "mapping_${baseName}.txt")
            if (!copyFile(srcMappingFile, targetMappingFile)) {
                Utils.log "Mapping File copy failure(from=$srcMappingFile to=$targetMappingFile)"
                return false
            }
        }

        return true
    }

    private static boolean signV2(Project project, File apkFile, StoreFile storeFile) {
        File tmpFile = new File(apkFile.parentFile, "${apkFile.name.subSequence(0, apkFile.name.length() - 4)}.out.apk")

        // 使用V2签名，生成tmpFile文件
        if (!SignTool.signV2(project, storeFile.storeFile, storeFile.storePassword, storeFile.keyAlias,
                storeFile.keyPassword, tmpFile, apkFile)) {
            return false
        }

        // 用生成的tmpFile文件覆盖apkFile文件
        for (int max = 3; max > 0; max--) {
            if (apkFile.delete()) {
                break
            }
            try {
                Thread.sleep(1000)
            } catch (Exception ignored) {
            }
        }
        return tmpFile.renameTo(apkFile)
    }

    static boolean copyFile(File srcFile, File destFile) {
        Utils.log "copy file[${srcFile.absolutePath}] to [${destFile.absolutePath}]"

        if (FileUtils.copyFile(srcFile, destFile)) {
            Utils.log "copy OK"
            return true
        } else {
            Utils.error "copy file [${srcFile.absolutePath}] to [${destFile.absolutePath}] fail"
            return false
        }
    }

    // 获取生成的信息
    ApkPublishInfo generateBasePublishInfo(ApplicationVariant variant) {
        Utils.log "GeneratePublishInfo"

        ApkPublishInfo publishInfo = new ApkPublishInfo()

        // 获取包名和版本号等信息
        publishInfo.pkgName = variant.getApplicationId()
        publishInfo.versionName = variant.getVersionName()
        publishInfo.versionCode = variant.getVersionCode()

        // 获取生成的APK路径
        AndroidConfigTool.getAPKPath(project, variant, publishInfo)

        // 获取签名信息
        AndroidConfigTool.getSigningConfig(variant, publishInfo)


        // 进行Manifest文件替换
        // 该功能目前已经废弃了，不再进行Manifest文件的替换
        if (publishInfo.splitsDensityEnabled && /*publishConfig.replaceManifest*/false) {
            // 查找通用包
            String universalApk = null
            for (String apk : publishInfo.apkList) {
                if (apk.splitName == AndroidConfigTool.UNIVERSAL) {
                    universalApk = apk.sourceApkFile
                    break
                }
            }

            // 将其他分包中的Manifest文件替换为通用包中的Manifest文件
            if (universalApk != null) {
                publishInfo.apkList.each { apk ->
                    if (apk.splitName != null && apk.splitName != AndroidConfigTool.UNIVERSAL) {
                        if (!AndroidManifest.replaceManifestFile(project, apk.sourceApkFile, universalApk)) {
                            return null
                        }
                    }
                }

                publishInfo.manifestFileIsReplaced = true
            }
        } else {
            publishInfo.manifestFileIsReplaced = false
        }

        return publishInfo
    }

}