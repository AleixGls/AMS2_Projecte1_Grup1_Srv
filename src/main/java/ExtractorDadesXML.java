import java.io.File;



import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;



public class ExtractorDadesXML {

    // Atributs
    private static String DIRECTORI_USUARI = System.getProperty("user.dir");
    private static File DIRECTORI_RESOURCES = new File(DIRECTORI_USUARI);

    private static String NOM_ARXIU_XML = "ESDEVENIMENTS.XML";
    private static File ARXIU_XML = new File(
        DIRECTORI_RESOURCES + File.separator + 
        "src" + File.separator + 
        "main" + File.separator + 
        "resources" + File.separator + 
        NOM_ARXIU_XML
    );



    // Constructor



    // Metodes
    public static void main(String[] args) {

        System.out.println(ARXIU_XML.exists());
        

    }

    public static Document parseXML(File inputFile) {
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

}
