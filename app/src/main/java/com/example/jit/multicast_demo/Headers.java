package com.example.jit.multicast_demo;

import android.widget.Toast;

import com.google.gson.Gson;

import java.util.ArrayList;

/**
 * Created by Zeus on 4/11/2017.
 */

public class Headers {

    ArrayList<PairSI> arr;
    Long sze;
    public void func(ArrayList<String> ar,Long sz){
        int cnt=1;
        arr=new ArrayList<>();
        for(String s : ar){
            PairSI p = new PairSI();
            p.s=s;
            p.i=cnt;
            cnt++;
            arr.add(p);
        }
        this.sze=sz;
    }

    public String toJson(){
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }

    public Headers fromJson(String jsoncan){
        Gson gson = new Gson();
        Headers can = gson.fromJson(jsoncan,Headers.class);
        return can;
    }
}
