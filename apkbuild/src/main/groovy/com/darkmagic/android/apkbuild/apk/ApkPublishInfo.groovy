package com.darkmagic.android.apkbuild.apk

/**
 * <pre>
 * Note:
 *
 * Author: Sens
 * Date  : 2016/12/26</pre>
 */
class ApkPublishInfo {

    boolean splitsAbiEnabled = false
    boolean splitsDensityEnabled = false

    String pkgName = null
    String versionName = null
    int versionCode
    StoreFile storeFile = null

    boolean minifyEnabled = false

    List<ApkInfo> apkList = []

    String sourceMappingFile = null

    boolean manifestFileIsReplaced = false

}
