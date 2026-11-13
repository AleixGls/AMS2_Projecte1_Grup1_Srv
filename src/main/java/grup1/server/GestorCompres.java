package grup1.server;



import org.json.JSONArray;
import org.json.JSONObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Random;



public class GestorCompres {

    // Atributs
    // Constants
    private final String NOM_BBDD = "cataleg.db";
    private final String LLETRES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final String NUMEROS = "0123456789";

    // Variables
    private Random random = new Random();



    // Metodes
    // Processa una peticio de compra i guarda la venda i les entrades a la base de dades
    public synchronized JSONObject processarCompra(JSONObject peticio) {

        // Llegir les dades del comprador i les entrades demanades
        String nom = peticio.getString("nom");
        String correu = peticio.getString("correu");
        JSONArray entrades = peticio.getJSONArray("entrades");

        ExtractorDadesBBDD extractor = new ExtractorDadesBBDD(NOM_BBDD);
        Connection connexio = null;

        try {
            connexio = extractor.connectar();

            // Fer tota la compra en una sola transaccio: o es guarda tot o no es guarda res
            connexio.setAutoCommit(false);

            // Crear la venda en estat pendent
            String idVenda = generarIdVenda(connexio);
            PreparedStatement insertVenda = connexio.prepareStatement(
                "INSERT INTO VENDA (ID_Venda, Nom_Comprador, Email_Comprador, Data_Reserva, Estat) " +
                "VALUES (?, ?, ?, ?, 'pendent')"
            );
            insertVenda.setString(1, idVenda);
            insertVenda.setString(2, nom);
            insertVenda.setString(3, correu);
            insertVenda.setString(4, LocalDateTime.now().toString());
            insertVenda.executeUpdate();

            // Afegir cada entrada a la venda i generar el seu localitzador
            JSONArray localitzadors = new JSONArray();
            for (int i = 0; i < entrades.length(); i++) {
                JSONObject entrada = entrades.getJSONObject(i);
                String idTipus = entrada.getString("tipus");
                int quantitat = entrada.getInt("quantitat");

                // Comprovar que el tipus existeix i que es de zona general
                double preu = obtenirPreuGeneral(connexio, idTipus);
                if (preu < 0) {
                    connexio.rollback();
                    return error("L'entrada " + idTipus + " no existeix o no es general");
                }

                for (int j = 0; j < quantitat; j++) {
                    String localitzador = generarLocalitzador(connexio, localitzadors);

                    PreparedStatement insertEntrada = connexio.prepareStatement(
                        "INSERT INTO ENTRADA (ID_Entrada, ID_Venda, ID_Tipus_Entrada, Preu_Pagament, Estat) " +
                        "VALUES (?, ?, ?, ?, 'venuda')"
                    );
                    insertEntrada.setString(1, localitzador);
                    insertEntrada.setString(2, idVenda);
                    insertEntrada.setString(3, idTipus);
                    insertEntrada.setDouble(4, preu);
                    insertEntrada.executeUpdate();

                    localitzadors.put(localitzador);
                }
            }

            // Confirmar la venda
            PreparedStatement updateVenda = connexio.prepareStatement(
                "UPDATE VENDA SET Estat = 'confirmada' WHERE ID_Venda = ?"
            );
            updateVenda.setString(1, idVenda);
            updateVenda.executeUpdate();

            // Guardar tots els canvis
            connexio.commit();

            // Crear la resposta d'exit amb els localitzadors
            JSONObject resposta = new JSONObject();
            resposta.put("type", "compraOk");
            resposta.put("localitzadors", localitzadors);
            return resposta;

        } catch (Exception e) {
            // Si falla alguna cosa (per exemple, s'ha superat la quota), desfer tota la compra
            System.out.println("ERROR processant la compra: " + e.getMessage());
            try {
                if (connexio != null) {
                    connexio.rollback();
                }
            } catch (Exception ex) {
                System.out.println("ERROR desfent la compra: " + ex.getMessage());
            }
            return error("No s'ha pogut fer la compra. Potser no queden prou entrades.");

        } finally {
            try {
                if (connexio != null) {
                    connexio.close();
                }
            } catch (Exception e) {
                System.out.println("ERROR tancant la connexio: " + e.getMessage());
            }
        }
    }

