// Property of Raphael Norwitz unauthorized usage or copying is forbidden
package Raphael.Norwitz.Squiggy.Receiver;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;


public class TransferData extends AppCompatActivity {

    // for testing
    public File dump;
    public FileOutputStream test_out_file;


    // file data recieved
    public static byte[][] files_bytes;

    // size of packets
    public static int PACKET_SIZE;

    // get wifi manager for lock
    public WifiManager wifi_Manager;

    // store names for display
    public ArrayList<String> file_progress_list;
    public ArrayAdapter<String> Name_Adapter;

    // Value fields
    public LinearLayout file_list;
    public TextView total_packets;
    public TextView[] file_display;

    // Add statistics fields
    public TextView packets_dequeued_1;
    public TextView packets_processed;
    public TextView last_index_received;
    public TextView packets_added;
    public TextView packet_max;


    // queue for storing packets to process
    public LinkedList<DatagramPacket> process_queue;
    public LinkedList<byte[]> add_to_queue;

    // get from GetFileInfo
    public ArrayList<Integer> file_indexes;
    public ArrayList<Integer> file_codes;
    public ArrayList<Integer> file_lengths;
    public ArrayList<String> names_of_files;

    // get from Setup
    public int Security_code;
    public int Port;
    public String Host;
    public int Number_files;
    public String Path;
    public static String Path_spec;

    // Hashmaps from prior intent
    // public HashMap<Integer, Byte[]> code_to_data;
    public static HashMap<Integer, String> code_to_name;
    public static HashMap<Integer, Integer> code_to_max_index;
    public static HashMap<Integer, Integer> code_to_lengh_bytes;

    // Hashmaps for data
    public HashMap<Integer, Integer> code_to_max_consecutive_idx;
    public HashMap<Integer, Integer> code_to_bytes_so_far;

    // Hashmap for checking recieved
    public HashMap<Integer, Boolean>[] check_collision_in_file;
    public HashMap<String, Boolean> check_received;

