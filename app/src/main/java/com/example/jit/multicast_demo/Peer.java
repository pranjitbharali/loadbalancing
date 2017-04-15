package com.example.jit.multicast_demo;

import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.text.format.Formatter;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by dem05a on 9/04/15.
 */

public class Peer {


    String id;
    String load;
    Float battery;
    String ip;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLoad() {
        return load;
    }

    public void setLoad(String load) {
        this.load = load;
    }

    public Float getBattery() {
        return battery;
    }

    public void setBattery(Float battery) {
        this.battery = battery;
    }

    public void printPeer(){
        System.out.println("Peer id : "+this.id);
        System.out.println("Peer ip : "+this.ip);
        System.out.println("Peer battery : "+this.battery);
    }

    public String toJson(){
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }

    public Peer fromJson(String jsoncan){
        Gson gson = new Gson();
        Peer can = gson.fromJson(jsoncan,Peer.class);

        return can;

    }

}