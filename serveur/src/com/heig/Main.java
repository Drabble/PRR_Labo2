/**
 * Project: Labo02
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Cette classe permet de créer un nouveau serveur de service echo et de le démarrer.
 * L'ip, le port, l'id du service et la liste des lieurs doit être passé en paramêtre à l'appel du programme.
 *
 * Pour appeler le programme serveur, il faut lui passer en paramêtre son port, l'id du service qu'il va servire
 * et la liste des lieurs
 *
 * Par example :
 *
 * java serveur.jar 1234 1 127.0.0.1 1111 127.0.0.1 2222
 */
public class Main {

    /**
     * Création d'un nouveau service qui va écouter sur le port spécifié et se souscrire à un lieur
     *
     * @param args
     * @throws InterruptedException
     * @throws IOException
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        // Récupération du port et de l'id
        if (args.length < 2) {
            System.out.println("Il faut fournir au moins le port et l'id du service");
            return;
        }
        int port = Integer.parseInt(args[0]);
        int id = Integer.parseInt((args[1]));

        // Ajout des lieurs à la liste
        ArrayList<Lieur> lieurs = new ArrayList<>();
        for (int i = 2; i < args.length - 1; i += 2) {
            lieurs.add(new Lieur(args[i], Integer.parseInt(args[i + 1])));
        }

        // Création et démarrage du serveur
        ServiceServeur serviceServeur = new ServiceServeur(port, id, lieurs.toArray(new Lieur[0]));
        serviceServeur.demarrer();
    }
}
