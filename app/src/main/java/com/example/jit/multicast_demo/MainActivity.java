package com.example.jit.multicast_demo;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.text.method.ScrollingMovementMethod;
import android.text.method.Touch;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.io.OutputStream;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Array;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.R.id.message;
import static android.provider.Telephony.Carriers.PORT;
import static java.util.UUID.randomUUID;

public class MainActivity extends AppCompatActivity {

    byte[] outBuf,inbuf;
    HashMap<String,ArrayList<Pair>> hm;
    // md5sum and Duration
    HashMap<String, Long> hm1;

    final static String MULTI_IP = "224.0.0.10";
    final static int PORT=8888;
    //final static int BUFFER_SIZE=1500;
    boolean b = true;
    Time t1;
    MulticastSocket sock;
    final static String TAG="D";
    String mePeer;
    InetAddress address;
    TextView tv1,tv2;
    Peer peer;
    Thread thread;
    static FileManager fm;
    String rec1;
    String headip;
    static FFmpeg ffmpeg;
    static String DurRes;
    static Semaphore semaForFFMPEG;
    public static final int buffSize = 10000000;      //10 MB
    public static final int sockTimeOut = 600000; //10 mins

    TextView subscribe;
    ImageView request;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        pr("ON");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv1= (TextView) findViewById(R.id.tv);
        subscribe = (TextView) findViewById(R.id.Subscribe);
        request = (ImageView) findViewById(R.id.displayb);
        request.setVisibility(View.INVISIBLE);
        tv1.setVisibility(View.INVISIBLE);

        subscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                f2(view);
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }

        // ffmpeg
        ffmpeg = FFmpeg.getInstance(getApplicationContext());
        loadFFMpegBinary();

        semaForFFMPEG = new Semaphore(1);

        fm= new FileManager(getIP());
        fm.getmd5sum(getApplicationContext());

        hm = new HashMap<String,ArrayList<Pair>>();
        hm1= new HashMap<>();
        tv1.setMovementMethod(new ScrollingMovementMethod());
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        tv2=(TextView)findViewById(R.id.are_u_head);
        if (wifi != null) {
            WifiManager.MulticastLock lock = wifi.createMulticastLock("WifiDevices");
            lock.acquire();
          //  Toast.makeText(MainActivity.this,"lock acquired",Toast.LENGTH_SHORT).show();
        }
        else {
        //    Toast.makeText(MainActivity.this, "Please enable Wi-Fi", Toast.LENGTH_SHORT).show();
        }

        try{
            address = InetAddress.getByName(MULTI_IP);
            sock = new MulticastSocket(PORT);
        }
        catch(IOException e){
            e.printStackTrace();
        }

        final ArrayList<String> peers= new ArrayList<>();


        final IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        //Peer creation
        peer = new Peer();
        peer.setId(randomUUID().toString());
        peer.setBattery(getBattery(ifilter));
        peer.setIp(getIP());
        mePeer = peer.toJson();

        //head server thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    hm.clear();
                    ServerSocket server = null;
                    Socket client=null;
                    BufferedReader in=null;
                    PrintWriter out=null;

                    // INPUT STREAM SOCKET
                    try{
                        server = new ServerSocket(4321);
                        server.setSoTimeout(sockTimeOut);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    while(true) {
                        try {
                            client = server.accept();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                        try {
                            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                            out = new PrintWriter(client.getOutputStream(),
                                    true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        try{
                            String line = in.readLine();

                            if(line.charAt(0)=='i'){        //client has sent his file information, update hashmaps

                                FileManager fm1 = fm.fromJson(line.substring(1));

                                for(Pair md5 : fm1.md5s){
                                    if(hm.get(md5.a)==null){
                                        hm.put(md5.a,new ArrayList<Pair>());
                                    }
                                    Pair p = new Pair();
                                    p.a=fm1.ip;
                                    p.b=md5.b;
                                    hm.get(md5.a).add(p);
                                }
                                for (Map.Entry<String, Long> entry : fm1.hdur.entrySet()) {
                                    String key = entry.getKey();
                                    Long value = entry.getValue();
                                    hm1.put(key,value);
                                }

                            }else if(line.charAt(0)=='r'){      //request button pressed by a client
                                ListSend ls=new ListSend();
                                ls.func(hm);
                                String sendthis=ls.toJson();
                                out.write(sendthis+"\n");
                                out.flush();
                            } else if(line.charAt(0)=='d'){           //client has requested for a file, send information about peers having that file

                                final String md5=line.substring(1);

                                ArrayList<String> ips=new ArrayList<>();
                                ArrayList<Pair> value = hm.get(md5);
                                for(final Pair p:value) {
                                    ips.add(p.a);
                                }
                                Headers hdr = new Headers();
                                hdr.func(ips,hm1.get(md5));
                                final String js=hdr.toJson();
                                out.write(js+"\n");
                                out.flush();
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // CLIENT SERVER THREAD

        new Thread(new Runnable() {
            @Override
            public void run() {

                //CLIENT SERVER

                ServerSocket server = null;
                Socket client=null;
                BufferedReader in=null;
                OutputStream out=null;

                // INPUT STREAM SOCKET
                try{
                    server = new ServerSocket(4325);
                    server.setSoTimeout(sockTimeOut);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                while(true) {
                    try {
                        client = server.accept();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        out = client.getOutputStream();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        final String line = in.readLine();

                        if(line.charAt(0)=='m'){
                            String lin=line.substring(1);
                            Chunk chk=new Chunk().fromJson(lin);
                            File f=fm.getFIle(chk.md5);
                            pr("m");
                            Filer flr=new Filer();
                            flr.func(f,chk.offset,chk.ChunkSize,out,client);

                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        thread = new Thread(){
            public void run(){

                while (true) {
                    if(!b) {
                        try {

                            inbuf = new byte[buffSize];
                            DatagramPacket msgPacket = new DatagramPacket(inbuf, inbuf.length);
                            if(Thread.interrupted())    //socket not blocked on receive
                                return;
                            sock.receive(msgPacket);
                            if(Thread.interrupted())   //in case socket was blocked on receive
                                return;
                            final String msg = new String(inbuf, 0, msgPacket.getLength());

                            if(msg.charAt(0)=='e'){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Election", Toast.LENGTH_SHORT).show();
                                    }
                                });

                                peers.clear();
                                peers.add(msg.substring(1));
                                hm.clear();
                                hm1.clear();

                                final Peer print_peer = new Peer().fromJson(msg.substring(1));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        tv1.setText(tv1.getText()+"\n\nCANDIDACY RECEIVED:\nIP Address: " + print_peer.getIp()
                                                +"\nBattery: "+ print_peer.getBattery().toString());
                                    }
                                });

                                if(!(msg.substring(1).equals(mePeer))) {
                                    send("p" + mePeer);
                                }

                                //head selection
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Thread.sleep(1000);
                                            float max=0;
                                            Peer mx=new Peer();
                                            for(String s:peers){
                                                Peer temp;
                                                temp=new Peer();
                                                temp=temp.fromJson(s);
                                                if(Float.parseFloat(temp.getBattery().toString())>max){
                                                    max=Float.parseFloat(temp.getBattery().toString());
                                                    mx=temp;
                                                }
                                            }
                                            final Peer mx_v = mx;
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {

                                                    tv1.setText(tv1.getText()+"\n\nHEAD SELECTED from "+ Integer.toString(peers.size()) +
                                                            " PEERS:\n\n" +"IP Address: " + mx_v.getIp()
                                                    +"\nBattery: "+ mx_v.getBattery().toString());
                                                    String yes_no;
                                                    headip=mx_v.getIp();
                                                    if(mx_v.getIp().equals(peer.getIp())){
                                                        yes_no = "YES";
                                                    }
                                                    else {
                                                        yes_no = "NO";
                                                    }
                                                    tv2.setText("Are you head ? : "+yes_no+"\n"+"IP of head : "+headip);
                                                }
                                            });
                                            {
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        String js = fm.toJson();

                                                        // send("i"+js);
                                                        Socket socket=null;
                                                        BufferedReader in=null;
                                                        PrintWriter out=null;

                                                        //Create socket connection
                                                        try{
                                                            socket = new Socket(headip, 4321);
                                                            socket.setSoTimeout(sockTimeOut );
                                                            out = new PrintWriter(socket.getOutputStream(),
                                                                    true);
                                                            in = new BufferedReader(new InputStreamReader(
                                                                    socket.getInputStream()));
                                                        } catch (UnknownHostException e) {
                                                            e.printStackTrace();
                                                            System.exit(1);
                                                        } catch  (IOException e) {
                                                            e.printStackTrace();
                                                            System.exit(1);
                                                        }
                                                        try{
                                                            out.write("i"+js+"\n");
                                                            out.flush();
                                                        } catch (Exception e){
                                                            e.printStackTrace();
                                                        }

                                                    }
                                                }).start();
                                            }

                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).start();
                            }
                            else if(msg.charAt(0)=='p'){
                                peers.add(msg.substring(1));

                                final Peer print_peer = new Peer().fromJson(msg.substring(1));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        tv1.setText(tv1.getText()+"\n\nCANDIDACY RECEIVED:\nIP Address: " + print_peer.getIp()
                                                +"\nBattery: "+ print_peer.getBattery().toString());
                                    }
                                });

                            }

                            else if(msg.charAt(0)=='q'){
                                rec1=msg.substring(1);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    //else break;
                }

            }
        };
        thread.start();




    }

    public static void getDuration(File f, final String md5){
        String path=f.getAbsolutePath();

        String cmd = "-i "+path;
        final String[] command = cmd.split(" ");
        new Thread(new Runnable() {
            @Override
            public void run() {
//                try {
//                    if (ffmpeg.isFFmpegCommandRunning())
//                        Thread.sleep(500);
//                }catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                //ffmpeg.setTimeout(200);
                ffmpegDuration(command,md5);
            }
        }).start();
    }

    private void loadFFMpegBinary() {
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {

                }
            });
        } catch (FFmpegNotSupportedException e) {
            e.printStackTrace();
        }
    }
    public static void pr(String s){
        System.out.println(s);
    }
    public static Long FollowDur(String s){
        String pattern = "Duration: ([0-9][0-9]):([0-9][0-9]):([0-9][0-9])";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(s);

        Long sec=0L;

        if(m.find()){
            sec=Long.parseLong(m.group(1))*60*60+Long.parseLong(m.group(2))*60+ Long.parseLong(m.group(3));
        }

        return sec;
    }
    public static void ffmpegDuration(final String[] command, final String md5) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Long dur = FollowDur(s);
                    fm.hdur.put(md5,dur);
                }

                @Override
                public void onSuccess(String s) {
                    Long dur = FollowDur(s);
                    fm.hdur.put(md5,dur);
                }

                @Override
                public void onProgress(String s) {
                    //    Toast.makeText(MainActivity.this,"Progress\n"+s,Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onStart() {
                    //  Toast.makeText(MainActivity.this,"Start",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFinish() {
                    //   Toast.makeText(MainActivity.this,"Finish",Toast.LENGTH_SHORT).show();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
            e.printStackTrace();
        }
    }


    @Override
    public void onBackPressed() {
        b=true;
        thread.interrupt();
        /*try {
            outBuf = "exit".getBytes();
            final DatagramPacket outPacket = new DatagramPacket(outBuf, outBuf.length, address, PORT);
            new Thread (new Runnable() {
                @Override
                public void run() {
                    try {
                        DatagramSocket socketClient = new DatagramSocket();
                        socketClient.send(outPacket);
                        socketClient.close();
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        this.finishAffinity();
        System.exit(0);
    }

    public void f2(View v){
        subscribe.setVisibility(View.INVISIBLE);
        request.setVisibility(View.VISIBLE);
        tv1.setVisibility(View.VISIBLE);
        try{
            if(b) {
                sock.joinGroup(address);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        send("e"+mePeer);   //request to start election

                    }
                }).start();

                b = false;
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void display1(View v){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket=null;
                BufferedReader in=null;
                PrintWriter out=null;

                //Create socket connection
                try{
                    socket = new Socket(headip, 4321);
                    socket.setSoTimeout(sockTimeOut);
                    out = new PrintWriter(socket.getOutputStream(),
                            true);
                    in = new BufferedReader(new InputStreamReader(
                            socket.getInputStream()));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    System.exit(1);
                } catch  (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                try{
                    out.write("r"+"\n");
                    out.flush();
                    String in1=in.readLine();
                    // changing intent
                    Intent myIntent = new Intent(MainActivity.this, Display.class);
                    myIntent.putExtra("arr",in1);
                    myIntent.putExtra("ip",headip);
                    myIntent.putExtra("hm", fm.hm);
                    startActivity(myIntent);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public  void f1(View v) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                    send("r");
            }
        }).start();

    }

    public void send(String can){
        try {
            DatagramSocket socketClient;
            socketClient = new DatagramSocket();
            outBuf = can.getBytes();

            DatagramPacket outPacket = new DatagramPacket(outBuf, outBuf.length, address, PORT);
            socketClient.send(outPacket);
            socketClient.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }


    public String getIP() {


        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    public Float getBattery(IntentFilter ifilter) {

        Intent batteryStatus = this.registerReceiver(null, ifilter);

        assert batteryStatus != null;
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        Float batteryPct = (level / (float) scale) * 100;


        return (batteryPct);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
}
