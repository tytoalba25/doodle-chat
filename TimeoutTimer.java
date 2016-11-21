import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.TimerTask;

public class TimeoutTimer extends TimerTask{
	int ID;
	String IP;
	int port;
	
	public TimeoutTimer(String IP, int port, int ID) {
		this.IP = IP;
		this.port = port;
		this.ID = ID;
	}
	
	public void run() {
		System.out.println("\t\tPing requested for peer #" + ID);
		sendRequest();
	}
	
	private void sendRequest() {
		try {
			Socket sock = new Socket("127.0.1.1", port);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			out.write("request-ping " + ID + "\n\n");
			out.flush();
			sock.close();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
}



// TODO: Only last member of group gets any timers.