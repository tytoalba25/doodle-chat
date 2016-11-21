import java.net.InetAddress;
import java.util.Timer;

public class Peer {
	private Timer alive;
	private int ID;
	private InetAddress addr;
	private int port;
	private String trackIP;
	private int trackPort;
	
	public Peer() {
		
	}
	
	public Peer(int id, InetAddress a, int p, String tip, int t) {
		alive = new Timer();
		
		ID = id;
		addr = a;
		port = p;
		
		trackIP = tip;
		trackPort = t;
	}
	
	public void restartTimer() {
		alive.cancel();
		alive = new Timer();
		alive.schedule(new TimeoutTimer(trackIP, trackPort, ID), 10 * 1000);		
	}
	
	public void startTimer() {
		alive.schedule(new TimeoutTimer(trackIP, trackPort, ID), 10 * 1000);
	}
	
	public void stopTimer() {
		alive.cancel();
	}
	
	public int getID() {
		return ID;
	}
	
	public InetAddress getAddr() {
		return addr;
	}
	
	public int getPort() {
		return port;
	}
}
