#!/usr/bin/env bash
set -euo pipefail

# Linux x64 CI runtime, matching JdkDownloader. SHA-256 published at:
# https://github.com/corretto/corretto-17/releases/tag/17.0.13.11.1
test_home=${1:-$HOME}
archive=amazon-corretto-17.0.13.11.1-linux-x64.tar.gz
temporary=$(mktemp -d)
trap 'rm -rf "$temporary"' EXIT
curl --fail --location --show-error --silent --retry 2 --max-time 300 \
    "https://corretto.aws/downloads/resources/17.0.13.11.1/$archive" -o "$temporary/$archive"
printf '%s  %s\n' 31ccb0791940add3fa49b82fff5f87a65dbea27d294f58a753b9775f9752ecc7 \
    "$temporary/$archive" | sha256sum --check -
mkdir -p "$test_home/.ssvm/jdk"
tar -xzf "$temporary/$archive" -C "$temporary"
verified="$test_home/.ssvm/jdk/amazon-corretto-17.0.13.11.1-linux-x64-31ccb0791940"
printf '%s' 31ccb0791940add3fa49b82fff5f87a65dbea27d294f58a753b9775f9752ecc7 > "$temporary/amazon-corretto-17.0.13.11.1-linux-x64/.verified-sha256"
if [ -d "$verified" ]; then
    cmp "$temporary/amazon-corretto-17.0.13.11.1-linux-x64/.verified-sha256" "$verified/.verified-sha256"
else
    mv "$temporary/amazon-corretto-17.0.13.11.1-linux-x64" "$verified"
fi
test -f "$verified/jmods/java.base.jmod"
