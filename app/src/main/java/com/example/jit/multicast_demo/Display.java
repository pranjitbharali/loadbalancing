package com.example.jit.multicast_demo;

import android.content.Intent;
import android.os.Environment;
import android.support.annotation.BoolRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import static com.example.jit.multicast_demo.MainActivity.pr;

public class Display extends AppCompatActivity {

    ListView lv1;

    //public static HashMap<Integer,Boolean> check;
    //public static Semaphore sema;
    public static Semaphore[] semarr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        lv1=(ListView) findViewById(R.id.lv);
        //check = new HashMap<>();
        String arr = getIntent().getStringExtra("arr");
        final String headip = getIntent().getStringExtra("ip");
        HashMap<String,File> myFiles = (HashMap<String, File>)getIntent().getSerializableExtra("hm");

        ListSend ls =new ListSend().fromJson(arr);

        ArrayList<Pair> temp1 = ls.arr;
        ArrayList<Pair> temp2 = new ArrayList<>();
        for(Pair p : temp1) {
            if(myFiles.containsKey(p.a))
                continue;
            temp2.add(p);
        }

        final ArrayList<Pair> popu= temp2;
        ArrayList<String> popu1 = new ArrayList<>();

        // shouldn't show his own files

        for(Pair p:popu){
            popu1.add(p.b);
        }

        lv1.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, popu1));

        lv1.setTextFilterEnabled(true);

// Bind onclick event handler
        lv1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    final int position, long id) {

                //check = new HashMap<>();
                //sema = new Semaphore(1);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(Display.this,"P1", Toast.LENGTH_SHORT).show();
                        Socket socket=null,socket1=null;
                        BufferedReader in=null;
                        PrintWriter out=null;

                        //Create socket connection
                        try{
                            socket = new Socket(headip, 4321);
                            socket.setSoTimeout(MainActivity.sockTimeOut);
                            out = new PrintWriter(socket.getOutputStream(),
                                    true);
                            in = new BufferedReader(new InputStreamReader(
                                    socket.getInputStream()));
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch  (IOException e) {
                            e.printStackTrace();
                        }
                        try{
                            out.write("d"+popu.get(position).a+"\n");
                            out.flush();
                            final String inline=in.readLine();

                            Headers hdr=new Headers();
                            hdr=hdr.fromJson(inline);
                            final Long sze=hdr.sze;
                            // new socket for seeders
                            final int noofseeders = hdr.arr.size();
                            Long offset=0L;
                            final Long ChunkSize=sze/noofseeders;
                            //offset=offset-ChunkSize;


                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(Display.this,"Size hai "+sze+noofseeders, Toast.LENGTH_SHORT).show();
                                }
                            });

                            semarr = new Semaphore[hdr.arr.size()];
                            for(int i=0; i<semarr.length; i++) {
                                semarr[i] = new Semaphore(1);
                            }

                            int cnt1=0;
                            for(final PairSI psi : hdr.arr){            //loops over each peer that has the file
                                final Long of=offset;
                                Long ck=ChunkSize;
                                if(of+ChunkSize>=sze){
                                    ck=sze-offset;
                                }

                                final int cnt=cnt1;
                                cnt1=cnt1+1;

                                try {
                                    semarr[cnt].acquire();
                                }  catch(Exception e) {
                                    e.printStackTrace();
                                }

                                offset=offset+ChunkSize;
                                final Long fck=ck;
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        System.out.println("Thread "+cnt+" started "+psi.s);

                                        Socket sock=null;
                                        InputStream in1=null;
                                        PrintWriter out1=null;

                                        try{
                                            sock = new Socket(psi.s, 4325);
                                            sock.setSoTimeout(MainActivity.sockTimeOut);
                                            out1 = new PrintWriter(sock.getOutputStream(),
                                                    true);
                                            in1 = sock.getInputStream();

                                        } catch (UnknownHostException e) {
                                            e.printStackTrace();
                                            System.exit(1);
                                        } catch  (IOException e) {
                                            e.printStackTrace();
                                            System.exit(1);
                                        }
                                        try{

                                            String str = new Chunk(popu.get(position).a,fck,of).toJson();
                                            out1.write("m"+str+"\n");
                                            out1.flush();

                                            System.out.println("Sent request for file to "+cnt);

                                            String path = Environment.getExternalStorageDirectory().getPath()+"/video";
                                            FileOutputStream fos = new FileOutputStream(path+"/"+"playTempVid"+cnt+".mp4");

                                            byte[] bytes = new byte[MainActivity.buffSize];
                                            int count;
                                            int sum =0;
                                            while ((count = in1.read(bytes)) > 0) {
                                                fos.write(bytes, 0, count);
                                                sum += count;
                                                //System.out.println("Received "+sum+" bytes from "+cnt);
                                            }
                                            in1.close();
                                            out1.close();
                                            fos.close();
                                            sock.close();
                                            System.out.println("Received file from "+cnt);
//                                            sema.acquire();
//                                            check.put(cnt,true);
//                                            sema.release();

                                            semarr[cnt].release();

                                        } catch(Exception e){
                                            e.printStackTrace();
                                        }
                                    }
                                }).start();
                            }
                            pr("TRYING TO CREATE ACTIVITY INTENT");
                            Intent intent = new Intent(Display.this,Play.class);
                            Bundle bun = new Bundle();
                            bun.putInt("noofseeders",noofseeders);
                            intent.putExtras(bun);
                            startActivity(intent);

                        } catch (Exception e){
                            e.printStackTrace();
                        }
/*
                        try{
                            Toast.makeText(Display.this,"P2", Toast.LENGTH_SHORT).show();
                            out.write("d"+popu.get(position).a+"\n");
                            out.flush();
                            Toast.makeText(Display.this,"Pp", Toast.LENGTH_SHORT).show();
                            String in1=in.readLine();
                            Toast.makeText(Display.this,in1, Toast.LENGTH_SHORT).show();
                            Headers hdr=new Headers();
                            hdr.fromJson(in1);

                        } catch (Exception e){
                            Toast.makeText(Display.this,"P2", Toast.LENGTH_SHORT).show();
                        }
                    */
                    }
                }).start();

            }
        });
    }
}
