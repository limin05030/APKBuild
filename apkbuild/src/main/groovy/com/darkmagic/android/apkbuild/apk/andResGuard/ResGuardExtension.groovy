package com.darkmagic.android.apkbuild.apk.andResGuard

class ResGuardExtension {

    boolean enabled = false
    File mappingFile
    boolean use7zip
    String sevenZipPath
    boolean useSign
    String metaName
    boolean keepRoot
    List<String> whiteList
    List<String> compressFilePattern
    boolean outputResMappingFile

    ResGuardExtension() {
        use7zip = true
        sevenZipPath = ""
        useSign = false
        metaName = "META-INF"
        keepRoot = false
        whiteList = []
        compressFilePattern = []
        mappingFile = null
        outputResMappingFile = true
    }

    @Override
    String toString() {
        """| sevenZipPath = ${sevenZipPath}
           | useSign = ${useSign}
           | metaName = ${metaName}
           | keepRoot = ${keepRoot}
           | whiteList = ${whiteList}
           | compressFilePattern = ${compressFilePattern}
        """.stripMargin()
    }
}