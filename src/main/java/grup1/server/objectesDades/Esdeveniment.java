package grup1.server.objectesDades;



import java.time.LocalDateTime;
import java.util.List;



public class Esdeveniment {

    // Atributs
    private String id; 
    private String recinte;
    private String nom;    
    private LocalDateTime dataHora;
    private String categoria;
    private String descripcio;
    private String imatge;
    private List<TipusEntrada> tipusEntrades;



    // Constructor
    public Esdeveniment(
        String id,
        String recinte,
        String nom,
        LocalDateTime dataHora,
        String categoria,
        String descripcio,
        String imatge,
        List<TipusEntrada> tipusEntrades
    ) {
        this.id = id;
        this.recinte = recinte;
        this.nom = nom;
        this.dataHora = dataHora;
        this.categoria = categoria;
        this.descripcio = descripcio;
        this.imatge = imatge;
        this.tipusEntrades = tipusEntrades;
    }



    // Metodes
    public String getId() {
        return id;
    }

    public String getRecinte() {
        return recinte;
    }

    public String getNom() {
        return nom;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getDescripcio() {
        return descripcio;
    }

    public String getImatge() {
        return imatge;
    }

    public List<TipusEntrada> getTipusEntrades() {
        return tipusEntrades;
    }
}