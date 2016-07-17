import java.io.File;
import java.util.HashMap;


/*
 * 
 * This is the super class for all packet populator classes
 * 
 * Given an arraylist, it creates the headers for each file in the transfer
 * 
 * All code is written by and property of Raphael Norwitz
 * 
 */

public class packetPopulator {
	
	// main instance variables
	public int packet_size;
	public int number_of_files;
	public int bytes_in_packet_chunk;
	public int transfer_identifier;
	
	// for subclasses
	public int taken_so_far;
	
		
	// hash table from file codes to files themselves 
	public HashMap<Integer, File> code_to_file;
	
	// hash table from file codes to file codes as bytes
	public HashMap<Integer, byte[]> code_to_code_bytes;
	
	
	
	// hash table from file codes to Headers for all packets
	// ******* Main attribute of this class *****
	public HashMap<Integer, byte[]> code_to_head_of_files;
	
	public packetPopulator(HashMap<Integer, File> file_list, int file_count, int overall_size, int chunk, int iden)
	{
		// Initialize instance variables
		packet_size = overall_size;		
		code_to_file = file_list;
		number_of_files = file_count;
		bytes_in_packet_chunk = chunk;
		transfer_identifier = iden;
		taken_so_far = 0;
		
		code_to_code_bytes = new HashMap<Integer, byte[]>();
		code_to_head_of_files = new HashMap<Integer, byte[]>();
		
		
		
		// enable file codes
		set_code_to_code_bytes();
		
		// set packet headers for all files
		set_all_packet_headers();

	}
	
	public void set_all_packet_headers(){
		
		for( int i = 0; i < number_of_files; i++){
			
			taken_so_far = 0;	
			
			byte[] payload = new byte[packet_size];
		
			// fill array with fields common to all packets
			System.arraycopy(add_identifier(transfer_identifier), 0, payload, 0, add_identifier(transfer_identifier).length);
			taken_so_far += add_identifier(transfer_identifier).length;
			
			System.arraycopy(code_to_code_bytes.get(i), 0, payload, taken_so_far, code_to_code_bytes.get(i).length);
			taken_so_far += code_to_code_bytes.get(i).length;
			
			code_to_head_of_files.put(i, payload);
			
			/*
			 * to be taken to name files
			 * 
			System.arraycopy(current_packet_index, 0, payload, taken_so_far,  current_packet_index.length);
			
			taken_so_far += current_packet_index.length;
			
			System.arraycopy(total_packets, 0, payload, taken_so_far, total_packets.length);
			taken_so_far += total_packets.length;
			*/
		}
	}
	
	public void set_code_to_code_bytes(){
		for(int i = 0; i < number_of_files; i++)
		{
			code_to_code_bytes.put(i, toBytes(i));
		}
	}
	
	
	
	public static int get_number_packets(File data, int payload_per_packet)
	{
		
		int num_packets =  (int) (data.length() / payload_per_packet) + 1;
		int leftover = (int) data.length()  % payload_per_packet;
		
		if (leftover > 0)
			num_packets++;
		
		
		return num_packets;
	}
	
	/* 
	 * Method from stack overflow to get the byte array that 
	 * represents an integer
	 */
	public static byte[] toBytes(int i)
	{
	  byte[] result = new byte[4];

	  result[0] = (byte) (i >> 24);
	  result[1] = (byte) (i >> 16);
	  result[2] = (byte) (i >> 8);
	  result[3] = (byte) (i /*>> 0*/);

	  return result;
	}
	
	/* 
	 * Method from stack overflow to get an integer from
	 * a byte array
	 */
	public static int byteArrayToInt(byte[] b) 
	{
	    int value = 0;
	    for (int i = 0; i < 4; i++) {
	        int shift = (4 - 1 - i) * 8;
	        value += (b[i] & 0x000000FF) << shift;
	    }
	    return value;
	}
	
	// easy way to get file name as a string
	public static String file_name_string(File target)
	{
		return target.getName().toString();
	}
	
	public static byte[] add_identifier(int iden){
		return toBytes(iden);
	}
	
		

}
