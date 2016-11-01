import java.util.ArrayList;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Tracker implements Runnable {
	
	// Our static shared directory of channels
	static ArrayList<Channel> channels;
	
	// Network stuff
	Socket csocket;
	Tracker(Socket csocket, ArrayList<Channel> channels) {
		this.channels = channels;
		this.csocket = csocket;
	}
	
	
	public static void main(String args[]) throws Exception {
		// Make our directory
		channels = new ArrayList<Channel>();
		
		// Open a server socket, listen for connections and create threads for them
		ServerSocket ssock = new ServerSocket(1234);
		System.out.println("Listening");
		while(true) {
			Socket sock = ssock.accept();
			System.out.println("Connected");
			new Thread(new Tracker(sock, channels)).start();
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
		for (int i=0; i<channels.size(); i++) {
			if (channels.get(i).name.equals(n)) {
				return 0;
			}
		}
		channels.add(new Channel(n));
		return 1;
	}
	
	// Join a channel already in the directory
	// If the channel doesn't exist, return 0
	// Otherwise add the ip to the channel and return 1
	public int joinChannel(String n, String m) {
		for (int i=0; i<channels.size(); i++) {
			if (channels.get(i).name.equals(n)) {
				channels.get(i).addMember(new IP(m));
				return 1;
			}
		}
		return 0;
	}
	
	// Leave a channel that you are already a member of
	// If the channel or member doesn't exist, return 0
	// Otherwise remove the member from the channel and return 1
	public int leaveChannel(String n, String m) {
		Channel c = channelExists(n);
		if (c == null) {
			return 0;
		}
		for (int i=0; i<c.members.size(); i++) {
			if (c.members.get(i).address.equals(m)) {
				c.members.remove(i);
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
			val += c.members.get(i);
			if (i < c.members.size()-1) {
				val += ",";
			}
		}
		return val;
	}
	
	public void run() {
		try {
			// Create our input/output
			BufferedReader in = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
			PrintStream out = new PrintStream(csocket.getOutputStream());
			
			// Read request
			String input = in.readLine();
			String output = "";
			
			// Process a get request
			if (input.startsWith("get")) {
				System.out.println("Processing get request");
				output = getChannels();
				output += "\n\n";
			}
			
			// Process a create request
			if (input.startsWith("create")) {
				System.out.println("Processing create request");
				String name = input.split("\\s+")[1];
				if (addChannel(name) != 1 || joinChannel(name, csocket.getRemoteSocketAddress().toString()) != 1) {
					output = "failure\n\n";
				} else {
					output = "success\n\n";
				}
			}
			
			// Process a join request
			if (input.startsWith("join")) {
				System.out.println("Processing join request");
				String name = input.split("\\s+")[1];
				if (joinChannel(name, csocket.getRemoteSocketAddress().toString()) != 1) {
					output = "failure\n\n";
				} else {
					output = "success";
					output += getMembers(name);
					output += "\n\n";
				}
			}
			
			// Process a leave request
			if (input.startsWith("leave")) {
				System.out.println("Processing leave request");
				String name = input.split("\\s+")[1];
				if (leaveChannel(name, csocket.getRemoteSocketAddress().toString()) != 1) {
					output = "failure\n\n";
				} else {
					output = "success\n\n";
				}
			}
			
			// Process a request-ping request NOT IMPLEMENTED
			if (input.startsWith("request-ping")) {
				output = "failure\n\n";
			}

			// Process a ping request NOT IMPLEMENTED
			if (input.startsWith("ping")) {
				output = "failure\n\n";
			}
			
			// Send response
			out.print(output);
			
			// Clean up
			out.close();
			in.close();
			csocket.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	// Channel class
	// These are individual members of the channels ArrayList
	// Each one contains a name and a list of members, represented by IP objects
	public class Channel {
		public String name;
		private ArrayList<IP> members;
		
		public Channel(String n) {
			name = n;
			members = new ArrayList<IP>();
		}
		
		public boolean sameName(Channel c) {
			if (name == c.name) {
				return true;
			} else {
				return false;
			}
		}
		
		// Add a member to the channel
		// If the member already exists, return 0
		// Otherwise add the member and return 1
		public int addMember(IP m) {
			if (members.contains(m)) {
				return 0;
			} else {
				members.add(m);
				return 1;
			}
		}
		
		// Remove a member from the channel
		// If the member doesn't exist, return 0
		// Otherwise remove the member and return 1
		public int removeMember(IP m) {
			if (members.contains(m)) {
				members.remove(m);
				return 1;
			} else {
				return 0;
			}
		}
		
		// Return a string representing the channel
		public String toString() {
			return name;
		}	
	}
	
	// IP class
	// Just a simple container for IP addresses used as members of the Channel class
	public class IP {
		private String address;
		
		public IP(String i) {
			address = i;
		}
		
		public boolean sameName(IP i) {
			if (address == i.address) {
				return true;
			} else {
				return false;
			}
		}
		
		public String toString() {
			return address;
		}
	}
	
}