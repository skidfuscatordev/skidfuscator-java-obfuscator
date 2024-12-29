#!/bin/bash

# Check if curl is installed
if ! command -v curl &> /dev/null
then
    echo "curl could not be found. Please install curl to proceed."
    exit 1
fi

# Get the latest release tag from GitHub
LATEST_TAG=$(curl -s https://api.github.com/repos/skidfuscatordev/skidfuscator-java-obfuscator/releases/latest | grep 'tag_name' | cut -d\" -f4)

# Download the latest Skidfuscator jar file
echo "Downloading the latest Skidfuscator jar..."
curl -L -o Skidfuscator.Community.$LATEST_TAG.jar https://github.com/skidfuscatordev/skidfuscator-java-obfuscator/releases/download/$LATEST_TAG/Skidfuscator.Community.$LATEST_TAG.jar

# Download the obfuscation tester jar
echo "Downloading the obfuscation tester jar..."
curl -L -o obf-test-1.0-SNAPSHOT.jar https://github.com/sim0n/jvm-obfuscation-tester/releases/download/1.0.0/obf-test-1.0-SNAPSHOT.jar

# Run the main skidfuscator command
echo "Running the main skidfuscator command..."
java -jar Skidfuscator.Community.$LATEST_TAG.jar obfuscate obf-test-1.0-SNAPSHOT.jar --config config.hocon

echo "Process completed."