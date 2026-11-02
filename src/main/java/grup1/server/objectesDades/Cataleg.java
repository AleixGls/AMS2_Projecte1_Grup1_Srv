package grup1.server.objectesDades;



import java.util.List;



public class Cataleg {

    // Atributs
    private List<Recinte> recintes;
    private List<Esdeveniment> esdeveniments;



    // Constructor
    public Cataleg(
        List<Recinte> recintes,
        List<Esdeveniment> esdeveniments
    ) {
        this.recintes = recintes;
        this.esdeveniments = esdeveniments;
    }



    // Metodes
    public List<Recinte> getRecintes() {
        return recintes;
    }

    public List<Esdeveniment> getEsdeveniments() {
        return esdeveniments;
    }
}
