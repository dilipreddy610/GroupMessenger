package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.PriorityQueue;

import android.net.Uri;
import android.view.Menu;
import android.view.View;
/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();

    int num;
    int max_seq = 0;
    int seq_propose=0;
    int max_sequence=0;
    String string_msg;
    int num_alive = 5;
    static int seqnumb=-1;
    int proposal_pro=-1;
    int flag=0;

    int counter=0;
    static String KEY="key";
    static String VALUE="value";
    static String[] portArray={"11108","11112","11116","11120","11124"};
    int processnum=-1;
    ///REF:https://docs.oracle.com/javase/7/docs/api/java/util/PriorityQueue.html
    PriorityQueue<Message> mess_queue=new PriorityQueue<Message>();
    PriorityQueue<Message> finl_queue=new PriorityQueue<Message>();

    ArrayList p_array = new ArrayList();
    ArrayList proposal_process=new ArrayList();

    static final int SERVER_PORT = 10000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        processnum=(Integer.parseInt(myPort)-11108)/4;
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Socket IO exception");
            return;
        }


        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        final EditText editText =(EditText) findViewById(R.id.editText1);
        Button button=(Button) findViewById(R.id.button4);

        button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                String text=editText.getText().toString()+ "\n";
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,text,myPort);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
    //ref://https://docs.oracle.com/javase/tutorial/essential/concurrency/locksync.html
    Uri uri=makeURI("content","edu.buffalo.cse.cse486586.groupmessenger2.provider");
    private Uri makeURI(String content, String path){

        Uri.Builder uriBuilder=new Uri.Builder();
        uriBuilder.scheme(content);
        uriBuilder.authority(path);
        return uriBuilder.build();

    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void>
    {
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            try {
                String strin;
                while(true) {
                    Socket socket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter serv_out = new PrintWriter(socket.getOutputStream(), true);
                    if ((strin = in.readLine()) != null ) {
                        String[] rcvmsg=strin.split(":");
                        if(rcvmsg[0].equals("msg")){
                            Message m=new Message();
                            m.msg=rcvmsg[3];
                            m.msgid=Integer.parseInt(rcvmsg[1]);
                            m.msgsender=Integer.parseInt(rcvmsg[2]);
                            m.proposed_process=processnum;
                            m.delstatus="N";
                            if(max_sequence>=seq_propose)
                            {
                                    seq_propose=max_sequence+1;
                                }
                            else
                            {
                                    seq_propose=seq_propose+1;
                            }
                            m.proposal=seq_propose;
                            synchronized (mess_queue) {
                                mess_queue.add(m);
                            }
                            String send=seq_propose+":"+processnum;
                            serv_out.println(send);
                            serv_out.flush();
                            serv_out.close();
                        }
                        else
                        {
                            string_msg = strin;
                            String[] msg_ack=string_msg.split(":");
                            if(seq_propose<Integer.parseInt(msg_ack[2]))
                            {
                                max_sequence=Integer.parseInt(msg_ack[2]);

                            }
                            synchronized (mess_queue) //REF:https://docs.oracle.com/javase/tutorial/essential/concurrency/locksync.html
                            {
                                for (Message m : mess_queue)
                                {
                                    if (m.msgid == Integer.parseInt(msg_ack[0]) && (m.msgsender == Integer.parseInt(msg_ack[1]))) {
                                        m.proposal = Integer.parseInt(msg_ack[2]);
                                        m.proposed_process = Integer.parseInt(msg_ack[3]);
                                        m.delstatus = "Y";
                                        finl_queue.add(m);

                                    }
                                }

                            }
                            //REF:https://docs.oracle.com/javase/tutorial/essential/concurrency/locksync.html
                            synchronized (finl_queue)
                            {
                                if(finl_queue.size()>24) {
                                        flag = 1;
                                        while(finl_queue.peek()!=null){
                                            Message first = finl_queue.poll();
                                            publishProgress(first.msg);
                                        }

                                    }
                            }

                        }


                   }
                }

                }

            catch(IOException e){
                Log.e(TAG,"Socket IO exception");
            }
            return null;
                }
        @Override
        protected void onProgressUpdate(String...strings){
            //REF:https://developer.android.com/guide/topics/providers/content-providers.html
            String strReceived=strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            seqnumb++;
            remoteTextView.append(seqnumb+"- "+strReceived + "\t\n");
            ContentValues contentValues= new ContentValues();
            contentValues.put(VALUE,strReceived);
            contentValues.put(KEY,Integer.toString(seqnumb));
            getContentResolver().insert(uri,contentValues);

        }
        }

    private class ClientTask extends AsyncTask<String, Void, Void>
    {

        @Override
        protected Void doInBackground(String... msgs) {
            counter++;
            try {

               for (int i = 0; i < portArray.length; i++) {

                           Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                   Integer.parseInt(portArray[i]));
                           String msgToSend = msgs[0];
                           msgToSend = "msg" + ":" + counter + ":" + processnum + ":"+ msgToSend;

                           PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                           BufferedReader client_in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                           try {

                               out.println(msgToSend);
                               out.flush();
                               try {
                                  String  str = client_in.readLine();
                                   String[] proposals = str.split(":");
                                   num = Integer.parseInt(proposals[0]);

                                       p_array.add(num);
                                       proposal_process.add(Integer.parseInt(proposals[1]));
                               } catch (Exception e) {
                                   e.printStackTrace();
                               }

                           } catch (Exception e) {
                               e.printStackTrace();
                           }
                       }

                       if (p_array.size() == num_alive) {

                               max_seq = (Integer) Collections.max(p_array);
                               proposal_pro = (Integer) proposal_process.get(p_array.indexOf(max_seq));


                           for (int i1 = 0; i1 < 5; i1++) {
                               Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                       Integer.parseInt(portArray[i1]));
                               PrintWriter out1 = new PrintWriter(sock.getOutputStream(), true);
                               out1.println(counter + ":" + processnum + ":" + max_seq + ":" + proposal_pro+":"+msgs[0]);
                               out1.flush();
                           }
                           p_array.clear();
                       }

                   } catch(UnknownHostException e){
                       Log.e(TAG, "ClientTask UnknownHostException");
                   } catch(IOException e){
                       Log.e(TAG, "ClientTask insert IOException");
                       e.printStackTrace();
                   } catch(Exception e){
                       Log.e(TAG, "Unknown exception");
                       e.printStackTrace();
                   }

                    return null;
                }

        }
    public class Message implements Comparable<Message>{
        public String msg;
        public int proposed_process;
        public int msgid;
        public int msgsender;
        public int proposal;
        public String delstatus;

        //REF:https://docs.oracle.com/javase/7/docs/api/java/lang/Comparable.html
        @Override
        public int compareTo(Message another) {
            if(this.proposal!=another.proposal)
            {
                if(this.proposal>another.proposal){
                    return 1;
                }
                else{
                    return -1;
                }
            }
            else{
                if(this.proposed_process>another.proposed_process){
                    return 1;
                }
                else{
                    return -1;
                }
            }
    }
    }

}