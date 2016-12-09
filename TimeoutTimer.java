import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class TimeoutTimer extends TimerTask{
	int ID;
	String channelName;
	
	// The peer to contact when first run
	String peerIP;
	int peerPort;
	
	
	// The tracker to contact if the peer fails to respond
	String trackerIP;
	int trackerPort;
	
	private Boolean verbose = false;
	
	
	public TimeoutTimer(String tip, int tpo, String pip, int ppo, int ID, String cn) {
		peerIP = pip;
		peerPort = ppo;
		
		trackerIP = tip;
		trackerPort = tpo;
		
		
		this.ID = ID;
		channelName = cn;
	}
	
	public void verbose() {
		verbose = true;
	}
	
	public void run() {
		sendP2PRequest();
		// If the peer replies it will interrupt the thread and remake the timer
		try {
			Thread.sleep(1000);;
			sendTrackRequest();
		} catch (InterruptedException e) {
			if(verbose)
				System.out.println("\t\t\tDEBUG: Peer " + ID + " replied, keeping alive.");
		}
	}
	
	// Sends a ping-request to the tracker.
	private void sendTrackRequest() {		
		try {
			Socket sock = new Socket(trackerIP, trackerPort);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			out.write("request-ping " + ID + " " + channelName + "\n\n");
			out.flush();
			sock.close();
		} catch (ConnectException e) {
			if(verbose)
				System.out.println("\t\t\tPING-ERROR: Tracker unreachable.");
		} catch (IOException e) {
			if(verbose)
				e.printStackTrace();
		} 
	}
	
	// Sends a ping request to the client. Uses ID 0 to mimic the tracker and thereby have access to special-case messages
	private void sendP2PRequest() {
		// Send request via 0~
		try {
			DatagramSocket sock = new DatagramSocket();
			String message = "0~pingP2P";
			byte[] buff = message.getBytes();
			
			try {
				DatagramPacket pack = new DatagramPacket(buff, buff.length, InetAddress.getByName(peerIP), peerPort);
				sock.send(pack);
			} catch (UnknownHostException e) {
				if(verbose)
					e.printStackTrace();
			} catch (IOException e) {
				if(verbose)
					e.printStackTrace();
			}
			
			sock.close();
			
		} catch (SocketException e) {
			if(verbose)
				e.printStackTrace();
		}
		
	}
}
