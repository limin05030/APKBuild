package com.darkmagic.android.apkbuild.apk.config

@SuppressWarnings("GroovyUnusedDeclaration")
class ReplaceExtension {

    List<ReplaceInfo> debugReplaceFieldValue = []
    List<ReplaceInfo> releaseReplaceFieldValue = []

    def debugReplace(path, modifier, type, name, newValue) {
        debugReplaceFieldValue.add(new ReplaceInfo(path, modifier, type, name, newValue))
    }

    def releaseReplace(path, modifier, type, name, newValue) {
        releaseReplaceFieldValue.add(new ReplaceInfo(path, modifier, type, name, newValue))
    }

    List<ReplaceInfo> debugReplaceLines = []
    List<ReplaceInfo> releaseReplaceLines = []

    def debugReplace(path, oldValue, newValue) {
        debugReplaceLines.add(new ReplaceInfo(path, "", "", oldValue, newValue))
    }

    def releaseReplace(path,  oldValue, newValue) {
        releaseReplaceLines.add(new ReplaceInfo(path, "", "", oldValue, newValue))
    }

    String charsetName = "UTF-8"

}