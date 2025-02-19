import java.io.*;
import java.net.*;
import java.util.*;

public class Worker {
    private static final int BLOCK_SIZE = 65536; // 64 KB
    private static final int D = 2; // Nombre d'enfants par Worker
    private static final int NbrWorkers = 6;
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
            if (childID <= NbrWorkers) {
                childrenPorts.add(BASE_PORT + childID);
            }
        }

        try {
            // 📥 Connexion au Parent
            Socket parentSocket = new Socket(parentHost, parentPort);
            InputStream inputStream = parentSocket.getInputStream();
            System.out.println("[" + workerName + "] 🔗 Connecté à Worker" + parentID + " (port " + parentPort + ")");

            // 📡 Démarrer un Serveur pour ses Enfants
            ServerSocket serverSocket = null;
            List<Socket> childSockets = new ArrayList<>();

            if (!childrenPorts.isEmpty()) {
                serverSocket = new ServerSocket(workerPort);
                System.out.println("[" + workerName + "] 🕓 En attente des connexions de ses enfants sur le port " + workerPort);

                for (int childPort : childrenPorts) {
                    Socket childSocket = serverSocket.accept();
                    childSockets.add(childSocket);
                    int childID = childPort - BASE_PORT;
                    System.out.println("[" + workerName + "] 🔗 Connecté à Worker" + childID);
                }
            }

            // 📥 Stocker tous les blocs avant d’envoyer aux enfants
            List<byte[]> fileData = new ArrayList<>();
            FileOutputStream fileOutputStream = new FileOutputStream(workerName + "_received.dat");
            byte[] buffer = new byte[BLOCK_SIZE];
            int bytesRead;
            int blockNumber = 1;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] dataBlock = Arrays.copyOf(buffer, bytesRead);
                fileData.add(dataBlock);
                fileOutputStream.write(buffer, 0, bytesRead);
                System.out.println("[" + workerName + "] 📥 Bloc #" + blockNumber + " (" + bytesRead + " octets) reçu de Worker" + parentID);
                blockNumber++;
            }

            fileOutputStream.close();
            System.out.println("[" + workerName + "] ✅ Tous les blocs reçus et enregistrés.");

            // 📤 Transmettre aux enfants après réception complète
            for (Socket childSocket : childSockets) {
                OutputStream outputStream = childSocket.getOutputStream();
                int childID = childrenPorts.get(childSockets.indexOf(childSocket)) - BASE_PORT;

                for (int i = 0; i < fileData.size(); i++) {
                    outputStream.write(fileData.get(i));
                    outputStream.flush();
                    System.out.println("[" + workerName + "] 🚀 Bloc #" + (i + 1) + " envoyé à Worker" + childID);
                }
            }

            parentSocket.close();
            for (Socket socket : childSockets) socket.close();
            if (serverSocket != null) serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
