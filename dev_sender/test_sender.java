// Property of Raphael Norwitz unauthorized usage or copying is forbidden

import java.io.*; 
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.LinkedList;
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

public class test_sender {
	public static int PACKET_SIZE; // 4096;
	

	public static void main(String[] args) {
		
		
		Scanner from_user = new Scanner(System.in);
		
		try {
			System.out.println(getNetworkLocalBroadcastAddressdAsInetAddress().toString());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if (args.length < 2){
			System.out.println("You must specify the size of the packets you will be sending first,"
					+ " then the path from you're root directory to the"
					+ " directory containing the files you wish to send as the second"
					+ " command line argument and the broadcast port as the third.");
			System.exit(1);
		}
		
		PACKET_SIZE = Integer.parseInt(args[0]);
		
		// get security code and ensure users have it before continuing
		
		int security = (int) Math.floor(Math.random() * 1000000);	
		System.out.println("Security code: " + security);
		System.out.println("Run setup by hitting return");
		String ret_code = from_user.nextLine();
		
		
		
		// get ArrayList of paths to files
		String path = args[1];
		ArrayList<File> to_Send = listFilesForFolder(new File(path));
		
		// get port value
		String port_string = args[2];
		int PORT = Integer.parseInt(port_string);
		
		DatagramPacket[][] files_packets = obtain_packets(path, PORT, security);
		
		LinkedList<DatagramPacket> first_round = get_initial_queue(files_packets);
		
		decode_packet(files_packets[0][0], security);
		
		try{
			
			MulticastSocket send_sock = new MulticastSocket();	
			send_sock.setBroadcast(true);
			int send_buffer = send_sock.getSendBufferSize();
			System.out.println("Current System send buffer size: "+ Integer.toString(send_buffer));
				
			for(int i = 0; i < 50; i++)
				{
					// if(i %10 == 0)
						Thread.sleep(100);
					DatagramPacket to_send = first_round.pop();
					send_sock.send(to_send);
					first_round.add(to_send);
					
				}
			send_sock.close();
			
			System.out.println("got out");
		
		}
			
		catch(Exception e){
			e.printStackTrace();
			
		}
		System.out.println("Run transfer by hitting return");
		
		ret_code = from_user.nextLine();
		
		int max_length_of_file_packets = 0;
		
		for(int i = 0; i < files_packets.length; i++){
			if(files_packets[i].length > max_length_of_file_packets)
			{
				max_length_of_file_packets = files_packets[i].length;
			}
		}
		
		System.out.println("maximum number of packets: " + Integer.toString(max_length_of_file_packets));
		
		LinkedList<DatagramPacket> transfer_round = get_sub_queue(files_packets, 1, max_length_of_file_packets);
		
		
		try{
			
			MulticastSocket send_sock = new MulticastSocket();	
			send_sock.setBroadcast(true);
				 Thread.sleep(1000);
				
			for(int i = 0; i < 1200000000; i++)
				{
					DatagramPacket to_send = transfer_round.pop();
					
						send_sock.send(to_send);
					transfer_round.add(to_send);
					
				}
			send_sock.close();
			
			System.out.println("got out");
		
		}
			
		catch(Exception e){
			e.printStackTrace();
			
		}
		
				
		from_user.close();
		

	}
	
	public static LinkedList<DatagramPacket> get_sub_queue(DatagramPacket[][] files_bytes, int min_index, int max_index)
	{
		
		LinkedList<DatagramPacket> ret_val = new LinkedList<DatagramPacket>();
		
		
		for(int j = min_index; j < max_index ; j++){
			for(int i = 0; i < files_bytes.length; i ++){
				if(j < files_bytes[i].length){
					System.out.println("File: "+ Integer.toString(i) + ", Index: " + Integer.toString(j) + "");
					ret_val.add(files_bytes[i][j]);
				}
			}
		}
		
		return ret_val;
		
		
	}
	
	// Note: This will likely return a jagged array
	public static DatagramPacket[][] obtain_packets(String path, int PORT, int security ){
		
		ArrayList<File> to_Send = listFilesForFolder(new File(path));
		
		long max_file_length = 0;
		int max_file_index = 0;
		
		
		String[] file_names = new String[to_Send.size()];
		
		
		for(int i = 0; i < to_Send.size() ; i++)
		{
			file_names[i] = to_Send.get(i).getName().toString();
			
			if(to_Send.get(i).length() + (file_names[i].length() * 16) > max_file_length + (file_names[max_file_index].length() * 16)) 
			{
				max_file_length = to_Send.get(i).length();
				max_file_index = i;
			}
		}
		
		
		int max_packets = get_number_packets(to_Send.get(max_file_index), PACKET_SIZE - 16); // 16 for security code, file code, index, max index. 4 + 4 for length of name and length of file mod packet size  
		
		System.out.println("Maximum number of packet: "+ Integer.toString(max_packets));
		
		// fill Datagram packet matrix by iterating through list of files 
		
		DatagramPacket[][] files_packets = new DatagramPacket[to_Send.size()][1];
		
		for(int i = 0; i < to_Send.size() ; i++)
		{
			System.out.println("File index: " + Integer.toString(i));
			
			// get file object in question
			File current_data = to_Send.get(i);
			
			// get the name of the file, it's binary
			String file_name = file_names[i];
			
			// get bytes of file
			Path to_file = Paths.get(path + "/" + file_name );
			byte[] file_bytes;
			
			try {
				file_bytes = Files.readAllBytes(to_file );
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				file_bytes = new byte[0];
				System.out.println("File not found.");
				e.printStackTrace();
				
			}//org.apache.commons.io./FileUtils.readFileToByteArray(current_data);
			
			if (file_bytes.length == 0){
				System.out.println("There is an empty file in the transmit directory. Get rid of it.");
				System.exit(0);
			}
			
			System.out.println("Size of file in bytes: " + Integer.toString(file_bytes.length));
				
			int packets_this_file = get_number_packets(current_data, PACKET_SIZE-16);
			
			files_packets[i] = new DatagramPacket[packets_this_file];
			
			System.out.println("Packets in this File: " + Integer.toString(packets_this_file));
			
			// get binary arrays for all known values
			byte[] binary_name;
			try {
				binary_name = file_name.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				System.out.println("Error: One of the files you're transmitting has a forbidden character in it's name:"
						+ "All names must consist entirely of ASCII characters.");
				binary_name = new byte[1];
				e1.printStackTrace();
			}
			byte[] security_bytes = toBytes(security);
			byte[] file_code = toBytes(i);
			System.out.println("file code: " + Arrays.toString(file_code));
			byte[] total_packets = toBytes(packets_this_file-1);
			
			int byte_pointer = 0;
			
			InetAddress b_cast_addr = null;
			
			try {
				b_cast_addr = getNetworkLocalBroadcastAddressdAsInetAddress();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			for(int j = 0; j < packets_this_file  ; j++)
			{	
				System.out.println(j);
				byte[] payload = new byte[PACKET_SIZE];
				byte[] payload_shorter; // only used for last packet
				
				
				byte[] current_packet_index = toBytes(j);
				
				int taken_so_far = 0;
				
				// fill array with fields common to all packets
				System.arraycopy(security_bytes, 0, payload, 0, security_bytes.length);
				taken_so_far += security_bytes.length;
				
				System.arraycopy(file_code, 0, payload, taken_so_far, file_code.length);
				taken_so_far += file_code.length;
				
				System.arraycopy(current_packet_index, 0, payload, taken_so_far,  current_packet_index.length);
				taken_so_far += current_packet_index.length;
				
				System.arraycopy(total_packets, 0, payload, taken_so_far, total_packets.length);
				taken_so_far += total_packets.length;
				
				// fill array with fields specific to packet in question
				if(j == 0)
				{
					// add size of file mod the packet size (used for allocation on receiver end)
					int size_of_file_mod = file_bytes.length % (PACKET_SIZE - 16);
					byte[] bytes_of_size_of_file = toBytes(size_of_file_mod);
					
					// add value to the array
					System.arraycopy(bytes_of_size_of_file, 0, payload, taken_so_far, bytes_of_size_of_file.length);
					taken_so_far += bytes_of_size_of_file.length;
					
					// add size of name to packet
					int size_of_name = binary_name.length;
					byte[] size_of_name_in_bytes = toBytes(size_of_name);
					
					System.arraycopy(size_of_name_in_bytes, 0, payload, taken_so_far, size_of_name_in_bytes.length);
					taken_so_far += size_of_name_in_bytes.length;
					
					// add name to packet
					System.arraycopy(binary_name, 0, payload, taken_so_far, binary_name.length);
					taken_so_far += binary_name.length;
				}
				
				// fill out payload if not one
				else{
	
					// bytes to read from this packet
					int to_read = PACKET_SIZE - taken_so_far;
					
					if ( file_bytes.length - byte_pointer < to_read)
					{
						to_read = file_bytes.length - byte_pointer;
						
						// create new array since it doesn't need PACKET_SIZE bytes
						payload_shorter = new byte[taken_so_far + file_bytes.length - byte_pointer];
						System.arraycopy(file_bytes, byte_pointer, payload, taken_so_far, to_read);
						
						// copy contents of old array into new array and set old array equal to the new array
						System.arraycopy(payload, 0, payload_shorter, 0, taken_so_far + file_bytes.length - byte_pointer);
						payload = payload_shorter;
					}
					
					else{
						System.arraycopy(file_bytes, byte_pointer, payload, taken_so_far, to_read);
					}
					byte_pointer += to_read;
				}
				
				
				
				try {
					
					
					// broadcast address
					files_packets[i][j] = new DatagramPacket(payload, payload.length, b_cast_addr, PORT);
					
				} catch (Exception e) { // UnknownHostException e) {
					System.out.println("Packet creation failed");
					e.printStackTrace();
				}
								
				
			}
			
			
			
		}
		return files_packets;
	}
	

	public static ArrayList<File> listFilesForFolder(File folder) {
		
		ArrayList<File> paths = new ArrayList<File>();
		
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            return listFilesForFolder(fileEntry);
	        } else {
	            paths.add(fileEntry);
	        }
	    }
	    
	    return paths;
	}
	
