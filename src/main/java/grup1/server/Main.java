package grup1.server;



import grup1.server.objectesDades.Cataleg;



public class Main {

    // Metodes
    public static void main(String[] args) {
        ServerConnection servidor = new ServerConnection(3000);
        servidor.start();

        // Crear el estractor y obtener el objeto Cataleg
        ExtractorDadesXML extractor = new ExtractorDadesXML("ESDEVENIMENTS.XML");
        Cataleg cataleg = extractor.processarFitxerXML();

        // Crear el conversor y obtener el String JSON
        ConversorDadesJSON conversor = new ConversorDadesJSON(cataleg);
        String catalegJSON = conversor.processarObjecteCataleg();
    }
}