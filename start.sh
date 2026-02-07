#!/bin/bash

# Load Java path from config.properties
CONFIG_FILE="run-config.properties"

if [[ -f "$CONFIG_FILE" ]]; then
    source "$CONFIG_FILE"
else
    echo "Error: Configuration file '$CONFIG_FILE' not found!"
    exit 1
fi

# Validate JAVA_HOME
if [[ -z "$JAVA_HOME" ]]; then
    echo "Error: JAVA_HOME is not set in '$CONFIG_FILE'"
    exit 1
fi

export PATH="$JAVA_HOME/bin:$PATH"

# Find the JAR file
JAR_FILE=$(find . -maxdepth 2 -type f -name "*.jar" ! -iname "*lib*" | head -n 1)

if [[ -z "$JAR_FILE" ]]; then
    echo "Error: No JAR file found in target directory!"
    exit 1
fi

echo "Using Java from: $JAVA_HOME"
echo "Starting application with JAR: $JAR_FILE"

# Run the Spring Boot application in the background
nohup java -jar "$JAR_FILE" > logs/console.log 2>&1 &

# Get Process ID (PID) and store it in a .pid file
echo $! > app.pid

echo "Application started with PID: $(cat app.pid)"
