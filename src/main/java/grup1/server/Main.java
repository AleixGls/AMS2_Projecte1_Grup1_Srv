package grup1.server;



public class Main {

    // Metodes
    public static void main(String[] args) {
        ServerConnection servidor = new ServerConnection(3000);
        servidor.start();

        System.out.println("Esperando conexiones...");
    }
}