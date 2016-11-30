public class Member {
	private String address;
	private int port;
	private int id;

	public Member(String a, int p, int i) {
		address = a;
		port = p;
		id = i;
	}

	public int getID() {
		return id;
	}

	public String toString() {
		return address + ":" + port + "/" + id;
	}

	public String getIP() {
		return address;
	}

	public int getPort() {
		return port;
	}
}