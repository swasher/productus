release:
	gradlew assembleRelease

check_sign:
	jarsigner -verify -verbose -certs app-release.apk