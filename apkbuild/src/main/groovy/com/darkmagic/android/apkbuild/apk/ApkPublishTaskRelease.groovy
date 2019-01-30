package com.darkmagic.android.apkbuild.apk
/**
 * <pre>
 * Note:
 *
 * Author: Sens
 * Date  : 2016/12/22</pre>
 */
class ApkPublishTaskRelease extends ApkPublishTaskImpl {

    @Override
    String getPublishType() {
        return "release"
    }
}