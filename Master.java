import java.io.*;
import java.net.*;
import java.util.*;

public class Master {
    private static final int MASTER_PORT = 5000;
    private static final int BLOCK_SIZE = 65536; // Taille d’un bloc
    private static final int D = 2; // Nombre d'enfants par nœud
    private static final String FILE_PATH = "codna.txt"; // Fichier source

    public static void main(String[] args) {
        List<Socket> workerSockets = new ArrayList<>();
        try (ServerSocket serverSocket = new ServerSocket(MASTER_PORT)) {
            System.out.println("[MASTER] En attente de connexions...");

            // Attente des connexions des D premiers Workers
            for (int i = 1; i <= D; i++) {
                Socket workerSocket = serverSocket.accept();
                workerSockets.add(workerSocket);
                System.out.println("[MASTER] Connexion établie avec Worker" + i + " (" + workerSocket.getInetAddress() + ")");
            }

            System.out.println("[MASTER]  Début de l'envoi des blocs...");

            // Lecture du fichier et envoi en blocs
            File file = new File(FILE_PATH);
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[BLOCK_SIZE];
            int bytesRead;
            int blockNumber = 1;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                for (int i = 0; i < workerSockets.size(); i++) {
                    OutputStream outputStream = workerSockets.get(i).getOutputStream();
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                    System.out.println("[MASTER]  Bloc #" + blockNumber + " (" + bytesRead + " octets) envoyé à Worker" + (i + 1));
                }
                blockNumber++;
            }

            System.out.println("[MASTER]  Envoi terminé.");
            fileInputStream.close();
            for (Socket socket : workerSockets) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
    

