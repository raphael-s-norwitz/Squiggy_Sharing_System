import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;

public class pathFinder implements ActionListener{
	

	public JButton signal;
	public JTextField update;
	public JFileChooser chosen;
	public String ret_val;
	public String starting_path;
	
	public pathFinder(String start_path, JButton button, JTextField path_spec){
		starting_path = start_path;
		signal = button;
		update = path_spec;


	}
	
	public void actionPerformed(ActionEvent ae){
		chosen = new JFileChooser();
		chosen.setCurrentDirectory(new File(starting_path));
		chosen.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if(chosen.showOpenDialog(signal) == JFileChooser.APPROVE_OPTION)
		{
			// nothing
		}
			
		try{
			ret_val = chosen.getSelectedFile().getAbsolutePath();
		}
		catch(NullPointerException e)
		{
			ret_val = starting_path;
		}
	
		update.setText(ret_val);
		
	}
	
	
	
}
