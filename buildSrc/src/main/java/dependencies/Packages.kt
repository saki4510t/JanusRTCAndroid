
import org.gradle.api.JavaVersion

object Packages {
	const val id = "com.serenegiant.janusrtcandroid"
	const val libwebrtc = "2025-08-29T04_03_50"

	object Sdk {
		val sourceCompatibility = JavaVersion.VERSION_17
		val targetCompatibility = JavaVersion.VERSION_17
		const val jvmTarget = "17"
		const val kotlinCompilerExtensionVersion = "1.5.14"

		const val compile = 35
		const val target = 35
		const val min = 21
	}

	object Version {
		private const val major = 2
		private const val minor = 11
		private const val build = 0

		const val code = 67
		const val name = "$major.$minor.$build"
	}
}
