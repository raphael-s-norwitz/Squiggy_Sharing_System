import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/*
 * 
 * This class represents the class used to run a thread as created by transferRound
 * 
 * all code written by and property of Raphael Norwitz
 * 
 */
public class transferThread implements Runnable {
	
	public Thread sendThread;
	public transferRound sendRound;
	public InetAddress broadcastAddress;
	public int broadcastPort;
	public DatagramSocket sendSock;
	
	public DatagramPacket vessel;
	
	public boolean run_bool;
	
	public transferThread(transferRound send_round , DatagramSocket send_socket ){
		sendRound = send_round;
		broadcastAddress = sendRound.broadcast_address;
		broadcastPort = sendRound.broadcast_port;
		sendSock = send_socket;
		run_bool = false;
		
		byte[] place_holder = {1,2,3};
		vessel = new DatagramPacket(place_holder, place_holder.length, broadcastAddress, broadcastPort);
		
	}
	
	public transferThread(transferRound send_round) throws SocketException{
		this(send_round, new DatagramSocket());

	}
	
	
	public void run(){
		
		while(!Thread.currentThread().isInterrupted()){
			try{
				byte[] new_payload = sendRound.next_payload();
				vessel.setData(new_payload, 0, new_payload.length);
				sendSock.send(vessel);
				
			}
			catch(IOException e)
			{
				System.out.println("wierd IO exception next packet");
				e.printStackTrace();
			}	
			
		}
		
		
	}

}