	public static int get_number_packets(File data, int payload_per_packet)
	{
		System.out.println("Length of data: " + data.length());
		
		int num_packets =  (int) (data.length() / payload_per_packet) + 1;
		int leftover = (int) data.length()  % payload_per_packet;
		
		if (leftover > 0)
			num_packets++;
		
		
		return num_packets;
	}
	
	public static byte[] toBytes(int i)
	{
	  byte[] result = new byte[4];

	  result[0] = (byte) (i >> 24);
	  result[1] = (byte) (i >> 16);
	  result[2] = (byte) (i >> 8);
	  result[3] = (byte) (i /*>> 0*/);

	  return result;
	}
	
	public static int byteArrayToInt(byte[] b) 
	{
	    int value = 0;
	    for (int i = 0; i < 4; i++) {
	        int shift = (4 - 1 - i) * 8;
	        value += (b[i] & 0x000000FF) << shift;
	    }
	    return value;
	}
	
    public static int get_packet_index(byte[] data){
        // extract index
        byte [] index_bytes = new byte[4];
        System.arraycopy(data, 8, index_bytes, 0, 4);
        int index = byteArrayToInt(index_bytes);
        return index;
    }
	
	public static void write_file(DatagramPacket[] data, String basepath, Date date)
	{
		String file_name = get_name(data[0]);
		System.out.println("file being copied: " + file_name);
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss.SSS");
		
		// Date date = new Date();
		System.out.println(sdf.format(date));
		
		String transfer_name = "transfer_" + sdf.format(date);
		
		create_dir(basepath);
		create_dir(basepath + transfer_name);

		
		for(DatagramPacket p: data){
			
			try{
				FileOutputStream output = new FileOutputStream(basepath + "/" + transfer_name + "/" + file_name, true);

				byte[] data_value = get_payload(p);
				output.write(data_value);
				output.close();
			}
			catch(NullPointerException e)
			{
				e.printStackTrace();
				break;
				
			}
			catch(Exception e)
			{
				System.out.println("No fucking clue");
				e.printStackTrace();
			}

			
 			
		}
	}
	
