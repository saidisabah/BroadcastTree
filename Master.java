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
            System.out.println("Master en attente de connexions...");

            // Attente des connexions des D premiers Workers
            for (int i = 1; i <= D; i++) {
                Socket workerSocket = serverSocket.accept();
                workerSockets.add(workerSocket);
                System.out.println("Connexion avec " + workerSocket.getInetAddress());
            }

            // Lecture du fichier et envoi en blocs
            File file = new File(FILE_PATH);
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[BLOCK_SIZE];
            int bytesRead;
            int blockNumber = 1;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                System.out.println("Master envoie le bloc #" + blockNumber + " (" + bytesRead + " octets)");
                for (Socket socket : workerSockets) {
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
                blockNumber++;
            }

            System.out.println("Master a terminé l'envoi.");
            fileInputStream.close();
            for (Socket socket : workerSockets) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
