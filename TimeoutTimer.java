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

import java.util.TimerTask;

public class TimeoutTimer extends TimerTask{
	int ID;
	String channelName;
	
	// Peer Info
	String peerIP;
	int peerPort;
	
	
	// Tracker Info
	String trackerIP;
	int trackerPort;
	
	
	public TimeoutTimer(String tip, int tpo, String pip, int ppo, int ID, String cn) {
		peerIP = pip;
		peerPort = ppo;
		
		trackerIP = tip;
		trackerPort = tpo;
		
		
		
		this.ID = ID;
		channelName = cn;
	}
	
	public void run() {
		sendP2PRequest();
		try {
			Thread.sleep(1000);;
			sendTrackRequest();
		} catch (InterruptedException e) {
			System.out.println("\t\t\tDEBUG: Peer " + ID + " replied, keeping alive.");
		}
	}
	
	// Sends a ping-request to the tracker.
	// TODO: Request a ping from the client first
	private void sendTrackRequest() {		
		try {
			Socket sock = new Socket(trackerIP, trackerPort);
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
