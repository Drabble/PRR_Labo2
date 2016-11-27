/**
 * Project: Labo02
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

import java.util.Date;

/**
 * Défini un service avec une ip, un port et un id de service
 */
public class Service {
    int idService;
    String ip;
    int port;
    Date derniereUtilisation;

    /**
     * Crée un nouveau service avec un id, une ip et un port
     *
     * @param idService
     * @param ip
     * @param port
     */
    public Service(int idService, String ip, int port) {
        this.idService = idService;
        this.ip = ip;
        this.port = port;
        this.derniereUtilisation = null;
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

    public Date getDerniereUtilisation() {
        return derniereUtilisation;
    }

    /**
     * Met à jour le champs dernière utilisation avec la date actuelle
     */
    public void utiliser() {
        this.derniereUtilisation = new Date();
    }

    /**
     * Transforme le service en String pour l'affichage
     *
     * @return
     */
    public String toString() {
        return "Service: id " + idService + ", ip " + ip + ", port " + port;
    }
}
