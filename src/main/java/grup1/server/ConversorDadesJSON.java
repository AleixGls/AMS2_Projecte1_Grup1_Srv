package grup1.server;



import grup1.server.objectesDades.Cataleg;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;



public class ConversorDadesJSON {

    // Atributs
    private Cataleg catalegObjecte;



    // Constructor
    public ConversorDadesJSON(
        Cataleg cataleg
    ) { 
        this.catalegObjecte = cataleg;
    }



    // Metodes
    public String processarObjecteCataleg() {

        // Crear i construir el mapper per poder escriure el JSON
        JsonMapper mapper = JsonMapper.builder().build();

        try {
            // Convertir l'objecte Cataleg a un String JSON
            String json = mapper.writeValueAsString(catalegObjecte);

            return json;

        } catch (JacksonException e) {
            System.out.println("ERROR: No s'ha pogut convertir el cataleg a JSON.");
            return null;
        }
    }
}