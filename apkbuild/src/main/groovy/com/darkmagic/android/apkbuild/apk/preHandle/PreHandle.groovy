package com.darkmagic.android.apkbuild.apk.preHandle

import com.darkmagic.android.apkbuild.apk.config.ApkPublishExtension
import com.darkmagic.android.apkbuild.apk.config.ReplaceExtension
import com.darkmagic.android.apkbuild.utils.FileUtils
import com.darkmagic.android.apkbuild.utils.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * 在进行编译之前，做一些预处理，比如修改替换某些属性
 */
abstract class PreHandle extends DefaultTask {

    // debug|release
    String buildType
    ApkPublishExtension publishConfig
    ReplaceExtension replaceConfig

    PreHandle() {
        buildType = getBuildType().toLowerCase()
    }

    abstract String getBuildType()

    @SuppressWarnings("GroovyUnusedDeclaration")
    @TaskAction
    void preHandle() {
        publishConfig = project.publishConfig
        replaceConfig = publishConfig.replaceConfig

        Utils.init(project, publishConfig.logEnable)
        Utils.log "PreHandle:${buildType.capitalize()}"

        String charsetName = replaceConfig.charsetName
        if (charsetName.trim().isEmpty()) {
            charsetName = "UTF-8"
        }

        try {
            if ("debug" == buildType) {
                replaceConfig.debugReplaceFieldValue.each { item ->
                    Utils.log "Replace[${item}]"
                    replace(item.path, item.modifier, item.type, item.name, item.newValue, charsetName)
                }
                replaceConfig.debugReplaceLines.each { item ->
                    Utils.log "Replace[${item}]"
                    replace(item.path, item.name, item.newValue, charsetName)
                }
            } else if ("release" == buildType) {
                replaceConfig.releaseReplaceFieldValue.each { item ->
                    Utils.log "Replace[${item}]"
                    replace(item.path, item.modifier, item.type, item.name, item.newValue, charsetName)
                }
                replaceConfig.releaseReplaceLines.each { item ->
                    Utils.log "Replace[${item}]"
                    replace(item.path, item.name, item.newValue, charsetName)
                }
            }
            Utils.log "Replace[Completed]"
        } catch (Throwable e) {
            throw Utils.createException(e)
        }
    }

    // 替换指定的成员变量的初始值
    void replace(String path, String modifier, String type, String name, String newValue, String charsetName) {
        File file = getReplaceFileRealPath(path)
        if (file == null) {
            return
        }

        List lines = readFile(file, charsetName)
        String line
        for (int i = 0; i < lines.size(); i++) {
            line = lines[i]
            if (line.trim().startsWith("//") || line.trim().startsWith("/*") || line.trim().startsWith("*")) {
                continue
            }

            int index1 = line.indexOf("=")
            int index2 = line.indexOf("//")
            int index3 = line.indexOf("/*")
            if (index1 == -1 || (index2 > 0 && index2 < index1) || (index3 > 0 && index3 < index1)) {
                continue
            }

            if (path.endsWith(".java")) {
                if (line.contains("$type ") && line.contains(" $name")) {
                    if (!Utils.isEmpty(modifier) && !line.contains("$modifier ")) {
                        // 如果配置了modifier，则必须同时包含modifier才行
                        continue
                    }

                    String[] split = line.split("=")
                    if ("string".equalsIgnoreCase(type)) {
                        if (newValue.startsWith("\"") && newValue.endsWith("\"")) {
                            lines[i] = "${split[0]}= $newValue;"
                        } else {
                            lines[i] = "${split[0]}= \"$newValue\";"
                        }
                    } else {
                        lines[i] = "${split[0]}= $newValue;"
                    }
                }
            } else if (path.endsWith(".kt")) {
                if ("val" != modifier && "var" != modifier) {
                    // modifier必须是val或者var
                    continue
                }

                if (line.contains("$modifier ") && line.contains(" $name")) {
                    if (!Utils.isEmpty(type) && !line.contains(" $modifier ")) {
                        index1 = line.indexOf(name)
                        index2 = line.indexOf(":")
                        index3 = line.indexOf(type)
                        if (index1 > index2 || index2 > index3) {
                            // 如果配置了type，格式必须为"name : type"，冒号前后的空格可有可无
                            continue
                        }
                    }

                    String[] split = line.split("=")
                    if (newValue.startsWith("\"") && newValue.endsWith("\"")) {
                        lines[i] = "${split[0]}= $newValue"
                    } else {
                        if (Utils.isEmpty(type)) {
                            if (isString(newValue)) {
                                lines[i] = "${split[0]}= \"$newValue\""
                            } else {
                                lines[i] = "${split[0]}= $newValue"
                            }
                        } else {
                            if ("string".equalsIgnoreCase(type)) {
                                lines[i] = "${split[0]}= \"$newValue\""
                            } else {
                                lines[i] = "${split[0]}= $newValue"
                            }
                        }
                    }
                }
            }

        }

        writeFile(file, lines, charsetName)
    }

    private static boolean isString(Object newValue) {
        if (newValue == "true" || newValue == "false") {
            return false
        }

        try {
            Integer.parseInt(newValue)
            return false
        } catch (NumberFormatException ignored) {
        }

        try {
            Long.parseLong(newValue)
            return false
        } catch (NumberFormatException ignored) {
        }

        try {
            Float.parseFloat(newValue)
            return false
        } catch (NumberFormatException ignored) {
        }

        try {
            Double.parseDouble(newValue)
            return false
        } catch (NumberFormatException ignored) {
        }

        return true
    }

    // 替换指定的任意字符串
    void replace(String path, String oldValue, String newValue, String charsetName) {
        Utils.log "Replace path: ${path}"
        File file = getReplaceFileRealPath(path)
        if (file == null) {
            return
        }

        List lines = readFile(file, charsetName)
        for (int i = 0; i < lines.size(); i++) {
            if (lines[i].contains(oldValue)) {
                lines[i] = lines[i].replaceAll(oldValue, newValue)
            }
        }

        writeFile(file, lines, charsetName)
    }

    File getReplaceFileRealPath(String path) {
        File file
        if (path.startsWith("/")) {
            file = new File(path)
        } else if (path.length() >= 2 && path.charAt(1) == (char) ':') {
            file = new File(path)
        } else {
            file = new File(new File("").absolutePath, path)
        }

        if (file.exists()) {
            Utils.log "Replace:Path:${file.absolutePath}"
            backupReplaceFile(file)
            return file
        } else {
            Utils.warn "Replace:File not extst, skip!: ${file.absolutePath}"
            return null
        }
    }

    void backupReplaceFile(File file) {
        File backupFile = publishConfig.backupFileMap.get(file.absolutePath)
        if (backupFile == null || !backupFile.exists()) {
            File replaceDir = new File(project.buildDir, "replace")
            if (!replaceDir.isDirectory()) {
                replaceDir.mkdirs()
            }

            backupFile = new File(replaceDir, file.name)
            int i = 0
            while (backupFile.exists()) {
                backupFile = new File(replaceDir, file.name + ".${i++}")
            }

            FileUtils.copyFile(file, backupFile)
            publishConfig.backupFileMap.put(file.absolutePath, backupFile)
        }
    }

    static List<String> readFile(File file, String charsetName) {
        List<String> lines = []
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charsetName))
        String line
        while ((line = reader.readLine()) != null) {
            lines.add(line)
        }
        reader.close()
        return lines
    }

    static void writeFile(File file, List<String> lines, String charsetName) {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), charsetName))
        lines.each {
            writer.write(it)
            writer.newLine()
        }

        writer.flush()
        writer.close()
    }

}