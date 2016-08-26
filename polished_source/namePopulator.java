/*
 * 
 * Packet Structure:
 * 
 * total: PACKET_SIZE bytes per packet
 * 
 * Normal packet:
 * 
 *   Security code        file-code           file_index             transfer_index			 dd  contents
 *   ----------------  ---------------    ------------------      --------------------     ------------....
 *  Integer (4 bytes)   Integer (4 byte)     Integer (4 bytes)    Integer (4 bytes)        PACKET_SIZE-16 bytes
 *  
 *  
 * First Packet:
 *  
 *   Security code        file-code           index                 max_index		    file size % Packet Size        Size of name                 name                      contents
 *   ----------------  ---------------  ------------------      --------------------    ------------------------    ------------------    -------------------------  ----------------------------------
 *  Integer (4 bytes)   Integer (4 byte)     Integer (4 bytes)    Integer (4 bytes)      Integer (4 bytes)          Integer (4 bytes)     > PACKET_SIZE - 24 bytes    PACKET_SIZE -24 - sizeof(name)   
 *  
 *  Structural notes:
 *  
 *  first packet: 100 bytes for packet name
 * 
 * 
 */
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;

/*
 * 
 * This class is a subclass of the packet populator which populates the name/meta data packes in the transfer
 * 
 * All code here is written by and property of Raphael Norwitz
 * 
 */
public class namePopulator {
	
	// used to place last index of name in packet
	public HashMap<Integer, Integer> code_to_taken;

	// hash table from file codes to name as string
	public HashMap<Integer, String>  code_to_name;  
	
	// hash table from file codes to name as bytes
	public HashMap<Integer, byte[]> code_to_name_bytes;
	
	// hash table from file codes to max indexes
	public HashMap<Integer, Integer> code_to_max_index;
	
	// hash table from file codes to max indexes bytes
	public HashMap<Integer, byte[]> code_to_max_index_bytes;
	
	// hash table from file codes to length in bytes
	public HashMap<Integer, Integer> code_to_length_in_bytes; 
	
	// hash table from file codes to length mod
	public HashMap<Integer, Integer> code_to_length_mod;
	
	// hash table from file codes to length mod in bytes
	public HashMap<Integer, byte[]> code_to_length_mod_bytes;
	
	// arraylist of first packets
	public HashMap<Integer, byte[] > first_packet_bytes;
	
	public packetPopulator base_of_names;
	
	public namePopulator(HashMap<Integer, File> file_list, int file_count, int overall_size, int chunk, int iden) {
		
		base_of_names = new packetPopulator(file_list, file_count, overall_size, chunk, iden);
		
		code_to_name = new HashMap<Integer, String>();
		code_to_name_bytes = new HashMap<Integer, byte[]>();
		
		code_to_max_index = new HashMap<Integer, Integer>();
		code_to_max_index_bytes = new HashMap<Integer, byte[]>();
		
		code_to_length_in_bytes = new HashMap<Integer, Integer>();
		
		code_to_length_mod = new HashMap<Integer, Integer>();
		code_to_length_mod_bytes = new HashMap<Integer, byte[]>();
		
		code_to_taken = new HashMap<Integer, Integer>();
		
		
		first_packet_bytes = new HashMap<Integer, byte[]>();
		
		
		// populate nessesary hash tables
		set_code_to_name();
		set_code_to_max_index();
		set_code_to_length_in_bytes();
		set_code_to_length_mod();
		
		set_first_packets();
	}
	
	public void set_first_packets()
	{
		
		byte[] first_packet_identifier = get_first_packet_tag();
		
		
		for( int i = 0; i < base_of_names.number_of_files; i++)
		{
			
			byte[] payload = new byte[base_of_names.packet_size];
			
			int used = base_of_names.taken_so_far;
			
			System.arraycopy( base_of_names.code_to_head_of_files.get(i) , 0, payload , 0,  used);
			
			
			System.arraycopy(first_packet_identifier, 0, payload, used, first_packet_identifier.length);
			used += first_packet_identifier.length;
			
			
			System.arraycopy(code_to_max_index_bytes.get(i), 0, payload, used, code_to_max_index_bytes.get(i).length);
			used += code_to_max_index_bytes.get(i).length;
			
			
			// add length of file mod packet-chunk size
			System.arraycopy(code_to_length_mod_bytes.get(i), 0, payload, used, code_to_length_mod_bytes.get(i).length);
			used += code_to_length_mod_bytes.get(i).length;
			
			// add size of name to packet
			int size_of_name = code_to_name_bytes.get(i).length;
			byte[] size_of_name_in_bytes = transmission.toBytes(size_of_name);
			
			System.arraycopy(size_of_name_in_bytes, 0, payload, used, size_of_name_in_bytes.length);
			used += size_of_name_in_bytes.length;
			
			// add name to packet
			System.arraycopy(code_to_name_bytes.get(i), 0, payload, used, code_to_name_bytes.get(i).length);
			used += code_to_name_bytes.get(i).length;
			
			
			code_to_taken.put(i, used);
			first_packet_bytes.put(i, payload);
			
			
			
		}
		
	}
	
	public byte[] get_first_packet_tag(){
		return transmission.toBytes( (int) 0);
	}
	
	public void set_code_to_length_mod(){
		for(int i = 0; i < base_of_names.number_of_files; i++){
			code_to_length_mod.put(i, code_to_length_in_bytes.get(i) % base_of_names.bytes_in_packet_chunk);
			code_to_length_mod_bytes.put(i, transmission.toBytes(code_to_length_mod.get(i)));
		}
	}
	
	public void set_code_to_length_in_bytes()
	{
		for(int i = 0; i< base_of_names.number_of_files; i++){
			code_to_length_in_bytes.put(i, (int) base_of_names.code_to_file.get(i).length() );
		}
	}
	
	public void set_code_to_max_index(){
		for(int i = 0; i < base_of_names.number_of_files; i++)
		{
			code_to_max_index.put(i, packetPopulator.get_number_packets( base_of_names.code_to_file.get(i),  base_of_names.bytes_in_packet_chunk) );
			code_to_max_index_bytes.put(i, transmission.toBytes( code_to_max_index.get(i) ));
		}
	}
	

	public void set_code_to_name(){
		for(int i = 0; i < base_of_names.number_of_files; i++){
			code_to_name.put(i, packetPopulator.file_name_string( base_of_names.code_to_file.get(i) ) ); 
			code_to_name_bytes.put(i, binary_name_from_file( base_of_names.code_to_file.get(i) ) );
		}
	}
	
	// Static method for translating the name of a file to bytes
	public static byte[] binary_name_from_file(File target){
		byte[] binary_name;
		try {
			binary_name = packetPopulator.file_name_string(target).getBytes("UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			System.out.println("Error: One of the files you're transmitting has a forbidden character in it's name:"
					+ "All names must consist entirely of ASCII characters.");
			binary_name = new byte[1];
			e1.printStackTrace();
		}
		
		return binary_name;
		
	}
	


}
