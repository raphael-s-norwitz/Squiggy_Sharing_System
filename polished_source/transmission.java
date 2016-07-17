/*
 * 
 * This class represents the highest level superclass transmission of a set of files
 * using my broadcast system
 * 
 * All Code written by and property of Raphael Norwitz
 * 
 */



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
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.LinkedList;

public class transmission {
	
	// essential instance variables, provided by user
	public int packet_size;
	public String transmission_path;
	public int broadcast_port;
	public int bytes_in_packet_chunk; // add facilities for this
	
	

	
	/*
	 * Generic constructor
	 */
	
	public transmission(int bytes_in_packet, String path_given, int main_port)
	{
		// instantiate main instance variables
		packet_size = bytes_in_packet;
		transmission_path = path_given;
		broadcast_port = main_port;
		
	}
	
	
		
	// method to get a the bytes of a file
	public static byte[] get_file_bytes(File target, String path_to)
	{
		String file_name = file_name_string(target);
		
		// get bytes of file
		Path to_file = Paths.get(path_to + "/" + file_name );
		byte[] file_bytes;
		
		try {
			file_bytes = Files.readAllBytes(to_file );
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			file_bytes = new byte[0];
			System.out.println("File not found.");
			e.printStackTrace();
		}
		
		return file_bytes;
		
	}
	
	
	// easy way to get file name as a string
	public static String file_name_string(File target)
	{
		return target.getName().toString();
	}
	
	// recursive function to get all file names
	public static ArrayList<File> listFilesForFolder(File folder) {
		
		ArrayList<File> paths = new ArrayList<File>();
		
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	        	// Check this, probably want continue
	            return listFilesForFolder(fileEntry);
	        } else {
	            paths.add(fileEntry);
	        }
	    }
	    
	    return paths;
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
	
	
	/*
	 * 
	 * Ugly, but effective method from stack overflow to get broadcast address
	 * 
	 * If there is an IOException from this function, check broadcast address
	 * 
	 */
	public static InetAddress getNetworkLocalBroadcastAddressdAsInetAddress() throws IOException {
		 System.setProperty("java.net.preferIPv4Stack", "true"); 
		    try
		    {
		    	// System.out.println("in network interface 5");
		      Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
		      if(niEnum.hasMoreElements()) // only a  
		      {
		    	  // System.out.println("in network interface 4");
		        NetworkInterface ni = niEnum.nextElement();
		        for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses())
		        { 
		        	// System.out.println("in network interface 3");
		        	
		        	if(ni.isUp())
		        	{
		        		// System.out.println("in network interface 1");
		        		InetAddress ret_val = interfaceAddress.getBroadcast();
		        		if(ret_val == null){
		        			System.out.println("ret val null");
		        			break;
		        			}
		        		return ret_val; // interfaceAddress.getBroadcast();
		        	}
		        	
		        	// System.out.println("in network interface 2");

		        }
		      }
		      
		    }
		    catch (SocketException e)
		    {
		    	System.out.println("socket exception");
		      e.printStackTrace();
		    }
		    catch (Exception r){
		    	System.out.println("some kind of error");
		    	r.printStackTrace();
		    }
		
	    return InetAddress.getByName("255.255.255.255");
	}
	
	public static void main(String[] args){
		InetAddress addr = null;
		try{
			addr = transmission.getNetworkLocalBroadcastAddressdAsInetAddress();
			System.out.println(addr.getHostName());
		}
		catch(IOException e){
			e.printStackTrace();
		}
		dev_blind_transfer demo = new dev_blind_transfer(addr);
		demo.main_frame.setVisible(true);
	}
	
	
	
	

}
