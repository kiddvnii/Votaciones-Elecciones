"""Check real packaged bytes, not just build configuration."""
import hashlib
from pathlib import Path
from zipfile import ZipFile

sha_chat = "c5f39b4191926786c3e6668b62af4d6eb15248dfc7a5348252a9b406f33f2108"
for paquete, prefijo in [
    ("app/build/outputs/apk/debug/app-debug.apk", "assets/"),
    ("app/build/outputs/bundle/release/app-release.aab", "modelos/assets/"),
]:
    with ZipFile(paquete) as z:
        chat = prefijo + "modelos/asistente-qwen3.litertlm"
        voz = prefijo + "modelos/voz-es.zip"
        assert 600_000_000 < z.getinfo(chat).file_size < 630_000_000
        assert 30_000_000 < z.getinfo(voz).file_size < 60_000_000
        with z.open(chat) as entrada:
            assert hashlib.file_digest(entrada, "sha256").hexdigest() == sha_chat
        assert z.getinfo(prefijo + "modelos/APACHE-2.0.txt").file_size > 10000
        if paquete.endswith(".aab"):
            assert "base/assets/modelos/asistente-qwen3.litertlm" not in z.namelist(), "LLM must not inflate the base Play module"
            assert "modelos/manifest/AndroidManifest.xml" in z.namelist()
        print(f"OK: {paquete}: ambos modelos y licencia incluidos; SHA-256 LLM correcto")
    print(f"Tamaño del archivo: {Path(paquete).stat().st_size / 1024 / 1024:.1f} MiB")
