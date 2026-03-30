# Mass Media Saver

A simple Android app for downloading all images and videos from any webpage, or saving complete offline archives with playable videos.

## Features

- **WebView Browser**: Load any webpage with JavaScript support
- **Download All Media**: Extract and download all images and videos in original highest quality
- **Save Page Complete**: Create offline archives with rewritten local links for playable videos
- **Progress Tracking**: Visual progress for downloads
- **Downloads & Archives Tabs**: View and manage saved content

## Requirements

- Android 7.0 (API 24) or higher
- For Termux building: Android SDK with build-tools 34.0.0 and platform android-34

## Termux Build Instructions

### Step 1: Setup Termux Environment

```bash
# Update and install required packages
pkg update && pkg upgrade

# Install git, wget, curl, and build tools
pkg install git wget curl unzip

# Install Gradle
pkg install gradle

# Install OpenJDK 21
pkg install openjdk-21

# Verify Java installation
java -version
```

### Step 2: Setup Android SDK

```bash
# Create Android SDK directory
mkdir -p ~/android-sdk/cmdline-tools

# Download Android command line tools
cd ~/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip

# Extract and setup
unzip cmdline-tools.zip
mv cmdline-tools latest
rm cmdline-tools.zip

# Set environment variables
echo 'export ANDROID_HOME=$HOME/android-sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> ~/.bashrc

# Reload bash
source ~/.bashrc

# Accept licenses and install required SDK components
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### Step 3: Fix Common Issues

Create gradle properties with aapt2 override for Termux:

```bash
mkdir -p ~/.gradle
echo 'android.aapt2FromMavenOverride=https://github.com/Termux-pv/aapt2-build/releases/download/v8.2.2-1/aapt2' > ~/.gradle/gradle.properties
```

### Step 4: Clone and Setup Project

```bash
# Clone the repository
git clone https://github.com/your-repo/MassMediaSaver.git
cd MassMediaSaver

# Create local.properties with SDK path
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

### Step 5: Build the APK

```bash
# Make gradlew executable
chmod +x gradlew

# Build debug APK (no daemon recommended for Termux)
./gradlew assembleDebug --no-daemon

# The APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

### Step 6: Install the APK

```bash
# Copy to Download folder for easy access
cp app/build/outputs/apk/debug/app-debug.apk /storage/emulated/0/Download/

# Or install directly using adb
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Gradle Properties Configuration

The project includes a `gradle.properties` file with:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
android.aapt2FromMavenOverride=https://github.com/Termux-pv/aapt2-build/releases/download/v8.2.2-1/aapt2
```

This configuration is optimized for Termux builds.

## GitHub Actions Build

The project includes a GitHub Actions workflow that builds the APK automatically on push to main.

1. Push your code to a GitHub repository
2. Go to Actions tab to see build progress
3. Download the APK from the Artifacts section

## App Usage

1. **Launch the app** - Accept the legal disclaimer
2. **Browse** - Enter a URL in the address bar and tap Go
3. **Download Media** - Tap "Download All" to save all images and videos
4. **Save Archive** - Tap "Save Page" to create an offline archive
5. **View Downloads** - Use the Downloads/Archives tabs to manage saved content

## Technical Details

- **Target SDK**: 34
- **Compile SDK**: 34
- **Min SDK**: 24
- **Kotlin**: 1.9.22
- **Compose BOM**: 2024.01.00
- **Jsoup**: 1.17.2 (HTML parsing)
- **OkHttp**: 4.12.0 (HTTP client)

## Storage Location

- Downloads: `Android/data/com.massmediasaver/files/MassMediaSaver/Downloads/`
- Archives: `Android/data/com.massmediasaver/files/MassMediaSaver/Archives/`

## Legal Notice

This app is for personal use only. Ensure you have the right to download and save content from websites you visit. Respect copyright laws and website terms of service.
