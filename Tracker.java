import java.util.ArrayList;

public class Tracker implements Runnable {
	
	private ArrayList<Channel> channels;
	
	public Tracker() {
		channels = new ArrayList<Channel>();
	}
	
	public int addChannel(String n, String m) {
		IP i = new IP(m);
		Channel c = new Channel(n,i);
		if (channels.contains(c)) {
			return 0;
		} else {
			channels.add(new Channel(n, i));
			return 1;
		}
	}
	
	public int joinChannel(String n, String m) {
		for (int i=0; i<channels.size(); i++) {
			if (channels.get(i).name.equals(n)) {
				channels.get(i).addMember(new IP(m));
				return 1;
			}
		}
		return 0;
	}
	
	// NOT WORKING
	public int leaveChannel(String n, String m) {
		for (int i=0; i<channels.size(); i++) {
			if (channels.get(i).name.equals(n)) {
				channels.get(i).removeMember(new IP(m));
				return 1;
			}
		}
		return 0;
	}
	
	public void displayChannels() {
		for (int i=0; i<channels.size(); i++) {
			System.out.print(channels.get(i));
		}
	}
	
	@Override
	public void run() {
		
	}
	
	public class Channel {
		public String name;
		private ArrayList<IP> members;
		
		public Channel(String n, IP m) {
			name = n;
			members = new ArrayList<IP>();
			members.add(m);
		}
		
		public int addMember(IP m) {
			if (members.contains(m)) {
				return 0;
			} else {
				members.add(m);
				return 1;
			}
		}
		
		public int removeMember(IP m) {
			if (members.contains(m)) {
				members.remove(m);
				return 1;
			} else {
				return 0;
			}
		}
		
		public String toString() {
			String val = name + "\n";
			for (int i=0; i<members.size(); i++) {
				val += "\t" + members.get(i).toString() + "\n";
			}
			return val;
		}
	}
	
	public class IP {
		private String address;
		
		public IP(String i) {
			address = i;
		}
		
		public String toString() {
			return address;
		}
	}
	
}