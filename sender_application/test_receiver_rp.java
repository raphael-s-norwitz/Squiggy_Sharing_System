import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;



public class test_receiver_rp {
	public static int PACKET_SIZE;
	public static int TRANSMIT_PORT;

	public static void main(String[] args) {
		
		// get packet size
		PACKET_SIZE = Integer.parseInt(args[0]);
		
		// get main port
		TRANSMIT_PORT = Integer.parseInt(args[1]);
		
		Scanner from_user = new Scanner(System.in);
		
		System.out.println("enter security code");
		
		int sec_code = Integer.parseInt(from_user.nextLine());

		
        // open a multicast socket
        MulticastSocket client_socket = null;
		try {
			client_socket = new MulticastSocket(TRANSMIT_PORT);
			 client_socket.setBroadcast(true);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


        // set timeout for blocking for 4 seconds
        // the payload
       byte[] msg = new byte[PACKET_SIZE];


        // create packet to receive
       DatagramPacket sent_packet = new DatagramPacket(msg, msg.length);
       for(int i = 0; i < 1200000; i++){
	        
	        try{
		
		        client_socket.receive(sent_packet);
		        
		        System.out.println("Packet number: " + Integer.toString(i) + " received");
		        
		        decode_packet(sent_packet, sec_code);
	        }
	
	
	        // catch some other exception
	        catch (Exception e){
	            e.printStackTrace();
	        }
		}
	


        // get data from packet
       //  byte[] data = sent_packet.getData();
        
       // decode_packet(sent_packet, sec_code);

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
	
	public static void decode_packet(DatagramPacket sent , int Security_code )
	{
		// get data from packet
		byte [] data = sent.getData();
		
		System.out.println("All data: " + Arrays.toString(data));
		
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
		
		System.out.println("Current index: " + index);

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
			name = new String(name_bytes, StandardCharsets.UTF_8);
			
			System.out.println("Name of file: " + name );
			
			/* get mod packet length
			byte[] mod_length_bytes = new byte[4];
			System.arraycopy(data, 24, mod_length_bytes, 0, 4);
			int mod_length = byteArrayToInt(mod_length_bytes);
			
			System.out.println("Mod length: " + Integer.toString(mod_length));
			*/
			
			// extract data
			payload = new byte[data.length - 20 -length];
			System.arraycopy(data, 20+length, payload, 0, data.length - 20 -length);	
			
			
		}
		
		else{
			
			// extract data
			payload = new byte[data.length - 16];
			System.arraycopy(data, 16, payload, 0, data.length -16);
	
		}
		
		String payload_string = new String(payload, StandardCharsets.UTF_8);
		
		System.out.println("Payload: " + Arrays.toString(payload));	
		
		
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

}
