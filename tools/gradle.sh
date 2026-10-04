#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
export GRADLE_USER_HOME="$PWD/.gradle-home"
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
for executable in "$HOME"/.gradle/wrapper/dists/gradle-8.8-bin/*/gradle-8.8/bin/gradle; do
 if [ -x "$executable" ]; then exec "$executable" "$@"; fi
done
echo 'Gradle 8.8 is required. No software was installed automatically.' >&2
exit 1
