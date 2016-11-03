import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MulticastReceiver implements Runnable {

	DatagramSocket sock = null;
	ArrayList<InetAddress> group;
	InetAddress tracker;
	
	public MulticastReceiver(String IP, DatagramSocket sock, ArrayList<InetAddress> group) {
		this.sock = sock;
		try {
			tracker = InetAddress.getByName(IP);
		} catch (UnknownHostException e) {
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
					trackerMessage(message);
				} else {
					System.out.println(message);					
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
	
	
	private void trackerMessage(String message) {
		String[] parts = message.split(" ");
		
		switch(parts[0]) {
		
		case "update":
			System.out.println("New members: " + parts[1]);
			updateMembers(parts[1]);
			break;
		default:
			System.err.println("Unknown tracker message: \"" + message + "\"");
			break;
		}
	}
	
	private synchronized void updateMembers(String members) {
		try {
			String[] peers = members.split(",");
			group = new ArrayList<InetAddress>();
			
			for(String peer : peers) {
				group.add(InetAddress.getByName(peer));
			}
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
