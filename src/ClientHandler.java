import java.io.*;
import java.net.Socket;
import java.lang.Math;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.concurrent.TimeUnit;


public class ClientHandler extends Thread{
    private Socket socket;
    private int clientNumber;
    final String[] commandList =
            {
                    "upload",
                    "mkdir",
                    "Delete",
                    "ls",
                    "cd",
                    "download"
            };
    static String serverRootName = "ServerFiles";

    public ClientHandler (Socket socket, int clientNumber)
    {
        this.socket= socket;
        this.clientNumber= clientNumber;
        System.out.println("New connection with client# " + clientNumber + " at " + socket);
    }

    /**
     * une thread se charge d'envoyer au client un message de bienvenue
     */
    public void run() {

        try
        {
            //Creation d'un canal sortant pour envoyer des messages au client
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out.writeUTF("Hello from server - you are client#" + clientNumber);

            //Créer un serverFile s'il n'existe pas
            createServerRootFile();

            String receivedFullCommand = "";
            String receivedMainCommand = "";
            //Pour aller plus loin dans les directory, on a juste à faire \\ + le nom d'un dossier
            String currentDirectoryPath = serverRootName;

            //fCom est la commande pour fermer la connexion entre le serveur et le client (POUR TESTS)
            while(!receivedFullCommand.equals("fCom"))
            {
                receivedFullCommand = in.readUTF();
                receivedMainCommand = receivedFullCommand.split(" ")[0];

                //Si c'est une commande valide, on va la montrer sur la console du serveur
                boolean isValidCommand = true;

                //Vérifier quelle commande a été entrée
                //upload
                if (receivedMainCommand.equals(commandList[0]))
                {
                    String fileNameToUpload = receivedFullCommand.split(" ")[1];
                    Boolean fichierExiste = in.readBoolean();

                    if (fichierExiste)
                    {
                        out.writeUTF("Confirmation de la réception de la commande");

                        try {
                            ClientHandler.receiveUploadedFile(fileNameToUpload, currentDirectoryPath, in, out);
                        } catch (Exception e) {}
                    }
                    else
                    {
                        out.writeUTF("Le fichier " + fileNameToUpload + " que vous voulez téléverser n'existe pas.");
                    }
                }
                //mkdir
                else if (receivedMainCommand.equals(commandList[1]))
                {
                    String directoryNameToCreate = receivedFullCommand.split(" ")[1];

                    try {
                        ClientHandler.createFile(directoryNameToCreate, currentDirectoryPath, out);
                    } catch (Exception e) {}
                }
                //Delete
                else if (receivedMainCommand.equals(commandList[2]))
                {
                    String directoryNameToDelete = receivedFullCommand.split(" ")[1];

                    try {
                        ClientHandler.deleteFile(directoryNameToDelete, currentDirectoryPath, out);
                    } catch (Exception e) {}
                }
                //ls command
                else if (receivedMainCommand.equals(commandList[3])) {
                    try {
                        ClientHandler.lsCommand(currentDirectoryPath ,out);
                    }
                    catch (Exception e) {}
                }

                //cd command
                else if(receivedMainCommand.equals(commandList[4])) {
                    String directory = receivedFullCommand.split(" ")[1];

                    try {
                        currentDirectoryPath = ClientHandler.cdCommand(directory, currentDirectoryPath, out);
                    }
                    catch (Exception e) {}
                }

                //download command
                else if(receivedMainCommand.equals(commandList[5])) {
                    String path = receivedFullCommand.split(" ")[1];
                    String zip = "";
                    if(receivedFullCommand.split(" ").length > 2)
                        zip = receivedFullCommand.split(" ")[2];

                    Boolean zipFile = false;
                    if(zip.equals("-z"))
                        zipFile = true;

                    try {
                        ClientHandler.sendFile(path,currentDirectoryPath, in, out, zipFile);
                    }
                    catch (Exception e) {}

                }
                else
                    isValidCommand = false;

                if (isValidCommand)
                {
                    ClientHandler.outputCommandreceived(receivedFullCommand, socket.getInetAddress().toString(), socket.getPort());
                }
                else
                    out.writeUTF("La commande " + receivedMainCommand + " n'est pas valide.");
            }

        }catch (IOException e)
        {
            System.out.println("Error handlind client#" + clientNumber + ": " + e);
        }
        finally {
            try
            {
                //Fermeture de la connexion avec le client
                socket.close();
            }
            catch (IOException e)
            {
                System.out.println("Couldn't close a socket, what's going on?");
            }
            System.out.println("Connection with client#" + clientNumber + " closed");
        }

    }

