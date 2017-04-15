package com.example.jit.multicast_demo;

import com.google.gson.Gson;

/**
 * Created by Zeus on 4/12/2017.
 */

public class Chunk {
    Long offset,ChunkSize;
    String md5;

    public Chunk(){

    }

    public Chunk(String m,Long a,Long b){
        this.md5=m;
        this.ChunkSize=a;
        this.offset=b;
    }

    public String toJson(){
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }

    public Chunk fromJson(String jsoncan){
        Gson gson = new Gson();
        Chunk can = gson.fromJson(jsoncan,Chunk.class);
        return can;
    }
}
