/*
 * 
 * This class contains the execution code for a sender thread for both 
 * a gui and for a Thread which hopefully can be executed from with Android
 * 
 * 
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Scanner;



public class BlindMetaSender extends blindOutgoingTransmission implements Runnable, ActionListener {
	

	/*
	 *Constructors 
	 */
	public BlindMetaSender(int bytes_in_packet, String path_given, int main_port, int bytes_per_chunk, InetAddress given_addr)
	{
		super( bytes_in_packet, path_given, main_port,  bytes_per_chunk, given_addr);
		this.set_broadcast_name_packets();
	}
	


	@Override
	public void actionPerformed(ActionEvent e) {
		new Thread(this).run();
		
	}

	@Override
	public void run() {
		try{
			DatagramSocket socket = new DatagramSocket();	
			// socket.setBroadcast(true);
			
			
			
			for(int i = 0; i < this.broadcast_name_packets.length; i++)
			{
				socket.send(this.broadcast_name_packets[i]);
			}
			
			socket.close();
		}
		catch(IOException e){
			System.out.println("Socket creation failed");
			e.printStackTrace();
		}
		catch(Exception pt){
			System.out.println("Broadcast packet length " + Integer.toString(broadcast_name_packets.length));
			pt.printStackTrace();
			
		}

		
	} 
	
	public static void main(String[] args){
		
		int bytes_per_packet = 1472;
		int bytes_p_chunk = 1472 - 16;
		String path = "/Users/shannonnorwitz/cuwork/multicast_south_africa/test_again";
		int port = 4848;
		
		InetAddress addr = null;
		try{
			addr = transmission.getNetworkLocalBroadcastAddressdAsInetAddress();
			System.out.println(addr.getHostName());
		}
		catch(IOException e){
			e.printStackTrace();
		}
		

		
		BlindMetaSender jk = new BlindMetaSender(bytes_per_packet, path, port, bytes_p_chunk, addr);	
		jk.set_broadcast_name_packets();
		
		Scanner from_user = new Scanner(System.in);
		System.out.println(jk.identifier);
		System.out.println("Press enter to continue");
		String ret_code = from_user.nextLine();
		
		/*try{
			MulticastSocket socket = new MulticastSocket();	
			socket.setBroadcast(true);
			
			for(int i = 0; i < jk.broadcast_name_packets.length; i++)
			{
				socket.send(jk.broadcast_name_packets[i]);
			}
			
			socket.close();
		}
		catch(Exception e){
			System.out.println("Socket creation failed");
		}*/
		
		jk.run();
		
		
	}

}
