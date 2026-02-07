#!/bin/bash

# Set Java Home (Update this path if needed)
export JAVA_HOME=/opt/java-21.0.6+7
export PATH=$JAVA_HOME/bin:$PATH

# Set Maven Wrapper Path
MVNW="./mvnw"

# Verify Java and Maven Versions
echo "Using Java:"
java -version

echo "Using Maven:"
$MVNW -version

# Clean and Build the Project
echo "Building the Spring Boot project..."
$MVNW clean package -DskipTests

# Find the generated JAR file
JAR_FILE=$(find target -type f -name "*.jar" | head -n 1)

if [[ -f "$JAR_FILE" ]]; then
    echo "Build successful! JAR file created: $JAR_FILE"
else
    echo "Build failed! No JAR file found."
    exit 1
fi

