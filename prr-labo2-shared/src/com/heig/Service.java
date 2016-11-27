/**
 * Project: Labo02
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

import java.util.Date;

/**
 * Defines a service which has a service id, an ip, a port and the date of its last use
 */
public class Service {
    int idService;
    String ip;
    int port;
    Date lastUse;

    /**
     * Create a new service with the specified service id, ip and port.
     *
     * @param idService
     * @param ip
     * @param port
     */
    public Service(int idService, String ip, int port) {
        this.idService = idService;
        this.ip = ip;
        this.port = port;
        this.lastUse = null;
    }

    /**
     * Get the service id
     *
     * @return the service id
     */
    public int getIdService() {
        return idService;
    }

    /**
     * Set the service id
     *
     * @param idService
     */
    public void setIdService(int idService) {
        this.idService = idService;
    }

    /**
     * Get the ip
     *
     * @return the ip
     */
    public String getIp() {
        return ip;
    }

    /**
     * Set the ip
     *
     * @param ip
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * Get the port
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the port
     *
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Get the last use date
     *
     * @return the last use date
     */
    public Date getLastUse() {
        return lastUse;
    }
    /**
     * Cette fonction permet de lancer un service en passant en ligne de commande ces paramètre
     * @param args le tableau contenant l'id, l'adresse ip et le port du service
     */
    public static void main(String...args){
        if(args.length != 3){
            System.out.println("You must privide 3 parameters");
            System.exit (1);
        }
        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        int id = Integer.parseInt(args[2]);
        new Service(id, ip, port);
    }

    /**
     * Set the lastUse date to the current time
     */
    public void use() {
        this.lastUse = new Date();
    }

    /**
     * Convert the service to string
     *
     * @return
     */
    public String toString() {
        return "Service: id " + idService + ", ip " + ip + ", port " + port;
    }
}
