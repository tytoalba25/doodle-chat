import java.io.BufferedReader;
import java.io.BufferedWriter;
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
		init(in, args);
		register();
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
	private static void init(Scanner in, String args[]) {
		
		String tracker = "";
		
		if(args.length < 1) {
			System.out.println("No args");
			System.out.println("Please enter tracker IP:Port");
			tracker = in.nextLine();
			
		} else {
			tracker = args[0];
		}
		String[] parts = tracker.split(":");
		try {
			trackIP = parts[0];
			trackPort = Integer.parseInt(parts[1]);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			System.exit(2);
		}
		
		System.out.println("Enter display-name");
		displayName = in.nextLine();
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
	
	//TODO: Connects to server to acquire channel list and outputs to console
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
	
	//TODO: Creates and joins a channel
	private static MulticastSocket createChannel(String channel) {
		MulticastSocket ms = null;
		
		if(!channelExists(channel)) {
			openSocket();
			try {
				sockOut.write("create " + ID + " " + channel + "\n\n");
				sockOut.flush();

				String reply = sockIn.readLine();
				if(reply.equals("success")) {
					ms = joinCreatedChannel(channel);
				} else {
					System.out.println(reply);
				}
				
			} catch (IOException e) {
				System.out.println("Creation failed.");
				e.printStackTrace();
				System.exit(3);
			}
			cleanUp();
		}
		
		return ms;
	}
	
	
	private static MulticastSocket joinCreatedChannel(String channel) {
		openSocket();
		
		MulticastSocket ms = createMS();
		try {
			sockOut.write("join " + ID  + " " + channel + " " + ms.getLocalPort() + "\n\n");
			sockOut.flush();
			
			joined(channel);
			
			initGroup(sock.getLocalAddress().toString().substring(1) + ":" + ms.getLocalPort() + "/" + ID);

		} catch (IOException e) {
			System.out.println(e.getMessage());
			//e.printStackTrace();
			System.exit(3);
		}

		// Get list of people
		// Convert to InetAddress
		cleanUp();
		return ms;
	}
	
	
	//TODO: Connects to pre-existing channel
	private static MulticastSocket joinChannel(String channel) {
		MulticastSocket ms = createMS();
		
		if(channelExists(channel)) {
			openSocket();
			try {
				sockOut.write("join " + ID  + " " + channel + " " + ms.getLocalPort() + "\n\n");
				sockOut.flush();
				
				joined(channel);
				
				String s = sockIn.readLine();
				s = s.split("\\s+")[1];
				initGroup(s);
				
			} catch (IOException e) {
				System.out.println(e.getMessage());
				//e.printStackTrace();
				System.exit(3);
			}
			
			// Get list of people
			// Convert to InetAddress
			
			cleanUp();
		}
		
		return ms;
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
	
	// TODO: Checks if the channel you are attempting to join or create already exists
	private static Boolean channelExists(String channel) {
		
		String channelList = listChannels();
		//System.out.println(channelList);
		String[] channels = channelList.split(",");
		
		for(int i = 0; i < channels.length; i++) {
			if(channel.equals(channels[i]))
				return true;
		}
		
		
		return false;
	}
	
	// Listens for user input and sends messages
	private static void chatLoop(Scanner in, MulticastSocket listen) {
		
		// Toggles behavior between multicast and P2PUDP
		boolean MC = false;
		
		try {

		//	groupMask = InetAddress.getByName("225.4.5.6");
			
			Thread receiver;
			// TODO: Remove if/else once multicast is functional
		/*	if(MC) {
				receiver = new Thread(new MulticastReceiver(IP, groupMask));
				receiver.start();				
			} else {
		*/		receiver = new Thread(new MulticastReceiver(trackIP, trackPort, group, listen, ID));
				receiver.start();
		//	}
			
			
			MulticastSocket sock = new MulticastSocket();
			
			Boolean chatting = true;
			String message = "";
			
			// TODO: Remove if/else once multicast is functional
			if(MC) {
//				multicast(sock, displayName + " has left the chat."); 
			} else  {
				P2PUDP(sock, displayName + " has joined the chat."); 
			}
			
			while(chatting) {
				message = in.nextLine();
				if(message.equals("/quit")) {
					try {
						// TODO: Remove if/else once multicast is functional
						if(MC) {
//							multicast(sock, displayName + " has left the chat."); 
						} else  {
							P2PUDP(sock, displayName + " has left the chat."); 
						}
						
						openSocket();
						sockOut.write("leave " + ID + " " + channelName + "\n\n");
						sockOut.flush();
						cleanUp();
					} catch (IOException e) {
						
						e.printStackTrace();
					}
					break;
				} else {	
					// TODO: Remove if/else once multicast is functional
					if(MC) {
//						multicast(sock, (displayName + ": " + message));
					} else  {
						P2PUDP(sock, (displayName + ": " + message));
					}
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
	private static synchronized void P2PUDP(DatagramSocket sock, String plainText) {
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
/*	
	// TODO: Fix this
	// Sends a multicast message to group.
	private static synchronized void multicast(MulticastSocket sock, String message) {
		byte[] buffer = message.getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupMask, 5556);
		try {
			sock.send(packet);
			System.out.println("Sent packet with message: " + message);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
*/	
}
