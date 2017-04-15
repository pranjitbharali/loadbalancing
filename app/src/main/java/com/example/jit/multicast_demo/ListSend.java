package com.example.jit.multicast_demo;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ListSend {
    ArrayList<Pair> arr;

    public void func(HashMap<String,ArrayList<Pair> > hm){
        arr=new ArrayList<>();
        for (HashMap.Entry<String, ArrayList<Pair>> item : hm.entrySet()) {
            String key = item.getKey();
            ArrayList<Pair> value = item.getValue();
            Pair p=new Pair();
            p.a=key;
            p.b=value.get(0).b;
            arr.add(p);
        }
    }
    public String toJson(){
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }

    public ListSend fromJson(String jsoncan){
        Gson gson = new Gson();
        ListSend can = gson.fromJson(jsoncan,ListSend.class);
        return can;
    }

}
