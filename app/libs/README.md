# Xray Library Integration

## Overview
This directory should contain the Xray-core Android library (libXray.aar) for VLESS VPN functionality.

## Getting the Library

### Option 1: Build from Source
1. Clone the Xray-core repository:
   ```bash
   git clone https://github.com/XTLS/Xray-core.git
   ```

2. Build for Android:
   ```bash
   cd Xray-core
   go get -v
   GOOS=android GOARCH=arm64 CGO_ENABLED=1 go build -buildmode=c-shared -o libxray.so
   ```

3. Package as AAR and place in this directory

### Option 2: Use Prebuilt Library
Download a prebuilt version from:
- https://github.com/XTLS/Xray-core/releases
- Or from third-party Android VPN projects using Xray

### Alternative: v2ray-core
You can also use v2ray-core as an alternative:
- https://github.com/v2fly/v2ray-core

## Integration Steps

1. Place `libxray.aar` in this directory
2. Uncomment the dependency in `app/build.gradle.kts`:
   ```kotlin
   implementation(files("libs/libxray.aar"))
   ```
3. Update `XrayManager.kt` to use the actual JNI methods
4. Implement proper packet forwarding in `TunelVpnService.kt`

## Current Status
The app is currently using stub implementations for Xray functionality. The VPN will establish but won't route traffic until the actual Xray library is integrated.

## Resources
- Xray Documentation: https://xtls.github.io/
- Android VPN Service: https://developer.android.com/guide/topics/connectivity/vpn
- Example projects using Xray on Android:
  - v2rayNG: https://github.com/2dust/v2rayNG
  - SagerNet: https://github.com/SagerNet/SagerNet

