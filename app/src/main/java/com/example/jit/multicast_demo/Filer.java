package com.example.jit.multicast_demo;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.R.attr.offset;
import static com.example.jit.multicast_demo.MainActivity.ffmpeg;
import static com.example.jit.multicast_demo.MainActivity.pr;

/**
 * Created by Zeus on 4/11/2017.
 */

public class Filer {
    File file;

    public Filer(){
    }

    public void func (final File f, final Long inpoffset, final Long len, final OutputStream out, final Socket client) {
        this.file=f;
        try {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    String off="";
                    Long offset = inpoffset;

                    Long hrs=offset/3600;
                    offset=offset-hrs*3600;
                    Long mn=offset/60;
                    offset=offset-mn*60;
                    Long sec=offset;

                    off = ((hrs<10)?"0"+hrs.toString():hrs.toString())+":"+((mn<10)?"0"+mn.toString():mn.toString())+":"+((sec<10)?"0"+sec.toString():sec.toString());

                    // -i input.mp4 -ss 00:00:50.0 -codec copy -t 20 output.mp4
                    Matcher mat = Pattern.compile("(.*)\\.(.*)").matcher(f.getAbsolutePath());
                    String newpath=null;
                    if(mat.find()){
                        newpath= mat.group(1)+"COPY."+mat.group(2);
                    }

                    String cmd = "-i "+f.getAbsolutePath()+" -ss "+off+" -codec copy -t "+len+" "+newpath ;
                    String[] command = cmd.split(" ");
                    pr("in filer");
                    try {
                        File toDelete = new File(newpath);
                        toDelete.delete();
                        System.out.println("COPY file REMOVED!");
                    }catch(Exception e) {
                        System.out.println("COPY file doesn't exist already");
                    }
                    ffmpegSplit(command, out, newpath,client);

                   /* init array with file length
                    bytesArray = new byte[len.intValue()];
                    FileInputStream fis = new FileInputStream(file);
                    fis.read(bytesArray,offset.intValue(),len.intValue()); //read file into bytes[]
                    fis.close();
                    */
                }
            }).start();

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void ffmpegSplit(final String[] command, final OutputStream out, final String newpath, final Socket client) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {

                    System.out.println("Splitting failed");
                    try {
                        client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onSuccess(String s) {
                    try {
                        System.out.println("Sucess split");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                File file = new File(newpath);
                                try {
                                    byte[] bytesArray = new byte[MainActivity.buffSize];
                                    FileInputStream fis = new FileInputStream(file);
                                    int count;

                                    int sum = 0;
                                    while ((count = fis.read(bytesArray)) > 0) {
                                        out.write(bytesArray, 0, count);
                                        sum += count;
                                        pr("sent " + sum + " bytes");
                                    }
                                    //   fis.read(bytesArray); //read file into bytes[]
                                    System.out.println("Sent all");
                                    fis.close();
                                    //out.close();
                                    //client.close();
                                    try {
                                        File f = new File(newpath);
                                        f.delete();
                                    } catch(Exception e) {
                                        System.out.println("Unable to delete COPY file");
                                    }
                                } catch(IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    try {
                                        out.close();
                                        client.close();
                                    }catch(Exception e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            }
                        }).start();

                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onProgress(String s) {
                    //    Toast.makeText(MainActivity.this,"Progress\n"+s,Toast.LENGTH_SHORT).show();
                    //pr("P");
                }

                @Override
                public void onStart() {
                    pr("Start Split");
                    //  Toast.makeText(MainActivity.this,"Start",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFinish() {
                    pr("Finish Split");
                    //   Toast.makeText(MainActivity.this,"Finish",Toast.LENGTH_SHORT).show();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
            e.printStackTrace();
        }
    }



}