	public static void decode_packet(DatagramPacket sent, int Security_code)
	{
		// get data from packet
		byte [] data = sent.getData();
		
		
		// extract security code
		byte [] authentication = new byte[4];
		System.arraycopy(data, 0, authentication, 0, 4);
		int code = byteArrayToInt(authentication);
		
		if (code != Security_code)
		{
			System.out.println("invalid security code");
			return;
			
		}
		
		else{
			System.out.println("valid security code");
		}
		
		// extract file code
		byte[] file_code = new byte[4];
		System.arraycopy(data, 4, file_code, 0, 4);
		
		
		System.out.println("File code: " + Arrays.toString(file_code));
		
		// extract index
		byte [] index_bytes = new byte[4];
		System.arraycopy(data, 8, index_bytes, 0, 4);
		int index = byteArrayToInt(index_bytes);
		

		// extract max index
		byte [] max_index_bytes = new byte[4];
		System.arraycopy(data, 12, max_index_bytes, 0, 4);
		int max_index = byteArrayToInt(max_index_bytes);
				
		System.out.println("Max index: " + max_index);
		
		// check kind of packet
		
		String name = "";
		byte[] payload;
		
		
		if(index == 0)
		{
			// extract length of name from the packet
			byte[] length_mod_bytes = new byte[4];
			System.arraycopy(data, 16, length_mod_bytes, 0, 4);
			int length_mod = byteArrayToInt(length_mod_bytes);
			
			System.out.println("mod length: " + Integer.toString(length_mod));
			
			System.out.println("Packet size - 16: " + Integer.toString(PACKET_SIZE - 16) + " Max index - 1: " + Integer.toString(max_index-1));
			
			System.out.println("Packet Size: " + Integer.toString(((PACKET_SIZE - 16) * (max_index -1)) + length_mod));
			
			// extract length of name from the packet
			byte[] length_name = new byte[4];
			System.arraycopy(data, 20, length_name, 0, 4);
			int length = byteArrayToInt(length_name);
			
			System.out.println("Name of file length: " + Integer.toString(length));
			
			// extract name from the packet
			byte[] name_bytes = new byte[length];
			System.arraycopy(data, 24, name_bytes, 0, length);
			name = new String(name_bytes, /* StandardCharsets.UTF_8 */ Charset.forName("UTF-8"));
			
			System.out.println("Name of file: " + name );
			
			
			// extract data
			payload = new byte[data.length - 20 -length];
			System.arraycopy(data, 20+length, payload, 0, data.length - 20 -length);	
			
			
		}
		
		else{
			
			// extract data
			payload = new byte[data.length - 16];
			System.arraycopy(data, 16, payload, 0, data.length -16);
	
		}
		
		
		
 	}
	
