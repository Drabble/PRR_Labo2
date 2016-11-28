/**
 * Project: Labo02
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Cette classe va créer un client qui va se envoyer et recevoir des paquets sur le port spécifié et communiquer
 * avec un dans lieur dans la liste des lieurs passé en paramêtre et utiliser le service avec l'id spécifié.
 *
 * Pour appeler le programme client, il faut lui passer en paramêtre son port, l'id du service qu'il va utiliser
 * et la liste des lieurs
 *
 * Par example :
 *
 * java client.jar 1234 1 127.0.0.1 1111 127.0.0.1 2222
 */
public class Main {

    /**
     * Crée un nouveau client avec un port, un id de service et une liste de lieurs
     *
     * @param args
     * @throws InterruptedException
     * @throws IOException
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        // Récupération du port et de l'id
        if (args.length < 4) {
            System.out.println("Il faut fournir au moins le port et l'id du service que le client va utiliser et l'ip et le prot d'un lieur");
            return;
        }
        int port = Integer.parseInt(args[0]);
        int id = Integer.parseInt((args[1]));

        // Ajout des lieurs à la liste
        ArrayList<Lieur> lieurs = new ArrayList<>();
        for (int i = 2; i < args.length - 1; i += 2) {
            lieurs.add(new Lieur(args[i], Integer.parseInt(args[i + 1])));
        }

        // Création et démarrage du client
        Client client = new Client(port, id, lieurs.toArray(new Lieur[0]));
        client.demarrer();
    }
}