.SILENT:

release:
	gradlew assembleRelease

debug:
	gradlew assembleDebug
	adb shell pm list packages | grep "com.swasher.productus" && adb uninstall com.swasher.productus || echo "Package com.swasher.productus not found"
	adb install app/build/outputs/apk/debug/app-debug.apk

check_sign_release:
	jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

check_sign_debug:
	jarsigner -verify -verbose -certs app/build/outputs/apk/debug/app-debug.apk

show_sha1:
	keytool -list -v -keystore "C:/Users/swasher/.keystore/release.jks" -alias myprojects-release

reinstall:
	#adb uninstall com.swasher.productus
	adb shell pm list packages | grep "com.swasher.productus" && adb uninstall com.swasher.productus || echo "Package com.swasher.productus not found"
	adb install app/build/outputs/apk/release/app-release.apk

sha1:
	gradlew signingReport

