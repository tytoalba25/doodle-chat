import java.net.InetAddress;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Peer {
	// Peer info
	private int ID;
	private InetAddress addr;
	private int port;
	private String channelName;
	
	// Tracker info
	private String trackIP;
	private int trackPort;
	
	// Timer stuff
	private final int PING_INTERVAL = 5;
	private ScheduledThreadPoolExecutor pool;
	private Future<?> future;
	
	
	public Peer() {
		
	}
	
	public Peer(int id, InetAddress a, int p, String tip, int t, ScheduledThreadPoolExecutor stpe, String cn) {
		
		ID = id;
		addr = a;
		port = p;
		channelName = cn;
		
		trackIP = tip;
		trackPort = t;
		pool = stpe;
	}
	
	public void restartTimer() {
		stopTimer();
		startTimer();	
	}
	
	public void startTimer() {
		future = pool.schedule(
				new TimeoutTimer(
						trackIP, trackPort, 
						addr.toString().substring(1).split(":")[0], port, 
						ID, channelName), 
				PING_INTERVAL, 
				TimeUnit.SECONDS
		);		
	}
	
	public void stopTimer() {
		future.cancel(true);
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
