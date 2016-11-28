/**
 * Project: Labo002
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Le client contacte un lieur dans sa liste de lieurs connus pour obtenir un service d'un type donné. Si un lieur est atteint
 * il renvoie l'ip et le port du service demandé. Si aucun service de ce type est connu, le lieur indique au client
 * qu'il ne connait pas de service de ce type et le client se termine. Si le service donné par le lieur fait un tempsMaxAttenteReponse,
 * le client va envoyer un message au lieur pour lui indiquer que le service n'est pas disponible. Dans le cas ou il y a
 * une réponse du service, le client attend 10 seconde et refait une demande auprès du lieur.
 */
public class Client {
    private final Lieur[] lieurs;            // Liste des lieurs
    final int idService;                     // Service demandé par le client
    final int port;                          // Port pour l'envoi et la récéption de paquets UDP
    final int tempsMaxAttenteReponse = 2000; // Temps d'attente maximale pour recevoir une réponse d'un serveur de service
    final int delaiEntreRequetes = 10000;    // Temps avant de refaire une requête au lieur

    /**
     * Création d'un nouveau client avec l'id du service qu'il va utiliser, son port et la liste des lieurs.
     *
     * @param port
     * @param idService
     * @param lieurs
     */
    public Client(int port, int idService, Lieur[] lieurs){
        this.lieurs = lieurs;
        this.port = port;
        this.idService = idService;
    }

    /**
     * Démarre le client, il va demander un service à un lieur aléatoire et utiliser le service donné par le lieur.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void demarrer() throws IOException, InterruptedException {
        DatagramSocket pointAPointSocket = new DatagramSocket(port);
        System.out.println("Démarrage du client");

        // On fait des requêtes tant que le service demandé au lieur existe
        while (true) {
            // Choix d'un lieur aléatoire
            Lieur lieur = lieurs[ThreadLocalRandom.current().nextInt(0, lieurs.length)];
            System.out.println("Le client va demander le service" + idService + " au lieur:");
            System.out.println(lieur);

            // Création et envoi du paquet de demande de service
            byte[] demandeServiceBuffer = {(byte)Protocole.DEMANDE_DE_SERVICE.ordinal(), (byte) idService};
            DatagramPacket demandeDeServicePaquet = new DatagramPacket(demandeServiceBuffer, 2, InetAddress.getByName(lieur.getIp()), lieur.getPort());
            pointAPointSocket.send(demandeDeServicePaquet);

            // Réception de la réponse du lieur
            byte[] buffer = new byte[8];
            DatagramPacket reponseDemandeDeServicePaquet = new DatagramPacket(buffer, buffer.length);
            try {
                pointAPointSocket.setSoTimeout(tempsMaxAttenteReponse);
                pointAPointSocket.receive(reponseDemandeDeServicePaquet);
            } catch (SocketTimeoutException e) {
                System.out.println("Le lieur n'a pas pu etre atteint");
                break;
            }

            // Si on obtient une réponse du lieur on la traite
            if (reponseDemandeDeServicePaquet.getData()[0] == Protocole.SERVICE_EXISTE_PAS.ordinal() || reponseDemandeDeServicePaquet.getData()[0] == Protocole.REPONSE_DEMANDE_DE_SERVICE.ordinal()) {
                System.out.println("Reponse du lieur recue");

                // Si le service n'a pas été trouvé on termine le client
                if (reponseDemandeDeServicePaquet.getData()[0] == Protocole.SERVICE_EXISTE_PAS.ordinal()) {
                    System.out.println("le service demandé n'a pas ete trouve");
                    return;
                }
                // Si le service a été trouvé on essaie de l'utiliser
                else if (reponseDemandeDeServicePaquet.getData()[0] == Protocole.REPONSE_DEMANDE_DE_SERVICE.ordinal()) {
                    // Récupération de l'ip et du port du service
                    InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(reponseDemandeDeServicePaquet.getData(), 2, 6));
                    byte[] portByte = new byte[2];
                    portByte[0] = reponseDemandeDeServicePaquet.getData()[7];
                    portByte[1] = reponseDemandeDeServicePaquet.getData()[6];
                    int port = new BigInteger(portByte).intValue();

                    System.out.println("Le service a été trouvé il est joignable a l'adresse: " + ip.getHostAddress() + ":" + port);

                    // Envoi du message d'echo
                    byte[] messageAEnvoyer = {(byte) Protocole.CONTACT_SERVICE.ordinal(), 4, 1, 1, 1, 1};
                    DatagramPacket contactServicePaquet = new DatagramPacket(messageAEnvoyer, messageAEnvoyer.length, ip, port);
                    System.out.println("Message envoyé au service");
                    pointAPointSocket.send(contactServicePaquet);

                    // Réception de la réponse
                    buffer = new byte[8];
                    DatagramPacket reponseServicePaquet = new DatagramPacket(buffer, buffer.length);
                    try {
                        pointAPointSocket.receive(reponseServicePaquet);

                        // On affiche la réponse du serveur si elle est correcte
                        if (reponseServicePaquet.getData()[0] == Protocole.REPONSE_DU_SERVICE.ordinal()) {
                            System.out.println("Reponse du serveur reçue");
                            System.out.println("taille " + + reponseServicePaquet.getData()[1]);
                            for( int i = 0 ; i < reponseServicePaquet.getData()[1]; i++ ) {
                                System.out.println( i + " : " + reponseServicePaquet.getData()[2+i]);
                            }
                        }
                    }
                    // Si le service n'a pas répondu à temps, on notifie le lieur
                    catch (SocketTimeoutException e) {
                        byte[] serviceExistePasBuffer = {(byte) Protocole.SERVICE_EXISTE_PAS.ordinal(), (byte) idService,
                                ip.getAddress()[0], ip.getAddress()[1], ip.getAddress()[2], ip.getAddress()[3],
                                reponseDemandeDeServicePaquet.getData()[6],
                                reponseDemandeDeServicePaquet.getData()[7]};
                        System.out.print("Timeout de la demande au service, envoi du message SERVICE_EXISTE_PAS au lieur");
                        DatagramPacket serviceNonAtteint = new DatagramPacket(serviceExistePasBuffer, 8, InetAddress.getByName(lieur.getIp()), lieur.getPort());
                        pointAPointSocket.send(serviceNonAtteint);
                    }

                    // On remet la valeur du tempsMaxAttenteReponse à 0 (infini)
                    pointAPointSocket.setSoTimeout(0);
                }
            }
            // Attendre et relancer une demande
            Thread.sleep(delaiEntreRequetes);
        }
    }
}
