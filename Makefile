.SILENT:

release:
	gradlew assembleRelease

debug:
	gradlew assembleDebug

uninstall:
	adb -s dd433f4e uninstall com.swasher.productus || true
	adb -s dd433f4e shell am force-stop com.swasher.productus
	adb -s dd433f4e shell pm uninstall -k --user 0 com.swasher.productus || true
	adb -s dd433f4e shell pm uninstall -k com.swasher.productus || true
	adb -s dd433f4e shell pm list packages | grep com.swasher.productus && \
		adb -s dd433f4e shell pm remove-user 0 || true

install_debug: uninstall
	gradlew assembleDebug
	adb -s dd433f4e shell pm list packages | grep "com.swasher.productus" && adb uninstall com.swasher.productus || echo "Package com.swasher.productus not found"
	adb -s dd433f4e install app/build/outputs/apk/debug/app-debug.apk

install_release: uninstall
	gradlew assembleRelease
	adb -s dd433f4e install app/build/outputs/apk/release/app-release.apk


check_sign_release:
	jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

check_sign_debug:
	jarsigner -verify -verbose -certs app/build/outputs/apk/debug/app-debug.apk

show_keystore:
	keytool -list -v -keystore "%USERPROFILE%/.keystore/release.jks" -alias myprojects-release

show_signing_report:
	gradlew signingReport

generate_keystore:
	keytool -genkeypair -v \
	  -keystore release.keystore \
	  -keyalg RSA -keysize 2048 -validity 10000 \
	  -alias my-release-key
