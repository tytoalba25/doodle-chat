import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.util.Map.Entry;

public class MulticastReceiver implements Runnable {

	MulticastSocket sock = null;
	InetAddress tracker;
	int trackPort;
	PeerGroup group;
	Boolean MC = false;
	int ID;
	private Boolean verbose = false;
	
	
	// Constructor for Basic Multicast. IP/Multicast would be preferable
	public MulticastReceiver(String tIP, int tPort, PeerGroup group, MulticastSocket sock, int myID) {
		this.sock = sock;
		try {
			this.group = group;
			tracker = InetAddress.getByName(tIP);
			trackPort = tPort;
		} catch (UnknownHostException e) {
			
			if(verbose)
				e.printStackTrace();
		}
		
		ID = myID;
	}
	
	public void verbose() {
		verbose = true;
	}
	
	public void run() {		
		try {
			
			byte[] buffer;
			// Always listening
			while(true) {
				buffer = new byte[65500];
				
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				sock.receive(packet);
				
				recieved(packet);
			}
		} catch (SocketException e) {
			
			if(verbose)
				e.printStackTrace();
		} catch (IOException e) {
			
			if(verbose)
				e.printStackTrace();
		}		
	}
	
	// Notifies the tracker that this peer is still alive
	private void pingTracker() {
		Socket sock;
		try {
			sock = new Socket(tracker, trackPort);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			out.write("ping " + ID + " " + group.getName() + "\n\n");
			out.flush();
			out.close();
			sock.close();
		} catch (IOException e) {
			if(verbose)
				e.printStackTrace();
		}
	}
	
	private synchronized void display(String message) {
		System.out.println(message);
	}
	
	private void trackerMessage(String message) {
		if(verbose)
			System.out.println("\t\t\tDEBUG: Special Purpose Message: " + message.trim());
		String[] parts = message.split(" ");
		
		switch(parts[0].trim()) {
		
		case "update":
			display("\tNew Members: " + parts[1]);
			updateMembers(parts[1]);
			break;
		case "keep-alive":
			group.getPeerByID(Integer.parseInt(parts[1].trim())).restartTimer();
			break;
		case "ping":
			pingTracker();
			break;
		case "pingP2P":
			pingP2P();
			break;
		case "recovery":
			recovery();
			break;
		default:
			display("\t\t\tDEBUG: Unknown tracker message: \"" + message.trim() + "\"");
			break;
		}
	}

	// Called when a tracker takes over for another
	// Replies will the current list of peers
	private void recovery() {
		try {
			System.out.println("Sending recovery");
			Socket sock = new Socket(tracker, trackPort);
			BufferedWriter sockOut = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			
			String message = ID + "~recover " + group.recover() + "\n";
			
			sockOut.write(message);
			sockOut.flush();
			
			
			
			sock.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if(verbose)
				e.printStackTrace();
		}
		
	}
	
	
	// Called when a peer requests a ping.
	// Broadcast to all peers that we are still alive.
	private void pingP2P() {
		// Broadcast to all

		String message = "0~keep-alive " + ID;

		byte[] buffer = message.getBytes();
		DatagramPacket packet;

		for (Entry<Integer, Peer> idPeer : group) {
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
	
	// Re-populates the list of peers
	private synchronized void updateMembers(String members) {
		group.addAll(members);
	}
	
	
	// Determines if received message is a tracker message or not.
	// It determines this by parsing for the ID at the start of string
	private void recieved(DatagramPacket packet) throws UnknownHostException {
		String data = new String(packet.getData());
		String[] parts = data.split("~");
		String[] nums = parts[0].split("\\*");
		int sender = Integer.parseInt(nums[0]);
		int messageID;
		if(sender != 0) 
			messageID = Integer.parseInt(nums[1]);
		
		
		if(sender == 0) {
			// Tracker messages have particular behavior to follow and don't need to be displayed
			trackerMessage(parts[1]);
		} else {	
			group.getPeerByID(sender).restartTimer();
			display(parts[1]);
		}
	}
}