	public static boolean contains_name(DatagramPacket arg){
		// get data from packet
		byte [] data = arg.getData();
		
		// extract index
		byte [] index_bytes = new byte[4];
		System.arraycopy(data, 8, index_bytes, 0, 4);
		int index = byteArrayToInt(index_bytes);
		
		// System.out.println("Current index: " + index);
		
		if(index == 0)
		{
			return true;
		}
		
		else{
			return false;
		}
		
	}
	
	public static String get_name(DatagramPacket arg){
		// get data from packet
		byte [] data = arg.getData();
		
		String name = "";
		// extract length of name from the packet
		byte[] length_name = new byte[4];
		System.arraycopy(data, 20, length_name, 0, 4);
		int length = byteArrayToInt(length_name);
		
		System.out.println("Name of file length: " + Integer.toString(length));
		
		// extract name from the packet
		byte[] name_bytes = new byte[length];
		System.arraycopy(data, 24, name_bytes, 0, length);
		name = new String(name_bytes, StandardCharsets.UTF_8);
		
		System.out.println("Name of file: " + name );
		
		return name;
		
	}
	
	public static byte[] get_payload(DatagramPacket arg){	
		// get data from packet
		byte [] data = arg.getData();
		
		String name;
		byte[] payload;
		
		if(contains_name(arg)){
			// extract packet 'remainder'
			byte[] leftover = new byte[4];
			System.arraycopy(data, 16, leftover, 0, 4);
			
			// extract length of name from the packet
			byte[] length_name = new byte[4];
			System.arraycopy(data, 20, length_name, 0, 4);
			int length = byteArrayToInt(length_name);
			
			System.out.println("Name of file length: " + Integer.toString(length));
			
			// extract name from the packet
			byte[] name_bytes = new byte[length];
			System.arraycopy(data, 24, name_bytes, 0, length);
			name = new String(name_bytes, StandardCharsets.UTF_8);
			
			System.out.println("Name of file: " + name );
			
			/* if first packet has data
			 * 
			 
				// extract data
				payload = new byte[data.length - 24 -length];
				System.arraycopy(data, 24+length, payload, 0, data.length - 24 -length);	
				return payload;
			*/
			return new byte[0];
			
		}
		else{
			// extract data
			payload = new byte[data.length - 16];
			System.arraycopy(data, 16, payload, 0, data.length - 16);
			return payload;
		}
		
	}
	
