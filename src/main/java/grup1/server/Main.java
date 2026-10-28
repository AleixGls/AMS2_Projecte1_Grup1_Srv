package grup1.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;

public class Main extends WebSocketServer {

    // Constructor
    public Main(int puerto) {
        super(new InetSocketAddress(puerto));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Cliente conectado: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Cliente desconectado: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Mensaje recibido: " + message);

        try {
            JSONObject obj = new JSONObject(message);

            String type = obj.getString("type");

            System.out.println("Tipo: " + type);

            if (type.equals("ping")) {

                System.out.println("PING recibido");

                JSONObject respuesta = new JSONObject();
                respuesta.put("type", "pong");

                conn.send(respuesta.toString());

                System.out.println("PONG enviado");
            }

            else if (type.equals("bounce")) {

                System.out.println("BOUNCE recibido");

                JSONObject respuesta = new JSONObject();
                respuesta.put("type", "bounce");
                respuesta.put("message", obj.optString("message", ""));

                conn.send(respuesta.toString());

                System.out.println("BOUNCE enviado: " + respuesta);
            }

            else {
                System.out.println("Tipo de mensaje desconocido: " + type);

                JSONObject respuesta = new JSONObject();
                respuesta.put("type", "error");
                respuesta.put("message", "Tipo de mensaje desconocido");

                conn.send(respuesta.toString());
            }

        } catch (Exception e) {

            System.out.println("Error procesando JSON: " + e.getMessage());

            JSONObject respuesta = new JSONObject();
            respuesta.put("type", "error");
            respuesta.put("message", "JSON invalido");

            conn.send(respuesta.toString());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("Error del servidor: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Servidor iniciado en el puerto " + getPort());
    }

    // Metodo main para iniciar el servidor
    public static void main(String[] args) {
        Servidor servidor = new Servidor(3000);
        servidor.start();

        System.out.println("Esperando conexiones...");
    }
}