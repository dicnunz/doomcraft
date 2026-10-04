#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
./tools/build-native.sh
clang ${DC_CFLAGS:-} -O1 -g -Ibuild/doom -Inative native/test_bridge.c -Lbuild/native -ldoomcraft -Wl,-rpath,"$PWD/build/native" -o build/native/test_bridge
./build/native/test_bridge
