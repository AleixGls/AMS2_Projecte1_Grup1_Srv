package grup1.server.objectesDades;



import java.util.List;



public class Recinte {

    // Atributs
    private String id;
    private String nom;
    private List<Zona> zones;



    // Constructor
    public Recinte(
        String id, 
        String nom, 
        List<Zona> zones
    ) {
        this.id = id;
        this.nom = nom;
        this.zones = zones;
    }



    // Metodes
    public String getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public List<Zona> getZones() {
        return zones;
    }
}