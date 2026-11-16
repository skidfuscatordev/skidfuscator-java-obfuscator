#!/usr/bin/env bash
set -euo pipefail
# No credentials are accepted in arguments or written to disk by this script.
: "${SIGNING_KEYSTORE:?Set SIGNING_KEYSTORE to a protected keystore file}"
: "${SIGNING_STOREPASS:?Inject SIGNING_STOREPASS securely}"
: "${SIGNING_ALIAS:?Set SIGNING_ALIAS}"
input=${1:?Usage: sign-artifact.sh input.jar output.jar}
output=${2:?Usage: sign-artifact.sh input.jar output.jar}
if [[ $input == "$output" || $input -ef $output ]]; then
    printf '%s\n' 'Signed output must differ from the input artifact' >&2
    exit 2
fi
tsa_args=()
if [[ -n ${SIGNING_TSA_URL:-} ]]; then
    [[ $SIGNING_TSA_URL == https://* ]]
    tsa_args=(-tsa "$SIGNING_TSA_URL")
fi
parent=$(dirname "$output")
work=$(mktemp -d "$parent/.sign-XXXXXXXX")
trap 'rm -rf "$work"' EXIT
cp "$input" "$work/artifact.jar"
jarsigner -keystore "$SIGNING_KEYSTORE" -storepass:env SIGNING_STOREPASS \
    -digestalg SHA-256 "${tsa_args[@]}" "$work/artifact.jar" "$SIGNING_ALIAS"
jarsigner -verify -strict -keystore "$SIGNING_KEYSTORE" -storepass:env SIGNING_STOREPASS "$work/artifact.jar"
mv "$work/artifact.jar" "$output"
sha256sum "$output" > "$output.sha256"
