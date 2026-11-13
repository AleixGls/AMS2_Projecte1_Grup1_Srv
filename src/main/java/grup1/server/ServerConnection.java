package grup1.server;



import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;
import grup1.server.objectesDades.Cataleg;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class ServerConnection extends WebSocketServer {

    // Atributs
    private Map<WebSocket, String> catalogosClientes = new ConcurrentHashMap<>();
    private GestorCompres gestorCompres = new GestorCompres();
    private GestorMapa gestorMapa = new GestorMapa();


    
    // Constructor
    public ServerConnection(int puerto) {
        super(new InetSocketAddress(puerto));
    }


    
    // Metodes
    private boolean actualitzarCataleg(WebSocket conn) {
        try {

            // Crear el extractor i obtenir el objecte Cataleg
            ExtractorDadesBBDD extractor = new ExtractorDadesBBDD("cataleg.db");
Cataleg cataleg = extractor.processarBBDD();

            // Comprovar que s'ha pogut llegir el XML
            if (cataleg == null) {
                System.out.println("ERROR: No s'ha pogut llegir el cataleg XML.");
                return false;
            }

            // Crear el conversor i obtenir el String JSON
            ConversorDadesJSON conversor = new ConversorDadesJSON(cataleg);
            String catalegJSON = conversor.processarObjecteCataleg();

            // Comprovar que s'ha pogut convertir el cataleg a JSON
            if (catalegJSON == null) {
                System.out.println("ERROR: No s'ha pogut convertir el cataleg a JSON.");
                return false;
            }

            // Guardar el cataleg JSON associat al client
            catalogosClientes.put(conn, catalegJSON);

            System.out.println("Cataleg actualitzat correctament.");

            return true;

        } catch (Exception e) {
            System.out.println("ERROR actualitzant el cataleg: " + e.getMessage());
            return false;
        }
    }

    @Override
    // Metode per quan un client es conecta
    public void onOpen(WebSocket conn, ClientHandshake handshake) {

        System.out.println("Cliente conectado: " + conn.getRemoteSocketAddress());

        // Crear i guardar el cataleg del client
        actualitzarCataleg(conn);

        System.out.println("Cataleg generat per al client: " + conn.getRemoteSocketAddress());
    }

    @Override
    // Metode per quan un client es desconecta
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {

        System.out.println("Cliente desconectado: " + conn.getRemoteSocketAddress());

        // Eliminar el cataleg del client
        catalogosClientes.remove(conn);

        System.out.println("Cataleg del client eliminat");
    }

    @Override
    // Metode per gestionar els missatges rebuts al servidor
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

            // Peticio per obtenir el cataleg i donarlo al client
            else if (type.equals("cataleg")) {

                System.out.println("CATALEG solicitado");

                // Actualitzar el cataleg del client
                if (actualitzarCataleg(conn)) {

                    // Obtenir el cataleg JSON del client
                    String catalegJSON = catalogosClientes.get(conn);

                    // Crear la resposta amb el type catalog
                    JSONObject respuesta = new JSONObject();
                    respuesta.put("type", "catalog");

                    // Afegir el catalog a la resposta
                    respuesta.put("catalog", new JSONObject(catalegJSON));

                    // Enviar el cataleg al client
                    conn.send(respuesta.toString());

                    System.out.println("CATALEG enviat");

                } else {

                    // Enviar un missatge d'error al client
                    JSONObject respuesta = new JSONObject();
                    respuesta.put("type", "error");
                    respuesta.put("message", "No s'ha pogut actualitzar el cataleg");

                    conn.send(respuesta.toString());
                }
            }

            // Peticio de compra d'entrades generals
            else if (type.equals("compra")) {

                System.out.println("COMPRA solicitada");

                // Processar la compra i enviar la resposta al client
                JSONObject respuesta = gestorCompres.processarCompra(obj);
                conn.send(respuesta.toString());

                System.out.println("COMPRA resposta: " + respuesta);
            }

            // Peticio de les entrades comprades amb un correu
            else if (type.equals("lesMevesEntrades")) {

                System.out.println("LES MEVES ENTRADES solicitades");

                // Consultar les entrades del correu i enviar-les al client
                JSONObject respuesta = gestorCompres.consultarEntrades(obj.getString("correu"));
                conn.send(respuesta.toString());

                System.out.println("LES MEVES ENTRADES enviades");
            }

            else if (type.equals("mapa")) {
                System.out.println("MAPA solicitado");

                String idEsdeveniment = obj.getString("esdeveniment");

                // Consultar el mapa en SQLite
                JSONObject respuesta = gestorMapa.consultarMapa(idEsdeveniment);

                respuesta.put("esdeveniment", idEsdeveniment);

                // Enviar los datos al cliente
                conn.send(respuesta.toString());

                System.out.println("MAPA respuesta: " + respuesta);
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
}