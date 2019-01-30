package com.darkmagic.android.apkbuild.apk.config

class ReplaceInfo {

    String path
    String modifier
    String type
    String name
    String newValue

    ReplaceInfo(String path, String modifier, String type, String name, String newValue) {
        this.path = path
        this.modifier = modifier
        this.type = type
        this.name = name
        this.newValue = newValue
    }

    @Override
    String toString() {
        return "ReplaceInfo[path=$path, modifier=$modifier, type=$type, name=$name, newValue=$newValue]"
    }
}