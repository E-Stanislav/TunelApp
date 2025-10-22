#!/bin/bash

# Quick APK Builder - Simple version
# Быстрая сборка APK файлов

set -e

DATE=$(date +"%Y%m%d_%H%M")
OUTPUT_DIR="apk"
mkdir -p "$OUTPUT_DIR"

echo "🚀 Building APK files..."

# Build Mobile
echo "📱 Building Mobile APK..."
./gradlew assembleMobileDebug --quiet
cp "app/build/outputs/apk/mobile/debug/app-mobile-debug.apk" "$OUTPUT_DIR/TunelApp_Mobile_${DATE}.apk"

# Build TV
echo "📺 Building TV APK..."
./gradlew assembleTvDebug --quiet
cp "app/build/outputs/apk/tv/debug/app-tv-debug.apk" "$OUTPUT_DIR/TunelApp_TV_${DATE}.apk"

echo "✅ Done! APK files created:"
ls -lh "$OUTPUT_DIR"/*.apk

echo ""
echo "📱 Install commands:"
echo "   adb install $OUTPUT_DIR/TunelApp_Mobile_${DATE}.apk"
echo "   adb install $OUTPUT_DIR/TunelApp_TV_${DATE}.apk"
