import java.io.IOException;
import java.net.Socket;

import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TestClient {

	public static void main(String args[]) {
		try {
			
		//====
			System.out.println("get");
			Socket sock = new Socket("localhost", 1234);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			PrintStream out = new PrintStream(sock.getOutputStream());
			
			out.print("get\n\n");
			String input = in.readLine();
			System.out.println(input);
			
			out.close();
			in.close();
			sock.close();
		//====
			System.out.println("create test");
			sock = new Socket("localhost", 1234);
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintStream(sock.getOutputStream());
			
			out.print("create test\n\n");
			input = in.readLine();
			System.out.println(input);
			
			out.close();
			in.close();
			sock.close();
		//====
			System.out.println("get");
			sock = new Socket("localhost", 1234);
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintStream(sock.getOutputStream());
			
			out.print("get\n\n");
			input = in.readLine();
			System.out.println(input);
			
			out.close();
			in.close();
			sock.close();
		//====
			System.out.println("join test");
			sock = new Socket("localhost", 1234);
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintStream(sock.getOutputStream());
			
			out.print("join test\n\n");
			input = in.readLine();
			System.out.println(input);
			
			out.close();
			in.close();
			sock.close();
		//====
			System.out.println("join test");
			sock = new Socket("localhost", 1234);
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintStream(sock.getOutputStream());
			
			out.print("join test\n\n");
			input = in.readLine();
			System.out.println(input);
			
			out.close();
			in.close();
			sock.close();
		//====
			System.out.println("leave test");
			sock = new Socket("localhost", 1234);
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintStream(sock.getOutputStream());
			
			out.print("leave test\n\n");
			input = in.readLine();
			System.out.println(input);
			
			out.close();
			in.close();
			sock.close();
		} catch (IOException e) {
			System.out.println(e);
		}

	
	}
}