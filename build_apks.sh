#!/bin/bash

# TunelApp APK Builder Script
# Создает DEBUG APK файлы для Mobile и TV с датой в имени

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get current date and time
DATE=$(date +"%Y%m%d_%H%M")
TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")

echo -e "${BLUE}🚀 TunelApp APK Builder (DEBUG)${NC}"
echo -e "${BLUE}===============================${NC}"
echo -e "📅 Date: $TIMESTAMP"
echo -e "📦 Building DEBUG APK files..."
echo ""

# Create output directory
OUTPUT_DIR="apk"
mkdir -p "$OUTPUT_DIR"

# Function to build APK
build_apk() {
    local variant=$1
    local flavor=$2
    local output_name=$3
    
    echo -e "${YELLOW}🔨 Building $variant ($flavor)...${NC}"
    
    # Build the APK
    if ./gradlew "assemble${flavor}Debug" --quiet; then
        # Find the generated APK
        FLAVOR_LOWER=$(echo "$flavor" | tr '[:upper:]' '[:lower:]')
        APK_PATH="app/build/outputs/apk/$FLAVOR_LOWER/debug/app-$FLAVOR_LOWER-debug.apk"
        
        if [ -f "$APK_PATH" ]; then
            # Copy to output directory with date
            cp "$APK_PATH" "$OUTPUT_DIR/$output_name"
            
            # Get file size
            SIZE=$(ls -lh "$OUTPUT_DIR/$output_name" | awk '{print $5}')
            
            echo -e "${GREEN}✅ $variant built successfully!${NC}"
            echo -e "   📁 File: $output_name"
            echo -e "   📏 Size: $SIZE"
            echo ""
        else
            echo -e "${RED}❌ APK file not found: $APK_PATH${NC}"
            return 1
        fi
    else
        echo -e "${RED}❌ Failed to build $variant${NC}"
        return 1
    fi
}

# Build Mobile APK
build_apk "Mobile" "Mobile" "TunelApp_Mobile_${DATE}.apk"

# Build TV APK
build_apk "TV" "Tv" "TunelApp_TV_${DATE}.apk"

# Summary
echo -e "${GREEN}🎉 Build Complete!${NC}"
echo -e "${GREEN}==================${NC}"
echo -e "📅 Built: $TIMESTAMP"
echo -e "📁 Output directory: ./$OUTPUT_DIR/"
echo ""

# List generated files
echo -e "${BLUE}📦 Generated APK files:${NC}"
ls -lh "$OUTPUT_DIR"/*.apk | while read line; do
    echo -e "   $line"
done

echo ""
echo -e "${YELLOW}💡 Installation commands:${NC}"
echo -e "   Mobile: adb install $OUTPUT_DIR/TunelApp_Mobile_${DATE}.apk"
echo -e "   TV:     adb install $OUTPUT_DIR/TunelApp_TV_${DATE}.apk"
echo ""

# Optional: Install to connected device
if command -v adb &> /dev/null; then
    DEVICE_COUNT=$(adb devices 2>/dev/null | grep -c "device$" || echo "0")
    if [ "$DEVICE_COUNT" -gt 0 ] 2>/dev/null; then
        echo -e "${BLUE}📱 Connected devices detected: $DEVICE_COUNT${NC}"
        echo -e "${YELLOW}Would you like to install Mobile APK? (y/N):${NC}"
        read -r response
        if [[ "$response" =~ ^[Yy]$ ]]; then
            echo -e "${YELLOW}Installing Mobile APK...${NC}"
            adb install "$OUTPUT_DIR/TunelApp_Mobile_${DATE}.apk"
            echo -e "${GREEN}✅ Mobile APK installed!${NC}"
        fi
    else
        echo -e "${YELLOW}📱 No devices connected. Connect device and run:${NC}"
        echo -e "   adb install $OUTPUT_DIR/TunelApp_Mobile_${DATE}.apk"
    fi
else
    echo -e "${YELLOW}📱 ADB not found. Install APK manually:${NC}"
    echo -e "   adb install $OUTPUT_DIR/TunelApp_Mobile_${DATE}.apk"
fi

echo ""
echo -e "${GREEN}✨ Done! APK files ready for distribution.${NC}"