    // Retorna les entrades comprades amb un correu
    public JSONObject consultarEntrades(String correu) {
        ExtractorDadesBBDD extractor = new ExtractorDadesBBDD(NOM_BBDD);

        try (Connection connexio = extractor.connectar()) {

            // Buscar les entrades de les vendes confirmades d'aquest correu
            PreparedStatement consulta = connexio.prepareStatement(
                "SELECT e.ID_Entrada, t.ID_Esdeveniment, e.ID_Tipus_Entrada, v.Nom_Comprador " +
                "FROM ENTRADA e " +
                "JOIN VENDA v ON v.ID_Venda = e.ID_Venda " +
                "JOIN TIPUS_ENTRADA t ON t.ID_Tipus_Entrada = e.ID_Tipus_Entrada " +
                "WHERE v.Email_Comprador = ? AND v.Estat = 'confirmada' " +
                "ORDER BY v.Data_Reserva DESC"
            );
            consulta.setString(1, correu);
            ResultSet resultat = consulta.executeQuery();

            // Crear la llista d'entrades per enviar a l'app
            JSONArray llista = new JSONArray();
            while (resultat.next()) {
                JSONObject entrada = new JSONObject();
                entrada.put("localitzador", resultat.getString("ID_Entrada"));
                entrada.put("esdeveniment", resultat.getString("ID_Esdeveniment"));
                entrada.put("tipus", resultat.getString("ID_Tipus_Entrada"));
                llista.put(entrada);
            }

            JSONObject resposta = new JSONObject();
            resposta.put("type", "lesMevesEntrades");
            resposta.put("entrades", llista);
            return resposta;

        } catch (Exception e) {
            System.out.println("ERROR consultant les entrades: " + e.getMessage());
            JSONObject resposta = new JSONObject();
            resposta.put("type", "error");
            resposta.put("message", "No s'han pogut consultar les entrades");
            return resposta;
        }
    }

    // Retorna el preu d'un tipus d'entrada si es de zona general, o -1 si no ho es
    private double obtenirPreuGeneral(Connection connexio, String idTipus) throws Exception {
        PreparedStatement consulta = connexio.prepareStatement(
            "SELECT t.Preu FROM TIPUS_ENTRADA t " +
            "JOIN ZONA z ON z.ID_Zona = t.ID_Zona " +
            "WHERE t.ID_Tipus_Entrada = ? AND z.Tipus = 'general'"
        );
        consulta.setString(1, idTipus);
        ResultSet resultat = consulta.executeQuery();

        if (resultat.next()) {
            return resultat.getDouble("Preu");
        }
        return -1;
    }

    // Genera un identificador de venda de 6 caracters que no existeixi (ex: V48213)
    private String generarIdVenda(Connection connexio) throws Exception {
        String idVenda = "V" + generarText(NUMEROS, 5);
        while (existeix(connexio, "SELECT 1 FROM VENDA WHERE ID_Venda = ?", idVenda)) {
            idVenda = "V" + generarText(NUMEROS, 5);
        }
        return idVenda;
    }

    // Genera un localitzador de 3 lletres i 3 numeros que no existeixi (ex: ABC123)
    private String generarLocalitzador(Connection connexio, JSONArray generats) throws Exception {
        String localitzador = generarText(LLETRES, 3) + generarText(NUMEROS, 3);
        while (existeix(connexio, "SELECT 1 FROM ENTRADA WHERE ID_Entrada = ?", localitzador)
                || jaGenerat(generats, localitzador)) {
            localitzador = generarText(LLETRES, 3) + generarText(NUMEROS, 3);
        }
        return localitzador;
    }

    // Genera un text aleatori amb els caracters indicats
    private String generarText(String caracters, int longitud) {
        String text = "";
        for (int i = 0; i < longitud; i++) {
            text = text + caracters.charAt(random.nextInt(caracters.length()));
        }
        return text;
    }

    // Comprova si una consulta troba algun resultat amb el valor indicat
    private boolean existeix(Connection connexio, String sql, String valor) throws Exception {
        PreparedStatement consulta = connexio.prepareStatement(sql);
        consulta.setString(1, valor);
        ResultSet resultat = consulta.executeQuery();
        return resultat.next();
    }

    // Comprova si un localitzador ja s'ha generat en aquesta mateixa compra
    private boolean jaGenerat(JSONArray generats, String localitzador) {
        for (int i = 0; i < generats.length(); i++) {
            if (generats.getString(i).equals(localitzador)) {
                return true;
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