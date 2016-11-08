import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MulticastReceiver implements Runnable {

	MulticastSocket sock = null;
	InetAddress tracker;
	ArrayList<InetAddress> group;
	Boolean MC = false;
	
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	
	// Old constructor for non-true multicasting
	public MulticastReceiver(String IP, ArrayList<InetAddress> group) {
				
				try {
					sock = new MulticastSocket(5556);
					this.group = group;
					tracker = InetAddress.getByName(IP);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	}
	
	public void run() {		
		try {
			byte[] buffer;
			while(true) {
				buffer = new byte[65500];
				
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				sock.receive(packet);
				
				String message = new String(packet.getData());
				if(packet.getAddress().equals(tracker)) {
					// Tracker messages have particular behavior to follow and don't need to be displayed
					trackerMessage(message);
				} else if(!packet.getAddress().equals(InetAddress.getLocalHost())) {
					display(message);
				} 
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
		
		try {
			String[] peers = members.split(",");
			group.clear();
			for(String peer : peers) {
				group.add(InetAddress.getByName(peer));
			}

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
