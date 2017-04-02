package com.example.jit.multicast_demo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.FileHandler;

public class MyFiles extends AppCompatActivity {

    ListView file_List;
    FileManager FM;
    ArrayList<File> videoFiles;
    ArrayList<String> FileNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_files);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }

        FM = new FileManager();
        videoFiles = FM.getVideoFiles(this);
        FileNames = new ArrayList<>();
        Toast.makeText(MyFiles.this,"size = "+videoFiles.size(),Toast.LENGTH_LONG).show();
        for(int i=0;i<videoFiles.size();i++){
            File file = videoFiles.get(i);
            FileNames.add(file.getName());
        }

        file_List = (ListView)findViewById(R.id.file_List);
        ListAdapter la = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,android.R.id.text1,FileNames);
        file_List.setAdapter(la);
    }


}
