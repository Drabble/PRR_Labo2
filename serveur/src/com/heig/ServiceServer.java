/**
 * Project: Labo002
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

import java.io.IOException;
import java.net.*;
import java.util.Random;

/**
 * Au lancement le service contacte un lieur, si il a une confirmation de la part de ce dernier, le serveur démarre.
 * Il répond au demandes de client et au verification d'existance des lieurs
 *
 */
public class ServiceServer {

    // List of the other linkers TODO : remove the values and set it with the args
    private final Linker[] linkers = {
            new Linker("127.0.0.1", 2224),
            new Linker("127.0.0.1", 2222)
    };

    private final byte idService = 1;

    private final int pointToPointPort = 12347; // TODO : Make this an argument

    /**
     * Creates a new linker which will listen on the specified port and will synchronise with the specified linkers.
     * TODO : Ajouter les arguments (liste de linkers et port sur lequel on écoute
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public ServiceServer(){

    }

    /**
     * Starts the linker. It will begin by synchronising with another linker than it will answer to requests
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void start() throws IOException, InterruptedException {
        // Utilisé pour générer des valeurs aléatoires
        Random rand = new Random();

        // Création du socket point à point pour l'envoi de packet udp
        DatagramSocket pointToPointSocket = new DatagramSocket(pointToPointPort);
        System.out.println("Serveur démarré!");

        // TODO : EST-CE qu'on essaie de se lier à tout les lieurs plutôt qu'un aléatoire ?
        // Souscription à un lieur aléatoire dans la liste des lieurs
        int linkerNumber = rand.nextInt(linkers.length);
        byte[] souscriptionBuffer = {(byte) Protocol.ABONNEMENT.ordinal(), idService};
        System.out.println("Tentative de souscription au lieur:");
        System.out.println(linkers[linkerNumber]);
        DatagramPacket linkerSubscribePacket = new DatagramPacket(souscriptionBuffer, souscriptionBuffer.length, InetAddress.getByName(linkers[linkerNumber].getIp()), linkers[linkerNumber].getPort());
        pointToPointSocket.send(linkerSubscribePacket);

        // Attente de la confirmation du lieur
        byte[] buffer = new byte[1];
        DatagramPacket linkerConfirmationPacket = new DatagramPacket(buffer, buffer.length);

        do {
            try {
                pointToPointSocket.setSoTimeout(4000);
                pointToPointSocket.receive(linkerConfirmationPacket);
            } catch (SocketTimeoutException e) {
                System.out.print("Le lieur n'a pas pu etre atteint, arret du serveur");
                return;
            }
        } while (linkerConfirmationPacket.getData()[0] != Protocol.CONFIRMATION_ABONNEMENT.ordinal());

        System.out.println("Confirmation de souscription reçue");

        // On remet le timeout du socket à 0 (infini)
        pointToPointSocket.setSoTimeout(0);

        // Performer le service à l'infini maintenant qu'on est souscris aux lieurs
        while (true) {
            System.out.println("Attente d'une nouvelle demande d'un client");

            // Wait for client request, taille maximal d'un demande: 1000 bytes
            byte[] requeteBuffer = new byte[1000];
            DatagramPacket clientPacket = new DatagramPacket(requeteBuffer, requeteBuffer.length);
            pointToPointSocket.receive(clientPacket);

            // Si c'est une requête au service d'echo
            if(clientPacket.getData()[0] == (byte)Protocol.CONTACT_SERVICE.ordinal()) {
                System.out.println("Réception d'une nouvelle demande du client " +
                                   clientPacket.getAddress().getHostAddress() + " " + clientPacket.getPort());

                // Création du paquet de réponse
                byte[] reponseBuffer = new byte[clientPacket.getData().length];
                reponseBuffer[0] = (byte) Protocol.REPONSE_AU_SERVICE.ordinal();
                for(int i = 1; i < clientPacket.getData().length; i++){
                    reponseBuffer[i] = clientPacket.getData()[i];
                }

                // Envoi de la réponse au client
                DatagramPacket clientResponsePacket = new DatagramPacket(reponseBuffer, clientPacket.getData().length, clientPacket.getAddress(), clientPacket.getPort());
                pointToPointSocket.send(clientResponsePacket);
            }
            // Sinon c'est un test d'existance de la part du lieur
            else
            {
                System.out.print("Reception de test d'existance de la part du lieur");

                // Envoi de la confirmation d'existance au lieur
                DatagramPacket sayItExist = new DatagramPacket(new byte[]{(byte) Protocol.J_EXISTE.ordinal()}, 1, clientPacket.getAddress(), clientPacket.getPort());
                pointToPointSocket.send(sayItExist);
            }
        }
    }

}
