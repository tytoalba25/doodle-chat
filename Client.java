import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import java.util.Map.Entry;
import java.util.Scanner;

public class Client {
	static Socket sock = null;
	static BufferedReader sockIn = null;
	static BufferedWriter sockOut = null;
	static String displayName = null;
	static Boolean joined = false;
	static String trackIP = "";
	static int trackPort = -1;
	static int ID = -1;
	static String channelName = "";
	static PeerGroup group;
	
	public static void main(String args[]) {
		Scanner in = new Scanner(System.in);
		MulticastSocket ms = null;
		init(args);		
		register();
		
		System.out.println("Enter display-name");
		displayName = in.nextLine();
		
		while (!joined) {
			System.out.println("What would you like to do " + displayName + "? ( list | create -channelName- | join -channelName- | quit)");
			String input = in.nextLine();
			String[] parts = input.split(" ");
			switch(parts[0].toLowerCase()) {

			case "list":
				System.out.println(listChannels() + "\n");
				break;
			case "create":
				if(parts.length == 2) {
					ms = createChannel(parts[1]);					
				} else {
					System.out.println("Format: create $channelName");
				}
				break;
			case "join":
				if(parts.length == 2) {
					channelName = parts[1];
					ms = joinChannel(parts[1]);					
				} else {
					System.out.println("Format: join $channelName");
				}
				break;
			case "quit":
				// Tell server you're leaving and to free up the ID
				System.exit(0);
			default:
				System.out.println("Invalid Entry.");
				break;
			}
		}
		
		//Talk loop
		System.out.println("\n\nWelcome to " + channelName + "!");
		
		chatLoop(in, ms);
		

		// Exit normally
		System.exit(0);
	}
	
	// Create connections and resources, catch any issues
	private static void init(String[] args) {
		String dir;
		
		if(args.length != 1) {
			dir = "trackers.torChat";
		} else {
			dir = args[1];
		}
		
		try {
			BufferedReader file = new BufferedReader (new FileReader(new File (dir)));
			String tracker = file.readLine();
			
			String[] parts = tracker.split(":");
			
			trackIP = parts[0];
			trackPort = Integer.parseInt(parts[1]);
			
			System.out.println(trackIP + " : " + trackPort);
			
			file.close();
			
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
			System.exit(2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void openSocket() {
			
		try {
			sock = new Socket(trackIP, trackPort);
			sockIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			sockOut = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
		} catch (UnknownHostException e) {
			System.out.println("Cannot connect to tracker");
			//e.printStackTrace();
			System.exit(1);
		} catch (ConnectException e) {
			System.out.println("Failed to connect to server.");
			//e.printStackTrace();
			System.exit(3);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(4);
		}
	}
	
	// Connects to server to acquire channel list and outputs to console
	private static String listChannels() {
		try {
			openSocket();
			sockOut.write("get " + ID + "\n\n");
			sockOut.flush();
			
			String message = sockIn.readLine();
			cleanUp();
			return message;
			
		} catch (IOException e) {
			System.out.println("List failed.");
			e.printStackTrace();
			System.exit(3);
		}
		return null;
	}
	
	// Creates and joins a channel
	private static MulticastSocket createChannel(String channel) {
		MulticastSocket ms = null;

		openSocket();
		try {
			sockOut.write("create " + ID + " " + channel + "\n\n");
			sockOut.flush();

			String reply = sockIn.readLine();
			String[] parts = reply.split(" ");
			if(parts[0].equals("success")) {
				
				ms = joinChannel(channel);
			} else {
				System.out.println(reply);
				return null;
			}

		} catch (IOException e) {
			System.out.println("Creation failed.");
			e.printStackTrace();
			System.exit(3);
		}
		cleanUp();
		
		return ms;
	}
	
	
	//TODO: Connects to pre-existing channel
	private static MulticastSocket joinChannel(String channel) {
		MulticastSocket ms = createMS();
		openSocket();
		try {
			sockOut.write("join " + ID  + " " + channel + " " + ms.getLocalPort() + "\n\n");
			sockOut.flush();

			String reply = sockIn.readLine();
			System.out.println(reply);
			String[] parts = reply.split(" ");
			if(parts[0].equals("success")) {
				
				if(parts[2].equals("true")) {
					changeTracker(parts[3]);
					joinChannel(channel);
				} else {
					System.out.println("false");
					joined(channel);
					initGroup(parts[3]);
				}
			} else {
				System.out.println(reply);
				return null;
			}
			

		} catch (IOException e) {
			System.out.println(e.getMessage());
			//e.printStackTrace();
			System.exit(3);
		}

		cleanUp();

		return ms;
	}
	
	// Re-assigns the tracker information
	// Only called on a join request where the current tracker doesn't host the room
	private static void changeTracker(String addr) {
		String[] parts = addr.split(":");
		trackIP = parts[0];
		trackPort = Integer.parseInt(parts[1].trim());
	}

	private static MulticastSocket createMS() {
		try {
			return new MulticastSocket();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	static void initGroup(String incoming) {		
		group = new PeerGroup(trackIP, trackPort, channelName);
		group.addAll(incoming);
	}
	
	
	private static void register() {
		openSocket();
		
		try {
				sockOut.write("register\n\n");
				sockOut.flush();
				
				String sID = sockIn.readLine();
				
				System.out.println("Your ID is: " + sID + "\n");
				ID = Integer.parseInt(sID);
				
			} catch (NumberFormatException e) {
				System.out.println("Registration Failed..");
				e.printStackTrace();
				System.exit(7);
			} catch (IOException e) {
				System.out.println("Registration Failed..");
				e.printStackTrace();
				System.exit(3);
			}
		
		cleanUp();
	}
	
	// Updates join status to exit while loop
	private static void joined(String channel) {
		channelName = channel;
		joined = true;
	}
	
	// Closes resources
	private static void cleanUp() {
		try {
			sockIn.close();
			sockOut.close();
			sock.close();
			
		} catch (IOException e) {
			System.out.println("Failed to close resources");
			e.printStackTrace();
		}
	}
	
	
	// Listens for user input and sends messages
	private static void chatLoop(Scanner in, MulticastSocket listen) {
		try {

		
			Thread receiver = new Thread(new MulticastReceiver(trackIP, trackPort, group, listen, ID));
			receiver.start();
		
			
			
			MulticastSocket sock = new MulticastSocket();
			
			Boolean chatting = true;
			String message = "";
			
			
			bMulticast(sock, displayName + " has joined the chat."); 
			
			
			while(chatting) {
				message = in.nextLine();
				if(message.equals("/quit")) {
					try {
						
						bMulticast(sock, displayName + " has left the chat."); 
						
						
						openSocket();
						sockOut.write("leave " + ID + " " + channelName + "\n\n");
						sockOut.flush();
						cleanUp();
					} catch (IOException e) {
						
						e.printStackTrace();
					}
					break;
				} else {
					bMulticast(sock, (displayName + ": " + message));
				}
			}

			receiver.interrupt();
			sock.close();
			
		} catch (IOException e1) {
			
			e1.printStackTrace();
		}
		
		System.out.println("Quiting...");
	}
	
	// Sends out a UDP message to each peer one at a time
	private static synchronized void bMulticast(DatagramSocket sock, String plainText) {
		//System.out.println(message);
		
		String message = ID + "~" + plainText;
		
		byte[] buffer = message.getBytes();
		DatagramPacket packet;
		
		for(Entry<Integer, Peer> idPeer : group) {
			Peer peer = idPeer.getValue();
			packet = new DatagramPacket(buffer, buffer.length, peer.getAddr(), peer.getPort());
			try {
				sock.send(packet);
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
	}
}
