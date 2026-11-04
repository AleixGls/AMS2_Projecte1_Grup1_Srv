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
import java.util.UUID;



public class GestorCompres {

    // Atributs
    // Constants
    private final String DIRECTORI_ENTRADES =
        System.getProperty("user.dir") + File.separator + "entrades";



    // Metodes
    // Processa una peticio de compra i retorna la resposta per enviar al client
    public synchronized JSONObject processarCompra(JSONObject peticio) {
        try {

            // Llegir les dades del comprador i les entrades demanades
            String nom = peticio.getString("nom");
            String correu = peticio.getString("correu");
            JSONArray entrades = peticio.getJSONArray("entrades");

            // Llegir el cataleg per saber les quotes de cada tipus d'entrada
            ExtractorDadesXML extractor = new ExtractorDadesXML("ESDEVENIMENTS.XML");
            Cataleg cataleg = extractor.processarFitxerXML();
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
                    String localitzador = idEsdeveniment + "-" + idTipus + "-" +
                        UUID.randomUUID().toString().substring(0, 8).toUpperCase();

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

    // Crea una resposta d'error per enviar al client
    private JSONObject error(String missatge) {
        JSONObject resposta = new JSONObject();
        resposta.put("type", "compraError");
        resposta.put("message", missatge);
        return resposta;
    }
}
