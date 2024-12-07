#!/usr/bin/env bash

set -e

# Constants
INSTALL_DIR="${HOME}/.skidfuscator"
WRAPPER_PATH="/usr/local/bin/skidfuscator"
REPO="skidfuscatordev/skidfuscator-java-obfuscator"
JAR_NAME="skidfuscator.jar"

# Check for Java
if ! command -v java &> /dev/null; then
    echo "Java is required but not installed. Install it and try again."
    exit 1
fi

# Fetch the latest release URL
echo "Fetching the latest Skidfuscator release..."
RELEASE_URL=$(curl -s "https://api.github.com/repos/${REPO}/releases/latest" | jq -r ".assets[] | select(.name == \"${JAR_NAME}\") | .browser_download_url")

if [[ -z "$RELEASE_URL" ]]; then
    echo "Failed to find the latest release. Ensure the repository and asset names are correct."
    exit 1
fi

# Create install directory
echo "Installing Skidfuscator to ${INSTALL_DIR}..."
mkdir -p "${INSTALL_DIR}"
curl -L "${RELEASE_URL}" -o "${INSTALL_DIR}/${JAR_NAME}"

# Create wrapper script
echo "Creating wrapper script at ${WRAPPER_PATH}..."
sudo bash -c "cat << 'EOF' > ${WRAPPER_PATH}
#!/usr/bin/env bash
java -jar ${INSTALL_DIR}/${JAR_NAME} "\$@"
EOF"

sudo chmod +x "${WRAPPER_PATH}"

echo "Installation complete. Run 'skidfuscator --help' to get started."
