import java.util.Hashtable;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java.net.UnknownHostException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;

// Main Tracker Class
public class Tracker implements Runnable {

	// Our static shared directory of channels
	static Directory dir;

	// Timers
	static Map<Integer, Future<?>> timers;
	static ScheduledThreadPoolExecutor pool;
	static final int MAX_TIMERS = 50;
	static final int PING_INTERVAL = 2;
	static Boolean running = true;

	// ID counter
	static int id;

	// Network stuff
	Socket csocket;

	// Constructor
	Tracker(Socket csocket, Directory dir) {
		Tracker.dir = dir;
		this.csocket = csocket;
	}

	// Main function
	// This sets up the directory and starts listening for connections
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
		
		// Display the address that should be used to connect to the tracker
		System.out.println("In order to connect to the tracker, use:\n\tAddress: " + InetAddress.getLocalHost()
				+ "\n\tPort: " + port);

		// Make our directory
		dir = new Directory();
		pool = new ScheduledThreadPoolExecutor(MAX_TIMERS);
		timers = new Hashtable<Integer, Future<?>>();

		// Trys to load tracker_copy.xml
		File f = new File("tracker_copy.xml");
		if (f.exists() && !f.isDirectory()) {
			dir.loadTracker("tracker_copy.xml");
		} else {
			System.out.println("Unable to open tracker_copy.xml, starting blank tracker");
		}

		// Set up our identifier
		id = 1;

		// Open a server socket, listen for connections and create threads for them
		ServerSocket ssock = new ServerSocket(port);
		while (running) {
			Socket sock = ssock.accept();
			new Thread(new Tracker(sock, dir)).start();
			dir.saveTracker("tracker_copy.xml");
		}
		ssock.close();
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

	// Send a message to update the members of a channel when a change in
	// membership occurs
	@SuppressWarnings("resource")
	public int updateMembers(String n) {
		System.out.println("Sending update to members of " + n);
		Channel c = dir.channelExists(n);
		if (c == null) {
			return 0;
		}
		synchronized (c) {
			String message = "0~update ";
			message += dir.getMembers(n);
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
			DatagramPacket packet;
			for (int i = 0; i < c.getPopulation(); i++) {
				try {
					ip = InetAddress.getByName(c.getMemberByIndex(i).getIP());
					port = c.getMemberByIndex(i).getPort();
					packet = new DatagramPacket(buffer, buffer.length, ip, port);
					sock.send(packet);
				} catch (IOException e) {
					return 0;
				}
			}

			sock.close();
		}

		System.out.println("\tUpdate sent to " + dir.getMembers(n));
		return 1;
	}

	//
	public int pingMember(final String n, final int memberID) {
		synchronized (timers) {
			if (timers.containsKey(memberID))
				return 1;
		}
		System.out.println("Sending ping to #" + memberID);

		Channel c = dir.channelExists(n);

		synchronized (c) {
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
				synchronized (timers) {
					if (!timers.containsKey(memberID)) {
						Future<?> timeout = pool.schedule(new TimerTask() {
							@Override
							public void run() {
								synchronized (timers) {
									timers.remove(memberID);
									dir.leaveChannel(n, memberID);
									System.out.println("TIMEOUT: " + memberID);
									updateMembers(n);
									dir.saveTracker("tracker_copy.xml");
								}
							}

						}, PING_INTERVAL, TimeUnit.SECONDS);

						timers.put(memberID, timeout);
					}
				}

			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				return 0;
			}
		}

		return 1;
	}

	// 
	@SuppressWarnings("resource")
	public int processPing(int ID, String n) {
		System.out.println("KEEP-ALIVE MEMBER #" + ID);
		Channel c = dir.channelExists(n);

		synchronized (c) {
			if (c == null) {
				return 0;
			}
			try {
				synchronized (timers) {
					Future<?> fut = timers.get(ID);
					if (fut != null) {
						fut.cancel(false);
						timers.remove(ID);
					}
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
			DatagramPacket packet;
			for (int i = 0; i < c.getPopulation(); i++) {
				try {
					//ip = InetAddress.getByName(c.members.get(i).getIP());
					ip = InetAddress.getByName(c.getMemberByIndex(i).getIP());
					//port = c.members.get(i).getPort();
					port = c.getMemberByIndex(i).getPort();
					packet = new DatagramPacket(buffer, buffer.length, ip, port);
					sock.send(packet);
				} catch (IOException e) {
					return 0;
				}
			}

			sock.close();

			System.out.println("\tKeep-Alive sent to " + dir.getMembers(n));
		}

		return 1;
	}

	// This is the thread called when a client connects to the Tracker
	// It reads the message from the input and splits it around any spaces into parts
	// The first part is the request
	// The second part is the sender's id
	// Any remaining parts are additional info needed for the request
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
					output = dir.getChannels();
				}

				// Process a create request
				if (input.startsWith("create")) {
					System.out.println("\tProcessing create request");
					if (dir.addChannel(parts[2]) != 1) {
						output = "failure";
					} else {
						output = "success";
					}
				}

				// Process a join request
				if (input.startsWith("join")) {
					System.out.println("\tProcessing join request");
					// Channel Name, IP, ID
					if (dir.joinChannel(parts[2], parts[3],
							csocket.getRemoteSocketAddress().toString().substring(1).split(":")[0],
							Integer.parseInt(parts[1])) != 1) {
						output = "failure";
					} else {
						output = "success " + parts[1] + " ";
						output += "false "; // Reroute flag
						output += dir.getMembers(parts[2]);
						output += "";
						updateMembers(parts[2]);
					}
				}

				// Process a leave request
				if (input.startsWith("leave")) {
					System.out.println("\tProcessing leave request");
					if (dir.leaveChannel(parts[2], Integer.parseInt(parts[1])) != 1) {
						output = "failure";
					} else {
						output = "success";
						updateMembers(parts[2]);
					}
				}

				// Process a request-ping request
				if (input.startsWith("request-ping")) {
					pingMember(parts[2], Integer.parseInt(parts[1]));
				}

				// Process a ping request
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
	}
}
