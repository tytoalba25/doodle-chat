import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
	static Socket sock = null;
	static BufferedReader sockIn = null;
	static BufferedWriter sockOut = null;
	static String displayName = null;
	static Boolean joined = false;
	
	
	public static void main(String args[]) {
		Scanner in = new Scanner(System.in);
		init(in);
		while (!joined) {
			System.out.println("What would you like to do " + displayName + "?");
			String input = in.nextLine();
			String[] parts = input.split(" ");
			switch(parts[0].toLowerCase()) {

			case "listchannels":
				listChannels();
				break;
			case "createchannels":
				createChannel(parts[1]);
				break;
			case "joinchannel":
				joinChannel(parts[1]);
				break;
			default:
				System.out.println("Command list: ListChannels | CreateChannel -channelName- | JoinChannel -channelName-");
				break;
			}
		}
		
		// Talk loop
		System.out.println("This is where chatting would take place");
		
		
		// Clean Up
		cleanUp();

		// Done
		System.exit(0);
	}
	
	// Create connections and resources
	private static void init(Scanner in) {
		try {
			sock = new Socket("192.168.0.1", 44);
			sockIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			sockOut = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
		} catch (UnknownHostException e) {
			System.out.println("Cannot connect to tracker");
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(2);
		}
		
		System.out.println("Enter display-name");
		displayName = in.nextLine();
	}
	
	// Connects to server to acquire channel list and outputs to console
	private static void listChannels() {
		try {
			sockOut.write("get\n\n");
			sockOut.flush();
			
			System.out.println(sockIn.readLine());
			
		} catch (IOException e) {
			System.out.println("List failed.");
			e.printStackTrace();
			System.exit(3);
		}
	}
	
	// Creates a channel, calls joinChannel to join newly created channel
	private static void createChannel(String channel) {
		
		
		
		joinChannel(channel);
	}
	
	
	// Connects to pre-existing channel
	private static void joinChannel(String channel) {
		
		
		
		
		
		joined();
	}
	
	// Updates join status to exit while loop
	private static void joined() {
		joined = true;
	}
	
	// Closes resources
	private static void cleanUp() {
		try {
			sockIn.close();
			sockOut.close();
			sock.close();
			
		} catch (IOException e) {
			System.out.println("Faield to close resources");
			e.printStackTrace();
		}
	}
}
