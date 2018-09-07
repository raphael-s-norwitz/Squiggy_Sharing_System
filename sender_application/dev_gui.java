/*
 * 
 * Development GUI for main transfer 
 * 
 * by: Raphael Norwitz
 * 
 * 
 */
import java.util.*;
import java.io.*;
import java.net.InetAddress;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;


public class dev_gui {
	// values for gui
	public JFrame main_frame;
	public JPanel main_panel;
	public JButton to_existing;
	public JButton create_group;
	public JButton blind_share;
	
	public InetAddress share_through;
	
	public dev_gui(InetAddress b_cast_addr){
		
		// instantiate main frames
		main_frame = new JFrame("Share");
		main_panel = new JPanel();
		to_existing = new JButton("Share with group");
		create_group = new JButton("Create new group");
		blind_share = new JButton("Run Blind Share");
		
		share_through = b_cast_addr;
		
		// group on frame
		main_panel.setLayout(new FlowLayout());
		main_panel.add(to_existing);
		main_panel.add(create_group);
		main_panel.add(blind_share);
		
		// set layout
		main_frame.setLayout(new BorderLayout());
		main_frame.add(main_panel, BorderLayout.SOUTH);
    	main_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	main_frame.pack();
    	main_frame.setVisible(true);
    	
    	blind_share.addActionListener(new dev_blind_transfer(share_through));
		
		
		
	}

	public static void main(String[] args) {
		
		InetAddress addr = null;
		boolean connected_to_network = false;
		// while(! connected to network)
			try{
				addr = transmission.getNetworkLocalBroadcastAddressdAsInetAddress();
				System.out.println(addr.getHostName());
			}
			catch(IOException e){
				e.printStackTrace();
			}
		
		dev_gui my_gui = new dev_gui(addr);

	}

}
