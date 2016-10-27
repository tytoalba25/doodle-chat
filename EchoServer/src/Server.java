import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Server {
	
	private final static int PORT = 5555;
	
	
	@SuppressWarnings("resource")
	public static void main(String args[]) {
		try {
			ServerSocket sock = new ServerSocket(PORT);
			Executor pool = Executors.newFixedThreadPool(32);
			System.out.println("Created echo-server at localhost:" + PORT);
			
			while(true) {
				System.out.println("Waiting for connection");
				pool.execute(new Worker(sock.accept()));
				System.out.println("Connection established.");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
	}
}
