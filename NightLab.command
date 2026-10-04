#!/bin/sh
set -eu
cd "$(dirname "$0")"
./tools/build-native.sh
exec ./tools/gradle.sh runClient -Pnight