    // counters for keeping track of packets
    public static int packet_counter;
    public static int in_process_counter;
    public static int out_process_counter;
    public static int added_counter;
    public static int last_idx;
    public static boolean GOT_ALL_DATA;
    public static int MIN_MAX_CONSEC;
    public static int Packets_needed;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_data);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get Setup intent
        Intent past_intent = getIntent();

        // get values from Setup
        Security_code = past_intent.getIntExtra(GetFileInfo.SECURITY_CODE, 0);
        Port = past_intent.getIntExtra(GetFileInfo.PORT_USED, 0);
        Host = past_intent.getStringExtra(GetFileInfo.HOST_IP);
        Number_files= past_intent.getIntExtra(GetFileInfo.NUMBER_OF_FILES, 0);
        Path = past_intent.getStringExtra(GetFileInfo.TARGET_DIRECTORY);

        // get values from GetFileInfo
        file_codes = past_intent.getIntegerArrayListExtra(GetFileInfo.FILE_CODES);
        file_indexes = past_intent.getIntegerArrayListExtra(GetFileInfo.FILE_MAX_INDEX);
        file_lengths = past_intent.getIntegerArrayListExtra(GetFileInfo.FILE_LENGTHS_IN_BYTES);
        names_of_files = past_intent.getStringArrayListExtra(GetFileInfo.NAMES);
        PACKET_SIZE = past_intent.getIntExtra(GetFileInfo.PACKET_SIZE, 0);


        // check that values are being transfered properly
        System.out.println("Security Code: " + Integer.toString(Security_code));
        System.out.println("Port: " + Integer.toString(Port));
        System.out.println("Host: " + Host);
        System.out.println("Number of files " + Integer.toString(Number_files));
        System.out.println("Path: " + Path);

        System.out.println(file_codes);
        System.out.println(file_indexes);
        System.out.println(file_lengths);
        System.out.println(names_of_files);
        System.out.println("Packet Size: " + Integer.toString(PACKET_SIZE));

        // initialize data array NOTE THIS IS A JAGGED ARRAY
        files_bytes = new byte[Collections.max(file_codes)+1][1];

        System.out.println("past byte creation");

        // get wifi manager object
        wifi_Manager = (WifiManager) getSystemService(WIFI_SERVICE);

        // create store of items for list on UI
        file_progress_list = new ArrayList<String>();

        Name_Adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, file_progress_list);

        // get the list-view by id
        ListView listView = (ListView) findViewById(R.id.name_view);
        listView.setAdapter(Name_Adapter);

        // get static field values
        total_packets = (TextView) findViewById(R.id.packet_count);
        packets_dequeued_1 = (TextView) findViewById(R.id.file_count);
        packets_processed = (TextView) findViewById(R.id.packets_processed);
        packets_added = (TextView) findViewById(R.id.packets_added);
        last_index_received = (TextView) findViewById(R.id.last_index_received);
        file_display = new TextView[file_codes.size()];
        packet_max = (TextView) findViewById(R.id.last_packet_rec);

        file_list = (LinearLayout) findViewById(R.id.l_layout);

        // activate queue
        process_queue = new LinkedList<DatagramPacket>();
        add_to_queue = new LinkedList<byte[]>();

        // For keeping track once done receiving file names
        packet_counter = 0;
        in_process_counter = 0;
        out_process_counter = 0;
        added_counter = 0;
        GOT_ALL_DATA = false;
        MIN_MAX_CONSEC = 0;
        Packets_needed = 0;

        System.out.println("past counters");

        // create directory in which files will be stored
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss.SSS");
        Date now = new Date();
        Path_spec = Path + "/transfer_" + sdf.format(now);
        create_dir(Path_spec, Path);

        System.out.println("Created directories");

        // fill out Hash tables
       //  code_to_data = new HashMap<Integer, Byte[]>();
        code_to_name = new HashMap<Integer, String>();
        code_to_max_index = new HashMap<Integer, Integer>();
        code_to_lengh_bytes = new HashMap<Integer, Integer>();
        code_to_max_consecutive_idx = new HashMap<Integer, Integer>();
        code_to_bytes_so_far = new HashMap<Integer, Integer>();

        check_received = new HashMap<String, Boolean>();
        //@SuppressWarnings("unchecked")
        check_collision_in_file = new HashMap[file_codes.size()];


        System.out.println("before loop");

        // TEST
        dump = new File(Path + "/dump.txt");




        // populate reference Hashtables with relevant values
        for(int i = 0; i < file_codes.size(); i++) {

            System.out.println("ith round: " + Integer.toString(i));

            // Hash file code to values
            code_to_lengh_bytes.put(file_codes.get(i), file_lengths.get(i));
            code_to_name.put(file_codes.get(i), names_of_files.get(i));
            code_to_max_index.put(file_codes.get(i), file_indexes.get(i));
            System.out.println("before file bytes");
            files_bytes[file_codes.get(i)] = new byte[file_lengths.get(i)];

            System.out.println("after Hash file code to values");

            // Hash file codes to max packet recieved
            code_to_max_consecutive_idx.put(file_codes.get(i), 0);
            code_to_bytes_so_far.put(file_codes.get(i), 0);

            // add index for collision
            check_collision_in_file[i]= new HashMap<Integer, Boolean>();

            // for display

            // array loader
            file_progress_list.add("" + names_of_files.get(i) + "- 0 of " + humanReadableByteCount((long) file_lengths.get(i), true) + " recieved");

            System.out.println("After arrayloader");

            // TextView
           /* file_display[i] = new TextView(this);
            file_display[i].setText("" + names_of_files.get(i) + "- 0 of " + humanReadableByteCount((long) file_lengths.get(i), true) + " recieved");
            file_list.addView(file_display[i]);*/

            Packets_needed += file_indexes.get(i);

            System.out.println("After TextView");


        }

        packet_max.setText("Total packets: " + Integer.toString(Packets_needed));

        System.out.println("got out of filling hashtables");

        // run the receiver thread
        try {
            System.out.println("receiver thread");
            receiver_thread rcv_thread = new receiver_thread("receiver_thread", Host, Port);
            rcv_thread.start();
        }
        catch(Exception e){
            System.out.println("here");
            e.printStackTrace();
        }

        System.out.println("ran reciever thread");

        // run UI thread
        // run the update thread
        try {
            System.out.println("running update UI thread");
            update_UI ud_UI = new update_UI("ud_ui_thread");
            ud_UI.start();
        }
        catch(Exception e){
            e.printStackTrace();
        }

        System.out.println("ran ui thread");


       /* try {
            System.out.println("running update thread");
            update_thread ud_thread = new update_thread("ud_thread");
            ud_thread.start();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("error on update end");
        }*/






        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               /* Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                GOT_ALL_DATA = true;

                /*try {
                    test_out_file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/



                for(int i = 0; i < file_codes.size(); i++){
                    try {
                        FileOutputStream output = new FileOutputStream(Path_spec + "/" + names_of_files.get(i));
                        output.write(files_bytes[file_codes.get(i)]);
                        output.close();
                    }
                    catch(Exception e){
                        System.out.println("Failed to write file");
                        e.printStackTrace();
                    }
                }



                System.out.println("registered");
                startActivity(new Intent(TransferData.this, Setup.class));

            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public static void create_dir(String path_name, String base_path){
        System.out.println(path_name);
        File new_file = new File(path_name);

        File base_dir = new File(base_path);

        System.out.println("Outside base directory");

        if( ! base_dir.exists() || ! base_dir.isDirectory()){
            System.out.println("inside base driectory");
            if(! base_dir.isDirectory())
            {
                base_dir.delete();
            }

            boolean worked = base_dir.mkdir();
            System.out.println(worked);
        }


        // if the directory does not exist, create it
        if (!new_file.exists()) {
            System.out.println("creating directory: " + new_file);
            boolean result = false;

            try{
                new_file.mkdir();
                result = true;
            }
            catch(SecurityException se) {
                //handle it
                System.out.println("DIR already there, clearing it out");
                String[] children = new_file.list();
                for (int i = 0; i < children.length; i++)
                {
                    new File(new_file, children[i]).delete();
                }
            }
            if(result) {
                System.out.println("DIR created");
            }
        }
        else if(new_file.isDirectory()){
            String[] children = new_file.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(new_file, children[i]).delete();
            }
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

                // for UDP
                // open a multicast socket
                MulticastSocket client_socket = new MulticastSocket(port);

               // System.out.println("BUFFER SIZE: " + Integer.toString(client_socket.getReceiveBufferSize()));

                client_socket.setReceiveBufferSize(Integer.MAX_VALUE);

                // client_socket.setReceiveBufferSize(4096);

               //  System.out.println("BUFFER SIZE New: " + Integer.toString(client_socket.getReceiveBufferSize()));


                // set timeout for blocking for 4 seconds
                client_socket.setSoTimeout(4000);

                client_socket.setBroadcast(true);



                // Socket client_socket = new Socket(Host, port);

                // the payload
               byte[] msg = new byte[PACKET_SIZE];


                /*

                    Test that an android application can send packets greater than 4096 bytes, it worked

                 */

                /* byte[] bs_buf = new byte[4096];
                Arrays.fill(bs_buf, (byte) 5);

                DatagramPacket bs_packet = null;

                try{
                    bs_packet = new DatagramPacket(bs_buf, bs_buf.length, InetAddress.getByName("255.255.255.255"), 4553);
                }
                catch(UnknownHostException e){

                    System.out.println("Bullshit packet creation failed");
                    e.printStackTrace();
                }

                client_socket.send(bs_packet);

                System.out.println("fine");

                */


               // test_out_file = new FileOutputStream(dPath + "/dump.txt");



                // keep going until it's the right number of names
                while (!GOT_ALL_DATA) {


                    /*try{
                        Thread.sleep(1);
                    }
                    catch(Exception e){
                        System.out.println("not liking sleep");
                        e.printStackTrace();
                    }*/


                    // create packet to receive
                    DatagramPacket sent_packet = new DatagramPacket(msg, msg.length);

                   //  System.out.println("File: " + get_file_code(sent_packet.getData()) + " Index: " + get_packet_index(sent_packet.getData()));




                    // try to recieve and if there is a timeout go back to beginning of loop
                    try {
                        client_socket.receive(sent_packet);
                    }



                    // catch socket timeout and continue
                    catch(SocketTimeoutException e){
                        continue;
                    }

                    //test_out_file.write(sent_packet.getData());


                    // catch some other exception
                    catch (Exception e){
                        total_packets.setText("crash here_1");
                        // e.printStackTrace();
                        continue;
                    }

                    // keep track of number of packets recieved
                    packet_counter++;

                    // packet received add it to the queue
                    //  process_queue.add(sent_packet);

                    /*

                    ################ see if this speeds it up (moved from update_thread) #########

                     */

                    // get data from packet
                    byte[] data = sent_packet.getData();


                    // get file code
                    int file_code = get_file_code(data);

                    // System.out.println("# File Code Before # : " + Integer.toString(file_code));


                    // System.out.println("file code: " + Integer.toString(file_code));


                    int file_idx = get_packet_index(data);

                    last_idx = file_idx;

                    int max_index =  code_to_max_index.get( file_code );

                    boolean done = false;

                    // don't accept name packets
                    if(file_idx == 0){
                        continue;
                    }

                    /*
                    ADD SECURITY CHECK HERE
                     */

                    // ignore if file code and index combination not valid
                    if(! file_codes.contains((Integer) file_code) || file_idx > max_index )
                    {
                        System.out.println("code and index invalid ");
                        continue;
                    }



                    // String lookup_key = "" + Integer.toString(file_code) + "**" + Integer.toString(file_idx) + "";

                    // ignore file if already recieved
                    // if( check_received.containsKey(lookup_key))
                    if(check_collision_in_file[file_code].containsKey(file_idx))
                    {
                        //System.out.println("already r1ecieved " + Integer.toString(file_code) + ", " + Integer.toString(file_idx)+ "");
                        continue;
                    }

                    in_process_counter++;



                    // check_received.put(lookup_key, true);
                    check_collision_in_file[file_code].put(file_idx, true);

                    /*
                    *
                    * if the index is only one more than the max consecutive number of packets
                    * received, fill it and find the next empty space. If the next empty space
                    * is the last packet write the file.
                    *
                    */
                    if(file_idx == code_to_max_consecutive_idx.get(file_code) + 1 )
                    {
                        int index_pointer = file_idx;


                        // while( check_received.containsKey("" + Integer.toString(file_code) + "**" + Integer.toString(index_pointer)) && index_pointer < max_index)
                        while(check_collision_in_file[file_code].containsKey(index_pointer) && index_pointer < max_index)
                        { // System.out.println("Inside loopthrough");
                            index_pointer++;
                        }

                        if(index_pointer == file_idx){

                            // set file done to true for processing thread
                            done = true;

                            try {
                                FileOutputStream output = new FileOutputStream(Path + "/" + names_of_files.get(file_code));
                                output.write(files_bytes[file_code]);
                                output.close();
                            }
                            catch(Exception e){
                                System.out.println("Failed to write file");
                                e.printStackTrace();
                            }

                        }
                        else {
                            code_to_max_consecutive_idx.put(file_code, index_pointer);
                            //check_collision_in_file[file_code].put(index_pointer, true);
                        }

                    }

                   /* try {
                        process_packet process_pkt = new process_packet("ud_thread", data, file_idx, max_index, file_code);
                        process_pkt.start();
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }*/

                    // System.out.println("File Code After processed: " + Integer.toString(file_code));

                    if(file_idx == max_index){
                        System.out.println("In max index");


                        // figure out length
                        int bytes_to_copy = code_to_lengh_bytes.get(file_code) % (PACKET_SIZE - 16);

                        try {
                            // write values to array
                            System.arraycopy(data, 16, files_bytes[file_code], code_to_lengh_bytes.get(file_code) - bytes_to_copy, bytes_to_copy);
                        }
                        catch(Exception e){
                            System.out.println("We got a problem in non-max");
                            System.out.println("MAXIMUM_INDEX: " + Integer.toString(max_index));
                            System.out.println("INDEX: " + Integer.toString(file_idx));
                            continue;

                        }

                        // update bytes count
                        int bytes_s_far = code_to_bytes_so_far.get(file_code);
                        code_to_bytes_so_far.put(file_code, bytes_s_far + bytes_to_copy);

                    }

                    else {
                        // System.out.println("File Code after accepted not final: " + Integer.toString(file_code));


                        // write array
                        try {
                            System.arraycopy(data, 16, files_bytes[file_code], (PACKET_SIZE - 16) * (file_idx - 1), data.length - 16);
                        }
                        catch(Exception e){
                            System.out.println("We got a problem");
                            System.out.println("MAXIMUM_INDEX: " + Integer.toString(max_index));
                            System.out.println("INDEX: " + Integer.toString(file_idx));
                            System.out.println("File index: " + Integer.toString(file_code));
                            System.out.println(e.getLocalizedMessage());

                            continue;
                        }


                        // update bytes count
                        int bytes_s_far = code_to_bytes_so_far.get(file_code);
                        code_to_bytes_so_far.put(file_code, bytes_s_far + data.length - 16);
                    }



                    added_counter++;



                    // update string for UI
                    file_progress_list.add(file_code, "" + code_to_name.get(file_code) + " - " + humanReadableByteCount((long) code_to_bytes_so_far.get( file_code), true) + " of " + humanReadableByteCount((long) code_to_lengh_bytes.get(file_code), true) + " received, max_consecutive packet index: " + Integer.toString(code_to_max_consecutive_idx.get(file_code)) + "");
                    file_progress_list.remove(file_code + 1);



                    out_process_counter++;




                }

                // close socket
                client_socket.close();
                multicastLock.release();


            } catch (IOException e) {
                System.out.println("here");
                total_packets.setText("crash here_2");
                e.printStackTrace();
            }

        }
        public void run() {
            run_sock(servername, port);

        }

        public void start() {
            t = new Thread(this, threadname);
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();

        }

    }




    // update user view thread class
    class update_UI implements Runnable {
        private Thread t;
        private String threadname;

        update_UI(String name) {
            threadname = name;


        }


        public void run_update() {

            while(! GOT_ALL_DATA)
            {
                // Wait 1 second
               try {
                    Thread.sleep(500);
                }
                catch(Exception e){
                    e.printStackTrace();
                }

                // updates packets
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        total_packets.setText("Total Packets Received: " + Integer.toString(packet_counter));
                        packets_dequeued_1.setText("Total Packets Processed: " + Integer.toString(in_process_counter));
                        packets_processed.setText("Total Packets Accepted: " + Integer.toString(out_process_counter));
                        packets_added.setText("Total Packets Written: " + Integer.toString(added_counter));
                        last_index_received.setText("Last Packet Index Received: " + Integer.toString(last_idx));


                        // System.out.println("before set text");
                        // System.out.println("Arraylist size: " + Integer.toString(file_progress_list.size()));
                        /*for(int i = 0; i < file_display.length; i++){
                            file_display[i].setText(file_progress_list.get(i));
                        }*/
                        Name_Adapter.notifyDataSetChanged();

                    }
                });


                // updates UI
                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        files_new.setText("Total Files Received: " + Integer.toString(file_counter));
                        Name_Adapter.notifyDataSetChanged();

                    }
                });*/


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





    // process_packet class
    class process_packet implements Runnable {
        private Thread t;
        private String threadname;
        private byte[] data;
        private int index;
        private int file_code;
        private int max_index;

        process_packet(String name, byte[] dat, int in, int m_in, int f_c) {
            threadname = name;
            data = dat;
            index = in;
            max_index = m_in;
            file_code = f_c;

        }


        public void run_process_update() {

            /*while(! GOT_ALL_DATA)
            {

                if(add_to_queue.isEmpty())
                {

                    continue;
                }

                byte[] data = add_to_queue.pop();


                // check if last packet
                int index = get_packet_index(data);
                int file_code = get_file_code(data);

                int max_index = code_to_max_index.get(file_code);
                */



                if(index == max_index){
                    System.out.println("In max index");
                    // figure out length
                    int bytes_to_copy = code_to_lengh_bytes.get(file_code) % (PACKET_SIZE - 16);

                    // write values to array
                    System.arraycopy(data, 16, files_bytes[file_code], code_to_lengh_bytes.get(file_code) - bytes_to_copy , bytes_to_copy );

                    // update bytes count
                    int bytes_s_far = code_to_bytes_so_far.get(file_code);
                    code_to_bytes_so_far.put(file_code, bytes_s_far + bytes_to_copy);

                }

                else {

                    // write array
                    System.arraycopy(data, 16, files_bytes[ file_code ], (PACKET_SIZE - 16) * (index - 1) , data.length - 16);

                    // update bytes count
                    int bytes_s_far = code_to_bytes_so_far.get(file_code);
                    code_to_bytes_so_far.put(file_code, bytes_s_far + data.length - 16);
                }

                added_counter++;



                /*
                *
                * if the index is only one more than the max consecutive number of packets
                * received, fill it and find the next empty space. If the next empty space
                * is the last packet write the file.
                *
                */

                /*
                if(index == code_to_max_consecutive_idx.get(file_code) + 1 )
                {
                    int index_pointer = index;


                    // while( check_received.containsKey("" + Integer.toString(file_code) + "**" + Integer.toString(index_pointer)) && index_pointer < max_index)
                    while(check_collision_in_file[file_code].containsKey(index_pointer) && index_pointer < max_index)
                    {
                        index_pointer++;
                    }

                    if(index_pointer == max_index){
                        // try write file
                        try {
                            FileOutputStream output = new FileOutputStream(Path + "/" + names_of_files.get(file_code));
                            output.write(files_bytes[file_code]);
                            output.close();
                        }
                        catch(Exception e){
                            System.out.println("Failed to write file");
                            e.printStackTrace();
                        }

                    }
                    else {
                        code_to_max_consecutive_idx.put(file_code, index_pointer);
                        //check_collision_in_file[file_code].put(index_pointer, true);
                    }

                }

                // update string for UI
                file_progress_list.add(file_code, "" + code_to_name.get(file_code) + " - " + humanReadableByteCount((long) code_to_bytes_so_far.get( file_code), true) + " of " + humanReadableByteCount((long) code_to_lengh_bytes.get(file_code), true) + " received, max_consecutive packet index: " + Integer.toString(code_to_max_consecutive_idx.get(file_code)) + "");
                file_progress_list.remove(file_code + 1);



            }
            */

        }
        public void run() {
            run_process_update();
        }

        public void start() {
            t = new Thread(this, threadname);
            t.start();
        }

    }





