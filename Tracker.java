import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;
import java.util.Arrays;
import java.util.Timer;
import java.util.concurrent.ThreadLocalRandom;

import java.lang.String;

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
import java.io.PrintWriter;

import static java.lang.System.out;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

// Main Tracker Class
public class Tracker implements Runnable {

	// Our static shared directory of channels
	static Directory dir;

	// Our directory of trackers
	static ArrayList<String> addressList;

	// Timers
	static Map<Integer, Future<?>> timers;
	static ScheduledThreadPoolExecutor pool;
	static final int MAX_TIMERS = 50;
	static final int PING_INTERVAL = 2;
	static Boolean recover = false;

	// ID counter
	static int id;

	// Network stuff
	Socket csocket;

	// Constructor
	Tracker(Socket csocket, Directory dir) {
		this.dir = dir;
		this.csocket = csocket;
	}

	// Main function
	// This sets up the directory and starts listening for connections
	public static void main(String args[]) throws Exception {

		// Check arguments
		if (args.length == 1)  {
			if(args[1].equals("-r")) {
				recover = true;
			} else {
				System.out.println("Acceptable arguments: -r for recovery");
			}			
		} 
		
		
		int port = 5555;
		
		// Display the address that should be used to connect to the tracker
		System.out.println("In order to connect to the tracker, use:\n\tAddress: " + InetAddress.getLocalHost()
				+ "\n\tPort: " + port);

		// Make our directory
		dir = new Directory();
		pool = new ScheduledThreadPoolExecutor(MAX_TIMERS);
		timers = new Hashtable<Integer, Future<?>>();

		// Asks user for a list of Tracker addresses
		String[] tempArray;
		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter a comma sperated list of tracker addresses (empty for none)");
		String scannerInput = scanner.nextLine();
		if (scannerInput.equals("")) {
			addressList = new ArrayList<String>();
		} else {
			tempArray = scannerInput.split(",");
			addressList = new ArrayList<String>(Arrays.asList(tempArray));
		}
		scanner.close();

		// Connects to listed trackers
		if (addressList.size() != 0) {
			String[] resp = rMulticast(addressList, 5555, "new-tracker -1");
		}

		// Trys to load tracker_copy.xml
		if(recover) {
			System.out.println("Attempting to recover from tracker_copy.xml");
			File f = new File("tracker_copy.xml");
			if (f.exists() && !f.isDirectory()) {
				dir.loadTracker("tracker_copy.xml");
			} else {
				System.out.println("Unable to open tracker_copy.xml, starting blank tracker");
			}
		} else {
			System.out.println("Starting fresh");
			// Attempt to clear the recovery file
			File f = new File("tracker_copy.xml");
			if (f.exists() && !f.isDirectory()) {
				PrintWriter writer = new PrintWriter(f);
				writer.print("");
				writer.close();
			}
		}

		// Set up our identifier
		id = 1;

		// Open a server socket, listen for connections and create threads for them
		ServerSocket ssock = new ServerSocket(port);

		// Set up periodic tracker updates
		// This also handles detecting crashed trackers
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				try {
					String content = new String(readAllBytes(get("tracker_copy.xml")));
					String[] response = rMulticast(addressList, 5555, content);

					for (int i=0; i<response.length; i++) {
						System.out.println("\t\t\t" + response[i]);
						
						try {						
							if (response[i].equals("error")) {
								System.out.println(addressList.get(i) + " has died");
								recoverDirectory("tracker_backup.xml");
								addressList.remove(i);	
							}
						} catch (NullPointerException e) {
							System.out.println(addressList.get(i) + " has died");
							recoverDirectory("tracker_backup.xml");
							addressList.remove(i);	
						}
					}
				} catch (IOException e) {
					System.out.println("Unable to load tracker_copy");
				}
			}
		}, 500, 5000);

		while (true) {
			Socket sock = ssock.accept();
			new Thread(new Tracker(sock, dir)).start();
			dir.saveTracker("tracker_copy.xml");
		}
	}

	public static void recoverDirectory(String deadAddress) {
		System.out.println("Taking over " + deadAddress + "'s channels");

		Directory deadDirectory = new Directory();
		deadDirectory.loadTracker(deadAddress);

		String[] channels = deadDirectory.getChannels().split(",");
		for (int i=0; i<channels.length; i++) {
			dir.addChannel(channels[i]);
		}

		try {
			DatagramSocket sock = new DatagramSocket();
			DatagramPacket packet;
			InetAddress ip;
			int port;
			String message = "0~recovery";
			byte[] buffer = message.getBytes();
			String[] members;
			for(int i=0; i<deadDirectory.getPop(); i++) {
				members = deadDirectory.getMembers(channels[i]).split(",");
				for (int j=0; j<members.length; j++) {
					try {
						ip = InetAddress.getByName(members[j].split(":")[0]);
						port = Integer.parseInt(members[j].split(":")[1].split("/")[0]);
						packet = new DatagramPacket(buffer, buffer.length, ip, port);
						sock.send(packet);
					} catch (Exception e) {
						System.out.println(e);
					}
				}
			}	
		} catch (Exception e) {
			System.out.println(e);
		}	
			
	}

	// Sends a message over TCP to a series of addresses
	// Returns a list of all of the responses
	public static String[] rMulticast(ArrayList<String> addresses, int port, String message) {
		Socket sock;
		PrintStream out;
		BufferedReader in;

		if (addresses == null) {
			return null;
		}

		String[] response = new String[addresses.size()];
		for (int i=0; i<addresses.size(); i++) {
			try {
				sock = new Socket(addresses.get(i), port);
				out = new PrintStream(sock.getOutputStream());
				in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				out.println(message);
				out.flush();
				response[i] = in.readLine();
				out.close();
				in.close();
				sock.close();

				System.out.println("\t\t DEBUG: Message sent to " + addresses.get(i));

			} catch (Exception e) {
				System.out.println(addresses.get(i) + " not reached");
				response[i] = "error";
			}
		}

		return response;
	}
			

	// Produce a new ID
	public int giveID(String val) {
		int salt = ThreadLocalRandom.current().nextInt(0, 1000001);
		System.out.println(val + "||" + salt);
		val = val + salt;
		return Math.abs(val.hashCode());
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
			int id;
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
					output = Integer.toString(giveID(csocket.getRemoteSocketAddress().toString().substring(1)));
				}

				// Process a population request
				if (input.startsWith("pop")) {
					System.out.println("\tProcessing pop request");
					output = "success " + parts[1] + " " + dir.getPop();				
				}

				// Process a get request
				if (input.startsWith("get")) {
					System.out.println("\tProcessing get request");
					output = dir.getChannels();
				}

				// Process a create request
				if (input.startsWith("create")) {
					System.out.println("\tProcessing create request");
					boolean duplicateFlag = false;

					System.out.println("Checking other tracker's for channel " + parts[2]);
					if (addressList.size() != 0) {
						String[] resp = rMulticast(addressList, 5555, "exists 0 " + parts[2]);

						if (resp != null) {
							for (int i=0; i<resp.length; i++) {
								System.out.println("\t\t" + resp[i]);
								if (resp[i].split(" ")[2].equals("success")) {
									System.out.println("\t" + addressList.get(i));
									duplicateFlag = true;
								}
							}
						}
					}
					if (duplicateFlag == false) {
						if (dir.addChannel(parts[2]) != 1) {
							output = "failure";
						} else {
							output = "success";
						}
					} else {
						output = "failure";
					}
				}

				// Process a join request
				if (input.startsWith("join")) {
					System.out.println("\tProcessing join request");
					boolean duplicateFlag1 = false;
					int index = 0;
					
					if (addressList.size() != 0) {

						System.out.println("Checking other tracker's for channel " + parts[2]);
						String[] resp = rMulticast(addressList, 5555, "exists 0 " + parts[2]);

						

						if (resp != null) {
							for (int i=0; i<resp.length; i++) {
								System.out.println("\t\t" + resp[i]);
								if (resp[i].split(" ")[2].equals("success")) {
									System.out.println("\t" + addressList.get(i));
									duplicateFlag1 = true;
									index = i;
								}
							}
						}
					}
								
					if (duplicateFlag1 == true) {
						output = "success " + Integer.parseInt(parts[1]) + " true " + addressList.get(index) + ":5555";
						System.out.println("=====\n" + output + "=====\n");
					} else if (dir.joinChannel(parts[2], parts[3],
							csocket.getRemoteSocketAddress().toString().substring(1).split(":")[0],
							Integer.parseInt(parts[1])) != 1) {
						output = "failure";
					} else {
						output = "success ";
						output += parts[1] + " false ";
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

				// Process an exists request
				if (input.startsWith("exists")) {
					System.out.println("\tProcessing exists request");

					String[] tIP = new String[1];
					tIP[0] = csocket.getRemoteSocketAddress().toString().substring(1).split(":")[0];

					if (dir.channelExists(parts[2]) != null) {
						output = "exists-response 0 success";
					} else {
						output = "exists-response 0 failure";
					}
				}
				
				// Process a new-tracker request
				if (input.startsWith("new-tracker")) {
					System.out.println("Processing new-tracker request");
					
					String msg = csocket.getRemoteSocketAddress().toString().substring(1).split(":")[0];

					System.out.println(msg);

					addressList.add(msg);

					output = "success";
				}

				// Process a directory-update request
				if (input.startsWith("<?xml")) {
					System.out.println("Recieving update from " + csocket.getRemoteSocketAddress().toString().substring(1).split(":")[0]);

					System.out.println(input);

					PrintWriter fout = new PrintWriter("tracker_backup.xml");
					fout.println(input);
					fout.close();
				
					output = "success";
				}

				// Process a recovery request
				if (input.startsWith("recover")) {
					System.out.println("Processing recover request");

					DatagramSocket dsock = new DatagramSocket();
					DatagramPacket packet;
					String msg = "0~tracker " + InetAddress.getLocalHost().getHostAddress() + ":5555";
					byte[] buffer = msg.getBytes();
					InetAddress ipRM;
					int portRM;

					String channelName = parts[2];
					String[] recoveryMembers = parts[3].split(",");
					for (int i=0; i<recoveryMembers.length; i++) {
						int portNum = Integer.parseInt(recoveryMembers[i].split(":")[1].split("/")[0]);
						String addressName = recoveryMembers[i].split(":")[0];
						int idNum =  Integer.parseInt(recoveryMembers[i].split("/")[1]);
						dir.joinChannel(parts[2], portNum + "", csocket.getRemoteSocketAddress().toString().substring(1).split(":")[0], Integer.parseInt(parts[1]));

						for (int j=0; j<dir.channelExists(parts[2]).getPopulation(); j++) {
							ipRM =  InetAddress.getByName(dir.channelExists(parts[2]).getMemberByIndex(j).getIP());
							portRM = dir.channelExists(parts[2]).getMemberByIndex(j).getPort();					

							System.out.println("\t==" + ipRM);
							System.out.println("\t==" + portRM);

							packet = new DatagramPacket(buffer, buffer.length, ipRM, portRM);
							dsock.send(packet);	
						}
					}
					dsock.close();
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
