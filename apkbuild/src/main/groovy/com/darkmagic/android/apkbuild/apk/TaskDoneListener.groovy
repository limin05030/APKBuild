package com.darkmagic.android.apkbuild.apk

import com.darkmagic.android.apkbuild.apk.config.ApkPublishExtension
import com.darkmagic.android.apkbuild.utils.FileUtils
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle

/**
 * <pre>
 * Note:
 *
 * Author: Sens
 * Date  : 2016/12/22</pre>
 */
class TaskDoneListener implements BuildListener {

    ApkPublishExtension publishConfig

    TaskDoneListener(ApkPublishExtension publishConfig) {
        this.publishConfig = publishConfig
    }

    @Override
    void buildFinished(BuildResult buildResult) {
        if (publishConfig != null) {
            publishConfig.backupFileMap.each { srcFile, tmpFile ->
                FileUtils.copyFile(tmpFile, new File(srcFile))
                tmpFile.delete()
            }

            if (!publishConfig.outputs.empty) {
                println("\r\nINSTALL PATH:")
                publishConfig.outputs.each { path -> println "${path}" }
                println()
            }
        }
    }

    @Override
    void buildStarted(Gradle gradle) {
    }

    @Override
    void settingsEvaluated(Settings settings) {
    }

    @Override
    void projectsLoaded(Gradle gradle) {
    }

    @Override
    void projectsEvaluated(Gradle gradle) {
    }
}
