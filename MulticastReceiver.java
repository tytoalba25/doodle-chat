import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class MulticastReceiver implements Runnable {

	DatagramSocket sock = null;
	
	public MulticastReceiver(DatagramSocket sock) {
		this.sock = sock;
	}
	
	@Override
	public void run() {		
		try {
			byte[] buffer;
			while(true) {
				buffer = new byte[65500];
				
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				sock.receive(packet);
				
				String message = new String(packet.getData());
				
				System.out.println(message);
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

}
