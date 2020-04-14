package com.matias.airconditioner.data;

/**
 * Created by Matias on 17/07/2017.
 */
public class Device {
    public String name;
    public String mac;
    public String connection_status;
    public int temp;

    public Device(String name, String mac, String connection_status, int temp){
        this.name = name;
        this.mac = mac;
        this.connection_status = connection_status;
        this.temp = temp;
    }
}