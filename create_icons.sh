#!/bin/bash

# Create a minimal 1x1 PNG file using base64
echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==" | base64 -d > app/src/main/res/mipmap-hdpi/ic_launcher.png
echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==" | base64 -d > app/src/main/res/mipmap-hdpi/ic_launcher_round.png

echo "Created minimal PNG files"





