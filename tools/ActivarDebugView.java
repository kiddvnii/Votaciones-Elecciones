import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

 
class ActivarDebugView {
    private record Resultado(int codigo, String texto) {}

    private static Resultado adb(String ejecutable, String... argumentos) throws Exception {
        List<String> comando = new ArrayList<>();
        comando.add(ejecutable);
        comando.addAll(List.of(argumentos));
        Process proceso = new ProcessBuilder(comando).redirectErrorStream(true).start();
        if (!proceso.waitFor(8, TimeUnit.SECONDS)) {
            proceso.destroyForcibly();
            throw new IOException("ADB tardó demasiado. Revisa la conexión del dispositivo.");
        }
        String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Resultado(proceso.exitValue(), salida.trim());
    }

    public static void main(String[] args) {
        try {
            if (args.length != 2 || !args[1].matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")) {
                throw new IllegalArgumentException("Uso: ActivarDebugView.java RUTA_ADB PAQUETE_ANDROID");
            }
            String ejecutable = args[0], paquete = args[1];
            Resultado lista = adb(ejecutable, "devices");
            if (lista.codigo() != 0) throw new IOException("No se pudo consultar ADB: " + lista.texto());
            List<String> dispositivos = new ArrayList<>();
            for (String linea : lista.texto().split("\\R")) {
                String[] campos = linea.trim().split("\\s+");
                if (campos.length >= 2 && campos[1].equals("device")) dispositivos.add(campos[0]);
            }
            if (dispositivos.isEmpty()) {
                System.out.println("[DebugView] No se activó: conecta un emulador o autoriza la depuración USB y vuelve a ejecutar debug.");
                return;
            }
            int activados = 0;
            for (String serial : dispositivos) {
                Resultado cambio = adb(ejecutable, "-s", serial, "shell", "setprop",
                    "debug.firebase.analytics.app", paquete);
                if (cambio.codigo() != 0) throw new IOException("El dispositivo rechazó activar DebugView: " + cambio.texto());
                Resultado valor = adb(ejecutable, "-s", serial, "shell", "getprop", "debug.firebase.analytics.app");
                if (valor.codigo() != 0 || !valor.texto().equals(paquete)) {
                    throw new IOException("No se pudo verificar la activación de DebugView en el dispositivo.");
                }
                activados++;
            }
            System.out.println("[DebugView] Activado y verificado para " + paquete + " en " + activados + " dispositivo(s). Abre la app y Firebase Analytics > DebugView.");
        } catch (Exception error) {
            System.err.println("[DebugView] No se completó la activación: " + error.getMessage());
            System.exit(1);
        }
    }
}
