plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.jetbrains.kotlin.android)
}

android {
	namespace = Packages.id
	compileSdk = Packages.Sdk.compile

	compileOptions {
		sourceCompatibility = Packages.Sdk.sourceCompatibility
		targetCompatibility = Packages.Sdk.targetCompatibility
	}

    defaultConfig {
		applicationId = Packages.id
		minSdk = Packages.Sdk.min
		targetSdk = Packages.Sdk.target
		versionCode = Packages.Version.code
		versionName = Packages.Version.name

		vectorDrawables.useSupportLibrary = true
		multiDexEnabled = true
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

	buildFeatures {
		buildConfig = true
	}
}

dependencies {
	// test
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)

	// androidx
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.legacy.v4)
	implementation(libs.androidx.recyclerview)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.androidx.preference.ktx)
	implementation(libs.google.material)

	// 外部のライブラリ
	implementation(libs.serenegiant.common)		// Apache V2
	implementation(libs.retrofit2)

	// プロジェクト内のモジュール
	implementation(project(":libwebrtc"))
	implementation(project(":janus"))
}
