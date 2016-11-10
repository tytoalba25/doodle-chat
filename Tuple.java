import java.net.InetAddress;

public class Tuple {
	
	private InetAddress addr;
	private int port;
	
	public Tuple(InetAddress addr, int port) {
		this.addr = addr;
		this.port = port;
	}
	
	public InetAddress getAddr() {
		return addr;
	}
	
	public int getPort() {
		return port;
	}
}
