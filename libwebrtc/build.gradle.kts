configurations.maybeCreate("default")
artifacts.add("default", file("libwebrtc_${Packages.libwebrtc}.aar"))
// artifacts.add("default", file("libwebrtc_${libs.versions.libwebrtc}.aar"))	// バージョンカタログでの指定だとsyncはできるけどビルドが通らなくなる
