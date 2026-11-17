package grup1.server;



import grup1.server.objectesDades.Cataleg;
import grup1.server.objectesDades.Esdeveniment;
import grup1.server.objectesDades.Recinte;
import grup1.server.objectesDades.TipusEntrada;
import grup1.server.objectesDades.Zona;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;



public class ExtractorDadesBBDD {

    // Atributs
    // Constants
    private final String DIRECTORI_USUARI = System.getProperty("user.dir");
    private final String DIRECTORI_RESOURCES =
        DIRECTORI_USUARI + File.separator +
        "src" + File.separator +
        "main" + File.separator +
        "resources";
    private final String DIRECTORI_FOTOS =
        DIRECTORI_RESOURCES + File.separator +
        "images";

    // Variables
    private File arxiuBBDD;



    // Constructor
    public ExtractorDadesBBDD(
        String nomArxiuBBDD
    ) {
        this.arxiuBBDD = new File(
            DIRECTORI_RESOURCES + File.separator + nomArxiuBBDD
        );
    }



    // Metodes
    // Obre una connexio amb la base de dades SQLite
    public Connection connectar() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + arxiuBBDD.getAbsolutePath());
    }

    private String convertirImatgeBase64(String nomImatge) {
        try {
            // Construir la ruta completa de la imatge
            String rutaImatge = DIRECTORI_FOTOS + File.separator + nomImatge;

            // Llegir el contingut de la imatge en bytes
            byte[] imatgeBytes = Files.readAllBytes(Paths.get(rutaImatge));

            // Codificar els bytes en Base64
            String imatgeBase64 = Base64.getEncoder().encodeToString(imatgeBytes);

            return imatgeBase64;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Cataleg processarBBDD() {

        // Comprovar que existeix el fitxer de la base de dades
        if (!arxiuBBDD.exists()) {
            System.out.println("No s'ha pogut accedir a la base de dades: " + arxiuBBDD.getAbsolutePath());
            return null;
        }

        try (Connection connexio = connectar()) {

            // Crear la llista on guardarem els recintes obtinguts
            List<Recinte> llistaRecintes = new ArrayList<>();

            // Obtenir tots els recintes
            PreparedStatement consultaRecintes = connexio.prepareStatement(
                "SELECT ID_Recinte, Nom FROM RECINTE ORDER BY ID_Recinte"
            );
            ResultSet recintes = consultaRecintes.executeQuery();

            // Iterar els recintes per agafar les seves dades
            while (recintes.next()) {

                // Obtenir les dades del recinte actual
                String idRecinte = recintes.getString("ID_Recinte");
                String nomRecinte = recintes.getString("Nom");


                // Crear la llista on guardarem les zones del recinte actual
                List<Zona> llistaZones = new ArrayList<>();

                // Obtenir les zones del recinte actual
                PreparedStatement consultaZones = connexio.prepareStatement(
                    "SELECT ID_Zona, Nom, Tipus, Aforament FROM ZONA WHERE ID_Recinte = ? ORDER BY ID_Zona"
                );
                consultaZones.setString(1, idRecinte);
                ResultSet zones = consultaZones.executeQuery();

                // Iterar les zones per agafar les seves dades
                while (zones.next()) {

                    // Crear l'objecte zona i afegirlo a la llista de zones
                    Zona zonaObjecte = new Zona(
                        zones.getString("ID_Zona"),
                        zones.getString("Nom"),
                        zones.getString("Tipus"),
                        zones.getInt("Aforament")
                    );
                    llistaZones.add(zonaObjecte);
                }


                // Crear l'objecte recinte i afegirlo a la llista de recintes
                Recinte recinteObjecte = new Recinte(
                    idRecinte,
                    nomRecinte,
                    llistaZones
                );
                llistaRecintes.add(recinteObjecte);
            }


            // Crear la llista on guardarem els esdeveniments obtinguts
            List<Esdeveniment> llistaEsdeveniments = new ArrayList<>();

            // Obtenir tots els esdeveniments
            PreparedStatement consultaEsdeveniments = connexio.prepareStatement(
                "SELECT ID_Esdeveniment, ID_Recinte, Nom, Data_Hora, Categoria, Descripcio, Imatge " +
                "FROM ESDEVENIMENT ORDER BY ID_Esdeveniment"
            );
            ResultSet esdeveniments = consultaEsdeveniments.executeQuery();

            // Iterar els esdeveniments per agafar les seves dades
            while (esdeveniments.next()) {

                // Obtenir les dades del esdeveniment actual
                String idEsdeveniment = esdeveniments.getString("ID_Esdeveniment");
                String recinteEsdeveniment = esdeveniments.getString("ID_Recinte");
                String nomEsdeveniment = esdeveniments.getString("Nom");
                LocalDateTime dataHoraEsdeveniment = LocalDateTime.parse(esdeveniments.getString("Data_Hora"));
                String categoriaEsdeveniment = esdeveniments.getString("Categoria");
                String descripcioEsdeveniment = esdeveniments.getString("Descripcio");
                String imatgeEsdeveniment = convertirImatgeBase64(esdeveniments.getString("Imatge"));


                // Crear la llista on guardarem les TipusEntrades del esdeveniment actual
                List<TipusEntrada> llistaTipusEntrades = new ArrayList<>();

                // Obtenir els tipus d'entrada del esdeveniment actual
                PreparedStatement consultaTipus = connexio.prepareStatement(
                    "SELECT ID_Tipus_Entrada, Nom, ID_Zona, Preu, Quota FROM TIPUS_ENTRADA " +
                    "WHERE ID_Esdeveniment = ? ORDER BY ID_Tipus_Entrada"
                );
                consultaTipus.setString(1, idEsdeveniment);
                ResultSet tipusEntrades = consultaTipus.executeQuery();

                // Iterar els tipus d'entrada per agafar les seves dades
                while (tipusEntrades.next()) {

                    // Crear l'objecte tipusEntrada i afegirlo a la llista de tipusEntrades
                    TipusEntrada tipusEntradaObjecte = new TipusEntrada(
                        tipusEntrades.getString("ID_Tipus_Entrada"),
                        tipusEntrades.getString("Nom"),
                        tipusEntrades.getString("ID_Zona"),
                        tipusEntrades.getDouble("Preu"),
                        tipusEntrades.getInt("Quota")
                    );
                    llistaTipusEntrades.add(tipusEntradaObjecte);
                }


                // Crear l'objecte esdeveniment i afegirlo a la llista de esdeveniments
                Esdeveniment esdevenimentObjecte = new Esdeveniment(
                    idEsdeveniment,
                    recinteEsdeveniment,
                    nomEsdeveniment,
                    dataHoraEsdeveniment,
                    categoriaEsdeveniment,
                    descripcioEsdeveniment,
                    imatgeEsdeveniment,
                    llistaTipusEntrades
                );
                llistaEsdeveniments.add(esdevenimentObjecte);
            }


            // Crear l'objecte cataleg amb tota l'informacio i retornarlo
            Cataleg catalegObjecte = new Cataleg(llistaRecintes, llistaEsdeveniments);
            return catalegObjecte;

        // En cas d'error amb la base de dades, donar feedback i retornar null
        } catch (Exception e) {
            System.out.println("ERROR llegint la base de dades: " + e.getMessage());
            return null;
        }
    }
}
