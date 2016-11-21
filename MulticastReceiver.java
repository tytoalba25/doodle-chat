import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MulticastReceiver implements Runnable {

	MulticastSocket sock = null;
	InetAddress tracker;
	PeerGroup group;
	Boolean MC = false;
	int ID;
	
	// New constructor for use with multicast
	// TODO: Fix this
	public MulticastReceiver(String trackerIP, InetAddress group) {
		try {
			sock = new MulticastSocket(5556);
			sock.joinGroup(group);
			
			tracker = InetAddress.getByName(trackerIP);

			// TODO: Remove. For test toggling only
			MC = false;
			
		} catch (UnknownHostException e) {
			
			e.printStackTrace();
		} catch (IOException e1) {
			
			e1.printStackTrace();
		}
		
	}
	
	
	// Old constructor for non-true multicasting
	public MulticastReceiver(String IP, PeerGroup group, MulticastSocket sock, int myID) {
		this.sock = sock;
		try {
			this.group = group;
			tracker = InetAddress.getByName(IP);
		} catch (UnknownHostException e) {
			
			e.printStackTrace();
		}
		
		ID = myID;
		
		group.startTimers();
	}
	
	public void run() {		
		try {
			
			byte[] buffer;
			while(true) {
				buffer = new byte[65500];
				
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				sock.receive(packet);
				
				recieved(packet);
			}
		} catch (SocketException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}		
	}
	
	private synchronized void display(String message) {
		System.out.println(message);
	}
	
	private void trackerMessage(String message) {
		String[] parts = message.split(" ");
		
		switch(parts[0]) {
		
		case "update":
			display("DEBUG: Members: " + parts[1]);
			updateMembers(parts[1]);
			break;
		case "keep-alive":
			System.out.println(parts[1] + " keep-alive");
			// Reset timer
			break;
		default:
			display("Unknown tracker message: \"" + message + "\"");
			break;
		}
	}
	
	// Re-populates the list of peers
	private synchronized void updateMembers(String members) {
		// TODO: Remove. For test toggling only
		if(MC)
			return;
		group.addAll(members);
	}
	
	private void recieved(DatagramPacket packet) throws UnknownHostException {
		String data = new String(packet.getData());
		String[] parts = data.split("~");
		int sender = Integer.parseInt(parts[0]);
		
		
		
		if(sender == 0) {
			// Tracker messages have particular behavior to follow and don't need to be displayed
			trackerMessage(parts[1]);
		} else {
			group.getPeerByID(sender).restartTimer();
			display(parts[1]);
		}
	}
}





