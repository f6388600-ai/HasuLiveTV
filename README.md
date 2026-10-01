# Hasu Live TV

Premium Android Live TV ecosystem built with Kotlin + Jetpack Compose + Media3.

## Editions

One Android Studio project produces three flavors:

- `mobile` — touch-first phone/tablet UI
- `tv` — Android TV / D-pad UI
- `admin` — channel management console

## Highlights

- Modern dark streaming UI
- Animated splash
- TV-first focus-friendly layouts
- Mobile bottom navigation
- Search and category discovery
- Featured/live/recent sections
- Favorites
- Media3 HLS/DASH-ready player
- Playback controls, buffering and native player error handling
- Admin dashboard with add/edit/delete channel flows
- Demo/local data works without Firebase
- Firebase-ready repository boundary
- No website dependency
- No hard-coded Firebase credentials
- GitHub Actions APK build
- Compact source structure: four Kotlin files

## Compact source layout

```text
HasuLiveTV/
├── app/
│   ├── src/main/java/com/hasu/livetv/
│   │   ├── MainActivity.kt
│   │   ├── Data.kt
│   │   ├── Core.kt
│   │   └── HasuLiveTvApplication.kt
│   ├── src/main/res/
│   ├── src/tv/AndroidManifest.xml
│   └── build.gradle.kts
├── .github/workflows/build-apk.yml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

## Open in Android Studio

1. Extract the ZIP.
2. Open the `HasuLiveTV` folder in Android Studio.
3. Allow Gradle sync to complete.
4. Select a build variant such as `mobileDebug`, `tvDebug`, or `adminDebug`.
5. Run on a phone, emulator, Android TV device, or TV emulator.

## Local builds

```bash
./gradlew assembleMobileDebug
./gradlew assembleTvDebug
./gradlew assembleAdminDebug
```

APK output:

```text
app/build/outputs/apk/debug/
```

## GitHub Actions

Workflow:

```text
.github/workflows/build-apk.yml
```

It installs JDK 17 and Android SDK components, then builds all three debug flavors and uploads them as a workflow artifact named `hasu-live-tv-apks`.

## Firebase later

Firebase is deliberately not required for the current demo build.

Implement the existing `LiveTvRepository` in:

```text
app/src/main/java/com/hasu/livetv/Data.kt
```

Then switch the repository created in:

```text
app/src/main/java/com/hasu/livetv/HasuLiveTvApplication.kt
```

Recommended Firebase services:

- Firebase Authentication for admin/user identity
- Cloud Firestore for channels, categories, banners, settings and users
- Firebase Storage for logos and banners
- Remote Config for optional app-wide configuration

Production authorization must be enforced by Firebase Security Rules, not by client-side UI checks.

## Logo

The local logo resource is:

```text
app/src/main/res/drawable/ic_hasu_logo.xml
```

Replace it with the developer's own vector or image resource. The launcher manifest points to this local resource, so no external URL is required.

## Demo stream

The demo catalog uses a public HLS test stream strictly for development/testing. Replace demo stream URLs with licensed/authorized streams before production release.

## Security

No Firebase service-account key, API secret, admin password, or private token is stored in the repository.

The Admin APK currently has a clearly-labelled demo login gate. Replace that gate with Firebase Authentication before production deployment.
