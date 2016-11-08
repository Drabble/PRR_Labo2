/**
 * Project: Labo02
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

/**
 * Defines a linker with an ip and a port
 */
public class Linker {
    String ip;
    int port;

    /**
     * Constructs a new linker with the specified ip and port
     *
     * @param ip
     * @param port
     */
    public Linker(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    /**
     * Get the linker's ip
     *
     * @return the linker's ip
     */
    public String getIp() {
        return ip;
    }

    /**
     * Set the linker's ip
     *
     * @param ip
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * Get the linker's port
     *
     * @return the linker's port
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the linker's port
     *
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }
}
