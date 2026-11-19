# Releasing Iskra EV Companion (`com.iskra.companion`)

1. One-time: create a release keystore **outside this folder**, then create `keystore.properties` (git-ignored) next to `settings.gradle.kts`:
   ```
   storeFile=C:/path/to/iskra-release.jks
   storePassword=...
   keyAlias=iskra
   keyPassword=...
   ```
   Back the keystore up. Losing it means you cannot update the app (Play App Signing reduces this risk).
2. Bump `versionCode` / `versionName` in `app/build.gradle.kts`; add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (and `uk`).
3. Build: `gradlew.bat bundleRelease assembleRelease` with `JAVA_HOME=C:\Users\oleha\.jdks\jbr-21.0.11`.
   - AAB: `app/build/outputs/bundle/release/app-release.aab`
   - APK: `app/build/outputs/apk/release/app-release.apk`
4. Commit, tag `vX.Y.Z`, push, then `gh release create vX.Y.Z app-release.apk#IskraEV.apk`.
5. Play Console: upload the AAB to the closed test track(s) with release notes.
