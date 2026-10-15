This is a Kotlin Multiplatform project targeting Android, iOS. The UI is native on each
platform (Jetpack Compose on Android, SwiftUI on iOS) and the logic is shared up to the
ViewModel; architecture and decisions live in [ARCHITECTURE.md](./ARCHITECTURE.md).

* [/iosApp](./iosApp/iosApp) contains the iOS application (SwiftUI). It consumes the
  `SharedLogic` framework, built by Xcode via the Gradle `embedAndSignAppleFrameworkForXcode` task.

* [/sharedLogic](./sharedLogic/src) is for the code that is shared between app targets.
  The most important subfolder is [commonMain](./sharedLogic/src/commonMain/kotlin).
  Platform folders (`androidMain`/`iosMain`) hold only what must touch each platform
  (Keystore/Keychain, OkHttp/Darwin engines, RSA).

* [/androidApp](./androidApp/src) contains the Android application (Jetpack Compose);
  it depends directly on `sharedLogic`.

* [/docs/reverse-engineering](./docs/reverse-engineering/README.md) is the static reverse-engineering documentation of the
  original app (endpoints, JSON schemas, security, methodology). It is versioned with the code that depends on it:
  update it in the same change. `python3 tools/checks/check_docs.py` checks its links and hygiene. The original APK and the
  decompiler output are never committed.

  The docs also include a live-verification pass (GET-only, 2026-10-15, owner's own account):
  see [11-capturas-live.md](./docs/reverse-engineering/11-capturas-live.md) and the raw JSON captures under
  `sharedLogic/src/commonTest/resources/capturas-live/`. Those captures contain **real personal data
  unmasked** — this repository must stay private, and the folder must not be moved into `commonMain`.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :sharedLogic:testAndroidHostTest :androidApp:test`
- iOS tests: `./gradlew :sharedLogic:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
