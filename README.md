# Shant Spend

An Android app (Kotlin + Jetpack Compose) for personal spend tracking.

## Branching model

- `main` is the production branch — whatever is on `main` is the live app.
- All feature work happens on separate branches (e.g. `feature/xyz`), merged into `main` via pull request.
- Opening a PR into `main` triggers the **CI** workflow (build + unit tests + lint), which is a required status check.
- Merging to `main` triggers the **Release** workflow, which builds a signed release APK and publishes it as a GitHub Release.

## Local development (Android Studio)

Open the project in Android Studio as usual and run/debug on an emulator or physical device — this is unaffected by CI/CD.

## Local development (Docker)

A `Dockerfile` provides a build/lint/test environment matching CI (JDK 17 + Android SDK platform 36 / build-tools 36.0.0). It does not include an emulator.

```bash
docker build -t shantspend-build .

docker run --rm -v "$(pwd):/app" -w /app shantspend-build ./gradlew assembleDebug
docker run --rm -v "$(pwd):/app" -w /app shantspend-build ./gradlew test
docker run --rm -v "$(pwd):/app" -w /app shantspend-build ./gradlew lint
```

To install a build produced this way on a physical device, run `adb install <apk-path>` from the **host** machine (not inside the container) — USB passthrough into Docker is not supported.

## Release signing

Release builds are signed using a keystore that is never committed to the repo. To (re)generate it:

```bash
keytool -genkeypair -v \
  -keystore shantspend-release.jks \
  -alias shantspend-upload \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -dname "CN=Shantspend, OU=Personal, O=Shalltear, L=Unknown, ST=Unknown, C=IN"
```

Keep the resulting `.jks` file outside the repo and back it up privately — losing it means the app can never be updated under the same signature again.

Base64-encode it and store it as GitHub Actions secrets:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("<path-to-jks>")) | Out-File -Encoding ascii keystore_b64.txt
```

```bash
gh secret set KEYSTORE_BASE64 < keystore_b64.txt
gh secret set KEYSTORE_PASSWORD --body "<storepass>"
gh secret set KEY_ALIAS --body "shantspend-upload"
gh secret set KEY_PASSWORD --body "<keypass>"
```

Delete the local `keystore_b64.txt` afterward.

## CI/CD

- `.github/workflows/ci.yml` — runs on every PR into `main`: assembles a debug build, runs unit tests, and runs lint. This is the required status check for merging.
- `.github/workflows/release.yml` — runs on every push to `main`: decodes the release keystore from secrets, builds a signed release APK (with an auto-incrementing `versionCode` from the GitHub Actions run number), and publishes a GitHub Release tagged `v<versionName>-build<run_number>` with the APK attached.
