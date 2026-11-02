package grup1.server;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClientTestCLI extends WebSocketClient {
    private CountDownLatch pruebasTerminadas = new CountDownLatch(2);

    private boolean pingCorrecto = false;
    private boolean bounceCorrecto = false;

    public ClientTestCLI(String direccion) throws Exception {
        super(new URI(direccion));
    }

    @Override
    public void onOpen(ServerHandshake handshake) {

        System.out.println("Conectado al servidor WebSocket.");
        System.out.println("--------------------------------");

        // Prueba 1: PING
        JSONObject ping = new JSONObject();
        ping.put("type", "ping");

        System.out.println("Enviando PING...");
        send(ping.toString());

        // Prueba 2: BOUNCE
        JSONObject bounce = new JSONObject();
        bounce.put("type", "bounce");
        bounce.put("message", "Hola servidor");

        System.out.println("Enviando BOUNCE...");
        send(bounce.toString());
    }

    @Override
    public void onMessage(String message) {

        System.out.println("Respuesta recibida: " + message);

        try {

            JSONObject respuesta = new JSONObject(message);
            String type = respuesta.getString("type");

            // Comprobar PONG
            if (type.equals("pong")) {

                pingCorrecto = true;

                System.out.println("PING: CORRECTO");
                pruebasTerminadas.countDown();
            }

            // Comprobar BOUNCE
            else if (type.equals("bounce")) {

                String mensaje = respuesta.getString("message");

                if (mensaje.equals("Hola servidor")) {

                    bounceCorrecto = true;
                    System.out.println("BOUNCE: CORRECTO");

                } else {

                    System.out.println("BOUNCE: INCORRECTO");
                }

                pruebasTerminadas.countDown();
            }

            else {

                System.out.println("Respuesta desconocida: " + type);
            }

        } catch (Exception e) {

            System.out.println("Error procesando respuesta: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

        System.out.println("Conexión cerrada: " + reason);
    }

    @Override
    public void onError(Exception ex) {

        System.out.println("Error de conexión: " + ex.getMessage());
    }

    public static void main(String[] args) {

        try {

            ClientTestCLI cliente = new ClientTestCLI("ws://localhost:3000");

            System.out.println("Conectando al servidor...");

            cliente.connectBlocking();

            // Esperar las respuestas de las dos pruebas
            boolean completado = cliente.pruebasTerminadas.await(
                5, TimeUnit.SECONDS
            );

            System.out.println();
            System.out.println("========== RESULTADOS ==========");

            System.out.println(
                "PING: " + (cliente.pingCorrecto ? "OK" : "FALLO")
            );

            System.out.println(
                "BOUNCE: " + (cliente.bounceCorrecto ? "OK" : "FALLO")
            );

            if (completado && cliente.pingCorrecto && cliente.bounceCorrecto) {

                System.out.println("RESULTADO FINAL: TODAS LAS PRUEBAS SUPERADAS");

            } else {

                System.out.println("RESULTADO FINAL: ERROR EN LAS PRUEBAS");
            }

            cliente.closeBlocking();

        } catch (Exception e) {

            System.out.println("No se pudo conectar con el servidor.");
            System.out.println("Error: " + e.getMessage());
        }
    }
}