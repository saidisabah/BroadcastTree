import java.io.*;
import java.net.*;
import java.util.*;

public class Worker {
    private static final int BLOCK_SIZE = 1024;
    private static final int D = 2; // Nombre d'enfants par Worker
    private static final int BASE_PORT = 5000; // Port du Master

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Worker <WorkerName>");
            return;
        }

        String workerName = args[0]; // Ex: Worker1
        int workerID = Integer.parseInt(workerName.substring(6)); // Ex: Worker1 → ID = 1
        int workerPort = BASE_PORT + workerID;

        // Trouver le parent
        int parentID = (workerID - 1) / D;
        String parentHost = "localhost"; // Toujours localhost
        int parentPort = BASE_PORT + parentID;

        // Trouver les enfants
        List<Integer> childrenPorts = new ArrayList<>();
        for (int i = 1; i <= D; i++) {
            int childID = workerID * D + i;
            if (childID <= 6) {
                childrenPorts.add(BASE_PORT + childID);
            }
        }

        try {
            // 📥 Connexion au Parent
            Socket parentSocket = new Socket(parentHost, parentPort);
            InputStream inputStream = parentSocket.getInputStream();
            System.out.println(workerName + " connecté à " + parentHost + " sur le port " + parentPort);

            // 📡 Démarrer un Serveur pour ses Enfants
            ServerSocket serverSocket = null;
            List<Socket> childSockets = new ArrayList<>();

            if (!childrenPorts.isEmpty()) {
                serverSocket = new ServerSocket(workerPort);
                System.out.println(workerName + " attend ses enfants sur le port " + workerPort);

                for (int childPort : childrenPorts) {
                    Socket childSocket = serverSocket.accept();
                    childSockets.add(childSocket);
                    System.out.println(workerName + " connecté à un enfant sur le port " + childPort);
                }
            }

            // 📥 Stocker tous les blocs avant d’envoyer aux enfants
            List<byte[]> fileData = new ArrayList<>();
            FileOutputStream fileOutputStream = new FileOutputStream("received_file_worker.dat");
            byte[] buffer = new byte[BLOCK_SIZE];
            int bytesRead;
            int blockNumber = 1;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] dataBlock = Arrays.copyOf(buffer, bytesRead);
                fileData.add(dataBlock); // Stocke les blocs en mémoire
                fileOutputStream.write(buffer, 0, bytesRead);
                System.out.println(workerName + " a reçu et enregistré le bloc #" + blockNumber + " (" + bytesRead + " octets) du Master");
                blockNumber++;
            }

            fileOutputStream.close();
            System.out.println(workerName + " a terminé la réception et l’enregistrement du fichier.");

            // 📤 Transmettre aux enfants après réception complète
            for (Socket childSocket : childSockets) {
                OutputStream outputStream = childSocket.getOutputStream();
                for (int i = 0; i < fileData.size(); i++) {
                    outputStream.write(fileData.get(i));
                    outputStream.flush();
                    System.out.println(workerName + " a envoyé le bloc #" + (i + 1) + " à un enfant");
                }
            }

            // Fermeture des flux
            parentSocket.close();
            for (Socket socket : childSockets) socket.close();
            if (serverSocket != null) serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
