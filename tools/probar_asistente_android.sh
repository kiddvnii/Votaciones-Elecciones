#!/usr/bin/env bash
set -euo pipefail
mkdir -p diagnostico
adb install -r paquetes/app-debug.apk
adb install -r paquetes/app-debug-androidTest.apk
adb logcat -c
adb shell am instrument -w -r -e class com.daniel.votacionesyelecciones.AsistenteMotorInstrumentedTest,com.daniel.votacionesyelecciones.ErroresFirebaseInstrumentedTest,com.daniel.votacionesyelecciones.IdentidadAplicacionInstrumentedTest com.daniel.votacioneselecciones.test/androidx.test.runner.AndroidJUnitRunner > diagnostico/instrumentacion.txt 2>&1
cat diagnostico/instrumentacion.txt
adb logcat -d > diagnostico/logcat.txt
python3 - <<'PY'
from pathlib import Path
resultado = Path("diagnostico/instrumentacion.txt").read_text()
assert "OK (5 tests)" in resultado, "La prueba Android no terminó correctamente"
print("JNI ANDROID X86_64: ASISTENTE Y VOSK NATIVOS VERIFICADOS")
PY
