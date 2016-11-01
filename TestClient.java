import java.io.IOException;
import java.net.Socket;

import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

public class TestClient {

	public static void main(String args[]) {
		
		Scanner reader = new Scanner(System.in);
		String s = "";

		while(true)
		try {
			// Open socket
			Socket sock = new Socket("localhost", 5555);
			
			// Create socket in/out
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			PrintStream out = new PrintStream(sock.getOutputStream());
			
			// Read input from user
			System.out.println("Enter Message: ");
			s = reader.nextLine();
			System.out.println(s);

			// Send message
			s += "\n\n";
			out.print(s);
			out.flush();

			// Read response
			String input = in.readLine();
			System.out.println(input);

			// Clean up
			out.close();
			in.close();
			sock.close();

		} catch (IOException e) {
			System.out.println(e);
		}

	
	}
}
