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

	// Our directory of trackers
	static ArrayList<String> addressList;

	// Timers
	static Map<Integer, Future<?>> timers;
	static ScheduledThreadPoolExecutor pool;
	static final int MAX_TIMERS = 50;
	static final int PING_INTERVAL = 2;

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

		// Asks user for a list of Tracker addresses
		String[] tempArray;
		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter a comma sperated list of tracker addresses (empty for none)");
		String scannerInput = scanner.nextLine();
		if (scannerInput.equals("")) {
			addressList = null;
		} else {
			tempArray = scannerInput.split(",");
			addressList = new ArrayList<String>(Arrays.asList(tempArray));
		}
		scanner.close();

		// Connects to listed trackers
	/*	if (addressList != null) {
			String[] tempArray1 = new String[addressList.size()];
			tempArray1 = addressList.toArray(tempArray1);
			rMulticast(tempArray1, 5555, "new-tracker -1");
		}
*/
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
		while (true) {
			Socket sock = ssock.accept();
			new Thread(new Tracker(sock, dir)).start();
			dir.saveTracker("tracker_copy.xml");
		}
	}

	// Sends a message over TCP to a series of addresses
	// Returns a list of all of the responses
	public static String[] rMulticast(String[] addresses, int port, String message) {
		Socket sock;
		PrintStream out;
		BufferedReader in;

		if (addresses == null) {
			return null;
		}

		String[] response = new String[addresses.length];
		for (int i=0; i<addresses.length; i++) {
			try {
				sock = new Socket(addresses[i], port);
				out = new PrintStream(sock.getOutputStream());
				in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				out.println(message);
				out.flush();
				response[i] = in.readLine();
				out.close();
				in.close();
				sock.close();

				System.out.println("\t\t DEBUG: Message sent to " + addresses[i]);

			} catch (UnknownHostException e) {
				System.out.println(addresses[i] + " not reached");
			} catch (IOException e) {
				System.out.println(e);
				System.out.println(addresses[i] + " not reached");
			}
		}

		return response;
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
					output = Integer.toString(giveID());
				// If not a register request then make sure that id is valid
				//} else if (!validID(Integer.parseInt(parts[1]))) {
				//	output = "failure";
				//	input = "";
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
					if (addressList != null) {
						String[] tempArray = new String[addressList.size()];
						tempArray = addressList.toArray(tempArray);
						String[] resp = rMulticast(tempArray, 5555, "exists 0 " + parts[2]);

						if (resp != null) {
							for (int i=0; i<resp.length; i++) {
								System.out.println("\t\t" + resp[i]);
								if (resp[i].split(" ")[2].equals("success")) {
									System.out.println("\t" + tempArray[i]);
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
					String[] tempArray = null;
					
					if (addressList != null) {
						tempArray = new String[addressList.size()];
						tempArray = addressList.toArray(tempArray);

						System.out.println("Checking other tracker's for channel " + parts[2]);
						String[] resp = rMulticast(tempArray, 5555, "exists 0 " + parts[2]);

						

						if (resp != null) {
							for (int i=0; i<resp.length; i++) {
								System.out.println("\t\t" + resp[i]);
								if (resp[i].split(" ")[2].equals("success")) {
									System.out.println("\t" + tempArray[i]);
									duplicateFlag1 = true;
									index = i;
								}
							}
						}
					}
								
					if (duplicateFlag1 == true) {
						output = "success " + Integer.parseInt(parts[1]) + " true " + tempArray[index] + ":5555";
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
					
					System.out.println(csocket.getRemoteSocketAddress().toString().substring(1).split(":")[0]);

					addressList.add(csocket.getRemoteSocketAddress().toString().substring(1).split(":")[0]);

					output = "success";
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
