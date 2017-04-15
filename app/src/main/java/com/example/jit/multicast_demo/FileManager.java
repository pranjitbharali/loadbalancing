package com.example.jit.multicast_demo;


import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;

public class FileManager  {

    ArrayList<File> videoFiles;
    HashMap<String, File> hm;
    HashMap<String,Long> hdur;
    ArrayList<Pair> md5s;
    String ip;

    public FileManager(String s){this.ip=s;}

    public ArrayList<File> getVideoFiles(Context c) {
        String path = Environment.getExternalStorageDirectory().getPath()+"/video";
//        Toast.makeText(c,path,Toast.LENGTH_LONG).show();
        System.out.println(path);
        File file = new File(path);
        videoFiles = new ArrayList<>();
        if (file.isDirectory()) {
            scanDirectory(file);
        } else {
            if(file.getName().endsWith(".mp4")){
                videoFiles.add(file);
            }
        }
        return videoFiles;
    }

    private void scanDirectory(File directory) {
        if (directory != null) {
            File[] listFiles = directory.listFiles();
            if (listFiles != null && listFiles.length > 0) {
                for (File file : listFiles) {
                    if (file.isDirectory()) {
                        scanDirectory(file);
                    } else {
                        if(file.getName().endsWith(".mp4")){
                            videoFiles.add(file);
                        }
                    }

                }
            }
        }
    }

    public void getmd5sum(Context c){
        getVideoFiles(c);
        md5s=new ArrayList<>();
        hm= new HashMap<>();
        hdur= new HashMap<>();
        for(File f :videoFiles) {

            try {
                MessageDigest md5Digest = MessageDigest.getInstance("MD5");
                String checksum = getFileChecksum(md5Digest, f);
                Pair p=new Pair();
                p.a=checksum;
                p.b=f.getName();
                md5s.add(p);
                hm.put(checksum,f);

                MainActivity.getDuration(f,checksum);

            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }



    public String toJson(){
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }

    public FileManager fromJson(String jsoncan){
        Gson gson = new Gson();
        FileManager can = gson.fromJson(jsoncan,FileManager.class);

        return can;

    }

    public File getFIle(String md5){
        File f=hm.get(md5);
        return f;
    }

    private static String getFileChecksum(MessageDigest digest, File file) throws IOException
    {
        //Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(file);

        //Create byte array to read data in chunks
        byte[] byteArray = new byte[MainActivity.buffSize];
        int bytesCount = 0;

        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };

        //close the stream; We don't need it now.
        fis.close();

        //Get the hash's bytes
        byte[] bytes = digest.digest();

        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for(int i=0; i< bytes.length ;i++)
        {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        //return complete hash
        return sb.toString();
    }

}
