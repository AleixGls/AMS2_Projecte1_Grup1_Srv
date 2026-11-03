package grup1.server;



import grup1.server.objectesDades.Cataleg;
import grup1.server.objectesDades.Esdeveniment;
import grup1.server.objectesDades.Recinte;
import grup1.server.objectesDades.TipusEntrada;
import grup1.server.objectesDades.Zona;



public class Main {

    // Metodes
    public static void main(String[] args) {
        ServerConnection servidor = new ServerConnection(3000);
        servidor.start();

        ExtractorDadesXML extractor = new ExtractorDadesXML("ESDEVENIMENTS.XML");
        Cataleg cataleg = extractor.processarFitxerXML();

        // EJEMPLO USO CATALOGO COMO STRING JSON, BORRAR MAS TARDE
        if (cataleg != null) {
            // Crear el conversor y obtener el String JSON
            ConversorDadesJSON conversor = new ConversorDadesJSON(cataleg);
            String catalegJSON = conversor.processarObjecteCataleg();
        
            if (catalegJSON != null) {
                System.out.println("========== CATALEG EN FORMAT JSON ==========");
                System.out.println(catalegJSON);
            } else {
                System.out.println("No s'ha pogut convertir el catàleg a JSON.");
            }
        
        } else {
            System.out.println("No s'ha pogut carregar el catàleg.");
        }
    }
}