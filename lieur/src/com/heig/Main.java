/**
 * Project: Labo02
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Cette classe permet de créer un nouveau lieur et de le démarrer.
 *
 * Pour appeler le programme lieur, il faut lui passer en paramêtre son port principal, son port pour la vérification de l'existence
 * d'un serveur et la listes des autres lieurs
 *
 * Par example :
 *
 * java serveur.jar 1111 1112 127.0.0.1 2222 127.0.0.1 3333
 */
public class Main {

    /**
     * Création et démarrage d'un lieur. Les paramêtres fournis doivent être dans cet ordre :
     * <port principal> <port de vérification> [<ip> <port>] (liste des lieurs).
     *
     * @param args
     * @throws InterruptedException
     * @throws IOException
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        // Récupération du port et de l'id
        if (args.length < 2) {
            System.out.println("Il faut fournir au moins le port principal et le port de verification");
            return;
        }
        int port = Integer.parseInt(args[0]);
        int portVerification = Integer.parseInt((args[1]));

        // Ajout des lieurs à la liste
        ArrayList<Lieur> lieurs = new ArrayList<>();
        for (int i = 2; i < args.length - 1; i += 2) {
            lieurs.add(new Lieur(args[i], Integer.parseInt(args[i + 1])));
        }

        // Création et démarrage du lieur
        LieurServeur lieurServeur = new LieurServeur(port, portVerification, lieurs.toArray(new Lieur[0]));
        lieurServeur.demarrer();
    }
}
