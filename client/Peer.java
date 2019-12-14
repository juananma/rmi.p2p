package salva.mario.client;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import salva.mario.common.*;
import salva.mario.common.Utils.*;

import salva.mario.tracker.ITracker;

import javax.annotation.processing.FilerException;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.xml.bind.DatatypeConverter;

public class Peer implements Serializable {


    public String getName() {
        return name;
    }

    public ITracker getTracker() {
        return tracker;
    }

    public Torrent getTorrent() {
        return torrent;
    }

    private Peer myself;
    private String name;
    private ITracker tracker;
    private Torrent torrent;

    /**
     * Un nuevo peer intenta instantáneamente conectarse al registro rmi
     * Guarda una referencia de si mi mismo para usarla posteriormente
     * Muestra el menu Cli, para la seleccion de la accion a realizar
     *
     * @throws RemoteException
     */
    Peer() throws RemoteException {
        try {
            // Connect to RMI registry
            tracker = (ITracker) LocateRegistry.getRegistry(2789).lookup("tracker");
            myself = this;
            showMenu();

        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println("No se pudo conectar al registry");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Goodbye!");
        }
    }

    /**
     * Main loop (loop principal)
     * Imprime el menú cli y maneja la entrada del teclado para elegir opciones
     */
    private void showMenu() {

        int opt = 0;
        do {
            opt = Cli.menu("Menu Principal", new String[]{"Compartir Archivo", "Descargar Archivos", "Ver todos los peers", "salir"});

            // El numero introducido por teclado es +1 esto
            switch (opt) {
                case 0:
                    generateTorrent();
                    break;
                case 1:
                    downloadFile();
                    break;
                case 2:
                    showPeers();
                    break;
                case 3:
                    try {
                        tracker.deletePeer(myself);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    opt = 5;
                    break;
            }
        } while (opt != 5);
    }

    /**
     * Este método ingresa al "modo seed" en el tracker, pidiéndole al usuario que elija un archivo y obtenga información de él.
     * y creando un "archivo torrent" en memoria.
     */
    private void generateTorrent() {
        // Seleccionamos un archivo usando una GUI JFileChooser
        File selectedFile = null;

        JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

        if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            selectedFile = jfc.getSelectedFile();
        }

        // Escribe el nombre para que el usuario esté seguro de lo que está haciendo
        String fileName = selectedFile.getName();
        String filePath = selectedFile.getPath();
        System.out.println("File name: " + fileName);

        // Hace el Hash del archivo y de los blockes(chunks)
        DigestHelper digestHelper = new DigestHelper(selectedFile);

        String fileHash = digestHelper.getFileHash();
        ArrayList<String> chunkHashList = digestHelper.getChunkHashList();

        System.out.println("File hash: " + fileHash);
        System.out.println("Chunk hashes: " + chunkHashList);

        // Obtiene más información para la creación de torrents y muestra su tamaño en MB
        long fileSize = selectedFile.length() / (1024 * 1024);

        if (fileSize == 0) {
            System.out.println("File size: <1 MB");
        } else {
            System.out.println("File size: " + fileSize + " MB");
        }

        // Crea el torrent en memoria
        torrent = new Torrent(fileName, filePath, fileSize, fileHash, chunkHashList);

        // Inicializa login, para iniciar sesion en el tracker
        login();
    }

