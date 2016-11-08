package com.heig;

/**
 * Created by oem on 01.11.16.
 */
public class Service {
    int IDservice;
    String ip;
    int port;

    public Service(int IDservice, String ip, int port) {
        this.IDservice = IDservice;
        this.ip = ip;
        this.port = port;
    }

    public int getIDservice() {
        return IDservice;
    }

    public void setIDservice(int IDservice) {
        this.IDservice = IDservice;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
