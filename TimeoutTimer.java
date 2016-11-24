import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.TimerTask;

public class TimeoutTimer extends TimerTask{
	int ID;
	String channelName;
	
	// Tracker Info
	String IP;
	int port;
	
	
	
	public TimeoutTimer(String IP, int port, int ID, String cn) {
		this.IP = IP;
		this.port = port;
		this.ID = ID;
		channelName = cn;
	}
	
	public void run() {
		sendRequest();
	}
	
	// Sends a ping-request to the tracker.
	// TODO: Request a ping from the client first
	private void sendRequest() {		
		try {
			Socket sock = new Socket(IP, port);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			out.write("request-ping " + ID + " " + channelName + "\n\n");
			out.flush();
			sock.close();
		} catch (ConnectException e) {
			System.out.println("\t\t\tPING-ERROR: Tracker unreachable.");
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
}