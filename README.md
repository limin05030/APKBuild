# APKBuild插件说明

## 插件主要功能
该插件主要功能：
1. 统计编译过程中每个步骤的耗时情况；
2. 一键式打包，包括debug和release，重命名apk文件，以及将打包好的apk文件、mapping文件复制到指定位置等；
3. 支持在打包前对指定文件进行修改；
4. apk瘦身，支持splits属性，同时内置了AndResGuard插件（一个微信的开源插件，具体见[这里](https://github.com/shwenzhang/AndResGuard/blob/master/doc/how_to_work.zh-cn.md)），利用该插件可以进一步对资源进行混淆压缩；


## 使用步骤
该插件已经编译好并上传到jcenter上了，所以可以直接在项目中集成。
1. 在项目的build.gradle文件中添加`com.darkmagic.android:apkbuild:1.4.5'`，即：
	```
	buildscript {
	    repositories {
	        jcenter()
	    }
	
	    dependencies {
	        classpath 'com.android.tools.build:gradle:2.2.3'
	        classpath 'com.darkmagic.android:apkbuild:1.4.5'
	
	        // NOTE: Do not place your application dependencies here; they belong
	        // in the individual module build.gradle files
	    }
		
	    ......
	}
	```

2. 修改模块（如app）下面的build.gradle文件，如下：
	```
	apply plugin: 'com.android.application'
    // 添加这一行，这里将APKBuild的相关配置放到一个单独的配置文件中，方便管理
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
	
	    // APK包名结构：[apkPrefix]_[versionName]_[date]_[debug|release]_[channel].apk
	    // 生成的APK包的前缀
	    apkPrefix "AMC"
        
        // 渠道号
        channel "GooglePlay"

        // 是否使用V2进行签名，如果不开启则只使用v1，开启后将同时使用v1和v2
        v2SigningEnabled true
	
	    // APK包发布到的位置
        // outDir '//192.168.1.250/Mobile/Product'
	    outDir "I:/android/out"
                
        // 如果是native项目，打包的时候需要备份带符号的so文件，
        // 这里指定debug模式和release模式打包时要复制带符号的的so路径 
        symbolDebugSOPath 'lib_ffmpeg/build/intermediates/cmake/release/obj'
        symbolReleaseSOPath 'lib_ffmpeg/build/intermediates/cmake/release/obj'
        
        // 要复制的so名称集合，该集合之外的so不会复制
        includeSONames 'libplayer.so'
	
	    // 文件修改（在打包之前），如果修改app module下面的java代码可以写相对路径，如果要修改其他文件需要写绝对路径
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
	        debugReplace "app/src/main/java/com/gradleplugin/MainActivity.java", "private", "boolean", "isDebug", "true"
	
	        // 将MainActivity.java文件中的"LOG_DEBUG: "字符串（包括双引号）替换成"BuildConfig.LOG_DEBUG1:"
	        // 参数1：要修改的文件
	        // 参数2：要替换的字符串
	        // 参数3：新的字符串
	        debugReplace "app/src/main/java/com/gradleplugin/MainActivity.java", "\"LOG_DEBUG: \"", "\BuildConfig.LOG_DEBUG1:\""
           
           // 同时也支持kotlin，
           debugReplace "app/src/main/java/com/gradleplugin/Constant.kt", "val", "", "isDebug", "true"

           // 对任意文件进行字符串替换
           debugReplace "lib_ffmpeg/src/main/cpp/android_log.h", "#define RELEASE", "#define DEBUG"
	
	        releaseReplace "gradleplugn/gradleplugn/MainActivity.java", "private", "boolean", "isDebug", "false"
	    }
	
	    // AndResGuard插件相关配置，具体见https://github.com/shwenzhang/AndResGuard/
	    resGuardOption {
	        // 是否开启资源混淆
	        enabled true
	
	        // 是否使用7zip压缩
	        use7zip true
	
            // 7zip工具路径，可以不用指定，插件会自动下载7zip
            // 这里可以配置7z的本地路径，如：'D:\\tools\\7z\\7z.exe'
            // 也可以配置依赖关系，如：'com.tencent.mm:SevenZip:1.2.13'
            // sevenZipPath 'D:\\tools\\7z\\7z.exe'
	
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


4. 打包，使用`gradlew apkPublishDebug`或则`gradlew apkPublishRelease`即可，如果想同时打包debug版本和release版本可以使用apkPublishAll.bat脚本。

**注意**：该插件打的debug包和release包其实都是release包（在buildTypes配置里都是使用的release配置），区别是在replaceConfig处配置有不同配置。