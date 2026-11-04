package grup1.server;



import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;



public class ClientTestCLI extends WebSocketClient {
    
    // Atributs
    private CountDownLatch pruebasTerminadas = new CountDownLatch(3);
    private boolean pingCorrecto = false;
    private boolean bounceCorrecto = false;
    private boolean catalegCorrecte = false;



    // Constructor
    public ClientTestCLI(String direccion) throws Exception {
        super(new URI(direccion));
    }



    // Metodes
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

        // Prueba 3: SOLICITAR CATALEG
        JSONObject cataleg = new JSONObject();
        cataleg.put("type", "cataleg");

        System.out.println("Solicitando CATALEG...");
        send(cataleg.toString());
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
        
            // Comprobar CATALOG
            else if (type.equals("catalog")) {
            
                System.out.println("CATALEG recibido correctamente");
            
                // Obtener el objeto catalog
                JSONObject cataleg = respuesta.getJSONObject("catalog");
            
                // Obtener los arrays del catálogo
                JSONArray esdeveniments =
                    cataleg.getJSONArray("esdeveniments");
            
                JSONArray recintes =
                    cataleg.getJSONArray("recintes");
            
                if (esdeveniments.length() > 0) {
                
                    // Obtener el primer evento
                    JSONObject primerEsdeveniment =
                        esdeveniments.getJSONObject(0);
                
                    System.out.println("\n========== PRIMER ESDEVENIMENT ==========");
                
                    System.out.println("Nom: " +
                        primerEsdeveniment.getString("nom"));
                
                    System.out.println("ID: " +
                        primerEsdeveniment.getString("id"));
                
                    // Obtener el identificador del recinto
                    String idRecinte =
                        primerEsdeveniment.getString("recinte");
                
                    // Buscar el recinto correspondiente
                    boolean recinteTrobat = false;
                
                    for (int i = 0; i < recintes.length(); i++) {
                    
                        JSONObject recinte = recintes.getJSONObject(i);
                    
                        if (recinte.getString("id").equals(idRecinte)) {
                        
                            System.out.println("\n========== RECINTE ==========");
                        
                            System.out.println("Nom: " +
                                recinte.getString("nom"));
                        
                            System.out.println("ID: " +
                                recinte.getString("id"));
                        
                            recinteTrobat = true;
                        
                            break;
                        }
                    }
                
                    if (!recinteTrobat) {
                    
                        System.out.println(
                            "No se ha encontrado el recinto del evento."
                        );
                    }
                
                    catalegCorrecte = true;
                
                } else {
                
                    System.out.println("El catálogo no contiene eventos.");
                }
            
                pruebasTerminadas.countDown();
            }
        
            // Comprobar ERROR
            else if (type.equals("error")) {
            
                System.out.println("ERROR DEL SERVIDOR: " +
                    respuesta.getString("message"));
            
                pruebasTerminadas.countDown();
            }
        
            // Tipo de mensaje desconocido
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

            System.out.println(
                "CATALEG: " + (cliente.catalegCorrecte ? "OK" : "FALLO")
            );

            if (
                completado && 
                cliente.pingCorrecto && 
                cliente.bounceCorrecto &&
                cliente.catalegCorrecte
            ) {

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