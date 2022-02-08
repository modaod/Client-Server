import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


public class Client {

	private static Socket socket;
	static String clientRootName = "ClientFiles";

	/**
	 *
	 * Application client
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		// Adresse IP et port du serveur que le client va devoir entrer
		System.out.println("Enter port number : " );
		//Demande au client de renter le numero du port
		Scanner port=new Scanner(System.in);
		int portValue=0;

		if (port.hasNextInt()) {
			portValue=port.nextInt();
			while (portValue<5002 ||  portValue>5049 ) {
				System.out.println("This port number is not in the range ");
				port=new Scanner(System.in);
				portValue=port.nextInt();
			}
		}
		System.out.println("Enter IP address : " );
		//Demande au client de renter le numero de l'addresse IP
		Scanner address=new Scanner(System.in);
		String serverAddress="okay";

		if (address.hasNextLine() ) {
			serverAddress=address.nextLine();
			String [] split = serverAddress.split("\\.");
			while (split.length!=4) {
				System.out.println("This address IP is not on 4 bytes ");
				address=new Scanner(System.in);
				serverAddress=address.nextLine();
				split = serverAddress.split("\\.");
			}

		}

		//Creation d'une nouvelle connexion avec le serveur
		try {
			socket = new Socket(serverAddress, portValue);
			System.out.format("The server is running on %s:%d%n", serverAddress, portValue);

			//Creation d'un canal entrant pour recevoir les messages envoyes par le serveur
			DataInputStream in = new DataInputStream(socket.getInputStream());

			//Attente de la reception d'un message envoye par le serveur sur le canal
			String helloMessageFromServer = in.readUTF();
			System.out.println(helloMessageFromServer);



			//Creation d'un canal sortant pour envoyer des messages au serveur
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());

			//On demandera au client d'entrer ses commandes et on attendra une rÃ©ponse du serveur
			String command = "";
			Scanner commandReader = new Scanner(System.in);
			String clientDirectory = clientRootName;

			//Creer un dossier ClientFiles s'il n'existe pas
			createClientRootFile();

			//fCom est la commande pour fermer la connexion entre le serveur et le client (POUR TESTS)
			//fCom est la commande pour fermer la connexion entre le serveur et le client (POUR TESTS)
			while(!command.equals("fCom"))
			{
				System.out.print("Veuillez entrer une commande : ");
				String enteredCommand = commandReader.nextLine();
				String mainCommand = "";
				mainCommand = enteredCommand.split(" ")[0];

				command = enteredCommand;
				out.writeUTF(command);

				//Pour la commande upload, on enverra la commande au serveur seulement si le fichier qu'on veut t�l�verser existe
				if (mainCommand.equals("upload"))
				{
					String fileNameToUpload = enteredCommand.split(" ")[1];
					Client.uploadFile(fileNameToUpload, clientDirectory, in, out);
				}
				else if(mainCommand.equals("download")) {
					String fileName = enteredCommand.split(" ")[1];
					Boolean fichierExiste = in.readBoolean();
					String zip = "";

					if(command.split(" ").length > 2)
						zip = command.split(" ")[2];

					if(fichierExiste)
						Client.receiveFile(fileName, clientDirectory, in, out, zip.equals("-z"));
				}

				else if(mainCommand.equals("ls")) {
					String listOfFiles = in.readUTF();
					String str[] = listOfFiles.split("@");
					for (String element: str) {
						System.out.println(element);
					}
					//in.readUTF();
				}

				else if(mainCommand.equals("cd")) {

				}

				//Lire la r�ponse du serveur avant d'envoyer une nouvelle commande
				System.out.println(in.readUTF());
				//Fermeture de la connexion avec le serveur

			}
			commandReader.close();
			socket.close();
			port.close();
			address.close();
		}
		catch (IOException e)
		{
			System.out.println("Error, port number or IP address incorrect" );
		}


	}

	static void createClientRootFile()
	{
		File newDirectoryToCreate = new File(clientRootName);
		newDirectoryToCreate.mkdir();
	}

	/** Fonction qui nous permet d'envoyer les informations n�cessaires au serveur pour qu'un fichier soit t�l�vers�.  On t�l�verse le fichier � la fin de la fonction. **/
	static void uploadFile(String fileName, String clientDirectory, DataInputStream in, DataOutputStream out) throws Exception
	{
		//Pour �tre clair, le r�pertoire du client se trouvera directement dans le r�pertoire du projet appel� CLientFiles,
		//donc un fichier � t�l�verser se trouvera dans ClientFiles
		File fileToUpload = new File(clientDirectory, fileName);
		Boolean fichierExiste = fileToUpload.exists();

		out.writeBoolean(fichierExiste); //On envoie au serveur si le fichier existe ou pas

		if (fichierExiste)
		{
			double fileSizeInBytes = fileToUpload.length();

			in.readUTF(); //Confirmation que le serveur a bien re�u la commande
			out.writeDouble(fileSizeInBytes);
			in.readUTF(); //On attend que le serveur confirme la r�ception
			//On transforme le file en tableau de bytes et on l'envoie au serveur
			out.write(Files.readAllBytes(fileToUpload.toPath()));
		}
	}

	/**
	 * Fonction qui telecharge le fichier re�u depuis le serveur via la commande download
	 * @param fileNameToUpload : nom du fichier a telecharger
	 * @param currentDirectoryPath : repertoire courant
	 * @param in : Stream pour la reception des donn�es depuis le serveur
	 * @param out : Stream pour l'envoi des donn�es vers le serveur
	 * @param zip : booleen qui indique si le fichier est zipper ou pas
	 * @throws Exception
	 */
	static void receiveFile(String fileNameToUpload, String currentDirectoryPath,  DataInputStream in, DataOutputStream out, Boolean zip) throws Exception
	{
		out.writeUTF("Confirmation de la reception de la commande");
		double totalFileSizeInBytes = in.readDouble();

		out.writeUTF("Confirmation de la r�ception de la grandeur du fichier");

		byte[] byteFileRead = new byte[(int) Math.ceil(totalFileSizeInBytes)];
		int amountOfBytesRead = 0;

		//On lit ce qui a dans le DataInputStream jusqu'� temps que �a corresponde � la longueur (en bytes) du fichier
		while (amountOfBytesRead < totalFileSizeInBytes)
		{
			int numberOfBytesAvailableToRead = in.available();
			byte[] currentBytesRead = new byte[numberOfBytesAvailableToRead];
			in.read(currentBytesRead);
			for (int i = 0; i <  currentBytesRead.length; i++)
			{
				byteFileRead[amountOfBytesRead] = currentBytesRead[i];
				amountOfBytesRead++;
			}
		}

		//Cr�er le fichier � l'aide des bytes lus
		if (zip)
		{
			fileNameToUpload = fileNameToUpload + ".zip";
		}

		File fileUploaded = new File(currentDirectoryPath, fileNameToUpload);

		if (fileUploaded.createNewFile())
		{
			Files.write(fileUploaded.toPath(), byteFileRead);
		}

	}
}
	
