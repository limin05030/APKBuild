package com.darkmagic.android.apkbuild

import com.darkmagic.android.apkbuild.apk.ApkPublishTaskDebug
import com.darkmagic.android.apkbuild.apk.ApkPublishTaskRelease
import com.darkmagic.android.apkbuild.apk.TaskDoneListener
import com.darkmagic.android.apkbuild.apk.andResGuard.ResGuardExtension
import com.darkmagic.android.apkbuild.apk.config.ApkPublishExtension
import com.darkmagic.android.apkbuild.apk.config.ReplaceExtension
import com.darkmagic.android.apkbuild.apk.preHandle.PreHandleDebug
import com.darkmagic.android.apkbuild.apk.preHandle.PreHandleRelease
import com.darkmagic.android.apkbuild.time.SpendTimeExtension
import com.darkmagic.android.apkbuild.time.TimeListener
import com.darkmagic.android.apkbuild.utils.Utils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * <pre>
 * Note:
 *
 * Author: Sens
 * Date  : 2016/12/22</pre>
 */
class BuildImpl implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.apply plugin: 'com.google.osdetector'

        spendTimeInit(project)
        apkPublishInit(project)

//        project.beforeEvaluate {
//            // 在解析setting.gradle之后，开始解析build.gradle之前执行
//            def manifest = new XmlParser().parse(project.android.sourceSets.main.manifestFile)
//            project.android.defaultConfig.applicationId = manifest.@package
//            println "beforeEvaluate: manifest.@package:${manifest.@package}"
//        }
//
//        project.afterEvaluate {
//            // 在所有.gradle解析完成后，开始执行task之前执行
//            // 此时所有的脚本已经解析完成， task，plugins等所有信息可以获取，task的依赖关系也已经生成，
//            // 如果此时需要做一些事情，可以写在afterEvaluate
//            def manifest = new XmlParser().parse(project.android.sourceSets.main.manifestFile)
//            project.android.defaultConfig.applicationId = manifest.@package
//            println "manifest.@package:${manifest.@package}"
//        }
    }

    static def spendTimeInit(Project project) {
        project.extensions.create("spendTimeOptions", SpendTimeExtension)
        project.gradle.addListener(new TimeListener(project.spendTimeOptions))
    }

    static def apkPublishInit(Project project) {
        project.extensions.create("publishConfig", ApkPublishExtension)
        project.publishConfig.extensions.create("resGuardOption", ResGuardExtension)
        project.publishConfig.extensions.create("replaceConfig", ReplaceExtension)

        project.gradle.addListener(new TaskDoneListener(project.publishConfig))

        createPublishTask(project, "debug", PreHandleDebug, ApkPublishTaskDebug)
        createPublishTask(project, "release", PreHandleRelease, ApkPublishTaskRelease)
    }

    static def createPublishTask(Project project, String variant, Object type1, Object type2) {
        // 前置任务（做一些预处理）
        String mainTaskName = "apkPublish${variant.capitalize()}"
        Task mainTask = project.tasks.findByPath(mainTaskName)
        if (mainTask == null) {
            mainTask = project.task(mainTaskName, type: type1)
            Utils.log "CreateTask:${mainTaskName}"
        }
        mainTask.dependsOn "clean"

        // 真正的APKPublish任务
        String publishTaskName = "repackage${variant.capitalize()}"
        Task publishTask = project.tasks.findByPath(publishTaskName)
        if (publishTask == null) {
            publishTask = project.task(publishTaskName, type: type2)
            Utils.log "CreateTask:${publishTaskName}"
        }
        publishTask.dependsOn "assembleRelease"

        mainTask.finalizedBy publishTaskName
    }

}
