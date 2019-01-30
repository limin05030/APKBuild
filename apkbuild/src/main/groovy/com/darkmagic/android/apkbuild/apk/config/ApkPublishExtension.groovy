package com.darkmagic.android.apkbuild.apk.config
/**
 * <pre>
 * Note:
 *
 * Author: Sens
 * Date  : 2016/12/26</pre>
 */
class ApkPublishExtension {

    public static String TAG = "ApkPublishTask"

    ApkPublishExtension() {}

    // 开启日志
    boolean logEnable = false

    // APK前缀，默认是"app"
    String apkPrefix = "app"

    // 渠道名称
    String channel = ""

    // apk包输出目录
    String outDir = null

    // 带符号列表的so路径
    String symbolDebugSOPath = null
    String symbolReleaseSOPath = null

    // 要复制的so名称，这些指定的so之外的so不会被复制，如果没有发现指定的so则会报错
    List<String> includeSONameList = []

    List<String> outputs = []

    // 是否使用v2进行签名
    boolean v2SigningEnabled = false

    // CrashReporter工具支持，将生成的mapping文件自动上传到crashReporterProGuard/<pkgName>/<version>[_channel].txt
    String crashReporterProGuard = null

    Map<String, File> backupFileMap = [:]

    void includeSONames(String... names) {
        includeSONameList.addAll(Arrays.asList(names))
    }
}
