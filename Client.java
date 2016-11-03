import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
	static Socket sock = null;
	static BufferedReader sockIn = null;
	static BufferedWriter sockOut = null;
	static String displayName = null;
	static Boolean joined = false;
	static String IP = "";
	static int port = -1;
	static int ID = -1;
	static ArrayList<InetAddress> group = new ArrayList();
	static String channelName = "";
	
	public static void main(String args[]) {
		Scanner in = new Scanner(System.in);
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
					createChannel(parts[1]);					
				} else {
					System.out.println("Format: create $channelName");
				}
				break;
			case "join":
				if(parts.length == 2) {
					channelName = parts[1];
					joinChannel(parts[1]);					
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
		
		//TODO: Talk loop
		System.out.println("This is where chatting would take place");
		
		chatLoop(in);
		

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
			IP = parts[0];
			port = Integer.parseInt(parts[1]);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			System.exit(2);
		}
		
		System.out.println("Enter display-name");
		displayName = in.nextLine();
	}
	
	private static void openSocket() {
			
		try {
			sock = new Socket(IP, port);
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
	private static void createChannel(String channel) {
		if(!channelExists(channel)) {
			openSocket();
			try {
				sockOut.write("create " + ID + " " + channel + "\n\n");
				sockOut.flush();

				String reply = sockIn.readLine();
				if(reply.equals("success")) {
					joinCreatedChannel(channel);
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
	}
	
	
	private static void joinCreatedChannel(String channel) {
		openSocket();
		try {
			sockOut.write("join " + ID  + " " + channel + "\n\n");
			sockOut.flush();

			String s = sockIn.readLine();
			s = s.split("\\s+")[1];
			initGroup(s);


			initGroup(sockIn.readLine());

		} catch (IOException e) {
			System.out.println(e.getMessage());
			//e.printStackTrace();
			System.exit(3);
		}

		// Get list of people
		// Convert to InetAddress

		joined();
		cleanUp();
	}
	
	
	//TODO: Connects to pre-existing channel
	private static void joinChannel(String channel) {
		if(channelExists(channel)) {
			openSocket();
			try {
				sockOut.write("join " + ID  + " " + channel + "\n\n");
				sockOut.flush();
				
				String s = sockIn.readLine();
				s = s.split("\\s+")[1];
				initGroup(s);
				
				
				initGroup(sockIn.readLine());
				
			} catch (IOException e) {
				System.out.println(e.getMessage());
				//e.printStackTrace();
				System.exit(3);
			}
			
			// Get list of people
			// Convert to InetAddress
			
			joined();
			cleanUp();
		}
	}
	
	private static void initGroup(String incoming) {
		String[] members = incoming.split(",");
		
		for(String member : members) {
			// Replacing the extra colons that were at the end of the IP
			String IPs = member.replaceAll(":.*", ""); // TODO: Does the server still send port numbers?
			System.out.println(IPs);
			try {
				group.add(InetAddress.getByName(IPs));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
	
	private static void chatLoop(Scanner in) {
		
		DatagramSocket sock;
		try {
			sock = new DatagramSocket(5556);
			Thread receiver = new Thread(new MulticastReceiver(IP, sock, group));
			receiver.start();
			
			Boolean chatting = true;
			String message = "";
			
			while(chatting) {
				message = in.nextLine();
				if(message.equals("/quit")) {
					try {
						sockOut.write("leave " + ID + " " + channelName + "\n\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
				multicast(sock, (displayName + ": " + message));
			}
			
			sock.close();
			receiver.interrupt();
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Quiting...");
		
	}
	
	private static synchronized void multicast(DatagramSocket sock, String message) {
		//System.out.println(message);
		byte[] buffer = message.getBytes();
		DatagramPacket packet;
		
		for(int i = 0; i < group.size(); i++) {
			packet = new DatagramPacket(buffer, buffer.length, group.get(i), 5556);
			try {
				sock.send(packet);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println("Sent message to peer " + group.get(i).toString());
		}
	}
	
}
