import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

public class MulticastReceiver implements Runnable {

	DatagramSocket sock = null;
	
	public MulticastReceiver(DatagramSocket sock) {
		try {
			while(true) {
				byte[] buffer = new byte[65500];
				
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
	
	
	class SayHello extends TimerTask {
	    public void run() {
	    	System.out.println("I are bot");
	    }
	}
	
	
	@Override
	public void run() {		
		Timer timer = new Timer();
		timer.schedule(new SayHello(), 0, 2500);
		
	}

}
