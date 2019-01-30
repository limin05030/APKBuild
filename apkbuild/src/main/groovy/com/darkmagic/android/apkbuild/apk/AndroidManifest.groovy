package com.darkmagic.android.apkbuild.apk

import com.android.build.gradle.AppExtension
import com.darkmagic.android.apkbuild.utils.FileUtils
import com.darkmagic.android.apkbuild.utils.Utils
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.ExecSpec

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class AndroidManifest {

    static String ManifestName = "AndroidManifest.xml"

    static boolean replaceManifestFile(Project project, final String apk, String universalApk) {
        Utils.log "Note: replace ${ManifestName}"

        File file = new File("app/${ManifestName}")
        File tmp = null
        if (file.exists()) {
            tmp = new File(file.getAbsolutePath() + ".tmp1")
            if (!file.renameTo(tmp)) {
                throw new GradleException("${file.getAbsolutePath()} can't be modify")
            }
        }

        // 从universalApk中提取出AndroidManifest.xml文件
        if (!extractManifestFile(universalApk, file)) {
            Utils.error "extract ${ManifestName} fail!"
            return false
        }

        Utils.log "AndroidManifest:${file.getAbsolutePath()}"

        final String aapt = getAaptPath(project)


        Utils.log "Note: remove old ${ManifestName}"
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        project.exec(new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec execSpec) {
                execSpec.executable(aapt)
                execSpec.args("r", apk, ManifestName)
                execSpec.setStandardOutput(out)
                execSpec.setErrorOutput(out)
            }
        })

        Utils.log "Note: add new ${ManifestName}"
        project.exec(new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec execSpec) {
                execSpec.executable(aapt)
                execSpec.args("a", apk, ManifestName)
                execSpec.setStandardOutput(out)
                execSpec.setErrorOutput(out)
            }
        })
        file.delete()

        return tmp != null && tmp.renameTo(file)
    }

    private static String getAaptPath(Project project) {
        AppExtension android = project.extensions.android
        File tmp_dir = new File(android.getSdkDirectory(), "build-tools")
        tmp_dir = new File(tmp_dir, android.buildToolsVersion)
        tmp_dir = new File(tmp_dir, "aapt")
        String aapt = new File(tmp_dir, "aapt").absolutePath
        Utils.log "aapt:${aapt}"
        return aapt
    }

    // 提取AndroidManifest.xml
    private static boolean extractManifestFile(String apkPath, File outFile) {
        ZipInputStream zis = null
        try {
            zis = new ZipInputStream(new FileInputStream(new File(apkPath)))
            ZipEntry nextEntry
            while ((nextEntry = zis.getNextEntry()) != null) {
                if (ManifestName == nextEntry.getName()) {
                    if (FileUtils.copyFile((InputStream) zis, outFile)) {
                        return true
                    }
                }
            }
        } catch (Exception e) {
            Utils.log "extractManifestFile:Exception:${e.getMessage()}"
        } finally {
            FileUtils.close(zis)
        }

        return false
    }

}