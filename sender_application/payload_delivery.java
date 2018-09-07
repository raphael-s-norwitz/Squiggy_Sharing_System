import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;

/*
 * 
 * This is the payload delivery class
 * 
 * It takes in an InetAddress, a port, an array of files, and hashtables(?), packet size, pure payload size
 * 
 * From there it dynamically generates and sends broadcast packets of the files which it then 
 * 
 */
public class payload_delivery implements ActionListener, Runnable{
	
	// main instance variables for the sender class
	
	FileInputStream[] in_files;
	ArrayList<File> descriptors;
	DatagramPacket vessel;
	InetAddress broadcast_address; 
	byte[] store_payloads;
	
	
	
	public payload_delivery(ArrayList<File> to_transmit, int packet_size, int pure_payload InetAddress b_cast_addr, ){
		
	}



	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	
	

}
