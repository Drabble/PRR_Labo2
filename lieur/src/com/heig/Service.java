package com.heig;

import java.util.Date;

/**
 * Created by oem on 01.11.16.
 */
public class Service {
    int idService;
    String ip;
    int port;
    Date lastUse;

    public Service(int idService, String ip, int port) {
        this.idService = idService;
        this.ip = ip;
        this.port = port;
        this.lastUse = null;
    }

    public int getIdService() {
        return idService;
    }

    public void setIdService(int idService) {
        this.idService = idService;
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

    public Date getLastUse() {
        return lastUse;
    }

    public void setLastUse(Date lastUse) {
        this.lastUse = lastUse;
    }
}
