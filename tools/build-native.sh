#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
python3 tools/prepare_native.py
JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home)}
mkdir -p build/native
sources=$(sed -n 's/^SRC_DOOM = //p' vendor/doomgeneric-master/doomgeneric/Makefile | sed 's/doomgeneric_xlib.o//g;s/\.o/.c/g')
for src in $sources; do
 clang ${DC_CFLAGS:-} -O1 -g -fPIC -Wno-incompatible-function-pointer-types -Wno-pointer-sign -Wno-deprecated-non-prototype -Ibuild/doom -Inative -c "build/doom/$src" -o "build/native/${src%.c}.o"
done
clang ${DC_CFLAGS:-} -O1 -g -fPIC -Ibuild/doom -Inative -c native/bridge.c -o build/native/bridge.o
clang ${DC_CFLAGS:-} -O1 -g -fPIC -Ibuild/doom -Inative -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" -c native/jni.c -o build/native/jni.o
clang ${DC_CFLAGS:-} -dynamiclib -Wl,-install_name,@rpath/libdoomcraft.dylib build/native/*.o -o build/native/libdoomcraft.dylib
