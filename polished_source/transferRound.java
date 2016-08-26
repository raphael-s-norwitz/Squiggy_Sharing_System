/*
 * 
 * Packet Structure:
 * 
 * total: PACKET_SIZE bytes per packet
 * 
 * Normal packet:
 * 
 *   Security code        file-code           index                 max index			     contents
 *   ----------------  ---------------  ------------------      ----------------------     ------------....
 *  Integer (4 bytes)   Integer (4 byte)     Integer (4 bytes)    Integer (4 bytes)        PACKET_SIZE-16 bytes
 *  
 *  
 * First Packet:
 *  
 *   Security code        file-code           index                 max index		    file size % Packet Size        Size of name                 name                      contents
 *   ----------------  ---------------  ------------------      ---------------------- ------------------------    ------------------    -------------------------  ----------------------------------
 *  Integer (4 bytes)   Integer (4 byte)     Integer (4 bytes)    Integer (4 bytes)      Integer (4 bytes)          Integer (4 bytes)     > PACKET_SIZE - 24 bytes    PACKET_SIZE -24 - sizeof(name)   
 *  
 *  Structural notes:
 *  
 *  first packet: 100 bytes for packet name
 * 
 * 
 */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;

/*
 * 
 * This is the payload delivery class
 * 
 * It takes in an InetAddress, a port, an array of files, and hashtables(?), packet size, pure payload size
 * 
 * From there it dynamically generates and sends broadcast packets of the files which it then 
 * 
 */
public class transferRound implements ActionListener, Runnable{
	
	// main instance variables for the sender class
	
	public FileInputStream[] in_files;
	public InetAddress broadcast_address; 
	public byte[][] store_payloads;
	public int broadcast_port;
	
	public packetPopulator base_packets;
	public int[] file_indicies;
	public int head_index;
	public int max_index;
	
	public boolean another_round;
	
	public LinkedList<FileInputStream> global_input_streams;
	public LinkedList<Integer> global_index;
	
	public int global_total_index;
	
	
	public transferRound( packetPopulator packet_pop ){	
		
		max_index = 0;
		
		base_packets = packet_pop;	
		in_files = new FileInputStream[base_packets.number_of_files];
		store_payloads = new byte[base_packets.number_of_files][base_packets.packet_size];
		
		file_indicies = new int[base_packets.number_of_files];
		another_round = false;
		
		for(int i = 0; i < base_packets.number_of_files; i++){
			
			store_payloads[i] = base_packets.code_to_head_of_files.get(i);
			
			file_indicies[i] = 1;
			try{
				in_files[i] = new FileInputStream( base_packets.code_to_file.get(i) );
				
			}
			catch(Exception e){
				System.out.println("Failed to open input stream closing program");
				e.printStackTrace();
			}
			
		}
		
		global_input_streams = full_files_queue(in_files);
		global_index = file_index_queue(in_files);
		global_total_index = 0;
		
		
	}
	
	public transferRound( namePopulator packet_pop ){
		this(packet_pop.base_of_names);
	}
	
	
	public transferRound( packetPopulator packet_pop, InetAddress b_cast_addr, int port){
		this( packet_pop );
		
		broadcast_address = b_cast_addr;
		broadcast_port = port;
	}
	
	public transferRound(namePopulator packet_pop, InetAddress b_cast_addr, int port){
		
		this( packet_pop.base_of_names );
		broadcast_address = b_cast_addr;
		broadcast_port = port;
	}
	
	
	

	
	/*
	 * Add 'self' payload to each packet
	 * 
	 */
	public byte[] prepare_next_packet( int payload_size, int index, int total_index, byte[] mask, int file_idx) throws IOException{
		
		int taken = base_packets.code_to_taken_payload.get(file_idx);
		
		// copy file transfer index and update taken
		byte[] file_transfer_index_bytes = transmission.toBytes(index);
		System.arraycopy(file_transfer_index_bytes, 0, mask, taken, file_transfer_index_bytes.length);
		taken += file_transfer_index_bytes.length;
		
		// copy total transfer index and update taken
		byte[] total_transfer_index_bytes = transmission.toBytes(total_index);
		System.arraycopy(total_transfer_index_bytes, 0, mask, taken, total_transfer_index_bytes.length);
		taken += total_transfer_index_bytes.length;
		
		// store payload
		in_files[file_idx].read(mask, taken, payload_size);
		taken += payload_size;
		
		return mask;
		
		
	}
	
