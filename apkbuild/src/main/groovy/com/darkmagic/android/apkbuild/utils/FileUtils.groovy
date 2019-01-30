package com.darkmagic.android.apkbuild.utils
/**
 * <pre>
 * Note:
 *
 * Author: Sens
 * Date  : 2016/3/1</pre>
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class FileUtils {

    static boolean writeFile(String outFile, String content) {
        return writeFile(new File(outFile), content, false)
    }

    static boolean writeFile(String outFile, String content, boolean append) {
        return writeFile(new File(outFile), content, append)
    }

    static boolean writeFile(File outFile, String content) {
        return writeFile(outFile, content, false)
    }

    static boolean writeFile(File outFile, String content, boolean append) {
        if (!append) {
            outFile.delete()
        }

        File parentFile = outFile.parentFile
        if (parentFile != null && !parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                return false
            }
        }

        OutputStreamWriter out = null
        try {
            out = new OutputStreamWriter(new FileOutputStream(outFile, append), "utf-8")
            out.append(content)
            out.flush()
            out.close()
            return true
        } catch (Exception e) {
            e.printStackTrace()
            println e.getMessage()
            throw Utils.createException(e)
        } finally {
            if (out != null) {
                try {
                    out.close()
                } catch (Exception ignored) {
                }
            }
        }
    }

    static boolean writeFile(File outFile, byte[] content, boolean append) {
        if (!append) {
            outFile.delete()
        }

        File parentFile = outFile.parentFile
        if (parentFile != null && !parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                return false
            }
        }

        try {
            OutputStream out = new FileOutputStream(outFile, append)
            if (writeFile(out, content, content.length)) {
                try {
                    out.flush()
                    out.getFD().sync()
                    out.close()
                } catch (Exception e) {
                    e.printStackTrace()
                    println e.getMessage()
                }
                return true
            }
        } catch (Exception e) {
            e.printStackTrace()
            println e.getMessage()
            throw Utils.createException(e)
        }
        return false
    }

    static boolean writeFile(OutputStream out, byte[] content, int length) {
        try {
            out.write(content, 0, length)
            return true
        } catch (Exception e) {
            e.printStackTrace()
            println e.getMessage()
            throw Utils.createException(e)
        }
    }

    static boolean writeFile(InputStream input, File file) {
        return writeFile(input, file, false)
    }

    static boolean writeFile(InputStream input, File file, boolean append) {
        if (!append) {
            file.delete()
        }

        File parentFile = file.getParentFile()
        if (parentFile != null && !parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                return false
            }
        }

        try {
            FileOutputStream out = new FileOutputStream(file, append)
            try {
                byte[] buffer = new byte[1024 * 4]
                int len
                while ((len = input.read(buffer)) >= 0) {
                    out.write(buffer, 0, len)
                }
            } finally {
                out.flush()
                try {
                    out.getFD().sync()
                } catch (IOException e) {
                    e.printStackTrace()
                    println e.getMessage()
                }
                out.close()
            }
            return true
        } catch (Exception e) {
            e.printStackTrace()
            println e.getMessage()
            throw Utils.createException(e)
        }
    }

    static int copyFolder(File srcFolder, File dstFolder, List<String> includeNames) {
        if (!dstFolder.exists()) {
            dstFolder.mkdirs()
        }

        if (srcFolder.isFile()) {
            if (includeNames != null && !includeNames.contains(srcFolder.getName())) {
                return 0
            }

            if (copyFile(srcFolder, new File(dstFolder, srcFolder.getName()))) {
                return 1
            } else {
                return 0
            }
        }

        File[] files = srcFolder.listFiles()
        if (files == null || files.length == 0) {
            return 0
        }

        int count = 0
        File destFile
        for (file in files) {
            destFile = new File(dstFolder, file.getName())
            if (file.isDirectory()) {
                count += copyFolder(file, destFile, includeNames)
            } else {
                if (includeNames == null || includeNames.contains(file.getName())) {
                    if (copyFile(file, destFile)) {
                        count++
                    }
                }
            }
        }

        return count
    }

    static boolean copyFile(String srcFile, String destFile) {
        return copyFile(new File(srcFile), new File(destFile))
    }

    static boolean copyFile(File srcFile, File destFile) {
        InputStream input = null
        try {
            input = new FileInputStream(srcFile)
            return copyFile(input, destFile)
        } catch (Exception e) {
            e.printStackTrace()
            println e.getMessage()
            throw Utils.createException(e)
        } finally {
            close(input)
        }
    }

    /**
     * Copy data from a source stream to destFile.
     * Return true if succeed, return false if failed.
     */
    static boolean copyFile(InputStream inputStream, File destFile) {
        return writeFile(inputStream, destFile, false)
    }

    static byte[] readFile(String file) {
        return readFile(new File(file))
    }

    static byte[] readFile(File file) {
        if (!file.exists()) {
            return null
        }

        FileInputStream input = null
        try {
            input = new FileInputStream(file)
            return readFile(input)
        } catch (Exception e) {
            e.printStackTrace()
            println e.getMessage()
            throw Utils.createException(e)
        } finally {
            close(input)
        }
    }

    static byte[] readFile(InputStream input) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream()
            byte[] buffer = new byte[1024 * 4]
            int len
            while ((len = input.read(buffer)) != -1) {
                out.write(buffer, 0, len)
            }
            return out.toByteArray()
        } catch (Exception e) {
            e.printStackTrace()
            println e.getMessage()
            throw Utils.createException(e)
        }
    }

    /**
     * 删除目录或文件
     *
     * @param file 要删除的文件或目录
     * @param onlyDeleteIfEmpty 如果是目录，是否只有当是空目录才删除
     * @return true：全部成功删除，false：没有删除成功或者只有部分删除成功
     */
    static boolean delete(File file, boolean onlyDeleteIfEmpty) {
        File[] files = file.listFiles()
        if (files == null || files.length == 0) {// empty directory or is not directory
            return file.delete()
        }

        if (onlyDeleteIfEmpty) {
            return true
        }

        // delete child file
        boolean isDelete = true
        for (File f : files) {
            if (f.isDirectory()) {
                if (!delete(f, false)) {
                    isDelete = false
                }
            } else {
                if (!f.delete()) {
                    isDelete = false
                }
            }
        }

        // delete self
        if (!file.delete()) {
            isDelete = false
        }

        return isDelete
    }

    /**
     * 关闭IO流
     */
    static void close(Closeable io) {
        if (io != null) {
            try {
                io.close()
            } catch (IOException e) {
                e.printStackTrace()
            }
        }
    }

}