    static void createServerRootFile()
    {
        File newDirectoryToCreate = new File(serverRootName);
        newDirectoryToCreate.mkdir();
    }

    /** On reçoit le fichier qui nous a été envoyé par le client grâce à cette fonction. **/
    static void receiveUploadedFile(String fileNameToUpload, String currentDirectoryPath,  DataInputStream in, DataOutputStream out) throws Exception
    {
        double totalFileSizeInBytes = in.readDouble();
        out.writeUTF("Confirmation de la réception de la grandeur du fichier");

        byte[] byteFileRead = new byte[(int) Math.ceil(totalFileSizeInBytes)];
        int amountOfBytesRead = 0;

        //On lit ce qui a dans le DataInputStream jusqu'à temps que ça corresponde à la longueur (en bytes) du fichier
        while (amountOfBytesRead < totalFileSizeInBytes)
        {
            int numberOfBytesAvailableToRead = in.available();
            byte[] currentBytesRead = new byte[numberOfBytesAvailableToRead];
            in.read(currentBytesRead);
            for (int i = 0; i <  currentBytesRead.length; i++)
            {
                byteFileRead[amountOfBytesRead] = (currentBytesRead[i]);
                amountOfBytesRead++;
            }
        }
        //Créer le fichier à l'aide des bytes lus
        File fileUploaded = new File(currentDirectoryPath, fileNameToUpload);

        if (fileUploaded.createNewFile())
        {
            Files.write(fileUploaded.toPath(), byteFileRead);
            out.writeUTF("Le fichier " + fileNameToUpload + " a bien été téléversé.");
        }
        else
        {
            out.writeUTF("Le nom du fichier " + fileNameToUpload + " existait déjà et n'a donc pas pu être téléversé.");
        }
    }

    /** Créer un fichier ou un répertoire avec un certain nom dans le répertoire dans lequel le client navigue présentement (qui est donné en paramètre) **/
    static void createFile(String directoryNameToCreate, String currentDirectoryPath, DataOutputStream out) throws Exception
    {
        File newDirectoryToCreate = new File(currentDirectoryPath, directoryNameToCreate);
        if (newDirectoryToCreate.mkdir())
        {
            out.writeUTF("Le dossier " + directoryNameToCreate + " a été créé.");
        }
        else
            out.writeUTF("Le dossier " + directoryNameToCreate + " existe déjà et ne peut être créé.");
    }

    /** Efface un fichier avec un certain nom dans le répertoire dans lequel on navigue présentement (qui est donné en paramètre) **/
    static void deleteFile(String directoryNameToDelete, String currentDirectoryPath, DataOutputStream out) throws Exception
    {
        //Directory peut être un file ici, ça n'a pas d'importance
        File directoryToDelete = new File(currentDirectoryPath, directoryNameToDelete);
        boolean directoryExists = false;

        if (directoryToDelete.exists())
        {
            if(directoryToDelete.delete())
            {
                directoryExists = true;
                out.writeUTF("Le dossier " + directoryNameToDelete + " a été supprimé.");
            }
        }

        if (!directoryExists)
            out.writeUTF("Le dossier " + directoryNameToDelete + " n'existe pas et ne peut être supprimé.");
    }

    /** Imprime la commande reçue par le serveur sur la console du serveur dans le format demandé **/
    static void outputCommandreceived(String command, String IpAdress, int port)
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd @ HH:mm:ss");
        Date date = new Date();
        String commandOutputText = "[" + IpAdress + ":" + port + " // " + formatter.format(date) + "]:" + command;

