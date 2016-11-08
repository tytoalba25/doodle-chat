import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
public class MulticastReceiver implements Runnable {

	MulticastSocket sock = null;
	InetAddress tracker;
	
	public MulticastReceiver(InetAddress group, String IP) {
		try {
			sock = new MulticastSocket(5556);
			sock.joinGroup(group);
			tracker = InetAddress.getByName(IP);

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
			//display("New members: " + parts[1]);
			//updateMembers(parts[1]);
			break;
		default:
			display("Unknown tracker message: \"" + message + "\"");
			break;
		}
	}
}
