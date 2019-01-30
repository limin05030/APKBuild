# APKBuild插件说明

## 插件主要功能
该插件主要是用来进行APK包瘦身，减小APK包体大小。在打包的时候移除无关的资源文件，主要优化点有两个：
1. 利用在`build.gradle`文件中配置`splits`属性对APK包进行拆分。正常情况下我们打出来的包包含drawable-hdpi、drawable-xhdpi、drawable-xxhdpi等目录，google很多库（如v4）会在这些目录里面带有大量资源文件，实际上我们根本用不到。使用该属性会将APK包按照drawable目录拆分成多个，我们只需要使用其中一个即可。为了防止将我们自己的资源文件给分离出去，可以将我们的资源文件统一放到一个dpi目录里面，我们使用这个dpi目录打的包或则将我们的资源文件全部放在mipmap目录里面（mipmap目录不会被分割）；
2. 对资源路径进行了混淆，这个主要是用的微信的一个开源库莱实现的，具体见[这里](https://github.com/shwenzhang/AndResGuard/blob/master/doc/how_to_work.zh-cn.md)。

该插件还支持一些其他的小功能，如下：
1. 统计编译过程中每个步骤的耗时情况；
2. 自定义APK的名称和输出路径；
3. 动态修改代码；

## 使用步骤
1. 在项目的build.gradle文件中添加`classpath 'com.darkmagic.android:apkbuild:1.0.1'`，即：
	```
	buildscript {
	    repositories {
	        jcenter()
	    }
	
	    dependencies {
	        classpath 'com.android.tools.build:gradle:2.2.3'
	        classpath 'com.darkmagic.android:apkbuild:1.0.1'
	
	        // NOTE: Do not place your application dependencies here; they belong
	        // in the individual module build.gradle files
	    }
		
	    ......
	}
	```

2. 修改模块（如app）下面的build.gradle文件，如下：
	```
	apply plugin: 'com.android.application'
	apply from: 'apkpublish.gradle'

	android {
        compileSdkVersion 25
	    buildToolsVersion "25.0.1"
	    defaultConfig {
	        applicationId "com.test"
	        minSdkVersion 15
	        targetSdkVersion 25
	        versionCode 100
	        versionName "0.1.0"
	    }
	
	    splits {
	        density {
	            enable true
	            reset()
	            include "hdpi"
	        }
	    }
	
	    ......
	}

	......
	```
	apkpublish.gradle是一个单独针对该插件的配置文件，这样方便管理，不至于将build.gradle写得过大。splits属性主要就是用来拆分包的，这里只保留了hdpi这个包。

3. 在build.gradle的同级目录新建一个apkpublish.gradle文件，配置如下：
	```
	apply plugin: 'com.darkmagic.android.apkbuild'

	// 统计编译时每一步的耗时情况
	spendTimeOptions {
	    enabled false // 是否开启耗时统计
	    sort "ASC"    // 耗时统计排序方式
	}

	publishConfig {
        // 是否开启日志
	    logEnable false
	
	    // 如果开启了splits分包属性，这里指定用哪个Density包作为发布包，如果不指定默认使用第一个Density
	    splitsDensity "hdpi"
	
	    // 默认APK包名结构：[apkPrefix]_[versionName]_[date]_[debug|release]_[apkSuffix].apk
	    // 生成的APK包的前缀
	    apkPrefix "AMC"
	    // 生成的APK包的后缀（比如当是渠道包的时候需要在后面加上渠道号）
	    apkSuffix "google"
	
	    // 自定义APK包名（不使用默认包名结构）
	    apkName ""
	    // 自定义mapping文件名，默认同APK包名
	    mappingName ""
	
	    // APK包发布到的位置
	    outDir "I:/android/out"
	
	    // 文件修改，如果修改app module下面的java代码可以写相对路径，如果要修改其他文件需要写绝对路径
	    replaceConfig {
	        // 打包命令有两个apkPublishDebug和apkPublishRelease，分别打debug模式和release模式的包
	        // debugReplace是打debug包时用的
	        // releaseReplace是打release包时用的
	        // 它们分别有三个参数或和五个参数，三个参数的使用来直接替换指定文件里指定的字符串，五个参数主要是用来替换变量的值
			
	        // 例如：将MainActivity.java里的 private boolean isDebug的值设置为true
	        // 参数1：要修改的文件
	        // 参数2：要修改的变量的权限类型
	        // 参数3：要修改的变量的类型
	        // 参数4：要修改的变量的名字
	        // 参数5：新的值
	        debugReplace "gradleplugn/gradleplugn/MainActivity.java", "private", "boolean", "isDebug", "true"
	
	        // 将MainActivity.java文件中的"LOG_DEBUG: "字符串（包括双引号）替换成"BuildConfig.LOG_DEBUG1:"
	        // 参数1：要修改的文件
	        // 参数2：要替换的字符串
	        // 参数3：新的字符串
	        debugReplace "gradleplugn/gradleplugn/MainActivity.java", "\"LOG_DEBUG: \"", "\BuildConfig.LOG_DEBUG1:\""
	
	        releaseReplace "gradleplugn/gradleplugn/MainActivity.java", "private", "boolean", "isDebug", "false"
	    }
	
	    // 配置资源混淆规则
	    resGuardOption {
	        // 是否开启资源混淆
	        enabled true
	
	        // 是否使用7zip压缩
	        use7zip true
	
	        // 7zip工具路径，可以不用指定，插件会自动下载7zip
	        // sevenZipPath "D:\\tools\\7z\\7z.exe"
	
	        // 是否开启Sign
	        useSign true
	
	        // 打开这个开关，会保留住所有资源的原始路径，只混淆资源的名字
	        keepRoot false
	
	        // 压缩白名单
	        whiteList = [
	                "R.drawable.icon",
	                "R.string.app_name"
	        ]
	
	        // 额外要压缩的文件
	        compressFilePattern = [
	                "*.png",
	                "*.jpg",
	                "*.jpeg",
	                "*.gif",
	                "resources.arsc"
	        ]
	
	        // 是否输出资源压缩的混淆文件
	        outputResMappingFile true
	    }
	}

	```
	其中资源文件的混淆具体规则可以看[这里](https://github.com/shwenzhang/AndResGuard/blob/master/doc/how_to_work.zh-cn.md)。

4. 打包，使用`gradlew apkPublishDebug`或则`gradlew apkPublishRelease`即可，如果想同时打包debug版本和release版本可以在工程的跟目录新建一个批处理文件（apkPublishAll.bat），然后直接执行apkPublishAll即可。批处理文件内容如下：
	```
	@echo off
	setlocal EnableDelayedExpansion
		
	set startTime=%time%
	set logFilePath=%TMP%\apkPublishAllLog.gradle
	del %logFilePath% 2> nul
	call gradlew -PlogFilePath=%logFilePath% apkPublishDebug && call gradlew -PlogFilePath=%logFilePath% apkPublishRelease
	set endTime=%time%
	
	echo.
	FOR /F "delims=" %%i IN (%logFilePath%) DO echo %%i
	
	set /a h=%endTime:~0,2%-%startTime:~0,2%
	set /a m=%endTime:~3,2%-%startTime:~3,2%
	set /a s=%endTime:~6,2%-%startTime:~6,2%
	set /a w=%endTime:~9,2%-%startTime:~9,2%
	set /a tw=%h%*3600000 + %m%*60000 + %s%*1000 + %s% + %w%*10
	set /a rw=%tw%%%1000
	set /a ts=%tw%/1000
	set /a rs=%ts%%%60
	set /a tm=%ts%/60
	set /a rm=%tm%%%60
	set /a rh=%tm%/60
	
	set outTime=%rs%.%rw% secs
	if %rm% gtr 0 (
		set outTime=%rm% mins %outTime%
	)
	if %rh% gtr 0 (
		set outTime=%rh% hours %outTime%
	)
	
	echo.
	echo Build Total Time: %outTime% secs
	
	endLocal
	```
