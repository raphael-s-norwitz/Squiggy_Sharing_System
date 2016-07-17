/*
 * 
 * Development GUI for blind transfer 
 * 
 * by: Raphael Norwitz
 * 
 * Code is property of Raphael Norwitz
 * 
 */
import java.util.*;
import java.io.*;
import java.net.InetAddress;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

public class dev_blind_transfer implements ActionListener{
		
		// values for main blind transfer gui
		public JFrame main_frame;
		public JPanel main_panel;
		public JPanel file_selected_panel;
		
		public JFrame transfer;
		public JFrame transfer_panel;
		
		public blindOutgoingTransmission send_data;
		
		
		
		
		public JButton select_file;
		public String default_path;
		public JTextField current_path;
		public JButton ok_to_proceed;
		
		// integer to keep track of buttons the user has pressed
		public int user_pressed;
		
		public InetAddress share_through;
		
		public JButton start_transfer_meta_data;
		public JButton start_transfer_data;
		public JButton finish;
		public pathFinder selection;
		
		public static String chosen_path;
		
		public dev_blind_transfer(InetAddress b_cast_addr){
			
			share_through = b_cast_addr;
			
			user_pressed = 0;
			
			// instantiate main frames
			main_frame = new JFrame("Share to Unspecified Group");
			main_panel = new JPanel();
			file_selected_panel = new JPanel();
			ok_to_proceed = new JButton("Continue");
			
			
			
			select_file = new JButton("Select files");
			start_transfer_meta_data = new JButton("Send First Round");
			start_transfer_data = new JButton("Start Sending");
			finish = new JButton("Pause transfer");
			
					
			
			// get default path for system to search
			default_path = System.getProperty("user.home");
			current_path = new JTextField(default_path);
			
			
			// group on frame
			main_panel.setLayout(new FlowLayout());
			main_panel.add(current_path);
			main_panel.add(select_file);
			

			
			// set layout
			main_frame.setLayout(new BorderLayout());
			main_frame.add(main_panel, BorderLayout.SOUTH);
	    	main_frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	    	main_frame.pack();
	    	/*main_frame.setVisible(true);*/
	    	
	    	/*
	    	 * create action listeners for subsequent actions
	    	 */
	    	
	    	// open dialogue for file selection
	    	selection = new pathFinder(default_path, select_file, current_path);
		    ActionListener after_file_selection = new ActionListener() {
		        public void actionPerformed(ActionEvent actionEvent) {
		        	
		        	main_panel.add(ok_to_proceed);
		        	main_panel.validate();
		        	main_frame.pack();
		        	
		        	// initialize data for this transfer
		        	// send_data = new blindOutgoingTransmission(1472, selection.ret_val, 4848, 1472); // TEST get vals from XML or something
		        	
		        	// ok_to_proceed.addActionListener( new ActionGui(send_data) );
		        	      	
		          
		        }
		      };
		      
		      // adds functionality to ok_to_proceed
		    ActionListener start_transfer_gui = new ActionListener() {
		        public void actionPerformed(ActionEvent actionEvent) {
		        	
		        	// initialize data for this transfer
		        	send_data = new blindOutgoingTransmission(1472, selection.ret_val, 4848, 1472 - 16, share_through); // TEST get vals from XML or something
		        	
		        	/* ok_to_proceed.addActionListener( */  new ActionGui(send_data) /*) */;
		        	      	
		          
		        }
		      };		      
	    	
	    	select_file.addActionListener(selection);
	    	select_file.addActionListener(after_file_selection);
	    	ok_to_proceed.addActionListener(start_transfer_gui);
	    	
	    	
			
		}
		
		public void actionPerformed(ActionEvent ae){
			
			dev_blind_transfer run = new dev_blind_transfer(share_through);
			/*run.main_frame.setLayout(new BorderLayout());
			run.main_frame.add(main_panel, BorderLayout.SOUTH);
	    	run.main_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    	run.main_frame.pack();*/ 
	    	run.main_frame.setVisible(true);
			
		}
		

		public static class ActionGui implements ActionListener {
			
			public JFrame transfer_frame;
			public JPanel control;
			
			public BlindMetaSender sendShot;
			
			public JButton MetaTransfer;
			public JButton StartPayload;
			public JButton StopPayload;
			
			public ActionGui(blindOutgoingTransmission full_transfer)
			{
				/*try{
					System.out.println( "Broadcast " +transmission.getNetworkLocalBroadcastAddressdAsInetAddress().getHostAddress() + "" );
				}
				catch(IOException e){
					e.printStackTrace();
				}*/
				
				sendShot = full_transfer.meta_round();
				
				transfer_frame = new JFrame("Control Transfer");
				control = new JPanel();
				MetaTransfer = new JButton("Send First Round");
				StartPayload = new JButton("Start Transfer");
				StopPayload = new JButton("Stop Transfer");
				
				ActionListener send_transfer = new ActionListener(){
					public void actionPerformed(ActionEvent ae){
						/* System.out.println(Arrays.toString(sendShot.broadcast_name_packets[0].getData()) + " " + sendShot.broadcast_name_packets[0].getAddress().getHostName());*/ 
						sendShot.set_broadcast_name_packets();
						sendShot.run();
						
					}
				};
				
				MetaTransfer.addActionListener(send_transfer);
				
				control.setLayout(new FlowLayout());
				control.add(MetaTransfer); control.add(StartPayload); control.add(StopPayload);
				
				transfer_frame.setLayout( new BorderLayout() );
				transfer_frame.add(control, BorderLayout.SOUTH );
				
				
				transfer_frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				transfer_frame.pack();
				transfer_frame.setVisible( true );
				
				
				
			}
			
			public void actionPerformed(ActionEvent ae){
				ActionGui run_command = new ActionGui(sendShot);
				
				
				
			}


			
		}

		
		public static void main(String args[]){
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
			// dev_blind_transfer demo = new dev_blind_transfer();
			/*demo.main_frame.setLayout(new BorderLayout());
			demo.main_frame.add(main_panel, BorderLayout.SOUTH);
	    	demo.main_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    	demo.main_frame.pack();*/
	    	// demo.main_frame.setVisible(true);
		}
		


}


