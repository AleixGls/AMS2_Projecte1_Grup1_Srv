package grup1.server;

import org.json.JSONObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class GestorValidacio {

    // Constants
    private final String NOM_BBDD = "cataleg.db";

    // Metodes
    public JSONObject validarEntrada(String idEntrada) {

        ExtractorDadesBBDD extractor =
                new ExtractorDadesBBDD(NOM_BBDD);

        try (Connection connexio = extractor.connectar()) {

            // Iniciar una transaccion
            connexio.setAutoCommit(false);

            // Consultar el estado de la entrada
            String sql = "SELECT Estat FROM ENTRADA WHERE ID_Entrada = ?";

            try (PreparedStatement consulta = connexio.prepareStatement(sql)) {

                consulta.setString(1, idEntrada);

                try (ResultSet resultat = consulta.executeQuery()) {

                    // Comprobar si la entrada existe
                    if (!resultat.next()) {
                        connexio.rollback();
                        return error("La entrada no se encuentra en la base de datos.");
                    }

                    // Obtener el estado actual
                    String estat = resultat.getString("Estat");

                    // Comprobar el estado
                    switch (estat) {

                        case "venuda":

                            // Actualizar solamente si sigue estando vendida
                            String sqlUpdate =
                                    "UPDATE ENTRADA SET Estat = 'validada' " +
                                    "WHERE ID_Entrada = ? AND Estat = 'venuda'";

                            try (PreparedStatement actualitzacio =
                                    connexio.prepareStatement(sqlUpdate)) {

                                actualitzacio.setString(1, idEntrada);

                                int filesModificades = actualitzacio.executeUpdate();

                                // Comprobar si se ha actualizado correctamente
                                if (filesModificades == 1) {

                                    connexio.commit();

                                    JSONObject resposta = new JSONObject();
                                    resposta.put("type", "validacioOk");
                                    resposta.put("message", "La entrada se ha validado correctamente.");

                                    return resposta;

                                } else {

                                    connexio.rollback();

                                    return error("No se ha podido validar la entrada. Puede que ya haya sido validada.");
                                }
                            }

                        case "validada":

                            connexio.rollback();

                            return error("La entrada ya había sido validada anteriormente. No es válida.");

                        case "reemborsada":

                            connexio.rollback();

                            return error("La entrada no es válida porque ha sido reembolsada.");

                        default:

                            connexio.rollback();

                            return error("La entrada tiene un estado desconocido.");
                    }
                }
            }

        } catch (Exception e) {

            System.out.println("ERROR validando la entrada: " + e.getMessage());

            return error("No se ha podido validar la entrada.");
        }
    }


    // Crear una respuesta de error
    private JSONObject error(String mensaje) {

        JSONObject resposta = new JSONObject();

        resposta.put("type", "validacioError");
        resposta.put("message", mensaje);

        return resposta;
    }
}