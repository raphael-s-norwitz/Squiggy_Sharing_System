import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Scanner;

/*
 * 
 * This class is a subclass of the outgoingTransmission class
 * 
 * The main difference lies in that this transmission contains the 
 * payloads wrapped in datagrams
 * 
 * All code written by and property of Raphael Norwitz
 * 
 */


public class blindOutgoingTransmission extends outgoingTransmission {
	
	// main instance variables for outgoing packets are transmissions themselves
	public DatagramPacket[] broadcast_name_packets;
	
	/*
	 * Constructors 
	 */
	
	public blindOutgoingTransmission(int bytes_in_packet, String path_given, int main_port, int bytes_per_chunk, InetAddress given_addr)
	{
		super( bytes_in_packet, path_given, main_port,  bytes_per_chunk, given_addr);
	}
	

	
	public void set_broadcast_name_packets()
	{
		ArrayList< byte[] > payloads = get_name_packets();
		
		
		// initialize broadcast_name_packets
		broadcast_name_packets = new DatagramPacket[payloads.size()];
		
		// System.out.println(broadcast_address.getHostAddress().toString());
			
		for(int i = 0; i < payloads.size(); i++)
			broadcast_name_packets[i] = pack_packet(payloads.get(i), broadcast_port, broadcast_address);
	}
	
	public BlindMetaSender meta_round()
	{
		return new BlindMetaSender( this.packet_size, this.transmission_path, this.broadcast_port,  this.bytes_in_packet_chunk, this.broadcast_address);
	}
	
	
	public static void main(String[] args)
	{
		
		
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
		

		
		blindOutgoingTransmission jk = new blindOutgoingTransmission(bytes_per_packet, path, port, bytes_p_chunk, addr);	
		jk.set_broadcast_name_packets();
		
		Scanner from_user = new Scanner(System.in);
		System.out.println(jk.identifier);
		System.out.println("Press enter to continue");
		String ret_code = from_user.nextLine();
		
		try{
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
		}
		
		
		
		
		
		
		
	}

}
