package com.example.jit.multicast_demo;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.provider.Telephony.Carriers.PORT;
import static java.util.UUID.randomUUID;

public class MainActivity extends AppCompatActivity {



    byte[] outBuf,inbuf;
    final static String MULTI_IP = "224.0.0.10";
    final static int PORT=8888;
    final static int BUFFER_SIZE=1500;
    boolean b = true;
    Time t1;
    MulticastSocket sock;
    final static String TAG="D";
    String mePeer;
    InetAddress address;
    TextView tv1;
    Peer peer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv1= (TextView) findViewById(R.id.tv);
        tv1.setMovementMethod(new ScrollingMovementMethod());
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            WifiManager.MulticastLock lock = wifi.createMulticastLock("WifiDevices");
            lock.acquire();
          //  Toast.makeText(MainActivity.this,"lock acquired",Toast.LENGTH_SHORT).show();
        }
        else {
        //    Toast.makeText(MainActivity.this, "wifi is null", Toast.LENGTH_SHORT).show();
        }

        try{
            address = InetAddress.getByName(MULTI_IP);
            sock = new MulticastSocket(PORT);
            Toast.makeText(MainActivity.this,"r",Toast.LENGTH_SHORT).show();
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

        Thread thread = new Thread(){
            public void run(){

                    while (true) {
                        if(!b) {
                            try {


                                inbuf = new byte[BUFFER_SIZE];
                                DatagramPacket msgPacket = new DatagramPacket(inbuf, inbuf.length);
                                sock.receive(msgPacket);
                                final String msg = new String(inbuf, 0, inbuf.length);

                                if(msg.charAt(0)=='e'){
                                    peers.clear();
                                    peers.add(msg.substring(1));

                                    if(!(msg.substring(1).trim().equals(mePeer))) {
                                        send("p" + mePeer);
                                    }
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                Thread.sleep(1000);
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            float max=0;
                                                            Peer mx=new Peer();
                                                            for(String s:peers){
                                                                Peer temp;
                                                                temp=new Peer();
                                                                temp=temp.fromJson(s.trim());
                                                                if(Float.parseFloat(temp.getBattery().toString())>max){
                                                                    max=Float.parseFloat(temp.getBattery().toString());
                                                                    mx=temp;
                                                                }
                                                            }
                                                            tv1.setText("HEAD SELECTED from "+ Integer.toString(peers.size()) +
                                                                    " PEERS :\n" +"\nIP Address: " + mx.getIp().toString()
                                                            +"\nBattery: "+ mx.getBattery().toString());
                                                        }
                                                    });


                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }

                                        }
                                    }).start();
                                }
                                else if(msg.charAt(0)=='p'){
                                    peers.add(msg.substring(1));
                                }
                            } catch (IOException e) {

                            }
                        }
                    }

            }
        };
        thread.start();



    }


    public void f2(View v){
        try{
            if(b) {
                sock.joinGroup(address);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        send("e"+mePeer);

                    }
                }).start();

                b = false;
            }
        } catch(IOException e){

        }
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
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }


    public String getIP() {


        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
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

}
