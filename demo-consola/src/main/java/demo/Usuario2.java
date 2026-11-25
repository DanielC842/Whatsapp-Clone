package demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Usuario 2 del chat. Ejecutar despues del Servidor.
 *
 * Se conecta al servidor, envia lo que se escribe en la consola y muestra
 * los mensajes que llegan del otro usuario. Escribe /salir para terminar.
 */
public class Usuario2 {

    private static final String NOMBRE = "Luis";
    private static final String HOST = "localhost";   // IP del PC servidor si es otro equipo
    private static final int PUERTO = 5000;

    public static void main(String[] args) {
        System.out.println("=== CHAT - " + NOMBRE + " (Usuario 2) ===");
        // Al cerrar el socket se cierran tambien sus flujos de entrada y salida.
        try (Socket socket = new Socket(HOST, PUERTO)) {
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            salida.println(NOMBRE);   // lo primero que se envia es el nombre
            System.out.println("Conectado al servidor. Escribe un mensaje y pulsa Enter (/salir para terminar).");

            // Hilo que escucha: imprime todo lo que llega del servidor.
            Thread escucha = new Thread(() -> {
                try {
                    String mensaje;
                    while ((mensaje = entrada.readLine()) != null) {
                        System.out.println(mensaje);
                    }
                    System.out.println("Se perdio la conexion con el servidor.");
                } catch (IOException e) {
                    // el socket se cerro al salir
                }
            });
            escucha.setDaemon(true);
            escucha.start();

            // Hilo principal: lee lo que escribe el usuario y lo envia.
            Scanner teclado = new Scanner(System.in);
            while (teclado.hasNextLine()) {
                String texto = teclado.nextLine().trim();
                if (texto.equalsIgnoreCase("/salir")) {
                    break;
                }
                if (!texto.isEmpty()) {
                    salida.println(texto);
                }
            }
            System.out.println("Saliste del chat.");
        } catch (IOException e) {
            System.out.println("No se pudo conectar al servidor en " + HOST + ":" + PUERTO
                    + ". Ejecuta primero la clase Servidor.");
        }
    }
}
