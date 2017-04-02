package com.example.jit.multicast_demo;


import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class FileManager  {

    ArrayList<File> videoFiles;

    public ArrayList<File> getVideoFiles(Context c) {
        String path = Environment.getExternalStorageDirectory().getPath();
        Toast.makeText(c,path,Toast.LENGTH_LONG).show();
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

}
