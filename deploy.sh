#!/bin/bash

# 1. The Build Phase
echo "🚀 Building Cleanthes via Gradle..."
./gradlew assembleDebug

# 2. The Locate Phase
# This finds the newest APK in your build folder
APK_PATH=$(find app/build/outputs/apk/debug/ -name "*.apk" -printf '%T@ %p\n' | sort -n | tail -1 | cut -f2- -d" ")

if [ -z "$APK_PATH" ]; then
    echo "❌ Build failed. No APK found."
    exit 1
fi

# 3. The Deployment Phase
# We copy it to a place the Android OS can see, then trigger the install
echo "📦 Deploying $APK_PATH..."
cp "$APK_PATH" /sdcard/Download/cleanthes-dev.apk

# This command 'whispers' to the host Android to open the file
termux-open /sdcard/Download/cleanthes-dev.apk

