package grup1.server.objectesDades;



public class Zona {

    // Atributs
    private String id;
    private String nom;
    private String tipus;
    private int aforament;
    
    

    // Constructor
    public Zona(
        String id, 
        String nom, 
        String tipus, 
        int aforament
    ) {
        this.id = id;
        this.nom = nom;
        this.tipus = tipus;
        this.aforament = aforament;
    }



    // Metodes
    public String getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getTipus() {
        return tipus;
    }

    public int getAforament() {
        return aforament;
    }
}