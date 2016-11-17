import java.net.InetAddress;

public class Tuple {
	
	private InetAddress addr;
	private int port;
	private int ID;
	
	public Tuple(InetAddress addr, int port, int ID) {
		this.addr = addr;
		this.port = port;
		this.ID = ID;
	}
	
	public InetAddress getAddr() {
		return addr;
	}
	
	public int getPort() {
		return port;
	}
	
	public int getID() {
		return ID;
	}
}