/*

    // update thread class
    class update_thread implements Runnable {
        private Thread t;
        private String threadname;


        update_thread(String name) {
            threadname = name;

        }


        public void run_update() {


            // run the packet processing thread
            try {
                System.out.println("running packet processing thread");
                process_packet process_pkt = new process_packet("ud_thread");
                process_pkt.start();
            }
            catch(Exception e){
                e.printStackTrace();
            }

            while(! GOT_ALL_DATA)
            {

                // continue if all the packets have been processed
                if (process_queue.isEmpty())
                {

                    try{
                        Thread.sleep(200);
                    }
                    catch(Exception e){
                        System.out.println("Update sleep error");
                        e.printStackTrace();
                    }
                    continue;
                }

                DatagramPacket sent_packet;



                // get next packet
                try {
                    // System.out.println("File code: " + Integer.toString(get_file_code(process_queue.peekFirst().getData())) + " File Index" + Integer.toString(get_packet_index(process_queue.peekFirst().getData())));
                    sent_packet = process_queue.pop();
                    // System.out.println("File code: " + Integer.toString(get_file_code(process_queue.peekFirst().getData())) + " File Index" + Integer.toString(get_packet_index(process_queue.peekFirst().getData())));
                }
                catch(Exception e){
                    System.out.println("Somehow past continue in Update Thread");
                    byte[] buf = new byte[256];
                    sent_packet = new DatagramPacket(buf, buf.length);
                    e.printStackTrace();

                }

                // get data from packet
                byte[] data = sent_packet.getData();


                // get file code
                int file_code = get_file_code(data);

                int file_idx = get_packet_index(data);

                last_idx = file_idx;

                // don't accept name packets
                if(file_idx == 0){
                    continue;
                }



                // ignore if file code and index combination not valid
                if(! file_codes.contains((Integer) file_code) || file_idx > code_to_max_index.get( file_code ) )
                {
                    System.out.println("code and index invalid ");
                    continue;
                }



                // String lookup_key = "" + Integer.toString(file_code) + "**" + Integer.toString(file_idx) + "";

                // ignore file if already recieved
                // if( check_received.containsKey(lookup_key))
                if(check_collision_in_file[file_code].containsKey(file_idx))
                {
                    System.out.println("already r1ecieved " + Integer.toString(file_code) + ", " + Integer.toString(file_idx)+ "");
                    continue;
                }

                in_process_counter++;



                // check_received.put(lookup_key, true);
                check_collision_in_file[file_code].put(file_idx, true);

                add_to_queue.add(data);

                out_process_counter++;



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

    */

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

    public static int get_packet_index(byte[] data){
        // extract index
        byte [] index_bytes = new byte[4];
        System.arraycopy(data, 8, index_bytes, 0, 4);
        int index = byteArrayToInt(index_bytes);
        return index;
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

    public static void write_payload(byte[] data){
        // check if last packet
        int index = get_packet_index(data);
        int file_code = get_file_code(data);
        int max_index = code_to_max_index.get(file_code);





        if(index == max_index){
            // figure out length
            int bytes_to_copy = code_to_lengh_bytes.get(file_code) % (PACKET_SIZE - 16);

            // write values to array
            System.arraycopy(data, 0, files_bytes[file_code], code_to_lengh_bytes.get(file_code) - bytes_to_copy , bytes_to_copy );
            return;

        }

        else {



            byte[] payload = new byte[data.length - 16];
            System.arraycopy(data, 16, files_bytes[ file_code ], (PACKET_SIZE - 16) * (max_index - 1) , data.length - 16);
        }
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }


}
