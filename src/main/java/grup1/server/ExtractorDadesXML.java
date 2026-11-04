package grup1.server;



import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import grup1.server.objectesDades.Esdeveniment;
import grup1.server.objectesDades.TipusEntrada;
import grup1.server.objectesDades.Recinte;
import grup1.server.objectesDades.Zona;
import grup1.server.objectesDades.Cataleg;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;



public class ExtractorDadesXML {

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
    private File arxiuXML;



    // Constructor
    public ExtractorDadesXML(
        String nomArxiuXML
    ) {
        this.arxiuXML = new File(
            DIRECTORI_RESOURCES + File.separator + nomArxiuXML
        );
    }



    // Metodes
    private static Document parseXML(File inputFile) {

        // Crea una factoria de constructors de documents
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            
            // Crea un constructor de documents
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            
            // Analitza el fitxer XML
            Document doc = dBuilder.parse(inputFile);

            // Retornar el document parsejat
            return doc;
            
        } catch(Exception e) {
            // Imprimeix la pila d'errors en cas d'excepció
            e.printStackTrace();
        }  

        // Retornar null per si no funciona el try
        return null;
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

    public Cataleg processarFitxerXML() {

        // Utlitzar el metode parseXML per pasar el XML a una variable de tipus Document
        Document doc = parseXML(arxiuXML);

        // Si el parseador crea la variable Document correctament, comença a extreure les dades
        if (doc != null) {

            //Obtenir el element cataleg del document XML
            Element cataleg = doc.getDocumentElement();


            // Crear la llista on guardarem els recintes obtinguts
            List<Recinte> llistaRecintes = new ArrayList<>();

            // Obtenir el element recintes
            Element recintes = (Element) cataleg.getElementsByTagName("recintes").item(0);

            // Obtenir la NodeList amb la llista de elements recinte dins del element recintes, i la seva quantitat
            NodeList llistaNodesRecinte = recintes.getElementsByTagName("recinte");
            int quantitatRecinte = llistaNodesRecinte.getLength();

            // Iterar la NodeList per agafar les dades de cada recinte
            for (int iteracioRecinte = 0; iteracioRecinte < quantitatRecinte; iteracioRecinte++) {

                // Obtenir el element recinte de l'iteracio actual
                Element recinte = (Element) llistaNodesRecinte.item(iteracioRecinte);

                // Obtenir els atributs del recinte actual
                String idRecinte = recinte.getAttribute("id");
                String nomRecinte = recinte.getAttribute("nom");


                // Crear la llista on guardarem les zones obtingudes del recinte actual
                List<Zona> llistaZones = new ArrayList<>();

                // Obtenir la NodeList amb la llista de elements zona dins del element recinte actual, i la seva quantitat
                NodeList llistaNodesZona = recinte.getElementsByTagName("zona");
                int quantitatZona = llistaNodesZona.getLength();

                // Iterar la NodeList per agafar les dades de cada zona
                for (int iteracioZona = 0; iteracioZona < quantitatZona; iteracioZona++) {

                    // Obtenir el element zona de l'iteracio actual
                    Element zona = (Element) llistaNodesZona.item(iteracioZona);

                    // Obtenir els atributs de la zona actual
                    String idZona = zona.getAttribute("id");
                    String nomZona = zona.getAttribute("nom");
                    String tipusZona = zona.getAttribute("tipus");
                    int aforamentZona = Integer.parseInt(zona.getAttribute("aforament"));

                    // Crear l'objecte zona i afegirlo a la llista de zones
                    Zona zonaObjecte = new Zona(
                        idZona, 
                        nomZona, 
                        tipusZona, 
                        aforamentZona
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

            // Obtenir el element esdeveniments 
            Element esdeveniments = (Element) cataleg.getElementsByTagName("esdeveniments").item(0);

            // Obtenir la NodeList amb la llista de elements esdevenimet dins del element esdeveniments, i la seva quantitat
            NodeList llistaNodesEsdeveniment = esdeveniments.getElementsByTagName("esdeveniment");
            int quantitatEsdeveniment = llistaNodesEsdeveniment.getLength();

            // Iterar la NodeList per agafar les dades de cada esdeveniment
            for (int iteracioEsdeveniment = 0; iteracioEsdeveniment < quantitatEsdeveniment; iteracioEsdeveniment++) {

                // Obtenir el element esdeveniment de l'iteracio actual
                Element esdeveniment = (Element) llistaNodesEsdeveniment.item(iteracioEsdeveniment);

                // Obtenir els atributs i elements del esdeveniment actual
                String idEsdeveniment = esdeveniment.getAttribute("id");
                String recinteEsdeveniment = esdeveniment.getAttribute("recinte");
                String nomEsdeveniment = esdeveniment.getElementsByTagName("nom").item(0).getTextContent();
                LocalDateTime dataHoraEsdeveniment = LocalDateTime.parse(esdeveniment.getElementsByTagName("dataHora").item(0).getTextContent());
                String categoriaEsdeveniment = esdeveniment.getElementsByTagName("categoria").item(0).getTextContent();
                String descripcioEsdeveniment = esdeveniment.getElementsByTagName("descripcio").item(0).getTextContent();
                String nomImatgeEsdeveniment = esdeveniment.getElementsByTagName("imatge").item(0).getTextContent();
                String imatgeEsdeveniment = convertirImatgeBase64(nomImatgeEsdeveniment);
                

                // Crear la llista on guardarem les TipusEntrades obtingudes del esdeveniment actual
                List<TipusEntrada> llistaTipusEntrades = new ArrayList<>();

                // Obtenir la NodeList amb la llista de elements tipusEntrada dins del element esdeveniment actual, i la seva quantitat
                NodeList llistaNodesTipusEntrada = esdeveniment.getElementsByTagName("tipusEntrada");
                int quantitatTipusEntrada = llistaNodesTipusEntrada.getLength();

                // Iterar la NodeList per agafar les dades de cada tipusEntrada
                for (int iteracioTipusEntrada = 0; iteracioTipusEntrada < quantitatTipusEntrada; iteracioTipusEntrada++) {

                    // Obtenir el element tipusEntrada de l'iteracio actual
                    Element tipusEntrada = (Element) llistaNodesTipusEntrada.item(iteracioTipusEntrada);
                    
                    // Obtenir els atributs de la tipusEntrada actual
                    String idTipusEntrada = tipusEntrada.getAttribute("id");
                    String nomTipusEntrada = tipusEntrada.getAttribute("nom");
                    String zonaTipusEntrada = tipusEntrada.getAttribute("zona");
                    double preuTipusEntrada = Double.parseDouble(tipusEntrada.getAttribute("preu"));
                    int quotaTipusEntrada = Integer.parseInt(tipusEntrada.getAttribute("quota"));
                    
                    // Crear l'objecte tipusEntrada i afegirlo a la llista de tipusEntrades
                    TipusEntrada tipusEntradaObjecte = new TipusEntrada(
                        idTipusEntrada, 
                        nomTipusEntrada, 
                        zonaTipusEntrada, 
                        preuTipusEntrada, 
                        quotaTipusEntrada
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
        
        // En cas de que no es pugi accedir al XML, donar feedback i retornar null
        } else {
            System.out.println("No s'ha pogut accedir al document: " + arxiuXML.getAbsolutePath());
            return null;
        }
    }
}
