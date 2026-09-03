import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "com.a10miaomiao.bilimiao"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.a10miaomiao.bilimiao"
        minSdk = 24
        targetSdk = 35
        versionCode = 118
        versionName = "2.5.0 alpha"

        flavorDimensions += listOf("default")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
            abiFilters.add("armeabi")
            abiFilters.add("x86")
            abiFilters.add("x86_64")
        }
    }

    val signingFile = file("signing.properties")
    if (signingFile.exists()) {
        val props = Properties()
        props.load(FileInputStream(signingFile))
        signingConfigs {
            create("miao") {
                keyAlias = props.getProperty("KEY_ALIAS")
                keyPassword = props.getProperty("KEY_PASSWORD")
                storeFile = file(props.getProperty("KEYSTORE_FILE"))
                storePassword = props.getProperty("KEYSTORE_PASSWORD")
            }
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "bilimiao dev")
            manifestPlaceholders["channel"] = "Development"
        }
        release {
            // 发布构建：启用 R8 压缩/混淆/优化
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.asMap["miao"]?.let {
                signingConfig = it
            }
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }

    productFlavors {
        create("full") {
            dimension = "default"
            val channelName = project.properties["channel"] ?: "Unknown"
            manifestPlaceholders["channel"] = channelName
        }
        create("foss") {
            dimension = "default"
            manifestPlaceholders["channel"] = "FOSS"
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    // 16 KB 页大小兼容：共享库使用未压缩存储（AGP 8.5.1+ 官方建议）
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

// 16 KB 页大小对齐：部分依赖库（datastore/gif/gav1/graphics-path）的
// ELF 段未按 16 KB 对齐（LOAD/RELRO 的 vaddr 与 p_align），Android 15+
// 会报对齐检查失败。在原生库合并后统一把 .so 的段重排到 16 KB 对齐
//（同步更新 section 表、动态段指针与重定位表，保证可正常加载）。
tasks.whenTaskAdded {
    if (name.startsWith("merge") && name.endsWith("NativeLibs")) {
        // 每次构建都重新合并并对齐（避免 UP-TO-DATE 跳过对齐处理）
        outputs.upToDateWhen { false }
        doLast {
            outputs.files.forEach { out ->
                val libRoot = out.resolve("lib")
                if (libRoot.exists()) {
                    libRoot.walkTopDown()
                        .filter { it.isFile && it.extension == "so" }
                        .forEach { so ->
                            ProcessBuilder(
                                "python3",
                                rootProject.file("scripts/realign_elf.py").absolutePath,
                                so.absolutePath,
                            )
                                .inheritIO()
                                .start()
                                .waitFor()
                        }
                }
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.profileinstaller)

    // Compose dependencies for Player wrapper
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.material)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kodein.di)

    implementation(libs.materialkolor)
    implementation(libs.hiddenapibypass)

    implementation(libs.mojito)
    implementation(libs.mojito.sketch)
    implementation(libs.mojito.glide)

    // 播放器相关
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.decoder)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    // mediamp (统一播放器抽象层，安卓端用 ExoPlayer 后端)
    implementation(libs.mediamp.api)
    implementation(libs.mediamp.exoplayer)

    implementation(libs.okhttp3)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.pbandk.runtime)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.microg.safeparcel)

    implementation(project(":bilimiao-comm"))
    implementation(project(":bilimiao-download"))
    implementation(project(":bilimiao-cover"))
    implementation(project(":bilimiao-compose"))
    // 弹幕引擎已通过 bilimiao-compose/bilimiao-comm 传递依赖 (KMP danmaku-engine)
    // DanmakuFlameMaster 已移除，统一使用 KMP danmaku-engine

    // 闭源库：百度统计、极验验证
    "fullImplementation"(libs.baidu.mobstat.sdk)
    "fullImplementation"(libs.geetest.sensebot)
    // av1解码器：https://github.com/androidx/media/tree/release/libraries/decoder_av1
    "fullImplementation"(files("libs/lib-decoder-av1-release.aar"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
