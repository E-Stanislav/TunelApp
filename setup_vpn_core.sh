#!/bin/bash

# Script to help get libcore/VPN core for TunelApp
# Since sing-box doesn't provide ready .aar files, we offer multiple options

echo "🚀 TunelApp VPN Core Setup"
echo "=========================="
echo ""
echo "⚠️  Important: sing-box doesn't provide ready Android .aar files"
echo "We need to get libcore from other sources."
echo ""

# Create libs directory
mkdir -p app/libs

echo "📋 Available Options:"
echo ""
echo "1. Clone NekoBox and extract libcore.aar (RECOMMENDED) ⭐"
echo "   - Time: ~5-10 minutes"
echo "   - Size: ~50MB download"
echo "   - Ready to use"
echo ""
echo "2. Download sing-box binary (manual integration required)"
echo "   - Time: ~1 minute"
echo "   - Size: ~10MB"
echo "   - Requires wrapper development"
echo ""
echo "3. Manual instructions (download yourself)"
echo ""

read -p "Choose option (1-3) or 'q' to quit: " choice
echo ""

case $choice in
  1)
    echo "📦 Option 1: Cloning NekoBox repository..."
    echo ""
    
    TEMP_DIR=$(mktemp -d)
    echo "Temporary directory: $TEMP_DIR"
    
    cd "$TEMP_DIR" || exit 1
    
    echo "Cloning NekoBox (this may take a few minutes)..."
    if git clone --depth 1 https://github.com/MatsuriDayo/NekoBoxForAndroid.git; then
        echo ""
        echo "✅ Clone successful!"
        echo "🔍 Searching for libcore.aar..."
        echo ""
        
        # Search for libcore
        LIBCORE_PATH=$(find NekoBoxForAndroid -name "libcore*.aar" -o -name "libbox*.aar" 2>/dev/null | head -1)
        
        if [ -n "$LIBCORE_PATH" ]; then
            # Copy to our project
            TARGET="$OLDPWD/app/libs/libcore.aar"
            cp "$LIBCORE_PATH" "$TARGET"
            
            FILE_SIZE=$(wc -c < "$TARGET" | tr -d ' ')
            SIZE_MB=$(($FILE_SIZE / 1024 / 1024))
            
            echo "✅ Success! libcore.aar copied"
            echo "📁 Location: app/libs/libcore.aar"
            echo "📦 Size: ${SIZE_MB}MB"
            echo ""
            echo "📝 Next Steps:"
            echo "1. Open app/build.gradle.kts"
            echo "2. Add this line to dependencies:"
            echo "   implementation(files(\"libs/libcore.aar\"))"
            echo "3. Sync Gradle"
            echo "4. See CORE_INTEGRATION_OPTIONS.md for code examples"
            echo ""
            echo "🎉 Core is ready to use!"
            
        else
            echo "❌ libcore.aar not found in repository"
            echo ""
            echo "💡 Alternative: Build libcore yourself"
            echo "   git clone https://github.com/SagerNet/sing-box-for-android"
            echo "   cd sing-box-for-android"
            echo "   ./gradlew :libcore:assembleRelease"
        fi
    else
        echo "❌ Failed to clone NekoBox repository"
        echo "Check your internet connection and try again"
    fi
    
    cd "$OLDPWD" || exit 1
    echo ""
    echo "🧹 Cleaning up temporary files..."
    rm -rf "$TEMP_DIR"
    ;;
    
  2)
    echo "📦 Option 2: Downloading sing-box binary..."
    echo ""
    
    VERSION="1.12.10"
    URL="https://github.com/SagerNet/sing-box/releases/download/v${VERSION}/sing-box-${VERSION}-android-arm64.tar.gz"
    
    echo "Version: $VERSION"
    echo "URL: $URL"
    echo ""
    
    if curl -L -o "app/libs/sing-box.tar.gz" "$URL"; then
        echo ""
        echo "✅ Download successful!"
        echo "📁 File: app/libs/sing-box.tar.gz"
        echo ""
        echo "⚠️  This is a binary, not an AAR library"
        echo "You'll need to:"
        echo "1. Extract the binary: tar -xzf app/libs/sing-box.tar.gz"
        echo "2. Create JNI wrapper to call it"
        echo "3. Or use Option 1 instead (recommended)"
    else
        echo "❌ Download failed"
    fi
    ;;
    
  3)
    echo "📖 Manual Instructions"
    echo "===================="
    echo ""
    echo "Method A: Use NekoBox libcore (Easiest)"
    echo "---------------------------------------"
    echo "1. Go to: https://github.com/MatsuriDayo/NekoBoxForAndroid/releases"
    echo "2. Download the latest APK"
    echo "3. Extract libcore.aar from the APK (it's a zip file)"
    echo "4. Copy to: app/libs/libcore.aar"
    echo ""
    echo "Method B: Build libcore yourself"
    echo "--------------------------------"
    echo "1. git clone https://github.com/SagerNet/sing-box-for-android"
    echo "2. Install Android NDK"
    echo "3. Run: ./gradlew :libcore:assembleRelease"
    echo "4. Copy build/outputs/aar/libcore-release.aar to your project"
    echo ""
    echo "Method C: Use v2rayNG with Xray"
    echo "-------------------------------"
    echo "1. git clone https://github.com/2dust/v2rayNG"
    echo "2. Find libs/*.aar files"
    echo "3. Copy to your project"
    echo ""
    echo "📄 See CORE_INTEGRATION_OPTIONS.md for detailed instructions"
    ;;
    
  q|Q)
    echo "Cancelled."
    exit 0
    ;;
    
  *)
    echo "❌ Invalid option"
    exit 1
    ;;
esac

echo ""
echo "✨ Done!"
echo ""

