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
public class ServiceServeur {
    private final Lieur[] lieurs; // Liste de tous les lieurs
    private final int idService;  // Id du service fourni
    private final int port;       // Port utilisé pour la réception des paquets point à poinr

    /**
     * Création d'un nouveau lieur avec son id, son ip, son port et la liste des lieurs
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public ServiceServeur(int port, int idService, Lieur[] lieurs) {
        this.lieurs = lieurs;
        this.port = port;
        this.idService = idService;
    }

    /**
     * Démarre le serveur de service. Il va se souscrire à un lieur et faire son service d'echo lors de la réception
     * de requêtes.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void demarrer() throws IOException, InterruptedException {
        // Utilisé pour générer des valeurs aléatoires
        Random rand = new Random();

        // Création du socket point à point pour l'envoi de packet udp
        DatagramSocket pointAPointSocket = new DatagramSocket(port);
        System.out.println("Serveur démarré!");

        // Souscription à un lieur aléatoire dans la liste des lieurs
        int linkerNumber = rand.nextInt(lieurs.length);
        byte[] souscriptionBuffer = {(byte) Protocole.ABONNEMENT.ordinal(), (byte)idService};

        System.out.println("Tentative de souscription au lieur:");
        System.out.println(lieurs[linkerNumber]);

        // Envoi du paquet de souscription
        DatagramPacket linkerSubscribePacket = new DatagramPacket(souscriptionBuffer, souscriptionBuffer.length, InetAddress.getByName(lieurs[linkerNumber].getIp()), lieurs[linkerNumber].getPort());
        pointAPointSocket.send(linkerSubscribePacket);

        // Attente de la confirmation du lieur
        byte[] buffer = new byte[1];
        DatagramPacket linkerConfirmationPacket = new DatagramPacket(buffer, buffer.length);
        do {
            try {
                pointAPointSocket.setSoTimeout(4000);
                pointAPointSocket.receive(linkerConfirmationPacket);
            } catch (SocketTimeoutException e) {
                System.out.print("Le lieur n'a pas pu etre atteint, arret du serveur");
                return;
            }
        } while (linkerConfirmationPacket.getData()[0] != Protocole.CONFIRMATION_ABONNEMENT.ordinal());

        System.out.println("Confirmation de souscription reçue");

        // On remet le timeout du socket à 0 (infini)
        pointAPointSocket.setSoTimeout(0);

        // Performer le service à l'infini maintenant qu'on est souscris aux lieurs
        while (true) {
            System.out.println("Attente d'une nouvelle demande d'un client");

            // Wait for client request, taille maximal d'un demande: 1000 bytes
            byte[] requeteBuffer = new byte[1000];
            DatagramPacket clientPacket = new DatagramPacket(requeteBuffer, requeteBuffer.length);
            pointAPointSocket.receive(clientPacket);

            // Si c'est une requête au service d'echo
            if(clientPacket.getData()[0] == (byte)Protocole.CONTACT_SERVICE.ordinal()) {
                System.out.println("Réception d'une nouvelle demande du client " +
                                   clientPacket.getAddress().getHostAddress() + " " + clientPacket.getPort());

                // Création du paquet de réponse
                byte[] reponseBuffer = new byte[clientPacket.getData().length];
                reponseBuffer[0] = (byte) Protocole.REPONSE_DU_SERVICE.ordinal();
                for(int i = 1; i < clientPacket.getData().length; i++){
                    reponseBuffer[i] = clientPacket.getData()[i];
                }

                // Envoi de la réponse au client
                DatagramPacket clientResponsePacket = new DatagramPacket(reponseBuffer, clientPacket.getData().length, clientPacket.getAddress(), clientPacket.getPort());
                pointAPointSocket.send(clientResponsePacket);
            }
            // Sinon c'est un test d'existance de la part du lieur
            else
            {
                System.out.print("Reception de test d'existance de la part du lieur");

                // Envoi de la confirmation d'existance au lieur
                DatagramPacket sayItExist = new DatagramPacket(new byte[]{(byte) Protocole.J_EXISTE.ordinal()}, 1, clientPacket.getAddress(), clientPacket.getPort());
                pointAPointSocket.send(sayItExist);
            }
        }
    }

}
