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

    /**
     * Cette fonction permet de lancer un linker en passant en ligne de commande ces paramètre
     * @param args le tableau contenant l'adresse ip et le port du linker
     */
    public static void main(String...args){
        if(args.length != 2){
            System.out.println("You must privide 2 parameters");
            System.exit (1);
        }
        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        new Linker(ip, port);
    }

    /**
     * Convert the linker to string
     *
     * @return
     */
    public String toString() {
        return "Service: ip " + ip + ", port " + port;
    }
}
