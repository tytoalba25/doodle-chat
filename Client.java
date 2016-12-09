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
	static Boolean verbose = false;
	
	public static void main(String args[]) {
		Scanner in = new Scanner(System.in);
		MulticastSocket ms = null;
		init(args);		
		register();
		createName(in);
		
		
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
	
	private static void createName(Scanner in) {
		Boolean invalid = true;
		String name = "";
		while(invalid) {
			System.out.println("Enter display-name");
			name = in.nextLine();
			if(name.matches("[a-zA-Z]+[a-zA-Z0-9]*")) {
				name = name.substring(0, 1).toUpperCase() 
						+	name.substring(1);
				invalid = false;
			} else {
				System.out.println("Please enter a valid name. [a-zA-Z]+[a-zA-Z0-9]\n");
			}
		}
		
		
		displayName = name;
	}
	
	
	// Create connections and resources, catch any issues
	private static void init(String[] args) {
		int min = Integer.MAX_VALUE;
		String lowestIP = "";
		int lowestPort = -1;
		
		String dir;
		
		
		
		if(args.length == 2 && args[0].equals("-v")) {
			verbose = true;
			dir = args[1];
		} else if (args.length == 1 && args[0].equals("-v")) {
			verbose = true;
			dir = "default.tracker";
		} else if (args.length == 1) {
			dir = args[0];
		} else {
			dir = "default.tracker";
		}
		
		try {
			
			// I am so sorry about this block. It is probably some of the ugliest stuff I've ever done.
			
			BufferedReader file = new BufferedReader (new FileReader(new File (dir)));
			
			
			//TODO: Load balancing needs fixing.
			String tracker;
			String[] parts;
			
			String tmpIP;
			int tmpPort;
			
			 while((tracker = file.readLine()) != null) {
				try {
					//String tracker = file.readLine();
					parts = tracker.split(":");
					tmpIP = parts[0];
					tmpPort = Integer.parseInt(parts[1]);
					
					parts = tracker.split(":");

					Socket sock = new Socket(tmpIP, tmpPort);
					BufferedReader sockIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
					BufferedWriter sockOut = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
					
					sockOut.write("pop -1\n\n");
					sockOut.flush();
					// Expected response: success ID pop

					String reply = sockIn.readLine();
					int pop = Integer.parseInt(reply.split(" ")[2]);
					
					
					sock.close();
					
					if(pop < min) {
						min = pop;
						lowestIP = tmpIP;
						lowestPort = tmpPort;
					}
				} catch (Exception e) {
					System.out.println("Tracker " + trackIP + " Unavailable");
					// I'm so sorry
					// This tracker is just not available so I want to just breeze past this.					
				}
			} 
			
		
			trackIP = lowestIP;
			trackPort = lowestPort;	
			
			if(lowestPort == -1) {
				System.out.println("No trackers found");
				System.out.println("Please verify the .tracker file.");
				System.exit(5);
			}			
						

			if(verbose)
				System.out.println("Chosen Tracker: " + trackIP + ":" + trackPort);
			
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
				if(verbose)
					System.out.println(reply);
				return null;
			}

		} catch (IOException e) {
			System.err.println("Creation failed.");
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
			if(verbose)
				System.out.println(reply);
			String[] parts = reply.split(" ");
			if(parts[0].equals("success")) {
				
				if(parts[2].equals("true")) {
					changeTracker(parts[3]);
					return joinChannel(channel);
				} else {
					joined(channel);
					initGroup(parts[3]);
				}
			} else {
				System.out.println(reply);
				return null;
			}
			

		} catch (IOException e) {
			if(verbose)
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
		if(verbose)
			System.out.println("Changing Tracker to: " + trackIP + ":" + trackPort);
	}

	private static MulticastSocket createMS() {
		try {
			return new MulticastSocket();
		} catch (IOException e) {
			if(verbose)
				e.printStackTrace();
		}
		
		return null;
	}
	
	static void initGroup(String incoming) {		
		group = new PeerGroup(trackIP, trackPort, channelName);
		if(verbose)
			group.verbose();
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
				if(verbose)
					e.printStackTrace();
				System.exit(7);
			} catch (IOException e) {
				System.out.println("Registration Failed..");
				if(verbose)
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
			if(verbose)
				e.printStackTrace();
		}
	}
	
	
	// Listens for user input and sends messages
	private static void chatLoop(Scanner in, MulticastSocket listen) {
		try {

			
			
			MulticastReceiver mr = new MulticastReceiver(group, listen, ID);
			if(verbose)
				mr.verbose();
		
			Thread receiver = new Thread(mr);
			
			receiver.start();
		
			
			
			MulticastSocket sock = new MulticastSocket();
			
			Boolean chatting = true;
			String message = "";
			
			int messageID = 0;
			
			bMulticast(sock, displayName + " has joined the chat.", messageID); 
			
			
			while(chatting) {
				message = in.nextLine();
				// Enforce 100 character limit
				if(message.length() > 100) {
					message = message.substring(0, 99);
					if(verbose)
						System.out.println("\t\t\tDEBUG: String exceeded limit");
				}
				if(message.equals("/quit")) {
					try {
						
						bMulticast(sock, displayName + " has left the chat.", messageID); 
						
						
						openSocket();
						sockOut.write("leave " + ID + " " + channelName + "\n\n");
						sockOut.flush();
						cleanUp();
					} catch (IOException e) {
						
						e.printStackTrace();
					}
					break;
				} else {
					bMulticast(sock, (displayName + ": " + message), messageID);
				}
				messageID++;
			}

			receiver.interrupt();
			sock.close();
			
		} catch (IOException e1) {
			
			e1.printStackTrace();
		}
		
		System.out.println("Quiting...");
	}
	
	// Sends out a UDP message to each peer one at a time
	private static synchronized void bMulticast(DatagramSocket sock, String plainText, int messageID) {
		//System.out.println(message);
		
		String message = ID + "*" + messageID + "~" + plainText;
		
		byte[] buffer = message.getBytes();
		DatagramPacket packet;
		
		for(Entry<Integer, Peer> idPeer : group) {
			Peer peer = idPeer.getValue();
			packet = new DatagramPacket(buffer, buffer.length, peer.getAddr(), peer.getPort());
			try {
				sock.send(packet);
			} catch (IOException e) {
				if(verbose)
					e.printStackTrace();
			}
		}
	}
}
