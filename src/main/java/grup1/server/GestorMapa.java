
package grup1.server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class GestorMapa {

    private final String NOM_BBDD = "cataleg.db";

    // Consultar el mapa de un evento
    public JSONObject consultarMapa(String idEsdeveniment) {

        ExtractorDadesBBDD extractor =
                new ExtractorDadesBBDD(NOM_BBDD);

        try (Connection connexio = extractor.connectar()) {

            // Obtener el recinto del evento
            String idRecinte;

            String sqlEsdeveniment =
                    "SELECT ID_Recinte FROM ESDEVENIMENT " +
                    "WHERE ID_Esdeveniment = ?";

            try (PreparedStatement consulta =
                    connexio.prepareStatement(sqlEsdeveniment)) {

                consulta.setString(1, idEsdeveniment);

                try (ResultSet resultat = consulta.executeQuery()) {

                    if (!resultat.next()) {
                        return error("El evento no existe.");
                    }

                    idRecinte = resultat.getString("ID_Recinte");
                }
            }

            // Obtener las zonas y su ocupación
            JSONArray zonas = new JSONArray();

            String sqlZonas =
                    "SELECT z.ID_Zona, z.Nom, z.Tipus, z.Aforament, " +
                    "(SELECT COUNT(*) " +
                    "FROM ENTRADA e " +
                    "JOIN VENDA v ON v.ID_Venda = e.ID_Venda " +
                    "JOIN TIPUS_ENTRADA t ON t.ID_Tipus_Entrada = e.ID_Tipus_Entrada " +
                    "WHERE t.ID_Esdeveniment = ? " +
                    "AND t.ID_Zona = z.ID_Zona " +
                    "AND v.Estat = 'confirmada' " +
                    "AND e.Estat IN ('venuda', 'validada') " +
                    "AND ((z.Tipus = 'general' AND e.ID_Seient IS NULL) " +
                    "OR (z.Tipus = 'numerada' AND e.ID_Seient IS NOT NULL))" +
                    ") AS Ocupacio " +
                    "FROM ZONA z " +
                    "WHERE z.ID_Recinte = ? " +
                    "ORDER BY z.ID_Zona";

            try (PreparedStatement consulta =
                    connexio.prepareStatement(sqlZonas)) {

                consulta.setString(1, idEsdeveniment);
                consulta.setString(2, idRecinte);

                try (ResultSet resultat = consulta.executeQuery()) {

                    while (resultat.next()) {

                        JSONObject zona = new JSONObject();

                        zona.put("id", resultat.getString("ID_Zona"));
                        zona.put("nombre", resultat.getString("Nom"));
                        zona.put("tipo", resultat.getString("Tipus"));
                        zona.put("aforament", resultat.getInt("Aforament"));
                        zona.put("ocupacion", resultat.getInt("Ocupacio"));

                        zonas.put(zona);
                    }
                }
            }

            // Obtener los asientos de las zonas numeradas
            JSONArray seients = new JSONArray();

            String sqlSeients =
                    "SELECT s.ID_Seient, s.Fila, s.Columna, " +
                    "s.ID_Zona, s.Estat, z.Nom AS Nom_Zona, " +
                    "v.ID_Entrada, v.ID_Venda, v.Nom_Comprador, " +
                    "v.Email_Comprador, v.Preu_Pagament, v.Estat_Entrada " +
                    "FROM SEIENT s " +
                    "JOIN ZONA z ON z.ID_Zona = s.ID_Zona " +
                    "LEFT JOIN (" +
                    "SELECT e.ID_Seient, e.ID_Entrada, e.ID_Venda, " +
                    "e.Preu_Pagament, e.Estat AS Estat_Entrada, " +
                    "v.Nom_Comprador, v.Email_Comprador, t.ID_Zona " +
                    "FROM ENTRADA e " +
                    "JOIN VENDA v ON v.ID_Venda = e.ID_Venda " +
                    "JOIN TIPUS_ENTRADA t ON t.ID_Tipus_Entrada = e.ID_Tipus_Entrada " +
                    "WHERE t.ID_Esdeveniment = ? " +
                    "AND e.ID_Seient IS NOT NULL " +
                    "AND v.Estat = 'confirmada' " +
                    "AND e.Estat IN ('venuda', 'validada')" +
                    ") v ON v.ID_Seient = s.ID_Seient " +
                    "AND v.ID_Zona = s.ID_Zona " +
                    "WHERE z.ID_Recinte = ? " +
                    "AND z.Tipus = 'numerada' " +
                    "ORDER BY z.ID_Zona, s.Fila, s.Columna";

            try (PreparedStatement consulta =
                    connexio.prepareStatement(sqlSeients)) {

                consulta.setString(1, idEsdeveniment);
                consulta.setString(2, idRecinte);

                try (ResultSet resultat = consulta.executeQuery()) {

                    while (resultat.next()) {

                        JSONObject seient = new JSONObject();

                        seient.put("id", resultat.getString("ID_Seient"));
                        seient.put("fila", resultat.getInt("Fila"));
                        seient.put("columna", resultat.getInt("Columna"));
                        seient.put("idZona", resultat.getString("ID_Zona"));
                        seient.put("nombreZona", resultat.getString("Nom_Zona"));
                        seient.put("estat", resultat.getString("Estat"));

                        // Comprobar si el asiento está vendido
                        String idEntrada = resultadoId(
                                resultat, "ID_Entrada"
                        );

                        boolean vendido = idEntrada != null;

                        seient.put("vendido", vendido);

                        // Añadir los datos de la venta si existe
                        if (vendido) {

                            JSONObject venta = new JSONObject();

                            venta.put("localizador", idEntrada);
                            venta.put("idVenta", resultat.getString("ID_Venda"));
                            venta.put("comprador", resultat.getString("Nom_Comprador"));
                            venta.put("correo", resultat.getString("Email_Comprador"));
                            venta.put("importe", resultat.getDouble("Preu_Pagament"));
                            venta.put("estadoEntrada", resultat.getString("Estat_Entrada"));

                            seient.put("venta", venta);

                        } else {

                            seient.put("venta", JSONObject.NULL);
                        }

                        seients.put(seient);
                    }
                }
            }


            // Consultar las entradas vendidas del evento
            JSONArray entradesVenudes = consultarEntradasVendidas(connexio, idEsdeveniment);
            // Crear respuesta final
            JSONObject respuesta = new JSONObject();

            respuesta.put("type", "mapa");
            respuesta.put("esdeveniment", idEsdeveniment);
            respuesta.put("recinte", idRecinte);
            respuesta.put("zonas", zonas);
            respuesta.put("seients", seients);
            respuesta.put("entradesVenudes", entradesVenudes);

            return respuesta;

        } catch (Exception e) {

            System.out.println(
                    "ERROR consultando el mapa: " + e.getMessage()
            );

            return error("No se pudo consultar el mapa.");
        }
    }

    // Consultar las entradas vendidas de un evento
    private JSONArray consultarEntradasVendidas(
            Connection connexio,
            String idEsdeveniment
    ) throws Exception {

        String sql = """
                SELECT
                    v.Data_Reserva AS fechaHoraCompra,
                    e.ID_Entrada AS localizador,
                    t.Nom AS tipoEntrada,
                    COALESCE(s.ID_Seient, '') AS asiento,
                    v.Nom_Comprador AS comprador,
                    e.Estat AS estadoEntrada

                FROM ENTRADA e

                JOIN VENDA v
                    ON v.ID_Venda = e.ID_Venda

                JOIN TIPUS_ENTRADA t
                    ON t.ID_Tipus_Entrada = e.ID_Tipus_Entrada

                LEFT JOIN SEIENT s
                    ON s.ID_Seient = e.ID_Seient

                WHERE t.ID_Esdeveniment = ?
                AND v.Estat = 'confirmada'
                AND e.Estat IN ('venuda', 'validada')

                ORDER BY v.Data_Reserva ASC, e.ID_Entrada ASC
                """;

        JSONArray entradas = new JSONArray();

        try (PreparedStatement consulta = connexio.prepareStatement(sql)) {

            consulta.setString(1, idEsdeveniment);

            try (ResultSet resultado = consulta.executeQuery()) {

                while (resultado.next()) {

                    JSONObject entrada = new JSONObject();

                    entrada.put(
                            "fechaHoraCompra",
                            resultado.getString("fechaHoraCompra")
                    );

                    entrada.put(
                            "localizador",
                            resultado.getString("localizador")
                    );

                    entrada.put(
                            "tipoEntrada",
                            resultado.getString("tipoEntrada")
                    );

                    entrada.put(
                            "asiento",
                            resultado.getString("asiento")
                    );

                    entrada.put(
                            "comprador",
                            resultado.getString("comprador")
                    );

                    entrada.put(
                            "estadoEntrada",
                            resultado.getString("estadoEntrada")
                    );

                    entradas.put(entrada);
                }
            }
        }

        return entradas;
    }

    // Obtener un identificador permitiendo valores nulos
    private String resultadoId(ResultSet resultado, String campo)
            throws Exception {

        return resultado.getString(campo);
    }

    // Crear respuesta de error
    private JSONObject error(String mensaje) {

        JSONObject respuesta = new JSONObject();

        respuesta.put("type", "error");
        respuesta.put("message", mensaje);

        return respuesta;
    }
}
