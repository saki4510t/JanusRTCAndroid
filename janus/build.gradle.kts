plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.jetbrains.kotlin.android)
	id("maven-publish")
}

android {
	namespace = "com.serenegiant.janus"
	compileSdk = Packages.Sdk.compile

	compileOptions {
		sourceCompatibility = Packages.Sdk.sourceCompatibility
		targetCompatibility = Packages.Sdk.targetCompatibility
	}

	defaultConfig {
		minSdk = 16
		lint.targetSdk = Packages.Sdk.target
		testOptions.targetSdk = Packages.Sdk.target

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}

	kotlinOptions {
		jvmTarget = Packages.Sdk.jvmTarget
	}
}

dependencies {
	// テスト
	testImplementation(libs.junit)
	testImplementation(libs.coroutinesTest)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(libs.androidx.multidex)
	testImplementation(libs.okhttp3.mockwebserver)

	// AndroidX	Apache V2
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.core.ktx)

	// 外部のライブラリ
	implementation(libs.gson)
	implementation(libs.okhttp3)
	implementation(libs.okhttp3.logging.interceptor)
	implementation(libs.retrofit2)
	implementation(libs.retrofit2.converter.gson)
	implementation(libs.retrofit2.adapter.rxjava3)

	implementation(libs.socketio) {
	  // excluding org.json which is provided by Android
		exclude(group = "org.json", module = "json")
	}

	implementation(libs.rxjava3)
	implementation(libs.rxjava3.rxkotlin)
	implementation(libs.rxjava3.rxandroid)
	implementation(libs.rxjava3.retrofit.adapter)

	implementation(libs.tinder.scarlet)
	implementation(libs.tinder.scarlet.websocket.okhttp)
	implementation(libs.tinder.scarlet.message.adapter.gson)
	implementation(libs.tinder.scarlet.stream.adapter.rxjava2)

	implementation(libs.serenegiant.common)		// Apache V2

	// プロジェクト内のモジュール
	compileOnly(project(":libwebrtc"))
}

val repo = File(rootDir, "repository")

afterEvaluate {
	publishing {
		repositories {
			maven {
				url = uri("file://${repo.absolutePath}")
			}
		}
		publications {
			create<MavenPublication>("release") {
				groupId = "com.serenegiant"
				artifactId = "janus"
				version = Packages.Version.name
				from(components["release"])
				pom {
					inceptionYear = "2018"
					licenses {
						license {
							name = "The WebRTC Software License"
							url = "https://webrtc.org/support/license"
//							distribution = "repo"
						}
					}
					developers {
						developer {
							id = "saki4510t"
							name = "t_saki"
							email = "t_saki@serenegiant.com"
						}
					}
				}
			}
		}
	}
}
