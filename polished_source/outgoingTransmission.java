/*
 * 
 * This is the super class for all transmissions from the sender side.
 * 
 * All code written by and property of Raphael Norwitz
 * 
 */

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;


public class outgoingTransmission {
	
	// instance variables for all outgoing transmissions
	public ArrayList<File> to_Send;
	public InetAddress broadcast_address;
	public int identifier;
	public int number_of_files;
	
	// for name packets
	public namePopulator name_packets;
	
	// hash table from file codes to files themselves 
	public HashMap<Integer, File> code_to_file;
	
	public transmission outgoing_transmission;
	
	public transferRound transfer_round;
	

	
	public outgoingTransmission(int bytes_in_packet, String path_given, int main_port, int bytes_per_chunk, int iden)
	{
		// use original constructor
		outgoing_transmission = new transmission(bytes_in_packet, path_given, main_port);
		
		
		// initialize instance variables for senders
		identifier = iden;
		outgoing_transmission.bytes_in_packet_chunk = bytes_per_chunk; 
		code_to_file = new HashMap<Integer, File>();
		
		// get files and info from files for transfer
		to_Send = transmission.listFilesForFolder(new File(outgoing_transmission.transmission_path));
		number_of_files = to_Send.size();
		
		set_code_to_file(); // populate code_to_file
		
		name_packets = new namePopulator(code_to_file, number_of_files, outgoing_transmission.packet_size, outgoing_transmission.bytes_in_packet_chunk, identifier);
		
		transfer_round = new transferRound(name_packets); 
		
	}
	
	public outgoingTransmission(int bytes_in_packet, String path_given, int main_port, int bytes_per_chunk){
		this(bytes_in_packet, path_given, main_port, bytes_per_chunk, outgoingTransmission.identifier_code_random());
	}
	
	public outgoingTransmission(int bytes_in_packet, String path_given, int main_port, int bytes_per_chunk , InetAddress given_addr, int iden ){
		this(bytes_in_packet, path_given, main_port, bytes_per_chunk, iden);
		broadcast_address = given_addr;
		
	}
	
	public outgoingTransmission(int bytes_in_packet, String path_given, int main_port, int bytes_per_chunk , InetAddress given_addr ) {
		this( bytes_in_packet, path_given, main_port, bytes_per_chunk);
		System.out.println("Constructor Identifier: " + Integer.toString(identifier));
		broadcast_address = given_addr;
	}
	
	
	// get the payloads of the name packets
	public ArrayList< byte[] > get_name_packets()
	{
		ArrayList< byte[] > ret_val = new ArrayList< byte[] >();
		
		for(int i = 0; i < name_packets.base_of_names.number_of_files; i++)
		{
			ret_val.add(name_packets.first_packet_bytes.get(i) );
		}
		
		return ret_val;
	}
	
	
	public void set_code_to_file(){
		for(int i = 0; i < number_of_files; i++)
			code_to_file.put(i, to_Send.get(i));
	}
	
	
	
	// give random identifier for the transmission
	public static int identifier_code_random()
	{
		return (int) Math.floor(Math.random() * 1000000);
	}
	
	
	// Static method to get number of packets
	public static int number_packets(File data, int payload_per_packet)
	{
		
		int num_packets =  (int) (data.length() / payload_per_packet) + 1;
		int leftover = (int) data.length()  % payload_per_packet;
		
		if (leftover > 0)
			num_packets++;
		
		return num_packets;
	}
	
	public static int get_number_packets(File data, int payload_per_packet)
	{
		
		int num_packets =  (int) (data.length() / payload_per_packet) + 1;
		int leftover = (int) data.length()  % payload_per_packet;
		
		if (leftover > 0)
			num_packets++;
		
		
		return num_packets;
	}
	
	public static DatagramPacket pack_packet(byte[] payload, int port_val, InetAddress b_cast_addr)
	{
		return new DatagramPacket(payload, payload.length, b_cast_addr, port_val);
	}
	

	


}
