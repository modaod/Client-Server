import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Scanner;

public class Serveur {
	private static ServerSocket listener;

	/**
	 * Application server
	 */
	public static void main (String [] args) throws Exception{

		//Compteur incremente a chaque connexion d'un client au serveur
		int clientNumber =0;

		// Adresse IP et port du serveur que l'utilisateur va devoir entrer
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


		//Creation de la connexion pour communiquer avec les clients
		listener = new ServerSocket();
		listener.setReuseAddress(true);
		InetAddress serverIP = InetAddress.getByName(serverAddress);

		//Assocation de l'addresse et du prot a la connexion
		listener.bind(new InetSocketAddress(serverIP, portValue));

		System.out.format("The server is runnin on %s:%d%n", serverAddress, portValue);

		try
		{
			/**
			 * A chaque fois qu'un nouveau client se connecte, on execute la fonction
			 * Run() de l'objet ClientHandler.
			 */

			while (true)
			{
				//Important : la fonction accept() est bloquante : on attend qu'un prochain client se connecte
				//Une nouvelle connection: on increment le compteur clientNumber
				new ClientHandler(listener.accept(), clientNumber++).start();
			}
		}
		finally
		{
			//Fermeture de la connexion
			listener.close();
			port.close();
			address.close();
		}
	}
}
