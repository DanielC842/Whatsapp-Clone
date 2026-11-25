package demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servidor del chat. Se ejecuta primero.
 *
 * Acepta conexiones por sockets TCP en el puerto 5000. Cada usuario que se conecta
 * se atiende en su propio hilo, y todo mensaje que llega se reenvia a los demas usuarios.
 */
public class Servidor {

    public static final int PUERTO = 5000;

    /** Usuarios conectados en este momento (lista segura para varios hilos). */
    private static final List<ClienteConectado> clientes = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("=== SERVIDOR DE CHAT ===");
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Escuchando en el puerto " + PUERTO + ". Ahora ejecuta Usuario1 y Usuario2.");
            while (true) {
                Socket socket = servidor.accept();          // espera a que alguien se conecte
                ClienteConectado cliente = new ClienteConectado(socket);
                new Thread(cliente).start();                 // un hilo por usuario
            }
        } catch (IOException e) {
            System.out.println("No se pudo iniciar el servidor: " + e.getMessage());
            System.out.println("Puede que el servidor ya este abierto en otra consola.");
        }
    }

    /** Envia un mensaje a todos los usuarios menos al que lo escribio. */
    static void reenviar(String mensaje, ClienteConectado remitente) {
        for (ClienteConectado c : clientes) {
            if (c != remitente) {
                c.enviar(mensaje);
            }
        }
    }

    static String hora() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /** Representa a un usuario conectado al servidor. */
    static class ClienteConectado implements Runnable {

        private final Socket socket;
        private PrintWriter salida;
        private String nombre;

        ClienteConectado(Socket socket) {
            this.socket = socket;
        }

        void enviar(String mensaje) {
            salida.println(mensaje);
        }

        @Override
        public void run() {
            try (socket;
                 BufferedReader entrada = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

                // La primera linea que envia el usuario es su nombre.
                nombre = entrada.readLine();
                if (nombre == null) {
                    return;
                }
                clientes.add(this);
                System.out.println("[" + hora() + "] " + nombre + " se conecto. Usuarios en linea: " + clientes.size());
                reenviar("*** " + nombre + " entro al chat ***", this);

                // Despues, cada linea es un mensaje de chat.
                String linea;
                while ((linea = entrada.readLine()) != null) {
                    String mensaje = "[" + hora() + "] " + nombre + ": " + linea;
                    System.out.println(mensaje);             // registro en la consola del servidor
                    reenviar(mensaje, this);
                }
            } catch (IOException e) {
                // el usuario cerro la conexion de forma brusca
            } finally {
                if (clientes.remove(this)) {
                    System.out.println("[" + hora() + "] " + nombre + " se desconecto. Usuarios en linea: " + clientes.size());
                    reenviar("*** " + nombre + " salio del chat ***", this);
                }
            }
        }
    }
}
