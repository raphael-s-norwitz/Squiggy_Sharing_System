package raphael.norwitz.squiggy.receiver;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.*;
import android.os.Process;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;

public class GetFileInfo extends AppCompatActivity {

    // from prior intent
    public final static String PACKET_LEN_STR = "com.dev.shannonnorwitz.PKT_LEN";
    public final static String SECURITY_CODE = "com.dev.shannonnorwitz.SECURE";
    public final static String PORT_USED = "com.dev.shannonnorwitz.PORT_VAL";
    public final static String HOST_IP = "com.dev.shannonnorwitz.HOST_IP";
    public final static String NUMBER_OF_FILES = "com.dev.shannonnorwitz.NUM_FILES";
    public final static String TARGET_DIRECTORY = "com.dev.shannonnorwitz.TARGET";

    // from this intent
    public final static String NAMES = "com.dev.shannonnorwitz.FILE_NAMES";
    public final static String FILE_LENGTHS_IN_BYTES = "com.dev.shannonnorwitz.LENGTH_ARRAY_IN_BYTES";
    public final static String FILE_MAX_INDEX = "com.dev.shannonnorwitz.FILE_MAX_IND";
    public final static String FILE_CODES = "com.dev.shannonnorwitz.F_CODES";
    public final static String PACKET_SIZE = "com.dev.shannonnorwitz.P_SIZE";

    // public final static int PACKET_LENGTH = 256;
    //public final static int PACKET_LENGTH =   8192; // 1472;

    // get from setup
    public static int PACKET_LENGTH;
    public int Security_code;
    public int Port;
    public String Host;
    public int Number_files;
    public String Path;

    // get wifi manager for lock
    public WifiManager wifi_Manager;

    // queue for storing packets to process
    public LinkedList<DatagramPacket> process_queue;

    // main context
    public Context main_context;

    // Value fields
    public TextView total_packets;
    public TextView files_new;

    // store names for display
    public ArrayList<String> names_received;
    public ArrayAdapter<String> Name_Adapter;

    // store values which will be sent by intent
    public ArrayList<Integer> file_indexes;
    public ArrayList<Integer> file_codes;
    public ArrayList<Integer> file_lengths;
    public ArrayList<String> names_to_send;

    // counters for keeping track of packets
    public static int packet_counter;
    public static int file_counter;
    public static boolean GOT_ALL_NAMES;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_file_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get Setup intent
        Intent past_intent = getIntent();

        // get values from Setup
        PACKET_LENGTH = past_intent.getIntExtra(Setup.PACKET_LEN_STR, 0);
        Security_code = past_intent.getIntExtra(Setup.SECURITY_CODE, 0);
        Port = past_intent.getIntExtra(Setup.PORT_USED, 0);
        Host = past_intent.getStringExtra(Setup.HOST_IP);
        Number_files= past_intent.getIntExtra(Setup.NUMBER_OF_FILES, 0);
        Path = past_intent.getStringExtra(Setup.TARGET_DIRECTORY);

        // get context
        main_context = this;

        // For keeping track once done receiving file names
        packet_counter = 0;
        file_counter = 0;
        GOT_ALL_NAMES = false;


        // get wifi manager object
        wifi_Manager = (WifiManager) getSystemService(WIFI_SERVICE);

