import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class PeerGroup implements Iterable<Entry<Integer, Peer>>{
	
	// Map of peers
	private  Map<Integer, Peer> map;
	
	// Tracker info
	private String trackIP;
	private int trackPort;
	
	// Timer stuff
	private ScheduledThreadPoolExecutor pool;
	private final int MAX_PEERS = 50;
	
	// Channel info
	private String channelName;
	
	private Boolean verbose = false;
	
	public PeerGroup(String trackIP, int trackPort, String cn) {
		map = new Hashtable<Integer, Peer>();
		this.trackIP = trackIP;
		this.trackPort = trackPort;
		pool = new ScheduledThreadPoolExecutor(MAX_PEERS);
		channelName = cn;
	}
	
	public void verbose() {
		verbose = true;
	}
	
	public void addPeer(Peer peer) {
		map.put(peer.getID(), peer);
		peer.startTimer();
	}
	
	public int getTrackPort() {
		return trackPort;
	}
	
	public String getTrackIP() {
		return trackIP;
	}
	
	public void setTrackPort(int port) {
		trackPort = port;
	}
	
	public void setTrackIP(String IP) {
		trackIP = IP;
	}
	
	public String recover() {
		// 172.19.1.23:42836/10,172.19.1.24:55519/11
		
		String message = channelName + " ";
		
		if(size() > 0) {
			for (Map.Entry<Integer, Peer> entry : map.entrySet()) {
				Peer peer = entry.getValue();
				message += peer.getAddr().toString().substring(1)
						+ ":"
						+ peer.getPort()
						+ "/"
						+ peer.getID()
						+ ",";
			}
			
			message = message.substring(0, message.length() - 2);
		}
		
		return message;		
	}
	
	// Parses through the string the Tracker returns and creates a new Peer for all the entries in that list
	public void addAll(String members) {
		if(members.length() > 0) {
			try {
				String[] peers = members.split(",");
				String[] parts;
				String[] rightParts;
				int peerID;
				Peer newPeer;
				for(String peer : peers) {
					parts = peer.split(":");
					rightParts = parts[1].split("/");
					peerID = Integer.parseInt(rightParts[1].trim());
					if( ! map.containsKey(peerID) ) {
						newPeer = new Peer(
								peerID,
								InetAddress.getByName(parts[0]), 
								Integer.parseInt(rightParts[0]),
								this,
								pool,
								channelName
						);
						addPeer(newPeer);
						if(verbose)
							newPeer.verbose();
						newPeer.startTimer();
					}
				}

			} catch (NumberFormatException n) {
				n.printStackTrace();
				
			} catch (UnknownHostException e) {
				
				e.printStackTrace();
			}
		}
	}
	
	public Peer getPeerByID (int ID) {
		return map.get(ID);
	}
	
	public void startTimers() {
		System.out.println("Group size: " + size());
		if(size() > 0) {
			for (Map.Entry<Integer, Peer> entry : map.entrySet()) {
				entry.getValue().startTimer();
			}
		}
	}
	
	public int size() {
		if (map.isEmpty())
			return 0;
		return map.size();
	}
	
	public void clear() {
		for (Map.Entry<Integer, Peer> entry : map.entrySet()) {
			entry.getValue().stopTimer();
		}
		map.clear();
	}

	@Override
	public Iterator<Entry<Integer, Peer>> iterator() {
		return map.entrySet().iterator();
	}	
	
	public String getName() {
		return channelName;
	}
}