	public LinkedList<FileInputStream> full_files_queue(FileInputStream[] infiles){
		LinkedList<FileInputStream> ret_val = new LinkedList<FileInputStream>();
		
		for(int i = 0; i < infiles.length; i++){
			ret_val.add(infiles[i]);
		}
		
		return ret_val;
		
	}
	
	public LinkedList<Integer> file_index_queue(FileInputStream[] infiles){
		LinkedList<Integer> ret_val = new LinkedList<Integer>();
		
		for(int i = 0; i < infiles.length; i++){
			ret_val.add(i);
		}
		
		return ret_val;
		
	}
	
	/*
	 * 
	 * Method to iterate through and get payloads for this stream
	 * 
	 */
	public byte[] next_payload() throws IOException{
		
		// the array to return
		byte [] ret_val = {0};
		

		
		if(!global_input_streams.isEmpty())
		{
			FileInputStream in_question = global_input_streams.pop();
			int file_code_in_question = global_index.pop();
			
			if(in_question.available() > base_packets.bytes_in_packet_chunk)
			{
				
				ret_val = prepare_next_packet( base_packets.bytes_in_packet_chunk, file_indicies[file_code_in_question], global_total_index, base_packets.code_to_head_of_files.get(file_code_in_question), file_code_in_question) ;
				
				// increment the file indicies and enqueue the index and file stream *** fix file_indixes in thread ***
				file_indicies[file_code_in_question]++;
				global_input_streams.add(in_question);
				global_index.add(file_code_in_question);
			}
			else{
				ret_val = prepare_next_packet( in_question.available(), file_indicies[file_code_in_question], global_total_index, base_packets.code_to_head_of_files.get(file_code_in_question), file_code_in_question) ;
				file_indicies[file_code_in_question] = 1;
			}
			
			global_total_index++;
		}
		else{
			global_input_streams = full_files_queue(in_files);
			global_index = file_index_queue(in_files);
			global_total_index = 1;
			
			// will work well as long as instance variables are not altered
			// continue making them private
			try{
				return next_payload();
			}
			catch(IOException e){
				System.out.println("Problem with next packet loop");
				e.printStackTrace();
			}
		}
		
		
		return ret_val;
	}


	@Override
	public void run() {
		
		// index to tag files
		int current_total_index = 1;
		
		// queues of the file streams to read from and the file codes
		LinkedList<FileInputStream> send_queue = full_files_queue(in_files);
		LinkedList<Integer> file_code_queue = file_index_queue(in_files); 
		
		int[] file_run_indicies = new int[in_files.length];
		
		for(int i = 0; i < in_files.length; i++)
		{
			file_run_indicies[i] = 1;
		}
		
		// create the socket
		DatagramSocket socket = null;
		
		try {
			socket = new DatagramSocket();
		} catch (SocketException e1) {
			System.out.println("Failed to open socket");
			e1.printStackTrace();
		}
		
		int counter = 0;
		
		DatagramPacket vessel = new DatagramPacket(store_payloads[0], store_payloads[0].length, broadcast_address, broadcast_port);
		
		// While there is still data left to read from the input streams
		while(!send_queue.isEmpty())
		{
			counter = counter + 1;
			FileInputStream in_question = send_queue.pop();
			int file_code_in_question = file_code_queue.pop();
			
			try{
			
				if(in_question.available() > base_packets.bytes_in_packet_chunk)
				{
					vessel.setData( prepare_next_packet( base_packets.bytes_in_packet_chunk, file_run_indicies[file_code_in_question], current_total_index, base_packets.code_to_head_of_files.get(file_code_in_question), file_code_in_question) );
					
					// increment the file indicies and enqueue the index and file stream
					file_run_indicies[file_code_in_question]++;
					send_queue.add(in_question);
					file_code_queue.add(file_code_in_question);
				}
				
				else{
					vessel.setData( prepare_next_packet( in_question.available(), file_run_indicies[file_code_in_question], current_total_index, base_packets.code_to_head_of_files.get(file_code_in_question), file_code_in_question) );
					file_run_indicies[file_code_in_question] = 1;
				}
				
				current_total_index++;
				
				socket.send(vessel);
			}
			catch(IOException e){
				System.out.println("Failed to Read from Input stream");
				e.printStackTrace();
			}
			
		}
		
 		
		
		
		
	}



	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
	

}
