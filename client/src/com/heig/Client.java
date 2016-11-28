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
 * Le client contact un lieur dans sa liste de lieurs connus pour obtenir un service d'un type. Si un lieur est atteint
 * il envoi l'ip et le port du service demandé, si aucun service de ce type est connu, le lieur indique au client
 * qu'il ne connais pas de service de ce type et le client se coupe, dans le cas ou il y a une réponse il effectué
 * une demande apres 10sec
 */
public class Client {

    private final Lieur[] lieurs; // Liste des lieurs
    final int idService;          // Service demandé par le client
    final int port;               // Port pour l'envoi et la récéption de paquets UDP

    /**
     * Création d'un nouveau client avec l'id du service qu'il va utiliser, son port et la liste des lieurs.
     */
    public Client(int port, int idService, Lieur[] lieurs){
        this.lieurs = lieurs;
        this.port = port;
        this.idService = idService;
    }

    /**
     * Demarre le client
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void demarrer() throws IOException, InterruptedException {

        DatagramSocket pointToPointSocket = new DatagramSocket(port);
        System.out.println("Démarrage du client");

        while (true) {
            Lieur lieur = lieurs[ThreadLocalRandom.current().nextInt(0, lieurs.length)];
            System.out.println("Le client va demander le service" + idService + " au lieur:");
            System.out.println(lieur);

            byte[] demandeServiceBuffer = new byte[2];
            demandeServiceBuffer[0] = (byte) Protocole.DEMANDE_DE_SERVICE.ordinal();
            demandeServiceBuffer[1] = (byte) idService;
            DatagramPacket demandeDeServicePaquet = new DatagramPacket(demandeServiceBuffer, 2, InetAddress.getByName(lieur.getIp()), lieur.getPort());

            pointToPointSocket.send(demandeDeServicePaquet);

            byte[] buffer = new byte[8];
            DatagramPacket reponseDemandeDeServicePaquet = new DatagramPacket(buffer, buffer.length);

            try {
                pointToPointSocket.setSoTimeout(2000);
                pointToPointSocket.receive(reponseDemandeDeServicePaquet);
            } catch (SocketTimeoutException e) {
                System.out.println("Le lieur n'a pas pu etre atteint");
                break;
            }

            if (reponseDemandeDeServicePaquet.getData()[0] == Protocole.SERVICE_EXISTE_PAS.ordinal() || reponseDemandeDeServicePaquet.getData()[0] == Protocole.REPONSE_DEMANDE_DE_SERVICE.ordinal()) {
                System.out.println("Reponse du lieur recue");

                // Service non trouvé
                if (reponseDemandeDeServicePaquet.getData()[0] == Protocole.SERVICE_EXISTE_PAS.ordinal()) {
                    System.out.println("le service demandé n'a pas ete trouve");
                    return;
                }
                // Service trouvé, utilisation du service
                else if (reponseDemandeDeServicePaquet.getData()[0] == Protocole.REPONSE_DEMANDE_DE_SERVICE.ordinal()) {
                    // Récupération de l'ip et du port du service
                    InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(reponseDemandeDeServicePaquet.getData(), 2, 6));
                    byte[] portByte = new byte[2];
                    portByte[0] = reponseDemandeDeServicePaquet.getData()[7];
                    portByte[1] = reponseDemandeDeServicePaquet.getData()[6];
                    int port = new BigInteger(portByte).intValue();

                    System.out.println("Le service a été trouvé il est joignable a l'adresse: " + ip.getHostAddress() + ":" + port);

                    // Envoyer le message d'echo
                    byte[] messageAEnvoyer = {(byte) Protocole.CONTACT_SERVICE.ordinal(), 4, 1, 1, 1, 1};

                    DatagramPacket contactServicePaquet = new DatagramPacket(messageAEnvoyer, messageAEnvoyer.length, ip, port);

                    System.out.println("Message envoyé au service");
                    pointToPointSocket.send(contactServicePaquet);

                    buffer = new byte[8];
                    DatagramPacket reponseServicePaquet = new DatagramPacket(buffer, buffer.length);

                    try {
                        pointToPointSocket.receive(reponseServicePaquet);
                        if (reponseServicePaquet.getData()[0] == Protocole.REPONSE_DU_SERVICE.ordinal()) {
                            System.out.println("Reponse du serveur reçue");
                            System.out.println("taille " + + reponseServicePaquet.getData()[1]);

                            for( int i = 0 ; i < reponseServicePaquet.getData()[1]; i++ )
                            {
                                System.out.println( i + " : " + reponseServicePaquet.getData()[2+i]);
                            }

                        }
                    } catch (SocketTimeoutException e) {
                        byte[] serviceExistePasBuffer = {(byte) Protocole.SERVICE_EXISTE_PAS.ordinal(), (byte) idService,
                                ip.getAddress()[0], ip.getAddress()[1], ip.getAddress()[2], ip.getAddress()[3],
                                reponseDemandeDeServicePaquet.getData()[6],
                                reponseDemandeDeServicePaquet.getData()[7]};

                        System.out.print("Timeout de la demande au service, envoi du message SERVICE_EXISTE_PAS au lieur");
                        DatagramPacket serviceNonAtteint = new DatagramPacket(serviceExistePasBuffer, 8, InetAddress.getByName(lieur.getIp()), lieur.getPort());
                        pointToPointSocket.send(serviceNonAtteint);
                    }

                    // On remet la valeur du timeout à 0 (infini)
                    pointToPointSocket.setSoTimeout(0);
                }
            }

            // Attendre et relancer une demande
            Thread.sleep(10000);
        }
    }
}
