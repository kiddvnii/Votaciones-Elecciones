"""Check real packaged bytes, not just build configuration."""
import hashlib
import io
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
        with z.open(voz) as entrada:
            voz_bytes = entrada.read()
        assert hashlib.sha256(voz_bytes).hexdigest() == "09b239888f633ef2f0b4e09736e3d9936acfd810bc65d53fad45261762c6511f"
        with ZipFile(io.BytesIO(voz_bytes)) as modelo_voz:
            assert modelo_voz.testzip() is None
            assert "vosk-model-small-es-0.42/am/final.mdl" in modelo_voz.namelist()
        assert z.getinfo(prefijo + "modelos/APACHE-2.0.txt").file_size > 10000
        if paquete.endswith(".aab"):
            assert "base/assets/modelos/asistente-qwen3.litertlm" not in z.namelist(), "LLM must not inflate the base Play module"
            assert "modelos/manifest/AndroidManifest.xml" in z.namelist()
        print(f"OK: {paquete}: ambos modelos y licencia incluidos; SHA-256 LLM correcto")
    print(f"Tamaño del archivo: {Path(paquete).stat().st_size / 1024 / 1024:.1f} MiB")
