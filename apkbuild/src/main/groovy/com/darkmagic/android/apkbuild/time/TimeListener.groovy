package com.darkmagic.android.apkbuild.time

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState

/**
 * <pre>
 * Note:
 *
 * Author: Sens
 * Date  : 2016/12/22</pre>
 */
class TimeListener implements TaskExecutionListener, BuildListener {

    private SpendTimeExtension spendTimeConfig
    private Map<String, Double> spendTime = [:]
    private long timeStart

    TimeListener(SpendTimeExtension spendTime) {
        spendTimeConfig = spendTime
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

    @Override
    void buildFinished(BuildResult buildResult) {
        if (spendTimeConfig?.enabled && buildResult.failure == null) {
            println "Task spend time:"

            if (spendTimeConfig.sort != null) {
                if (spendTimeConfig.sort.toUpperCase() == "ASC") {
                    spendTime = spendTime.sort { a, b -> a.value > b.value ? 1 : (a.value < b.value ? -1 : 0) }
                } else if (spendTimeConfig.sort.toUpperCase() == "DESC") {
                    spendTime = spendTime.sort { a, b -> b.value > a.value ? 1 : (b.value < a.value ? -1 : 0) }
                }
            }

            spendTime.each { path, time ->
                printf "${String.format("%11.03fms", time)} $path\n"
            }

            println "\nBUILD END\n"
        }
    }

    @Override
    void beforeExecute(Task task) {
        timeStart = System.nanoTime()
    }

    @Override
    void afterExecute(Task task, TaskState taskState) {
        double ms = (System.nanoTime() - timeStart) / 1000_000F
        spendTime.put(task.path, ms)
        task.project.logger.warn "${task.path} -> [spendTime: ${String.format("%.03f", ms)}ms]"
    }
}
