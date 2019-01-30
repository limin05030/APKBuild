package com.darkmagic.android.apkbuild.apk.andResGuard

import com.android.build.gradle.BaseExtension
import com.darkmagic.android.apkbuild.apk.ApkInfo
import com.darkmagic.android.apkbuild.apk.ApkPublishInfo
import com.darkmagic.android.apkbuild.apk.StoreFile
import com.darkmagic.android.apkbuild.utils.Utils
import com.tencent.mm.resourceproguard.InputParam
import com.tencent.mm.resourceproguard.Main
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration


class ResGuard {

    static boolean compression(Project project, ResGuardExtension resGuard, ApkPublishInfo publishInfo) {
        Utils.log "CompressionApkResource"

        ArrayList<String> whiteList = new ArrayList<>()
        resGuard.whiteList.each { res ->
            if (res.startsWith("R")) {
                whiteList.add("${publishInfo.pkgName}.$res".toString())
            } else {
                whiteList.add(res)
            }
        }

        String sevenPath = getSevenPath(project, resGuard.sevenZipPath)
        String zipAlignPath = getZipAlignPath(project)

        for (ApkInfo apk : publishInfo.apkList) {
            if (!resApk(resGuard, apk, publishInfo.storeFile, whiteList, sevenPath, zipAlignPath)) {
                return false
            }
        }

        return true
    }

    /**
     * 对APK文件进行资源文件混淆
     */
    private static boolean resApk(ResGuardExtension resGuard, ApkInfo apk, StoreFile storeFile,
                                  ArrayList<String> whiteList, String sevenPath, String zipAlignPath) {
        String outDir = new File(new File(apk.sourceApkFile).parentFile, "resGuard_${apk.splitName}").absolutePath

        Utils.log "andResGuard:mappingFile:${resGuard.mappingFile}"
        Utils.log "andResGuard:whiteList:$whiteList"
        Utils.log "andResGuard:Use7zip:${resGuard.use7zip}"
        Utils.log "andResGuard:sevenZipPath:$sevenPath"
        Utils.log "andResGuard:metaName:${resGuard.metaName}"
        Utils.log "andResGuard:keepRoot:${resGuard.keepRoot}"
        Utils.log "andResGuard:compressFilePattern:${resGuard.compressFilePattern}"
        Utils.log "andResGuard:ZipAlignPath:$zipAlignPath"
        Utils.log "andResGuard:apkPath:${apk.sourceApkFile}"
        Utils.log "andResGuard:outDir:$outDir"
        Utils.log "andResGuard:useSign:${resGuard.useSign}"
        Utils.log "andResGuard:SignFile:${storeFile?.storeFile}"

        InputParam.Builder builder = new InputParam.Builder()
        builder.setMappingFile(resGuard.mappingFile)
        builder.setWhiteList(whiteList)
        builder.setUse7zip(resGuard.use7zip)
        builder.setSevenZipPath(sevenPath)
        builder.setMetaName(resGuard.metaName)
        builder.setKeepRoot(resGuard.keepRoot)
        builder.setCompressFilePattern(resGuard.compressFilePattern)
        builder.setZipAlign(zipAlignPath)
        builder.setApkPath(apk.sourceApkFile)
        builder.setOutBuilder(outDir)
        builder.setUseSign(resGuard.useSign)
        builder.logEnabled(Utils.mLogEnable)

        if (resGuard.useSign) {
            if (storeFile == null || Utils.isEmpty(storeFile.storeFile)) {
                throw new GradleException("ResGuard: Not found the storeFile")
            }

            builder.setSignFile(new File(storeFile.storeFile))
            builder.setKeypass(storeFile.keyPassword)
            builder.setStorealias(storeFile.keyAlias)
            builder.setStorepass(storeFile.storePassword)
        }

        Main.Result result = Main.gradleRun(builder.create())
        if (result == null) {
            return false
        }

        if (result.outApkFile != null && result.outApkFile.exists()) {
            apk.sourceApkFile = result.outApkFile.absolutePath
        } else {
            Utils.log "andResGuard: Not found the APK file"
            return false
        }

        if (resGuard.outputResMappingFile) {
            if (result.outMappingFile != null && result.outMappingFile.exists()) {
                apk.sourceResMappingFile = result.outMappingFile.absolutePath
            } else {
                Utils.log "andResGuard: Not found the res mapping file"
                return false
            }
        }

        return true
    }

    /**
     * 获取7z路径
     *
     * @param path 7z的本地路径，如：'D:\\tools\\7z\\7z.exe'，也可以仓库地址，如：'com.tencent.mm:SevenZip:1.2.3'
     */
    private static String getSevenPath(Project project, String path) {
        if (!Utils.isEmpty(path) && new File(path).exists()) {
            return path
        }

        String groupId, artifactId, version
        String artifact = Utils.isEmpty(path) ? 'com.tencent.mm:SevenZip:1.2.13' : path
        (groupId, artifactId, version) = artifact.split(":")

        Configuration config = project.configurations.findByName("AndResGuardLocatorSevenZip")
        if (config == null) {
            config = project.configurations.create("AndResGuardLocatorSevenZip") {
                visible = false
                transitive = false
                extendsFrom = []
            }
            def notation = [group     : groupId,
                            name      : artifactId,
                            version   : version,
                            classifier: project.osdetector.classifier,
                            ext       : 'exe']
            project.dependencies.add(config.name, notation)
        }

        File file = null
        config.getDependencies().each { dep ->
            if (artifactId == dep.name) {
                file = config.fileCollection(dep).singleFile
                if (!file.canExecute() && !file.setExecutable(true)) {
                    throw new GradleException("Cannot set ${file} as executable")
                }
            }
        }

        return file?.path
    }

    static String getZipAlignPath(Project project) {
        BaseExtension android = project.extensions.android
        File tmp = new File(android.getSdkDirectory(), "build-tools")
        tmp = new File(tmp, android.buildToolsVersion)
        return new File(tmp, "zipalign").absolutePath
    }

}