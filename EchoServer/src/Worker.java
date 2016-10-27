import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;

public class Worker implements Runnable{

	private Socket sock;
	
	public Worker(Socket socket) {
		this.sock = socket;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
				String incoming = "";
				while(sock.isConnected()) {
					incoming = in.readLine();
					if(null == incoming) 
						return;						
					System.out.println(incoming);
					out.write("ECHO: " + incoming + "\n");
					out.flush();
				}
				sock.close();
			} catch(SocketException e) {
				System.err.println("Client disconnected unexpentently");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
	}

}
