package com.darkmagic.android.apkbuild.utils

import com.darkmagic.android.apkbuild.apk.config.ApkPublishExtension
import org.gradle.api.GradleException
import org.gradle.api.Project

class Utils {

    static boolean mLogEnable = false
    static Project mProject = null

    def static init(Project project, boolean logEnable) {
        mProject = project
        mLogEnable = logEnable
    }

    def static log = {
        if (mLogEnable) {
            if (mProject == null) {
                println "${ApkPublishExtension.TAG}:${it}"
            } else {
                mProject.logger.debug ":${ApkPublishExtension.TAG}:${it}"
            }
        }
    }

    def static i = {
        if (mLogEnable) {
            if (mProject == null) {
                println it
            } else {
                mProject.logger.info it
            }
        }
    }

    def static error = {
        if (mProject == null) {
            println "error:${it}"
        } else {
            mProject.logger.error ":error:${it}"
        }
    }

    def static warn = {
        if (mProject == null) {
            println "warn:${it}"
        } else {
            mProject.logger.warn ":warn:${it}"
        }
    }

    static boolean isEmpty(String s) {
       return s == null || s.length() == 0
    }

    static GradleException createException(Throwable e) {
        GradleException ge = new GradleException()
        ge.initCause(e)
        if (isEmpty(ge.getMessage())) {
            ge = new GradleException(e.getMessage())
        }
        return ge
    }
}