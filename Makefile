release:
	gradlew assembleRelease

check_sign:
	jarsigner -verify -verbose -certs app-release.apk

show_sha1:
	keytool -list -v -keystore "C:/Users/swasher/.keystore/release.jks" -alias myprojects-release