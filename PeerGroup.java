import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class PeerGroup implements Iterable<Entry<Integer, Peer>>{
	private  Map<Integer, Peer> map;;
	private String trackIP;
	private int trackPort;
	
	public PeerGroup(String trackIP, int trackPort) {
		map = new Hashtable<Integer, Peer>();
		this.trackIP = trackIP;
		this.trackPort = trackPort;
	}
	
	public void addPeer(Peer peer) {
		map.put(peer.getID(), peer);
	}
	
	public void addAll(String members) {
		
		// TODO: Don't clear members that are already accounted for. This allows for accurate timers.
		
		if(members.length() > 0) {
			clear();
			try {
				String[] peers = members.split(",");
				String[] parts;
				String[] rightParts;
				for(String peer : peers) {
					parts = peer.split(":");
					rightParts = parts[1].split("/");
					addPeer(new Peer(
							Integer.parseInt(rightParts[1].trim()),
							InetAddress.getByName(parts[0]), 
							Integer.parseInt(rightParts[0]),
							trackIP,
							trackPort
							)
					);
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
	
	public Boolean pingPeer() {
		return true;
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
}
