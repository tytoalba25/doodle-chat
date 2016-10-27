import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
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
			System.out.println("What would you like to do " + displayName + "? ( list | create -channelName- | join -channelName- | quit)");
			String input = in.nextLine();
			String[] parts = input.split(" ");
			switch(parts[0].toLowerCase()) {

			case "list":
				listChannels();
				break;
			case "create":
				if(parts.length == 2) {
					createChannel(parts[1]);					
				} else {
					System.out.println("Format: create $channelName");
				}
				break;
			case "join":
				if(parts.length == 2) {
					joinChannel(parts[1]);					
				} else {
					System.out.println("Format: join $channelName");
				}
				break;
			case "quit":
				try {
					sock.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.exit(0);
			default:
				System.out.println("Invalid Entry.");
				break;
			}
		}
		
		//TODO: Talk loop
		System.out.println("This is where chatting would take place");
		
		
		// Clean Up
		cleanUp();

		// Done
		System.exit(0);
	}
	
	// Create connections and resources, catch any issues
	private static void init(Scanner in) {
		System.out.println("Please enter tracker IP:Port");
		String tracker = in.nextLine();
		String[] parts = tracker.split(":");
		try {
			sock = new Socket(parts[0], Integer.parseInt(parts[1]));
			sockIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			sockOut = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
		} catch (UnknownHostException e) {
			System.out.println("Cannot connect to tracker");
			e.printStackTrace();
			System.exit(1);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			System.exit(2);
		} catch (ConnectException e) {
			System.out.println("Failed to connect to server.");
			//e.printStackTrace();
			//System.exit(3);
			System.out.println("DEBUG: Continuing without connection.");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(4);
		}
		
		System.out.println("Enter display-name");
		displayName = in.nextLine();
	}
	
	//TODO: Connects to server to acquire channel list and outputs to console
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
	
	//TODO: Creates and joins a channel
	private static void createChannel(String channel) {
		if(channelExists()) {
			try {
				sockOut.write("create " + channel + "\n\n");
				sockOut.flush();
				
				System.out.println(sockIn.readLine());
				
			} catch (IOException e) {
				System.out.println("Creation failed.");
				e.printStackTrace();
				System.exit(3);
			}
			
			
			joined();
		}
	}
	
	
	//TODO: Connects to pre-existing channel
	private static void joinChannel(String channel) {
		if(channelExists()) {
			try {
				sockOut.write("join " + channel + "\n\n");
				sockOut.flush();
				
				System.out.println(sockIn.readLine());
				
			} catch (IOException e) {
				System.out.println("Creation failed.");
				e.printStackTrace();
				System.exit(3);
			}
			
			
			
			joined();
		}
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
	
	// TODO: Checks if the channel you are attempting to join or create already exists
	private static Boolean channelExists() {
		return true;
	}
}
