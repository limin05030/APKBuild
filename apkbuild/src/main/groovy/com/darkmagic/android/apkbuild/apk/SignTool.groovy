package com.darkmagic.android.apkbuild.apk

import com.android.build.gradle.AppExtension
import com.darkmagic.android.apkbuild.utils.Utils
import org.gradle.api.Project

/**
 * <pre>
 * Note:
 *
 * Author: Sens
 * Date  : 2017/08/21</pre>
 */
class SignTool {

    static signV2(Project project, String storeFile, String storePassword, String keyAlias, String keyPassword,
                  File outApk, File inApk) {

        Utils.log "signing(v2) apk: ${inApk.absolutePath}"
        String[] argv = [
                "java",
                "-jar", getApkSignerPath(project),
                "sign",
                "--ks", storeFile,
                "--ks-key-alias", keyAlias,
                "--ks-pass", "pass:$storePassword",
                "--key-pass", "pass:$keyPassword",
                "--out", outApk.absolutePath,
                inApk.absolutePath
        ]

        Process pro = null
        BufferedReader reader = null
        try {
            pro = Runtime.getRuntime().exec(argv)

            reader = new BufferedReader(new InputStreamReader(pro.errorStream))
            String line
            while ((line = reader.readLine()) != null) {
                Utils.log line
            }

            //destroy the stream
            pro.waitFor()
        } catch (Throwable e) {
            e.printStackTrace()
        } finally {
            reader?.close()
            pro?.destroy()
        }

        if (outApk.exists()) {
            return true
        } else {
            Utils.log "Can't Generate signed v2 APK. Plz check your sign info is correct."
            return false
        }
    }


    private static String getApkSignerPath(Project project) {
        AppExtension android = project.extensions.android
        File tmp = new File(android.getSdkDirectory(), "build-tools")
        tmp = new File(tmp, android.buildToolsVersion)
        return new File(new File(tmp, "lib"), "apksigner.jar").absolutePath
    }
}