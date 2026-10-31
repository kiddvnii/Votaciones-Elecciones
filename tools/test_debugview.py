from pathlib import Path
import json
import os
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = shutil.which("java")
PAQUETE = "com.daniel.votacioneselecciones"
FAKE_ADB = r'''#!/usr/bin/env python3
from pathlib import Path
import json, os, sys
config_path = Path(os.environ["FAKE_ADB_CONFIG"])
config = json.loads(config_path.read_text())
args = sys.argv[1:]
with Path(os.environ["FAKE_ADB_CALLS"]).open("a") as stream:
    stream.write(json.dumps(args) + "\n")
state_path = Path(os.environ["FAKE_ADB_STATE"])
if args == ["devices"]:
    print("List of devices attached")
    for serial, status in config.get("devices", []): print(serial + "\t" + status)
    sys.exit(0)
if len(args) >= 5 and args[0] == "-s" and args[2] == "shell":
    serial, command = args[1], args[3]
    state = json.loads(state_path.read_text()) if state_path.exists() else {}
    if command == "setprop":
        if config.get("deny"):
            print("Permission denied")
            sys.exit(1)
        state[serial] = args[5]
        state_path.write_text(json.dumps(state))
        sys.exit(0)
    if command == "getprop":
        print("otro.paquete" if config.get("wrong_property") else state.get(serial, ""))
        sys.exit(0)
print("unexpected command")
sys.exit(1)
'''

@unittest.skipUnless(JAVA, "Se necesita Java 17 para ejecutar el activador")
class ActivacionDebugViewTest(unittest.TestCase):
    def escenario(self, config, paquete=PAQUETE):
        with tempfile.TemporaryDirectory(prefix="debugview-test-") as temporal:
            carpeta = Path(temporal)
            adb = carpeta / "SDK con espacios" / "adb"
            adb.parent.mkdir()
            adb.write_text(FAKE_ADB)
            adb.chmod(0o755)
            configuracion = carpeta / "config.json"
            configuracion.write_text(json.dumps(config))
            llamadas = carpeta / "calls.jsonl"
            entorno = dict(os.environ, FAKE_ADB_CONFIG=str(configuracion),
                           FAKE_ADB_CALLS=str(llamadas), FAKE_ADB_STATE=str(carpeta / "state.json"))
            resultado = subprocess.run([JAVA, "--source", "17", str(ROOT / "tools/ActivarDebugView.java"),
                                        str(adb), paquete], env=entorno, text=True,
                                       capture_output=True, timeout=30)
            ordenes = [json.loads(line) for line in llamadas.read_text().splitlines()] if llamadas.exists() else []
            return resultado, ordenes

    def test_activa_y_verifica_en_todos_los_dispositivos_autorizados(self):
        resultado, llamadas = self.escenario({"devices": [["emulator-5554", "device"], ["ABC123", "device"]]})
        self.assertEqual(resultado.returncode, 0, resultado.stderr)
        self.assertIn("Activado y verificado", resultado.stdout)
        self.assertIn("2 dispositivo(s)", resultado.stdout)
        for serial in ("emulator-5554", "ABC123"):
            self.assertIn(["-s", serial, "shell", "setprop", "debug.firebase.analytics.app", PAQUETE], llamadas)
            self.assertIn(["-s", serial, "shell", "getprop", "debug.firebase.analytics.app"], llamadas)

    def test_sin_dispositivo_no_informa_activacion(self):
        resultado, llamadas = self.escenario({"devices": []})
        self.assertEqual(resultado.returncode, 0, resultado.stderr)
        self.assertIn("No se activó", resultado.stdout)
        self.assertEqual(llamadas, [["devices"]])

    def test_ignora_unauthorized_y_offline(self):
        resultado, llamadas = self.escenario({"devices": [["sin-permiso", "unauthorized"], ["apagado", "offline"]]})
        self.assertEqual(resultado.returncode, 0, resultado.stderr)
        self.assertNotIn("Activado y verificado", resultado.stdout)
        self.assertEqual(llamadas, [["devices"]])

    def test_denegacion_no_se_presenta_como_exito(self):
        resultado, _ = self.escenario({"devices": [["prueba", "device"]], "deny": True})
        self.assertEqual(resultado.returncode, 1)
        self.assertIn("rechazó", resultado.stderr)
        self.assertNotIn("Activado y verificado", resultado.stdout)

    def test_comprueba_el_valor_real_de_la_propiedad(self):
        resultado, llamadas = self.escenario({"devices": [["prueba", "device"]], "wrong_property": True})
        self.assertEqual(resultado.returncode, 1)
        self.assertIn("verificar", resultado.stderr)
        self.assertNotIn("Activado y verificado", resultado.stdout)
        self.assertEqual(llamadas[-1][3], "getprop")

    def test_paquete_invalido_no_ejecuta_adb(self):
        resultado, llamadas = self.escenario({"devices": [["prueba", "device"]]}, "paquete invalido; comando")
        self.assertEqual(resultado.returncode, 1)
        self.assertEqual(llamadas, [])

if __name__ == "__main__":
    unittest.main(verbosity=2)
