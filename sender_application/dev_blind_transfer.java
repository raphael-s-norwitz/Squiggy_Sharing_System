/*
 * 
 * Development GUI for blind transfer 
 * 
 * by: Raphael Norwitz
 * 
 * 
 */
import java.util.*;
import java.io.*;
import java.net.InetAddress;
import java.net.SocketException;
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
		        	System.out.println("Security Code: " + Integer.toString(send_data.blind_outgoing_transmission.identifier));
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
			
			public BlindMetaSender full_metadata;
			public blindOutgoingTransmission complete_transfer;
			public transferRound transfer_data;
			
			public transferThread run_round;
			public Thread current_run_thread;
			
			public JButton metaTransfer;
			public JButton startPayload;
			public JButton stopPayload;
			
			public boolean another_payload_round;
			
			public ActionGui(blindOutgoingTransmission full_transfer)
			{
				another_payload_round = false;
				
				complete_transfer = full_transfer;
				
				full_metadata = full_transfer.meta_round();
				
				transfer_data = full_transfer.transfer_payload;
				try {
					run_round = new transferThread(transfer_data);
				} catch (SocketException e) {
					System.out.println("socket issue");
					e.printStackTrace();
				}
				
				
				transfer_frame = new JFrame("Control Transfer");
				control = new JPanel();
				metaTransfer = new JButton("Send First Round");
				startPayload = new JButton("Start Transfer");
				stopPayload = new JButton("Stop Transfer");
				
				stopPayload.setEnabled(false);
				
				
				ActionListener send_metadata_on_click = new ActionListener(){
					public void actionPerformed(ActionEvent ae){
						full_metadata.run();
						
					}
				};
				
				metaTransfer.addActionListener(send_metadata_on_click);
				
				ActionListener send_payload_onclick = new ActionListener(){
					public void actionPerformed(ActionEvent ae){
						// another_payload_round = true;
						
						startPayload.setEnabled(false);
						
						current_run_thread = new Thread(run_round, "Pthread");

						current_run_thread.start();
						
						stopPayload.setEnabled(true);
						
					}
				};
				
				ActionListener stop_send_payload_onclick = new ActionListener()  {
					public void actionPerformed(ActionEvent ae)  {
						stopPayload.setEnabled(false);
						current_run_thread.interrupt();
						startPayload.setEnabled(true);
					}
				};
				
				startPayload.addActionListener(send_payload_onclick);
				stopPayload.addActionListener(stop_send_payload_onclick);
				
				control.setLayout(new FlowLayout());
				control.add(metaTransfer); control.add(startPayload); control.add(stopPayload);
				
				transfer_frame.setLayout( new BorderLayout() );
				transfer_frame.add(control, BorderLayout.SOUTH );
				
				
				transfer_frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				transfer_frame.pack();
				transfer_frame.setVisible( true );
				
				
				
			}
			
			public void actionPerformed(ActionEvent ae){
				ActionGui run_command = new ActionGui(complete_transfer);
				
				
				
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
			
			demo.main_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			// System.out.println("Security Code: " + Integer.toString(demo.send_data.blind_outgoing_transmission.identifier));
			demo.main_frame.setVisible(true);
			
			
			// dev_blind_transfer demo = new dev_blind_transfer();
			/*demo.main_frame.setLayout(new BorderLayout());
			demo.main_frame.add(main_panel, BorderLayout.SOUTH);
	    	demo.main_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    	demo.main_frame.pack();*/
	    	// demo.main_frame.setVisible(true);
		}
		


}


