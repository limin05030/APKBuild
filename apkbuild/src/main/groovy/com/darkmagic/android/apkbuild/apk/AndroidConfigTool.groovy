package com.darkmagic.android.apkbuild.apk

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.dsl.AbiSplitOptions
import com.android.build.gradle.internal.dsl.DensitySplitOptions
import com.android.build.gradle.internal.dsl.Splits
import com.android.builder.model.BuildType
import com.android.builder.model.SigningConfig
import com.darkmagic.android.apkbuild.utils.Utils
import org.gradle.api.Project

import java.text.SimpleDateFormat

class AndroidConfigTool {

    static final String UNIVERSAL = "universal"

    static void getAPKPath(Project project, ApplicationVariant variant, ApkPublishInfo publishInfo) {

        List<String> densityList = []
        List<String> abiList = []

        AppExtension android = project.android
        Splits splits = android.getSplits()
        if (splits != null) {
            DensitySplitOptions density = splits.getDensity()
            if (density != null && density.isEnable()) {
                Set<String> list = density.getApplicableFilters()
                if (list != null && list.size() > 0) {
                    publishInfo.splitsDensityEnabled = true
                    densityList.addAll(list)
                }
            }

            AbiSplitOptions abi = splits.getAbi()
            if (abi != null && abi.isEnable()) {
                Set<String> list = abi.getApplicableFilters()
                if (list != null && list.size() > 0) {
                    publishInfo.splitsAbiEnabled = true
                    abiList.addAll(list)
                }
            }
        }

        // 仅设置了splits的density分包策略
        if (publishInfo.splitsDensityEnabled && !publishInfo.splitsAbiEnabled) {
            variant.outputs.each { output ->
                final File apkFile = output.outputFile
                final String apkName = apkFile.name

                if (apkName.contains(UNIVERSAL)) {
                    ApkInfo apk = new ApkInfo()
                    apk.splitName = UNIVERSAL
                    apk.sourceApkFile = apkFile.absolutePath
                    publishInfo.apkList.add(apk)
                } else {
                    for (String density : densityList) {
                        if (apkName.contains("-$density-")) {
                            ApkInfo apk = new ApkInfo()
                            apk.splitName = density
                            apk.sourceApkFile = apkFile.absolutePath
                            publishInfo.apkList.add(apk)
                            break
                        }
                    }
                }
            }
        }
        // 仅设置了splits的abi分包策略
        else if (!publishInfo.splitsDensityEnabled && publishInfo.splitsAbiEnabled) {
            variant.outputs.each { output ->
                File apkFile = output.outputFile
                String apkName = apkFile.name

                if (apkName.contains(UNIVERSAL)) {
                    ApkInfo apk = new ApkInfo()
                    apk.splitName = UNIVERSAL
                    apk.sourceApkFile = apkFile.absolutePath
                    publishInfo.apkList.add(apk)
                } else {
                    for (String abi : abiList) {
                        if (apkName.contains("-$abi-")) {
                            ApkInfo apk = new ApkInfo()
                            apk.splitName = abi
                            apk.sourceApkFile = apkFile.absolutePath
                            publishInfo.apkList.add(apk)
                            break
                        }
                    }
                }
            }
        }
        // 同时设置了splits的density和abi分包策略
        else if (publishInfo.splitsDensityEnabled && publishInfo.splitsAbiEnabled) {
            variant.outputs.each { output ->
                File apkFile = output.outputFile
                String apkName = apkFile.name

                // 通用包
                if (apkName.contains(UNIVERSAL)) {
                    ApkInfo apk = new ApkInfo()
                    apk.splitName = UNIVERSAL
                    apk.sourceApkFile = apkFile.absolutePath
                    publishInfo.apkList.add(apk)
                    return
                }

                // 只包含abi的包
                for (String abi : abiList) {
                    if (apkName.contains("-$abi-")) {
                        ApkInfo apk = new ApkInfo()
                        apk.splitName = abi
                        apk.sourceApkFile = apkFile.absolutePath
                        publishInfo.apkList.add(apk)
                        return
                    }
                }

                // 同时包含density和abi的包（没有单独包含density的包）
                for (String abi : abiList) {
                    // 将abi首字母转换成大写
//                    String abi2 = "${abi.substring(0, 1).toUpperCase()}${abi.substring(1)}"
                    String abi2 = abi.capitalize()
                    for (String density : densityList) {
                        // 格式：app-hdpiArmeabi-v7a-debug（filter和abi之间没有横线且abi首字母大写）
                        if (apkName.contains("-$density$abi2-")) {
                            ApkInfo apk = new ApkInfo()
                            apk.splitName = "${density}_$abi"
                            apk.sourceApkFile = apkFile.absolutePath
                            publishInfo.apkList.add(apk)
                            return
                        }
                    }
                }
            }
        } else {
            ApkInfo apk = new ApkInfo()
            apk.splitName = null
            apk.sourceApkFile = variant.outputs[0].outputFile.absolutePath
            publishInfo.apkList.add(apk)
        }

        // 获取生成的mapping文件路径
        publishInfo.sourceMappingFile = variant.getMappingFile().absolutePath
    }

    static void getSigningConfig(ApplicationVariant variant, ApkPublishInfo publishInfo) {
        BuildType type = variant.getBuildType()
        if (type != null && "release" == type.name) {
            publishInfo.minifyEnabled = type.isMinifyEnabled()
            SigningConfig sign = type.getSigningConfig()
            if (sign != null) {
                publishInfo.storeFile = new StoreFile()
                publishInfo.storeFile.storeFile = sign.getStoreFile().getAbsolutePath()
                publishInfo.storeFile.storePassword = sign.getStorePassword()
                publishInfo.storeFile.keyAlias = sign.getKeyAlias()
                publishInfo.storeFile.keyPassword = sign.getKeyPassword()
                publishInfo.storeFile.storeType = sign.getStoreType()
            }
        }
    }

    // versionName_date_{debug|release}_channel
    static String generateBaseName(String versionName, String buildType, String channel, String time) {
        String baseName = "${versionName}_${time}_${buildType.toLowerCase()}"

        if (!Utils.isEmpty(channel)) {
            baseName = "${baseName}_${channel}"
        }

        return baseName
    }

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss")

    static String generateTime() {
        return format.format(new Date())
    }

}