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
	
	
	
	public PeerGroup(String trackIP, int trackPort, String cn) {
		map = new Hashtable<Integer, Peer>();
		this.trackIP = trackIP;
		this.trackPort = trackPort;
		pool = new ScheduledThreadPoolExecutor(MAX_PEERS);
		channelName = cn;
	}
	
	public void addPeer(Peer peer) {
		map.put(peer.getID(), peer);
		peer.startTimer();
	}
	
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
								trackIP,
								trackPort,
								pool,
								channelName
						);
						addPeer(newPeer);
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
