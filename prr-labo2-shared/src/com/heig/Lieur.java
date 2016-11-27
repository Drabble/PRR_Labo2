/**
 * Project: Labo02
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

/**
 * Défini un lieur avec une IP et un port
 */
public class Lieur {
    String ip;
    int port;

    /**
     * Construit un nouveau lieur avec l'ip et le port spécifié
     *
     * @param ip
     * @param port
     */
    public Lieur(String ip, int port) {
        this.ip = ip;
        this.port = port;
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

    /**
     * Converti le lieur en String
     *
     * @return
     */
    public String toString() {
        return "Service: ip " + ip + ", port " + port;
    }
}
