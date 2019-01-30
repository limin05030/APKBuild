package com.tencent.mm.resourceproguard;

import com.tencent.mm.androlib.AndrolibException;
import com.tencent.mm.androlib.ApkDecoder;
import com.tencent.mm.androlib.ResourceApkBuilder;
import com.tencent.mm.androlib.res.decoder.ARSCDecoder;
import com.tencent.mm.directory.DirectoryException;
import com.tencent.mm.util.FileOperation;

import java.io.File;
import java.io.IOException;
import java.util.Locale;


/**
 * @author shwenzhang
 * @author simsun
 */
public class Main {
    public static final int ERRNO_ERRORS = 1;
    public static final int ERRNO_USAGE  = 2;
    protected static long          mRawApkSize;
    protected static String        mRunningLocation;
    protected static long          mBeginTime;
    /**
     * 是否通过命令行方式设置
     */
    public boolean mSetSignThroughCmd    = false;
    public boolean mSetMappingThroughCmd = false;
    public String  m7zipPath             = null;
    public String  mZipalignPath         = null;
    protected        Configuration config;
    protected        File          mOutDir;

    public static class Result {
        public File outApkFile;
        public File outMappingFile;
    }

    public static boolean signApk(File apk, File storeFile, String keyPassword, String keyAlias, String storePassword, boolean log) {
        return ResourceApkBuilder.signApk(apk, storeFile, keyPassword, keyAlias, storePassword, log);
    }

    public static Result gradleRun(InputParam inputParam) {
        Main m = new Main();
        return m.run(inputParam);
    }

    private Result run(InputParam inputParam) {
        loadConfigFromGradle(inputParam);
        Result result = resourceProguard(new File(inputParam.outFolder), inputParam.apkPath);
        clean();
        return result;
    }

    protected void clean() {
        config = null;
        ARSCDecoder.mTableStringsProguard.clear();
    }

    private void loadConfigFromGradle(InputParam inputParam) {
        try {
            config = new Configuration(inputParam);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected Result resourceProguard(File outputFile, String apkFilePath) {
        ApkDecoder decoder = new ApkDecoder(config);
        File apkFile = new File(apkFilePath);
        if (config.mLogEnabled) {
            System.out.println("resourceProguard: " + apkFile.exists());
        }
        if (!apkFile.exists()) {
            System.err.println(String.format(Locale.getDefault(), "the input apk %s does not exit", apkFile.getAbsolutePath()));
            goToError();
        }
        mRawApkSize = FileOperation.getFileSizes(apkFile);
        try {
            decodeResource(outputFile, decoder, apkFile);
            return buildApk(decoder, apkFile);
        } catch (AndrolibException | IOException | DirectoryException | InterruptedException e) {
            e.printStackTrace();
            goToError();
            return null;
        }
    }

    private void decodeResource(File outputFile, ApkDecoder decoder, File apkFile) throws AndrolibException, IOException, DirectoryException {
        decoder.setApkFile(apkFile);
        if (outputFile == null) {
            mOutDir = new File(mRunningLocation, apkFile.getName().substring(0, apkFile.getName().indexOf(".apk")));
        } else {
            mOutDir = outputFile;
        }
        decoder.setOutDir(mOutDir.getAbsoluteFile());
        decoder.decode();
    }

    private Result buildApk(ApkDecoder decoder, File apkFile) throws AndrolibException, IOException, InterruptedException {
        ResourceApkBuilder builder = new ResourceApkBuilder(config);
        String apkBasename = apkFile.getName();
        apkBasename = apkBasename.substring(0, apkBasename.indexOf(".apk"));
        builder.setOutDir(mOutDir, apkBasename);
        builder.buildApk(decoder.getCompressData());

        Result result = new Result();
        if (builder.mAlignedWith7ZipApk.exists()) {
            result.outApkFile = builder.mAlignedWith7ZipApk;
        } else if (builder.mSignedWith7ZipApk.exists()) {
            result.outApkFile = builder.mSignedWith7ZipApk;
        } else if (builder.mAlignedApk.exists()) {
            result.outApkFile = builder.mAlignedApk;
        } else if (builder.mSignedApk.exists()) {
            result.outApkFile = builder.mSignedApk;
        } else if (builder.mUnSignedApk.exists()) {
            result.outApkFile = builder.mUnSignedApk;
        } else {
            return null;
        }
        result.outMappingFile = decoder.getResMappingFile();
        return result;
    }

    public double diffApkSizeFromRaw(long size) {
        return (mRawApkSize - size) / 1024.0;
    }

    protected void goToError() {
        System.exit(ERRNO_USAGE);
    }
}