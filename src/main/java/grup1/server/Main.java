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

        // EJEMPLO USO CATALOGO, BORRAR MAS TARDE
        if (cataleg != null) {

            // Primer esdeveniment
            Esdeveniment esdeveniment = cataleg.getEsdeveniments().get(0);

            System.out.println("========== PRIMER ESDEVENIMENT ==========");
            System.out.println("ID: " + esdeveniment.getId());
            System.out.println("Nom: " + esdeveniment.getNom());
            System.out.println("Recinte: " + esdeveniment.getRecinte());
            System.out.println("Data i hora: " + esdeveniment.getDataHora());
            System.out.println("Categoria: " + esdeveniment.getCategoria());
            System.out.println("Descripció: " + esdeveniment.getDescripcio());
            System.out.println("Imatge (Base64): "
                    + (esdeveniment.getImatge() != null ? "Carregada correctament" : "No disponible"));

            System.out.println("\nTipus d'entrades:");
            for (TipusEntrada entrada : esdeveniment.getTipusEntrades()) {
                System.out.println("  - " + entrada.getNom()
                        + " | Zona: " + entrada.getZona()
                        + " | Preu: " + entrada.getPreu() + " €"
                        + " | Quota: " + entrada.getQuota());
            }

            // Primer recinte
            Recinte recinte = cataleg.getRecintes().get(0);

            System.out.println("\n========== PRIMER RECINTE ==========");
            System.out.println("ID: " + recinte.getId());
            System.out.println("Nom: " + recinte.getNom());

            System.out.println("\nZones:");
            for (Zona zona : recinte.getZones()) {
                System.out.println("  - " + zona.getNom()
                        + " | ID: " + zona.getId()
                        + " | Tipus: " + zona.getTipus()
                        + " | Aforament: " + zona.getAforament());
            }

        } else {
            System.out.println("No s'ha pogut carregar el catàleg.");
        }
    }
}