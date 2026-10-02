#!/usr/bin/env bash
# Downloads the offline narrator voice into the app before it is built (decision #43):
# the sherpa-onnx speech engine (Apache-2.0) and the Kokoro English voice (Apache-2.0).
# Both are too big for git, so CI fetches them; the app falls back to the phone's own
# text-to-speech when they are missing.
set -euo pipefail

SHERPA_VERSION="v1.13.8"
VOICE="kokoro-int8-en-v0_19"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JNI="$ROOT/app/src/main/jniLibs/arm64-v8a"
ASSETS="$ROOT/app/src/main/assets/kokoro"
WORK="$(mktemp -d)"

if [ ! -f "$JNI/libsherpa-onnx-jni.so" ]; then
  curl -fsSL -o "$WORK/libs.tar.bz2" \
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/$SHERPA_VERSION/sherpa-onnx-$SHERPA_VERSION-android.tar.bz2"
  tar -xjf "$WORK/libs.tar.bz2" -C "$WORK"
  mkdir -p "$JNI"
  cp "$WORK"/jniLibs/arm64-v8a/*.so "$JNI/"
fi

if [ ! -f "$ASSETS/model.int8.onnx" ]; then
  curl -fsSL -o "$WORK/voice.tar.bz2" \
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/$VOICE.tar.bz2"
  tar -xjf "$WORK/voice.tar.bz2" -C "$WORK"
  rm -rf "$ASSETS"
  mkdir -p "$ASSETS"
  cp "$WORK/$VOICE/model.int8.onnx" "$WORK/$VOICE/voices.bin" "$WORK/$VOICE/tokens.txt" "$WORK/$VOICE/LICENSE" "$ASSETS/"
  cp -r "$WORK/$VOICE/espeak-ng-data" "$ASSETS/"
  # English only: drop the other languages' pronunciation dictionaries.
  find "$ASSETS/espeak-ng-data" -maxdepth 1 -name '*_dict' ! -name 'en_dict' -delete
fi

rm -rf "$WORK"
ls -la "$JNI" "$ASSETS"
du -sh "$ASSETS"