    /**
     * Este método agrega una referencia a nosotros mismos a la lista de peers del tracker
     * Si ya existimos, volvemos a iniciar sesión con un nuevo archivo
     * Un peer puede transmitir un archivo al mismo tiempo
     * Pero el tracker puede trackear n objetos
     */
    private void login() {
        try {
            if (name == null) {
                name = Cli.input("Autenticarse", "Escriba su nombre: ");
                tracker.addPeer(myself);
                System.out.println("Login exitoso");
            } else {
                tracker.deletePeer(myself);
                tracker.addPeer(myself);
                System.out.println("Relogged with new file");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Le preguntamos al usuario qué archivo desea descargar en el tracker
     * En torrent "normal", generalmente se requiere una interfaz web separada
     * Esto es similar a las redes eDonkey
     */
    private void downloadFile() {
        try {

            // Consigue todos los torrentes en el tracker
            ArrayList<Torrent> torrentList = new ArrayList<>();
            for (Peer peer : tracker.getPeerList()) {
                if (peer.getTorrent() != null) {
                    torrentList.add(peer.getTorrent());
                }
            }

            if (torrentList.size() > 0) {
                // Esta función completa es propensa a las condiciones de carrera si otro usuario estaba usando el sistema de alguna manera
                // Print available files to the user
                System.out.println("=== File - Hash ===");

                ArrayList<String> options = new ArrayList<>();

                for (Torrent torrent : torrentList) {
                    String option = torrent.getFileName() + " ❤️ " + torrent.getFileHash();
                    options.add(option);
                }

                // Seleccionamos el archivo
                int opt = 0;
                opt = Cli.menu("Pick your file!", options.toArray(new String[0]));

                // Recuperamos archivo original
                Torrent pickedTorrent = torrentList.get(opt);

                System.out.println("File selected: " + pickedTorrent.getFileName() + " ❤️ " + pickedTorrent.getFileHash());

                // Vamos a traves de todos los peers que estan compartiendo el torrent seleccionado
                // Y lo descargamos de ellos

                performBlockDownload(pickedTorrent);

            } else {
                System.out.println("No hay archivos disponibles");
            }

        } catch (RemoteException e) {
            System.out.println("Network error");
        }
    }

    /**
     * Imprime todos los pares en el enjambre (swarm) y qué archivos están compartiendo
     */
    private void showPeers() {
        try {
            List<Peer> peerList = tracker.getPeerList();

            System.out.println("Peers en la red: " + peerList.size());

            for (Peer peer : peerList) {
                if (peer.getTorrent() != null) {
                    System.out.println("Client " + peer.getName() + " sharing file " + peer.getTorrent());
                }
            }

        } catch (RemoteException e) {
            System.out.println("Network error");
        }
    }

    // Cuando se imprime, nos muestra por nuestro nombre
    @Override
    public String toString() {
        return name;
    }

    /**
     * Va entre los peers y descarga bloques de ellos
     *
     * @param torrent
     */
    private void performBlockDownload(Torrent torrent) {
        try {

            System.out.println("Attempting to download:");
            System.out.println("File: " + torrent.getFileName());
            System.out.println("Hash: " + torrent.getFileHash());

            // Llega a donde el usuario quiere guardar el archivo
            File selectedFile = null;

            JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

            if (jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                selectedFile = jfc.getSelectedFile();
            }

            // Obtiene que pares tienen el archivo que queremos
            // Esto tambien podria/deberia ser hecho por el tracker
            // Pero no importa mucho en RMI
            ArrayList<Peer> peersWithTorrent = new ArrayList<>();
            for (Peer otherPeer : tracker.getPeerList()) {
                System.out.println("Peer: " + otherPeer.getName());
                if (otherPeer.getTorrent() != null) {
                    System.out.println("File: " + otherPeer.getTorrent().getFileName());
                    if (torrent.getFileHash().equals(otherPeer.getTorrent().getFileHash())) {
                        peersWithTorrent.add(otherPeer);
                        System.out.println("Added Peer " + otherPeer.getName() +
                                " with torrent " + otherPeer.getTorrent().getFileName() +
                                " to download list");
                    }
                }
            }

            System.out.println("From: " + peersWithTorrent);

            // Descargar archivo
            // El archivo final será un poco más grande que el original, debido a los ceros añadidos al final
            // (porque aceptamos fragmentos de 512 KB, incluso si no hay más archivo para tomar)
            // Esto no causa problemas y los archivos siguen siendo completamente funcionales.
            long chunkCount = torrent.getChunkHashList().size();

            Peer targetPeer;

            // Iterar a través de todos los fragmentos del archivo
            for (int i = 0; i < (chunkCount); i++) {
                Random rand = new Random();

                // Si hay mas de un seed compartiendo el archivo, toma los blockes (chunks) aleatoriamente de estos
                // de lo contrario los toma del unico seed
                if (peersWithTorrent.size() > 1) {
                    targetPeer = peersWithTorrent.get(rand.nextInt(peersWithTorrent.size() - 1));
                } else {
                    targetPeer = peersWithTorrent.get(0);
                }

                // Obtenemos el chunk siguiente
                byte[] chunky = targetPeer.getChunk(i);

                // Si el chunk no es null, verifica su hash y forma el archivo
                if (chunky == null) {
                    System.out.println("Received null block, that isn't supposed to happen");
                    System.out.println("Network error or file was deleted");
                } else {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    String expectedHash = torrent.getChunkHashList().get(i);
                    String receivedHash = DatatypeConverter.printHexBinary(md.digest(chunky));

                    //Comprobamos que los hash de los chunks descargados son iguales que los de cuando se subio el archivo
                    if (!expectedHash.equals(receivedHash) &&
                            i != (chunkCount - 1)) {
                        System.out.println("Hash error!");
                    } else {
                        if (i != (chunkCount - 1)) {
                            System.out.println("Expected hash: " + expectedHash);
                            System.out.println("Received hash: " + receivedHash);
                        }
                    }

                    // Usa un archivo de acceso aleatorio para escribir los chunks en un solo archivo
                    File file = new File(selectedFile.getAbsolutePath());
                    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                    randomAccessFile.seek(512 * 1024 * i);
                    randomAccessFile.write(chunky);
                    randomAccessFile.close();

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // Método llamado por otros pares para extraer chunks de nuestro archivo compartido
    // Tiene que ser public
    public byte[] getChunk(long offset) {
        try {
            File file = new File(torrent.getFilePath());

            // Chunks tamaño estandar de 512KiB
            byte[] chunk = new byte[1024 * 512];

            FileInputStream fileInputStream = new FileInputStream(file);

            fileInputStream.getChannel().position(512 * 1024 * offset);

            fileInputStream.read(chunk);

            return chunk;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
