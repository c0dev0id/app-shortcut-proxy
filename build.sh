#!/usr/bin/env bash
# Manual Android APK build script.
# Used when Gradle cannot download dependencies (e.g. Google Maven unreachable).
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_DIR="$PROJECT_DIR/app"
SRC_DIR="$APP_DIR/src/main"
BUILD_DIR="$APP_DIR/build/manual"

ANDROID_SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/usr/local/lib/android/sdk}}"
BUILD_TOOLS="$ANDROID_SDK/build-tools/34.0.0"
PLATFORM="$ANDROID_SDK/platforms/android-34"
KOTLIN_HOME="${KOTLIN_HOME:-/usr/share/kotlinc}"

ANDROID_JAR="$PLATFORM/android.jar"
KOTLIN_STDLIB="$KOTLIN_HOME/lib/kotlin-stdlib.jar"

echo "=== ShortcutProxy Manual APK Build ==="

# Validate required tools
if [ ! -f "$ANDROID_JAR" ]; then
  echo "ERROR: android.jar not found at $ANDROID_JAR" >&2
  echo "  Set ANDROID_SDK_ROOT or ANDROID_HOME to your SDK location." >&2
  exit 1
fi
if [ ! -f "$KOTLIN_STDLIB" ]; then
  echo "ERROR: kotlin-stdlib.jar not found at $KOTLIN_STDLIB" >&2
  echo "  Set KOTLIN_HOME to your Kotlin installation directory." >&2
  exit 1
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"/{res-compiled,gen,classes,dex,apk-staging}

echo "[1/6] Compiling resources..."
"$BUILD_TOOLS/aapt2" compile --dir "$SRC_DIR/res" -o "$BUILD_DIR/res-compiled/"

echo "[2/6] Linking resources..."
"$BUILD_TOOLS/aapt2" link \
  --proto-format \
  -o "$BUILD_DIR/resources.pb" \
  -I "$ANDROID_JAR" \
  --manifest "$SRC_DIR/AndroidManifest.xml" \
  --java "$BUILD_DIR/gen" \
  $(find "$BUILD_DIR/res-compiled" -name "*.flat" | sed 's/^/-R /') \
  --auto-add-overlay \
  --package-id 0x7f

echo "[3/6] Compiling Kotlin sources..."
KT_SOURCES=()
while IFS= read -r -d '' f; do
  KT_SOURCES+=("$f")
done < <(find "$SRC_DIR/java" -name "*.kt" -print0)
if [ ${#KT_SOURCES[@]} -eq 0 ]; then
  echo "ERROR: No .kt source files found in $SRC_DIR/java" >&2
  exit 1
fi
kotlinc \
  -classpath "$ANDROID_JAR:$KOTLIN_STDLIB" \
  -d "$BUILD_DIR/classes" \
  "${KT_SOURCES[@]}" \
  "$BUILD_DIR/gen/de/codevoid/appshortcutproxy/R.java"

echo "[4/6] Converting to Dex..."
"$BUILD_TOOLS/d8" \
  --lib "$ANDROID_JAR" \
  --output "$BUILD_DIR/dex" \
  $(find "$BUILD_DIR/classes" -name "*.class") \
  "$KOTLIN_STDLIB"

echo "[5/6] Building APK..."
"$BUILD_TOOLS/aapt2" link \
  -o "$BUILD_DIR/apk-staging/base.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$SRC_DIR/AndroidManifest.xml" \
  $(find "$BUILD_DIR/res-compiled" -name "*.flat" | sed 's/^/-R /') \
  --auto-add-overlay \
  --package-id 0x7f
cp "$BUILD_DIR/dex/classes.dex" "$BUILD_DIR/apk-staging/"
cd "$BUILD_DIR/apk-staging" && zip base.apk classes.dex && cd "$PROJECT_DIR"

echo "[6/6] Aligning APK..."
"$BUILD_TOOLS/zipalign" -f 4 \
  "$BUILD_DIR/apk-staging/base.apk" \
  "$BUILD_DIR/ShortcutProxy-unsigned.apk"

echo ""
echo "Build complete: $BUILD_DIR/ShortcutProxy-unsigned.apk"
echo "Sign with: apksigner sign --ks <keystore> --out ShortcutProxy.apk ShortcutProxy-unsigned.apk"