	public static void create_dir(String path_name){
		File new_file = new File(path_name);

		//File theDir = new File("new folder");

		// if the directory does not exist, create it
		if (!new_file.exists()) {
		    System.out.println("creating directory: " + new_file);
		    boolean result = false;

		    try{
		        new_file.mkdir();
		        result = true;
		    } 
		    catch(SecurityException se){
		        //handle it
		    	System.out.println("DIR already there");
		    }        
		    if(result) {    
		        System.out.println("DIR created");  
		    }
		}
	}
	
	public static LinkedList<DatagramPacket> get_initial_queue(DatagramPacket[][] files_bytes){
		LinkedList<DatagramPacket> initial_sender = new LinkedList<DatagramPacket>();
		
		for(int i =0; i < files_bytes.length; i++){
			initial_sender.add(files_bytes[i][0]);
		}
		
		return initial_sender;
	}
	


	
	public static String ipToString(long ip, boolean broadcast) {
	    String result = new String();

	    Long[] address = new Long[4];
	    for(int i = 0; i < 4; i++)
	        address[i] = (ip >> 8*i) & 0xFF;
	    for(int i = 0; i < 4; i++) {
	        if(i != 3)
	            result = result.concat(address[i]+".");
	        else result = result.concat("255.");
	    }
	    return result.substring(0, result.length() - 2);
	}
	
	public static int pack(byte[] bytes) {
		  int val = 0;
		  for (int i = 0; i < bytes.length; i++) {
		    val <<= 8;
		    val |= bytes[i] & 0xff;
		  }
		  return val;
		}
	
	// from stack overflow
	public static InetAddress getNetworkLocalBroadcastAddressdAsInetAddress() throws IOException {
		 System.setProperty("java.net.preferIPv4Stack", "true"); // yah.. its just that ugly, no other way around..
		    try
		    {
		      Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
		      if(niEnum.hasMoreElements()) // only a  
		      {
		        NetworkInterface ni = niEnum.nextElement();
		        System.out.println("new interface:" + ni.getDisplayName() + "\n");
		        for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses())
		        { 
		        	/*System.out.println("address" + interfaceAddress.getAddress().getHostAddress() + " name:"
		              + interfaceAddress.getAddress().getHostName() + " prefix:"
		              + interfaceAddress.getNetworkPrefixLength() + " broadcast:"
		              + interfaceAddress.getBroadcast());*/
		        	if(ni.isUp())
		        		return interfaceAddress.getBroadcast();
		        	
		         
		        	

		        }
		      }
		    }
		    catch (SocketException e)
		    {
		      e.printStackTrace();
		    }
		
		/* for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	        NetworkInterface intf = en.nextElement();
	        //if(VERSION.SDK_INT < 9) { 
	            if(!intf.getInetAddresses().nextElement().isLoopbackAddress()){
	                byte[] quads = intf.getInetAddresses().nextElement().getAddress();
	                quads[0] = (byte)255;
	                quads[1] = (byte)255;
	                System.out.println(Arrays.toString(quads));
	                return InetAddress.getByAddress(quads);
	            }
	        //}
	        
	        else{
	            if(!intf.isLoopback()){
	                List<InterfaceAddress> intfaddrs = intf.getInterfaceAddresses();
	                return intfaddrs.get(0).getBroadcast(); //return first IP address
	            }
	        }
	    }*/
	    return null;
	}
	
	public static class Message {
		private String tag;
		private String message;
		private long epoch = 0;
		private InetAddress ip;
		
		public Message(String message) throws IllegalArgumentException {
			this(message, (InetAddress) null);
		}
		
		public Message(String message, InetAddress ip) throws IllegalArgumentException {
			String split[] = message.split(" ");
			if(split.length < 3)
				throw new IllegalArgumentException();
			
			tag = split[0];
			epoch = Integer.parseInt(split[1]);
			this.ip = ip;
			
			message = "";
			for(int i = 2; i < split.length; i++)
				message = message.concat(split[i] + " ");
			
			this.message = message.substring(0, message.length() - 1);
			
		}
		
		public Message(String tag, String message) {
			this(tag, message, null);
		}
		
		public Message(String tag, String message, InetAddress ip) {
			this(tag, message, ip, System.currentTimeMillis()/1000);
		}
		
		public Message(String tag, String message, InetAddress ip, long time) {
			this.tag = tag;
			this.message = message;
			epoch = time;
			this.ip = ip;
		}
		
		public String getTag() { return tag; }
		public String getMessage() { return message; }
		public long getEpochTime() { return epoch; }
		public InetAddress getSrcIp() { return ip; }
		
		public String toString() { return tag+" " + epoch + " " + message; }
	}

}


