import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.net.UnknownHostException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import org.xml.sax.SAXException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class Tracker implements Runnable {
	
	// Our static shared directory of channels
	static ArrayList<Channel> channels;
	static Map<Integer, Future<?>> timers;
	static ScheduledThreadPoolExecutor pool;
	static final int MAX_TIMERS = 50;

	static int id;
	
	// This is a workaround for static/non-static
	// Ideally loadTracker will be called in main, but right now every network thread checks for this flag
	// If it is set to true, the thread will call loadTracker
	// While it is set true, the tracker will not save
	static boolean loadFlag= false;
	
	// Network stuff
	Socket csocket;
	Tracker(Socket csocket, ArrayList<Channel> channels) {
		this.channels = channels;
		this.csocket = csocket;
	}
	
	// Loads an XML file representing the tracker
	public int loadTracker(String path) {
		try {
			// Build our xml document
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(path);
			
			// Parse the xml document to build the tracker
			Element root = doc.getDocumentElement();
			String idS = root.getAttribute("counter");
			System.out.println("===\n" + idS + "===");
			if (!idS.equals("")) {
				id = Integer.parseInt(idS);
			}
			NodeList cList = doc.getElementsByTagName("channel");
			for (int i=0; i<cList.getLength(); i++) {
				Node cNode = cList.item(i);
				Element cElement = (Element) cNode;
				
				addChannel(cElement.getAttribute("name"));
				Channel c = channelExists(cElement.getAttribute("name"));
		
				NodeList mList = root.getElementsByTagName("member");
				for (int j=0; j<mList.getLength(); j++) {
						Node mNode = mList.item(j);
						Element mElement = (Element) mNode;
	
						String ip = mElement.getAttribute("ip");
						String port = mElement.getAttribute("port");
						int id = Integer.parseInt(mElement.getAttribute("id"));
	
						c.addMember(new Member(ip, Integer.parseInt(port), id));
				}
			}
			
		} catch (ParserConfigurationException e) {
			System.out.println(e);
			return 0;
		} catch (UnsupportedEncodingException e) {
			System.out.println(e);
			return 0;
		} catch (SAXException e) {
			System.out.println(e);
			return 0;
		} catch (IOException e) {
			System.out.println(e);
			return 0;
		}
		
		System.out.println("Tracker loaded");
		return 1;
	}
	
	// Saves an XML file representing the tracker
	public static int saveTracker(String path) {
		try {
			// Build our xml document
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			
			// Add the root element
			Element root = doc.createElement("tracker");
			doc.appendChild(root);
			
			// Add the id counter
			Attr counterAttr = doc.createAttribute("counter");
			counterAttr.setValue(Integer.toString(id));
			root.setAttributeNode(counterAttr);

			// Add the channels
			for (int i=0; i<channels.size(); i++) {
				Element chanElement = doc.createElement("channel");
				Attr attr = doc.createAttribute("name");
				attr.setValue(channels.get(i).name);
				chanElement.setAttributeNode(attr);
				root.appendChild(chanElement);
				// As well as the members
				for (int j=0; j<channels.get(i).members.size(); j++) {
					Element memberElement = doc.createElement("member");
					Attr idAttr = doc.createAttribute("id");
					idAttr.setValue(String.valueOf(channels.get(i).members.get(j).getID()));
					memberElement.setAttributeNode(idAttr);
					Attr ipAttr = doc.createAttribute("ip");
					ipAttr.setValue(channels.get(i).members.get(j).getIP());
					memberElement.setAttributeNode(ipAttr);
					Attr portAttr = doc.createAttribute("port");
					portAttr.setValue(Integer.toString(channels.get(i).members.get(j).getPort()));
					memberElement.setAttributeNode(portAttr);
					chanElement.appendChild(memberElement);
				}
			}

			// Write the xml to file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(path));
			transformer.transform(source, result);
			
		} catch (ParserConfigurationException e) {
			System.out.println(e);
			return 0;
		} catch (TransformerException e) {
			System.out.println(e);
			return 0;
		}
		
		System.out.println("Tracker saved");
		return 1;
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
		pool = new ScheduledThreadPoolExecutor(MAX_TIMERS);
		timers = new Hashtable<Integer, Future<?>>();

		// Trys to load tracker_copy.xml
		File f = new File("tracker_copy.xml");
		if (f.exists() && !f.isDirectory()) {
			loadFlag = true;
		} else {
			System.out.println("Unable to open tracker_copy.xml, starting blank tracker");
		}
		
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
			c.addMember(new Member(m, Integer.parseInt(p), id));
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
			port = c.members.get(i).getPort();
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
	
	public int pingMember(String n, int memberID) {
		synchronized(timers) {
			if(timers.containsKey(memberID))
				return 1;
		}
		System.out.println("PINGING MEMBER #" + memberID);
		
		Channel c = channelExists(n);
		if (c == null) {
			return 0;
		}
		
		
		String message = "0~ping \n\n";
		
		DatagramSocket sock;
		byte[] buffer = message.getBytes();
		
		try {
			sock = new DatagramSocket(5556);
			Member member = c.getMemberByID(memberID);
			String ipName = member.getIP();

			InetAddress ip = InetAddress.getByName(ipName);
			int port = member.getPort();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ip, port);
			
			sock.send(packet);

			sock.close();
			
			
			
			Future<?> timeout = pool.schedule(new TimerTask() {
					@Override
					public void run() {
						synchronized (timers) {
							timers.remove(memberID);
							leaveChannel(n, memberID);
							System.out.println("TIMEOUT: " + memberID);
							System.out.println(pool.getTaskCount());
						}
					}
				
				}, 10, TimeUnit.SECONDS
			);
			synchronized(timers) {
				timers.put(memberID, timeout);				
			}

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			return 0;
		}
		
		return 1;
	}
	
	public int processPing(int ID, String n) {		
		System.out.println("KEEP-ALIVE MEMBER #" + ID);
		Channel c = channelExists(n);
		if (c == null) {
			return 0;
		}
		try {
			synchronized (timers) {
				Future<?> fut = timers.get(ID);
				fut.cancel(false);
				timers.remove(ID);
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		String message = "0~keep-alive " + ID + "\n\n";

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
			port = c.members.get(i).getPort();
			packet = new DatagramPacket(buffer, buffer.length, ip, port);
				sock.send(packet);
			} catch (IOException e) {
				return 0;
			}
		}

		sock.close();

		System.out.println("\tKeep-Alive sent to " + getMembers(n));
		return 1;
	}
		

	// This is the thread called when a client connects to the Tracker
	public void run() {
		// Load tracker_copy.xml is flag set
		if (loadFlag == true) {
			loadTracker("tracker_copy.xml");
			loadFlag = false;
		}

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
				pingMember(parts[2], Integer.parseInt(parts[1]));
			}

			// Process a ping request NOT IMPLEMENTED
			if (input.startsWith("ping")) {
				processPing(Integer.parseInt(parts[1]), parts[2]);
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
		
		// Save the tracker after work is done
		if (loadFlag == false) {
			saveTracker("tracker_copy.xml");
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
		
		public Member getMemberByID(int ID) {
			for(Member member : members) {
				if(member.getID() == ID)
					return member;
			}
			return null;
		}
	}
	
	// Member class
	// Contains a pairing of the user's ip address, UDP port, and unique id
	public class Member {
		private String address;
		private int id;
		private int port;
		
		public Member(String a, int p, int i) {
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
		
		public int getPort() {
			return port;
		}
	}
	
}
