package grup1.server;



import org.json.JSONArray;
import org.json.JSONObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;



public class GestorSeients {


    private final String NOM_BBDD = "cataleg.db";




    public JSONObject consultarSeients(String idEsdeveniment, String idTipus) {
        ExtractorDadesBBDD extractor = new ExtractorDadesBBDD(NOM_BBDD);

        try (Connection connexio = extractor.connectar()) {

            PreparedStatement consulta = connexio.prepareStatement(
                "SELECT s.ID_Seient, s.Fila, s.Columna, s.Estat, " +
                "EXISTS (SELECT 1 FROM ENTRADA e " +
                "        JOIN VENDA v ON v.ID_Venda = e.ID_Venda " +
                "        JOIN TIPUS_ENTRADA t2 ON t2.ID_Tipus_Entrada = e.ID_Tipus_Entrada " +
                "        WHERE e.ID_Seient = s.ID_Seient " +
                "        AND t2.ID_Esdeveniment = ? " +
                "        AND e.Estat IN ('venuda', 'validada') " +
                "        AND v.Estat IN ('pendent', 'confirmada')) AS Ocupat " +
                "FROM SEIENT s " +
                "JOIN TIPUS_ENTRADA t ON t.ID_Zona = s.ID_Zona " +
                "WHERE t.ID_Tipus_Entrada = ? " +
                "ORDER BY s.Fila, s.Columna"
            );
            consulta.setString(1, idEsdeveniment);
            consulta.setString(2, idTipus);
            ResultSet resultat = consulta.executeQuery();

            JSONArray llista = new JSONArray();
            while (resultat.next()) {
                String estat = "lliure";
                if (resultat.getString("Estat").equals("bloquejat")) {
                    estat = "bloquejat";
                } else if (resultat.getInt("Ocupat") == 1) {
                    estat = "ocupat";
                }

                JSONObject seient = new JSONObject();
                seient.put("id", resultat.getString("ID_Seient"));
                seient.put("fila", resultat.getInt("Fila"));
                seient.put("columna", resultat.getInt("Columna"));
                seient.put("estat", estat);
                llista.put(seient);
            }

            JSONObject resposta = new JSONObject();
            resposta.put("type", "seients");
            resposta.put("tipus", idTipus);
            resposta.put("seients", llista);
            return resposta;

        } catch (Exception e) {
            System.out.println("ERROR consultant els seients: " + e.getMessage());
            JSONObject resposta = new JSONObject();
            resposta.put("type", "error");
            resposta.put("message", "No s'han pogut consultar els seients");
            return resposta;
        }
    }
}