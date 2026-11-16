#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
java_bin=${JAVA_BIN:-java}
home_dir=${SKIDFUSCATOR_USER_HOME:-$HOME}
jar_dir="$root/dev.skidfuscator.integration/build/libs"
fixture="$jar_dir/integration.jar"
test -f "$fixture"
classpath=$(cat "$root/dev.skidfuscator.integration/build/runtime-classpath.txt")
work=$(mktemp -d)
finish() {
    status=$?
    mkdir -p "$root/build/reports/frameworks"
    cp "$work"/*.log "$work"/*.report.json "$root/build/reports/frameworks/" 2>/dev/null || true
    rm -rf "$work"
    exit "$status"
}
trap finish EXIT
cc -shared -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -x c -o "$work/libfixture.so" - <<'C'
#include <jni.h>
JNIEXPORT jint JNICALL Java_fixtures_NativeBridge_twice(JNIEnv *env,jclass type,jint value) { return value*2; }
C
"$java_bin" -Xverify:all -cp "$fixture:$jar_dir/dependencies/*" fixtures.FrameworkApp "$work/libfixture.so" "$work/persisted.bin" > "$work/baseline.log" 2>&1
grep '^RESULT:' "$work/baseline.log" > "$work/expected"
for profile in CONSERVATIVE BALANCED STRONG; do
    (
        cd "$work"
        "$java_bin" -Xmx3g "-Duser.home=$home_dir" -jar "$root/dev.skidfuscator.client.standalone/build/libs/client-standalone-all.jar" \
          obfuscate "$fixture" --output "$work/$profile.jar" --libs "$jar_dir/dependencies" \
          --seed 12345 --profile "$profile" --offline --non-interactive > "$profile-protection.log" 2>&1 || { cat "$profile-protection.log"; exit 1; }
        "$java_bin" -Xverify:all -cp "$work/$profile.jar:$jar_dir/dependencies/*" fixtures.FrameworkApp "$work/libfixture.so" "$work/persisted.bin" > "$profile-run.log" 2>&1 || { cat "$profile-run.log";exit 1; }
        grep '^RESULT:' "$profile-run.log" > actual
        cmp expected actual
        for seed in 0 1 9223372036854775807; do
            "$java_bin" -Xverify:all -cp "$fixture" fixtures.DifferentialCases "$seed" > expected-cases
            "$java_bin" -Xverify:all -cp "$work/$profile.jar" fixtures.DifferentialCases "$seed" > actual-cases
            cmp expected-cases actual-cases
        done
        "$java_bin" -cp "$classpath" fixtures.Repackage "$work/$profile.jar" "$work/$profile-boot.jar" "$jar_dir/dependencies"
        "$java_bin" -Xverify:all -jar "$work/$profile-boot.jar" "$work/libfixture.so" "$work/persisted.bin" > boot.log 2>&1 || { cat boot.log;exit 1; }
        grep '^RESULT:' boot.log > actual
        cmp expected actual
        if [[ ${EVALUATE_PROTECTION:-0} == 1 ]]; then
            python3 "$root/scripts/evaluate-protection.py" "$fixture" "$work/$profile.jar" fixtures.DifferentialCases \
              "$root/dev.skidfuscator.integration/build/tools/cfr-0.152.jar" "$root/build/reports/protection-$profile.json" \
              --classpath "$jar_dir/dependencies/*"
        fi
        printf '%s: frameworks, JNI, executable Boot JAR and 3000 differential cases passed\n' "$profile"
    )
done
