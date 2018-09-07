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
 * 
 */


public class blindOutgoingTransmission  {
	
	// main instance variables for outgoing packets are transmissions themselves
	public DatagramPacket[] broadcast_name_packets;
	public outgoingTransmission blind_outgoing_transmission;
	
	public BlindMetaSender first_round_sender;
	public transferRound transfer_payload;
	
	
	
	/*
	 * Constructors 
	 */
	
	public blindOutgoingTransmission(int bytes_in_packet, String path_given, int main_port, int bytes_per_chunk, InetAddress given_addr)
	{
		blind_outgoing_transmission = new outgoingTransmission( bytes_in_packet, path_given, main_port,  bytes_per_chunk, given_addr);
		transfer_payload = new transferRound(blind_outgoing_transmission.name_packets, given_addr, main_port);
		first_round_sender = meta_round();
	}
	
	public blindOutgoingTransmission(int bytes_in_packet, String path_given, int main_port, int bytes_per_chunk)
	{
		blind_outgoing_transmission = new outgoingTransmission( bytes_in_packet, path_given, main_port,  bytes_per_chunk);
		transfer_payload = new transferRound(blind_outgoing_transmission.name_packets);
		first_round_sender = meta_round();
	}
	

	/*
	 * 
	 * Do not use if you are not having the classes write broadcast packets
	 * 
	 */
	public void set_broadcast_name_packets()
	{
		ArrayList< byte[] > payloads = blind_outgoing_transmission.get_name_packets();
		
		
		// initialize broadcast_name_packets
		broadcast_name_packets = new DatagramPacket[payloads.size()];
		
		// System.out.println(broadcast_address.getHostAddress().toString());
			
		for(int i = 0; i < payloads.size(); i++)
			broadcast_name_packets[i] = outgoingTransmission.pack_packet(payloads.get(i), blind_outgoing_transmission.outgoing_transmission.broadcast_port, blind_outgoing_transmission.broadcast_address);
		
		
	}
	
	public BlindMetaSender meta_round()
	{
		return new BlindMetaSender(this);
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
		System.out.println(jk.blind_outgoing_transmission.identifier);
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
