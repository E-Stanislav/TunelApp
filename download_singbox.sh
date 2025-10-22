#!/bin/bash

# Script to download sing-box for Android
# This will download the pre-built sing-box library

echo "🚀 Downloading sing-box for Android..."
echo ""

# Create libs directory if it doesn't exist
mkdir -p app/libs

# sing-box version
VERSION="1.12.10"
URL="https://github.com/SagerNet/sing-box/releases/download/v${VERSION}/sing-box-${VERSION}-android-arm64.aar"

echo "📦 Downloading sing-box v${VERSION}..."
echo "URL: $URL"
echo ""

# Download using curl (works on macOS)
if command -v curl &> /dev/null; then
    curl -L -o "app/libs/libsingbox.aar" "$URL"
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "✅ Download successful!"
        echo "📁 File saved to: app/libs/libsingbox.aar"
        echo ""
        echo "📝 Next steps:"
        echo "1. Open app/build.gradle.kts"
        echo "2. Uncomment this line:"
        echo "   implementation(files(\"libs/libsingbox.aar\"))"
        echo "3. Sync Gradle"
        echo "4. Run the app"
        echo ""
    else
        echo "❌ Download failed!"
        echo "Please download manually from:"
        echo "$URL"
        exit 1
    fi
elif command -v wget &> /dev/null; then
    wget -O "app/libs/libsingbox.aar" "$URL"
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "✅ Download successful!"
        echo "📁 File saved to: app/libs/libsingbox.aar"
    else
        echo "❌ Download failed!"
        exit 1
    fi
else
    echo "❌ Neither curl nor wget is available!"
    echo "Please download manually from:"
    echo "$URL"
    echo "And save it to: app/libs/libsingbox.aar"
    exit 1
fi

# Check file size
FILE_SIZE=$(wc -c < "app/libs/libsingbox.aar" | tr -d ' ')
if [ "$FILE_SIZE" -gt 1000000 ]; then
    echo "✅ File size looks good: $(($FILE_SIZE / 1024 / 1024)) MB"
else
    echo "⚠️  File size seems too small: $FILE_SIZE bytes"
    echo "The download might have failed. Please try again."
    exit 1
fi

echo ""
echo "🎉 sing-box is ready!"
echo ""

