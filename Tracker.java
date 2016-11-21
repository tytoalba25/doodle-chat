import java.util.ArrayList;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Tracker implements Runnable {
	
	// Our static shared directory of channels
	static ArrayList<Channel> channels;

	static int id;
	
	// Network stuff
	Socket csocket;
	Tracker(Socket csocket, ArrayList<Channel> channels) {
		this.channels = channels;
		this.csocket = csocket;
	}
	
	public static void main(String args[]) throws Exception {
		// Check arguments
		if (args.length != 1) {
			System.out.println("Usage:\n\t java Tracker <port>");
			System.exit(1);
		}
		int port = 0;
		try {
			port = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			System.out.println("Invalid port");
			System.exit(1);
		}

		// Display the ip that should be used to connect to the tracker
		System.out.println("In order to connect to the tracker, use:\n\tAddress: " + InetAddress.getLocalHost() + "\n\tPort: " + port);

		// Make our directory
		channels = new ArrayList<Channel>();

		// Set up our identifier
		id = 1;

		// Open a server socket, listen for connections and create threads for them
		ServerSocket ssock = new ServerSocket(port);
		System.out.println("Listening for clients on port " + port);
		while(true) {
			Socket sock = ssock.accept();
			new Thread(new Tracker(sock, channels)).start();
		}
	}

	// Produce a new ID
	public int giveID() {
		id += 1;
		return id - 1;
	}

	// Make sure that the ID given is registered
	public boolean validID(int i) {
		if (i > id) {
			return false;
		} else {
			return true;
		}
	}
	
	// Checks if a channel exists in the directory
	// If it does, return the channel
	// If it doesn't, return null
	public Channel channelExists(String n) {
		for (int i=0; i<channels.size(); i++) {
			if (channels.get(i).name.equals(n)) {
				return channels.get(i);
			}
		}
		return null;
	}
	
	// Add a channel to the directory
	// If the channel already exists, return 0
	// Otherwise create the channel and return 1
	public int addChannel(String n) {
		Channel c = new Channel(n);
		if (channelExists(n) != null) {
			return 0;
		}
		channels.add(new Channel(n));
		return 1;
	}
	
	// Join a channel already in the directory
	// If the channel doesn't exist, return 0
	// Otherwise add the ip to the channel and return 1
	public int joinChannel(String n, String p, String m, int id) {
		Channel c = channelExists(n);
		if (c != null) {
			c.addMember(new Member(m, p, id));
			updateMembers(n);
			return 1;
		}
		return 0;
	}
	
	// Leave a channel that you are already a member of
	// If the channel or member doesn't exist, return 0
	// Otherwise remove the member from the channel and return 1
	public int leaveChannel(String n, int id) {
		Channel c = channelExists(n);
		if (c == null) {
			return 0;
		}
		for (int i=0; i<c.members.size(); i++) {
			if (c.members.get(i).id == id) {
				c.members.remove(i);
				updateMembers(n);
				return 1;
			}
		}
		return 0;
	}
	
	// Return a single line string listing the names of all channels seperated by commas
	public String getChannels() {
		String val = "";
		for (int i=0; i<channels.size(); i++) {
			val += channels.get(i);
			if (i < channels.size()-1) {
				val += ",";
			}
		}
		return val;
	}
	
	// Return a single line string listing the ip's of all members of a channel, seperated by commas
	public String getMembers(String channel) {
		String val = "";
		Channel c = null;
		for (int i=0; i<channels.size(); i++) {
			if (channels.get(i).name.equals(channel)) {
				c = channels.get(i);
				break;
			}
		}
		if (c == null) {
			return "";
		}
		for (int i=0; i<c.members.size(); i++) {
			val += c.members.get(i).toString();//.split(":")[0];
			if (i < c.members.size()-1) {
				val += ",";
			}
		}
		return val;
	}

	// Send a message to update the members of a channel when a change in membership occurs
	public int updateMembers(String n) {
		System.out.println("UPDATING MEMBERS");
		Channel c = channelExists(n);
		if (c == null) {
			return 0;
		}
		String message = "0~update ";
		message += getMembers(n);
		message += "\n\n";

		System.out.println(message);
		
		DatagramSocket sock;
		byte[] buffer = message.getBytes();
		
		try {
			sock = new DatagramSocket(5556);
		} catch (IOException e) {
			return 0;
		}

		InetAddress ip;
		int port;
		int id;
		DatagramPacket packet;
		for (int i=0; i<c.members.size(); i++) {
			try {
			ip = InetAddress.getByName(c.members.get(i).getIP());
			port = Integer.parseInt(c.members.get(i).getPort());
			packet = new DatagramPacket(buffer, buffer.length, ip, port);
				sock.send(packet);
			} catch (IOException e) {
				return 0;
			}
		}

		sock.close();

		System.out.println("\tUpdate sent to " + getMembers(n));
		return 1;
	}
		

	// This is the thread called when a client connects to the Tracker
	public void run() {
		try {
			System.out.println("Client connected from " + csocket.getRemoteSocketAddress().toString());

			// Create our input/output
			BufferedReader in = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
			PrintStream out = new PrintStream(csocket.getOutputStream());
			
			// Read request
			String input = in.readLine();
			System.out.println("\tInput: " + input);
			String output = "";

			// Split message by space
			String[] parts = input.split("\\s+");
			
			// If any errors occur (most likely nullpointer) assume malformed request
			try {

			// Process a register request
			if (input.startsWith("register")) {
				System.out.println("\tProcessing register request");
				output = Integer.toString(giveID());
			// If not a register request then make sure that id is valid
			} else if (!validID(Integer.parseInt(parts[1]))) {
				output = "failure";
				input = "";
			}

			// Process a get request
			if (input.startsWith("get")) {
				System.out.println("\tProcessing get request");
				output = getChannels();
			}
			
			// Process a create request
			if (input.startsWith("create")) {
				System.out.println("\tProcessing create request");
				if (addChannel(parts[2]) != 1) {
					output = "failure";
				} else {
					output = "success";
				}
			}
			
			// Process a join request
			if (input.startsWith("join")) {
				System.out.println("\tProcessing join request");
							// Channel Name, IP, ID
				if (joinChannel(parts[2], parts[3], csocket.getRemoteSocketAddress().toString().substring(1).split(":")[0], Integer.parseInt(parts[1])) != 1) {	
					output = "failure";
				} else {
					output = "success ";
					output += getMembers(parts[2]);
					output += "";
				}
			}
			
			// Process a leave request
			if (input.startsWith("leave")) {
				System.out.println("\tProcessing leave request");
				if (leaveChannel(parts[2], Integer.parseInt(parts[1])) != 1) {
					output = "failure";
				} else {
					output = "success";
				}
			}
			
			// Process a request-ping request NOT IMPLEMENTED
			if (input.startsWith("request-ping")) {
				output = "Ping requested for peer #" + parts[1];
			}

			// Process a ping request NOT IMPLEMENTED
			if (input.startsWith("ping")) {
				output = "Ping Failure";
			}
			
			} catch (Exception e) {
				System.out.println(e);
				e.printStackTrace();
				output = "invalid";
			}

			// Send response
			out.println(output);
			out.flush();
			
			// Clean up
			out.close();
			in.close();
			csocket.close();

		} catch (IOException e) {
			System.out.println(e);
			e.printStackTrace();
		}
	}
	
	// Channel class
	// These are individual members of the channels ArrayList
	// Each one contains a name and a list of members, represented by Member objects
	public class Channel {
		public String name;
		private ArrayList<Member> members;
		
		public Channel(String n) {
			name = n;
			members = new ArrayList<Member>();
		}
		
		// Add a member to the channel
		// If the member already exists, return 0
		// Otherwise add the member and return 1
		public int addMember(Member m) {
			for (int i=0; i<members.size(); i++) {
				if (m.getID() == members.get(i).getID()) {
					return 0;
				}
			}
			members.add(m);
			return 1;
		}
		
		// Return a string representing the channel
		public String toString() {
			return name;
		}	
	}
	
	// Member class
	// Contains a pairing of the user's ip address, UDP port, and unique id
	public class Member {
		private String address;
		private int id;
		private String port;
		
		public Member(String a, String p, int i) {
			address = a;
			id = i;
			port = p;
		}
		
		public int getID() {
			return id;
		}

		public String toString() {
			return address + ":" + port + "/" + id;
		}
		
		public String getIP() {
			return address;
		}
		
		public String getPort() {
			return port;
		}
	}
	
}