        // for the names scrolling list
        names_received = new ArrayList<String>();
        Name_Adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names_received);

        // create vectors to store other values to pass
        file_codes = new ArrayList<Integer>();
        file_lengths = new ArrayList<Integer>();
        file_indexes = new ArrayList<Integer>();
        names_to_send = new ArrayList<String>();

        // get the list-view by id
        ListView listView = (ListView) findViewById(R.id.name_view);
        listView.setAdapter(Name_Adapter);

        // get static field values
        total_packets = (TextView) findViewById(R.id.packet_count);
        files_new = (TextView) findViewById(R.id.file_count);

        // activate queue
        process_queue = new LinkedList<DatagramPacket>();

        // run the receiver thread
        try {
            System.out.println("running receiver thread");
            receiver_thread rcv_thread = new receiver_thread("receiver_thread", Host, Port);
            rcv_thread.start();
        }
        catch(Exception e){
            e.printStackTrace();
        }

        try {
            System.out.println("running update thread");
            update_UI ud_UI = new update_UI("ud_thread");
            ud_UI.start();
        }
        catch(Exception e){
            e.printStackTrace();
        }

        // run the update thread
       /* try {
            System.out.println("running update thread");
            update_thread ud_thread = new update_thread("ud_thread");
            ud_thread.start();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        */


        FloatingActionButton move_on = (FloatingActionButton) findViewById(R.id.fab);
        move_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/

                if (file_codes.isEmpty()) {
                    return;
                }


                GOT_ALL_NAMES = true;

                process_queue.clear();

                // total_packets.setText("Total Packets Received: " + Integer.toString(packet_counter));

                System.out.println("File Codes: " + file_codes.toString());
                System.out.println("File Names: " + names_received.toString());
                System.out.println("File lengths in bytes: " + file_lengths.toString());
                System.out.println("File Max packet indexes: " + file_indexes.toString());

                // create intent and add values
                Intent intent = new Intent(view.getContext(), TransferData.class);

                // add new field
                System.out.println(names_to_send);
                intent.putStringArrayListExtra(NAMES, names_to_send);
                intent.putIntegerArrayListExtra(FILE_LENGTHS_IN_BYTES, file_lengths);
                intent.putIntegerArrayListExtra(FILE_MAX_INDEX, file_indexes);
                intent.putIntegerArrayListExtra(FILE_CODES, file_codes);

                // add old fields
                intent.putExtra(SECURITY_CODE, Security_code);
                intent.putExtra(PORT_USED, Port);
                intent.putExtra(HOST_IP, Host);
                intent.putExtra(NUMBER_OF_FILES, Number_files);
                intent.putExtra(TARGET_DIRECTORY, Path);
                intent.putExtra(PACKET_SIZE, PACKET_LENGTH);

                // start the new activity
                startActivity(intent);


            }
        });
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);






    }

    // update thread class
    class update_UI implements Runnable {
        private Thread t;
        private String threadname;

        update_UI(String name) {
            threadname = name;


        }


        public void run_update() {

            while(! GOT_ALL_NAMES)
            {
                // Wait 1 second
                /*try {
                    Thread.sleep(1000);
                }
                catch(Exception e){
                    e.printStackTrace();
                }*/



                try {
                    // updates packets
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            total_packets.setText("Total Packets Received: " + Integer.toString(packet_counter));

                        }
                    });

                    // updates UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            files_new.setText("Total Files Received: " + Integer.toString(file_counter));
                            Name_Adapter.notifyDataSetChanged();

                        }
                    });

                    Thread.sleep(300);
                }
                catch(InterruptedException e){
                    e.printStackTrace();
                }

            }

        }
        public void run() {
            run_update();
        }

        public void start() {
            t = new Thread(this, threadname);
            t.start();
        }

    }



    // update thread class
    class update_thread implements Runnable {
        private Thread t;
        private String threadname;


        update_thread(String name) {
            threadname = name;


        }




        public void run_update() {

            // run UI thread
            // run the update thread
            try {
                System.out.println("running update thread");
                update_UI ud_UI = new update_UI("ud_thread");
                ud_UI.start();
            }
            catch(Exception e){
                e.printStackTrace();
            }

            while(! GOT_ALL_NAMES)
            {

                // continue if all the packets have been processed
                /*if (process_queue.isEmpty())
                {
                    continue;
                }*/

                DatagramPacket sent_packet = null;

                // get next packet
                try {
                    sent_packet = process_queue.pop();
                }
                catch(Exception e){
                    System.out.println(process_queue.isEmpty());
                    continue;
                }

                // get data from packet
                byte[] data = sent_packet.getData();

                // get packet index
                if(get_packet_index(data) != 0){
                    continue;
                }

                // get file code
                int file_code = get_file_code(data);



                /*
                ADD SECURITY CHECK HERE
                 */

                // continue if already recieved
                if(file_codes.contains((Integer) file_code))
                {
                    continue;
                }

                // if not add values to fields to pass on:
                int file_max_idx = get_max_file_index(data);
                String received = get_name(data);
                int file_bytes_length = get_length_in_bytes(data);

                // add them to the relevant ArrayLists
                names_received.add(received + " - " + humanReadableByteCount(file_bytes_length, true) );
                file_codes.add((Integer) file_code);
                file_indexes.add((Integer) file_max_idx);
                file_lengths.add((Integer) file_bytes_length);
                names_to_send.add(received);


                // increment file counter
                file_counter++;



            }


        }
        public void run() {
            run_update();


        }

        public void start() {
            t = new Thread(this, threadname);
            t.start();

        }

    }

    // reciever thread cleas
    class receiver_thread implements Runnable {
        private Thread t;
        private String threadname;
        private String servername;
        private int port;


        receiver_thread(String name, String server, int port_b) {
            threadname = name;
            servername = server;
            port = port_b;

        }


            public void run_sock(String serverName, int port) {



                // Try open up a multicast socket and recieve packets from the client
                try {

                    // Get multicast lock
                    WifiManager.MulticastLock multicastLock = wifi_Manager.createMulticastLock("multicast_lock");
                    multicastLock.acquire();


                    // Broadcast

                    MulticastSocket client_socket = new MulticastSocket(port);
                    // DatagramSocket client_socket = new DatagramSocket(port);
                    System.out.println("test for socket implementation");
                    // InetAddress group = InetAddress.getByName("224.0.0.110");
                    // client_socket.joinGroup(group);

                    // set timeout for blocking for 4 seconds
                    client_socket.setSoTimeout(4000);

                    client_socket.setBroadcast(true);



                    // Multicast
                    /*
                    MulticastSocket client_socket = new MulticastSocket(port);
                    // DatagramSocket client_socket = new DatagramSocket(port);
                    System.out.println("test for socket implementation");
                    InetAddress group = InetAddress.getByName("224.0.0.110");
                    client_socket.joinGroup(group);
                    System.out.println("Multicast ON");

                    // set timeout for blocking for 4 seconds
                    client_socket.setSoTimeout(4000);

                    client_socket.setBroadcast(true);
                    */


                    // Unicast

                    /*
                    // MulticastSocket client_socket = new MulticastSocket(port);
                    DatagramSocket client_socket = new DatagramSocket(port);
                    System.out.println("test for socket implementation");
                    // InetAddress group = InetAddress.getByName("224.0.0.110");
                    // client_socket.joinGroup(group);

                    // set timeout for blocking for 4 seconds
                    client_socket.setSoTimeout(4000);

                    // client_socket.setBroadcast(true);
                    */

                    // the payload
                   byte[] msg = new byte[PACKET_LENGTH];



                    // keep going until it's the right number of names
                    while (!GOT_ALL_NAMES) {

                        /*try{
                            Thread.sleep(1);
                            System.out.println("Sleeping");
                        }
                        catch(Exception e){
                            System.out.println("not liking sleep");
                            e.printStackTrace();
                        }*/

                        // create packet to receive
                        DatagramPacket sent_packet = new DatagramPacket(msg, msg.length);


                        // try to receive and if there is a timeout go back to beginning of loop
                        try {
                            client_socket.receive(sent_packet);
                        }

                        // catch socket timeout and continue
                        catch(SocketTimeoutException e){
                            System.out.println("in timeout");
                            continue;
                        }

                        // catch some other exception
                        catch (Exception e){
                            e.printStackTrace();
                        }

                        // packet received add it to the queue
                        // process_queue.add(sent_packet);

                        // keep track of number of packets recieved
                        packet_counter++;

                        // get data from packet
                        byte[] data = sent_packet.getData();

                        // get packet index
                        if(get_packet_index(data) != 0){
                            continue;
                        }

                        // get file code
                        int file_code = get_file_code(data);



                        /*
                        ADD SECURITY CHECK HERE
                         */

                        // continue if already recieved
                        if(file_codes.contains((Integer) file_code))
                        {
                            continue;
                        }

                        // if not add values to fields to pass on:
                        int file_max_idx = get_max_file_index(data);
                        String received = get_name(data);
                        int file_bytes_length = get_length_in_bytes(data);

                        // add them to the relevant ArrayLists
                        names_received.add(received + " - " + humanReadableByteCount(file_bytes_length, true) );
                        file_codes.add((Integer) file_code);
                        file_indexes.add((Integer) file_max_idx);
                        file_lengths.add((Integer) file_bytes_length);
                        names_to_send.add(received);


                        // increment file counter
                        file_counter++;







                    }

                    // close socket
                    client_socket.close();
                    multicastLock.release();

                    System.out.println(file_codes);


                } catch (IOException e) {
                    System.out.println("here");
                    e.printStackTrace();
                }

            }
        public void run() {
            run_sock(servername, port);

        }

        public void start() {
            t = new Thread(this, threadname);
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            t.start();

        }

    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }


    public static String get_name(byte[] data){

        String name = "";
        // extract length of name from the packet
        byte[] length_name = new byte[4];
        System.arraycopy(data, 20, length_name, 0, 4);
        int length = byteArrayToInt(length_name);

        // System.out.println("Name of file length: " + Integer.toString(length));

        // extract name from the packet
        byte[] name_bytes = new byte[length];
        System.arraycopy(data, 24, name_bytes, 0, length);
        name = new String(name_bytes, Charset.forName("UTF-8")); // StandardCharsets.UTF_8);


        return name;

    }

    public static int get_file_code(byte[] data){

        // extract file code
        byte[] file_code = new byte[4];
        System.arraycopy(data, 4, file_code, 0, 4);

        return byteArrayToInt(file_code);

    }

    public static int get_max_file_index(byte [] data){
        // extract max index
        byte [] max_index_bytes = new byte[4];
        System.arraycopy(data, 12, max_index_bytes, 0, 4);
        int max_index = byteArrayToInt(max_index_bytes);

        return max_index ;

    }

    // get length of file in bytes
    public static int get_length_in_bytes(byte [] data){
        // get max file index
        int max_index = get_max_file_index(data);

        // get modulus value
        byte[] length_mod_bytes = new byte[4];
        System.arraycopy(data, 16, length_mod_bytes, 0, 4);
        int length_mod = byteArrayToInt(length_mod_bytes);

        return ((PACKET_LENGTH - 16) * (max_index -1)) + length_mod;

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




}
