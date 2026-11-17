package grup1.server.objectesDades;



public class TipusEntrada {

    // Atributs
    private String id;
    private String nom;
    private String zona;
    private double preu;
    private int quota;



    // Constructor
    public TipusEntrada(
        String id, 
        String nom,
        String zona,
        double preu, 
        int quota
    ) {
        this.id = id;
        this.nom = nom;
        this.zona = zona;
        this.preu = preu;
        this.quota = quota;
    }



    // Metodes
    public String getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getZona() {
        return zona;
    }

    public double getPreu() {
        return preu;
    }

    public int getQuota() {
        return quota;
    }
}
