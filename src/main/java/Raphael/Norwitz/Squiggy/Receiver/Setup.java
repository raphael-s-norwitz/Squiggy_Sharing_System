// Property of Raphael Norwitz unauthorized usage or copying is forbidden

package raphael.norwitz.squiggy.receiver;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import java.io.File;

public class Setup extends AppCompatActivity {

    public final static String PACKET_LEN_STR = "com.dev.shannonnorwitz.PKT_LEN";
    public final static String SECURITY_CODE = "com.dev.shannonnorwitz.SECURE";
    public final static String PORT_USED = "com.dev.shannonnorwitz.PORT_VAL";
    public final static String HOST_IP = "com.dev.shannonnorwitz.HOST_IP";
    public final static String NUMBER_OF_FILES = "com.dev.shannonnorwitz.NUM_FILES";
    public final static String TARGET_DIRECTORY = "com.dev.shannonnorwitz.TARGET";

    public EditText Packet_length;
    public EditText Security;
    public EditText Port;
    public EditText Host;
    public EditText Number_Files;
    public EditText Directory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get root file for downloads
        File root = android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        // Get path string from download and redirect to the 'to_transmit' directory
        String path_to_Download = root.getAbsolutePath();
        final String path_to_rcv = path_to_Download + "/../received_files";


        // Get EditText instances
        Packet_length = (EditText) findViewById(R.id.pkt_len);
        Security = (EditText) findViewById(R.id.security_cde);
        Port = (EditText) findViewById(R.id.host_port);
        Host = (EditText) findViewById(R.id.host_ip);
        Number_Files = (EditText) findViewById(R.id.number_files);
        Directory = (EditText) findViewById(R.id.target_dir);


        // set default field values
        Packet_length.setText("1472");
        Port.setText("4848");
        Host.setText("0.0.0.0");
        Directory.setText(path_to_rcv);
        Number_Files.setText("1"); // depreciated work out of code
        Security.setText("0");     // ''       ''





        FloatingActionButton setup = (FloatingActionButton) findViewById(R.id.run_setup);
        setup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                // get user input values
                int Packet_bytes = Integer.parseInt(Packet_length.getText().toString());
                int Security_Code = Integer.parseInt(Security.getText().toString());
                int port_num = Integer.parseInt(Port.getText().toString());
                String host_addr = Host.getText().toString();
                int total_files = Integer.parseInt(Number_Files.getText().toString());
                String tgt_dir = Directory.getText().toString();

                System.out.println(Packet_bytes);
                System.out.println(Security_Code);
                System.out.println(port_num);
                System.out.println(host_addr);
                System.out.println(total_files);
                System.out.println(tgt_dir);

                // create intent and add values
                Intent intent = new Intent(view.getContext(), GetFileInfo.class);

                intent.putExtra(PACKET_LEN_STR, Packet_bytes);
                intent.putExtra(SECURITY_CODE, Security_Code);
                intent.putExtra(PORT_USED, port_num);
                intent.putExtra(HOST_IP, host_addr);
                intent.putExtra(NUMBER_OF_FILES, total_files);
                intent.putExtra(TARGET_DIRECTORY, tgt_dir);

                // start the new activity
                startActivity(intent);






            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_setup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
