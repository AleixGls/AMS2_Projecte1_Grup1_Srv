package grup1.server;



import grup1.server.objectesDades.Cataleg;
import grup1.server.objectesDades.Esdeveniment;
import grup1.server.objectesDades.Recinte;
import grup1.server.objectesDades.TipusEntrada;
import grup1.server.objectesDades.Zona;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.nio.file.Files;
import java.util.Random;



public class GestorCompres {

    // Atributs
    // Constants
    private final String DIRECTORI_ENTRADES =
        System.getProperty("user.dir") + File.separator + "entrades";
    private final String LLETRES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final String NUMEROS = "0123456789";

    // Variables
    private Random random = new Random();



    // Metodes
    // Processa una peticio de compra i retorna la resposta per enviar al client
    public synchronized JSONObject processarCompra(JSONObject peticio) {
        try {

            // Llegir les dades del comprador i les entrades demanades
            String nom = peticio.getString("nom");
            String correu = peticio.getString("correu");
            JSONArray entrades = peticio.getJSONArray("entrades");

            // Llegir el cataleg per saber les quotes de cada tipus d'entrada
            ExtractorDadesBBDD extractor = new ExtractorDadesBBDD("cataleg.db");
Cataleg cataleg = extractor.processarBBDD();
            if (cataleg == null) {
                return error("No s'ha pogut llegir el cataleg");
            }

            // Primer comprovar totes les entrades, sense guardar res
            for (int i = 0; i < entrades.length(); i++) {
                JSONObject entrada = entrades.getJSONObject(i);
                String idEsdeveniment = entrada.getString("esdeveniment");
                String idTipus = entrada.getString("tipus");
                int quantitat = entrada.getInt("quantitat");

                TipusEntrada tipus = buscarTipus(cataleg, idEsdeveniment, idTipus);
                if (tipus == null) {
                    return error("No existeix l'entrada " + idTipus + " de " + idEsdeveniment);
                }

                if (!esGeneral(cataleg, tipus.getZona())) {
                    return error("L'entrada " + tipus.getNom() + " no es general");
                }

                JSONArray venudes = llegirEntrades(idEsdeveniment);
                int ocupades = comptarTipus(venudes, idTipus);
                if (ocupades + quantitat > tipus.getQuota()) {
                    return error("No queden prou entrades de tipus " + tipus.getNom());
                }
            }

            // Si tot es correcte, guardar les entrades i generar els localitzadors
            JSONArray localitzadors = new JSONArray();
            for (int i = 0; i < entrades.length(); i++) {
                JSONObject entrada = entrades.getJSONObject(i);
                String idEsdeveniment = entrada.getString("esdeveniment");
                String idTipus = entrada.getString("tipus");
                int quantitat = entrada.getInt("quantitat");

                JSONArray venudes = llegirEntrades(idEsdeveniment);
                for (int j = 0; j < quantitat; j++) {
                    // Generar localitzadors fins que en surti un que no existeixi
                    String localitzador = generarLocalitzador();
                    while (existeixLocalitzador(localitzador, localitzadors)) {
                        localitzador = generarLocalitzador();
                    }

                    JSONObject nova = new JSONObject();
                    nova.put("tipusEntrada", idTipus);
                    nova.put("localitzador", localitzador);
                    nova.put("nom", nom);
                    nova.put("correu", correu);
                    venudes.put(nova);

                    localitzadors.put(localitzador);
                }
                guardarEntrades(idEsdeveniment, venudes);
            }

            // Crear la resposta d'exit amb els localitzadors
            JSONObject resposta = new JSONObject();
            resposta.put("type", "compraOk");
            resposta.put("localitzadors", localitzadors);
            return resposta;

        } catch (Exception e) {
            System.out.println("ERROR processant la compra: " + e.getMessage());
            return error("Error processant la compra");
        }
    }

    // Busca un tipus d'entrada dins d'un esdeveniment del cataleg
    private TipusEntrada buscarTipus(Cataleg cataleg, String idEsdeveniment, String idTipus) {
        for (Esdeveniment esdeveniment : cataleg.getEsdeveniments()) {
            if (esdeveniment.getId().equals(idEsdeveniment)) {
                for (TipusEntrada tipus : esdeveniment.getTipusEntrades()) {
                    if (tipus.getId().equals(idTipus)) {
                        return tipus;
                    }
                }
            }
        }
        return null;
    }

    // Comprova si una zona es de tipus general
    private boolean esGeneral(Cataleg cataleg, String idZona) {
        for (Recinte recinte : cataleg.getRecintes()) {
            for (Zona zona : recinte.getZones()) {
                if (zona.getId().equals(idZona)) {
                    return zona.getTipus().equalsIgnoreCase("general");
                }
            }
        }
        return false;
    }

    // Compta quantes entrades d'un tipus hi ha venudes
    private int comptarTipus(JSONArray venudes, String idTipus) {
        int total = 0;
        for (int i = 0; i < venudes.length(); i++) {
            if (venudes.getJSONObject(i).getString("tipusEntrada").equals(idTipus)) {
                total++;
            }
        }
        return total;
    }

    // Llegeix el fitxer <id_esdeveniment>.json, o retorna una llista buida si no existeix
    private JSONArray llegirEntrades(String idEsdeveniment) throws Exception {
        File fitxer = new File(DIRECTORI_ENTRADES, idEsdeveniment + ".json");
        if (!fitxer.exists()) {
            return new JSONArray();
        }
        return new JSONArray(Files.readString(fitxer.toPath()));
    }

    // Guarda la llista d'entrades al fitxer <id_esdeveniment>.json
    private void guardarEntrades(String idEsdeveniment, JSONArray venudes) throws Exception {
        File directori = new File(DIRECTORI_ENTRADES);
        directori.mkdirs();
        File fitxer = new File(directori, idEsdeveniment + ".json");
        Files.writeString(fitxer.toPath(), venudes.toString(4));
    }

    // Genera un localitzador de 3 lletres i 3 numeros (ex: ABC123)
    private String generarLocalitzador() {
        String localitzador = "";
        for (int i = 0; i < 3; i++) {
            localitzador = localitzador + LLETRES.charAt(random.nextInt(LLETRES.length()));
        }
        for (int i = 0; i < 3; i++) {
            localitzador = localitzador + NUMEROS.charAt(random.nextInt(NUMEROS.length()));
        }
        return localitzador;
    }

    // Comprova si un localitzador ja existeix en algun fitxer d'entrades o en la compra actual
    private boolean existeixLocalitzador(String localitzador, JSONArray generats) throws Exception {

        // Comprovar els localitzadors generats en aquesta mateixa compra
        for (int i = 0; i < generats.length(); i++) {
            if (generats.getString(i).equals(localitzador)) {
                return true;
            }
        }

        // Comprovar tots els fitxers d'entrades de tots els esdeveniments
        File directori = new File(DIRECTORI_ENTRADES);
        File[] fitxers = directori.listFiles();
        if (fitxers == null) {
            return false;
        }

        for (File fitxer : fitxers) {
            if (fitxer.getName().endsWith(".json")) {
                JSONArray venudes = new JSONArray(Files.readString(fitxer.toPath()));
                for (int i = 0; i < venudes.length(); i++) {
                    if (venudes.getJSONObject(i).getString("localitzador").equals(localitzador)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // Crea una resposta d'error per enviar al client
    private JSONObject error(String missatge) {
        JSONObject resposta = new JSONObject();
        resposta.put("type", "compraError");
        resposta.put("message", missatge);
        return resposta;
    }
}