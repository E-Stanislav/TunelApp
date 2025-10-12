#!/bin/bash

# Set Java 17 environment
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "Starting IDE with Java 17..."
echo "JAVA_HOME: $JAVA_HOME"

# Start Android Studio or IntelliJ IDEA
# Uncomment the line for your IDE:

# For Android Studio:
# open -a "Android Studio" /Users/stanislave/Documents/Projects/TunelApp

# For IntelliJ IDEA:
open -a "IntelliJ IDEA" /Users/stanislave/Documents/Projects/TunelApp