        System.out.println(commandOutputText);
    }

    /**
     * Fonction qui permet de lister les dossiers et fichiers qui se trouvent dans le repertoire courant
     * @param path : repertoire sur lequel on se trouce
     * @param out : Stream pour l'envoi des données vers le client
     * @throws Exception
     */
    static void lsCommand(String path ,DataOutputStream out) throws Exception {
        File dir = new File(path);
        String listOfFiles = "";
        String childs[] = dir.list();
        for(String child: childs) {
            //out.writeUTF(child);
            listOfFiles += child + "@";
        }
        out.writeUTF(listOfFiles);
        out.writeUTF("");
    }

    /**
     * Fonction qui permet de changer de repertoire
     * @param directory : repertoire de destination
     * @param currentPath : repertoire de depart
     * @param out : Stream pour l'envoi des données vers le client
     * @return : retourne le nouveau repertoire local
     * @throws Exception
     */
    static String cdCommand(String directory, String currentPath,DataOutputStream out) throws Exception {
        if(!directory.equals("..")) {
            String tempPath = currentPath + "//" + directory;
            File dir = new File(tempPath);
            if (dir.isDirectory()) {
                currentPath = tempPath;
                //System.setProperty("user.dir", currentPath);
                out.writeUTF("vous etes actuellement dans dans le dossier " + directory);
            } else
                out.writeUTF(directory + " is not a directory");
        }
        else {
            String str[] = currentPath.split("//");
            String temp = str[0];
            for (int i = 1; i < str.length -1; i++) {
                temp += "//" + str[i];
            }
            currentPath = temp;
            out.writeUTF("vous etes actuellement dans dans le dossier " + str[str.length -2]);
        }
        return currentPath;
    }


    /**
     * Fonction qui envoyer des fichiers vers le client via la commande "download"
     * @param fileName : fichier qui va etre telecharger par le client
     * @param currentDirectory : repertoire local où le fichier se trouve
     * @param in : Stream utilisé pour recevoir des données depuis le client
     * @param out : Stream utilisé pour envoyer des données vers le client
     * @param zip : boolen pour verifier si le fichier va etre zipper ou pas
     * @throws Exception
     */
    static void sendFile(String fileName, String currentDirectory, DataInputStream in, DataOutputStream out, Boolean zip) throws Exception
    {
        String filePath = fileName;

        //Pour être clair, le répertoire du client se trouvera directement dans le répertoire du projet appelé CLientFiles,
        //donc un fichier à téléverser se trouvera dans ClientFiles
        File fileToSend = new File(currentDirectory, fileName);
        boolean fichierExiste = fileToSend.exists();

        out.writeBoolean(fichierExiste); //On envoie au serveur si le fichier existe ou pas

        if (fichierExiste)
        {
            if(zip) {
                zipFile(fileName, currentDirectory);
                fileName = fileName.concat(".zip");
            }

            File fileToUpload = new File(currentDirectory, fileName);
            double fileSizeInBytes = fileToUpload.length();

            in.readUTF(); //Confirmation que le serveur a bien reçu la commande
            out.writeDouble(fileSizeInBytes);
            in.readUTF(); //On attend que le serveur confirme la réception
            //On transforme le file en tableau de bytes et on l'envoie au serveur
            out.write(Files.readAllBytes(fileToUpload.toPath()));
            TimeUnit.SECONDS.sleep(1);
            out.writeUTF("Le fichier " + fileName + " a bien ete telechargé.");
            if (zip)
            {
                fileToUpload.delete();
            }
        }
        else
        {
            out.writeUTF("Le fichier " + filePath + " que vous voulez téléverser n'existe pas.");
        }
    }

    /**
     * Fonction qui zip un fichier
     * @param fileName : fichier qui va etre zipped
     * @param currentDirectoryPath : repertoire local
     */
    private static void zipFile(String fileName, String currentDirectoryPath) {
        String fil = currentDirectoryPath + "//" + fileName;
        try {
            File file = new File(fil);
            String zipFileName = file.getName().concat(".zip");
            File zipFile = new File(currentDirectoryPath + "//" + zipFileName);
            zipFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            zos.putNextEntry(new ZipEntry(file.getName()));

            byte[] bytes = Files.readAllBytes(Paths.get(fil));
            zos.write(bytes, 0, bytes.length);
            zos.flush();
            zos.closeEntry();
            zos.close();

        } catch (FileNotFoundException ex) {
            System.err.format("The file %s does not exist", fil);
        } catch (IOException ex) {
            System.err.println("I/O error: " + ex);
        }
    }
}
