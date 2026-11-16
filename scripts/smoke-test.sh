#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
java_bin=${JAVA_BIN:-java}
test_home=${SKIDFUSCATOR_USER_HOME:-$HOME}
runtime_args=()
if [[ -n ${SKIDFUSCATOR_RUNTIME:-} ]]; then
    runtime_args=(--runtime "$SKIDFUSCATOR_RUNTIME")
fi
temporary=$(mktemp -d)
trap 'rm -rf "$temporary"' EXIT
cat > "$temporary/SmokeApp.java" <<'JAVA'
public class SmokeApp {
    public static void main(String[] args) {
        int sum = 0;
        for (int i = 0; i <= 10; i++) sum += i;
        System.out.println("protected:" + sum);
    }
}
JAVA
javac --release 17 -d "$temporary/classes" "$temporary/SmokeApp.java"
jar --create --file "$temporary/input.jar" --main-class SmokeApp -C "$temporary/classes" .
"$java_bin" -Xverify:all -jar "$temporary/input.jar" > "$temporary/expected.txt"
printf 'protected:55\n' | cmp - "$temporary/expected.txt"
(
    cd "$temporary"
    "$java_bin" -Xmx2g -Djava.awt.headless=true "-Duser.home=$test_home" \
        -jar "$root/dev.skidfuscator.client.standalone/build/libs/client-standalone-all.jar" \
        obfuscate input.jar --output output.jar --notrack "${runtime_args[@]}" > obfuscation.log 2>&1 || {
        cat obfuscation.log
        exit 1
    }
    "$java_bin" -Xverify:all -jar output.jar > actual.txt
    cmp expected.txt actual.txt
    # Usage errors must never appear as a successful protection run.
    set +e
    "$java_bin" -jar "$root/dev.skidfuscator.client.standalone/build/libs/client-standalone-all.jar" \
        obfuscate --invalid-option > cli-error.log 2>&1
    status=$?
    set -e
    test "$status" -eq 2
    cat actual.txt
)
