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

    // List of the other linkers TODO : remove the values and set it with the args
    private final Linker[] linkers = {
            new Linker("127.0.0.1", 2224),
            new Linker("127.0.0.1", 2222)
    };

    final int idService = 1;

    final int pointToPointPort = 12342;


    /**
     * Creates a new linker which will listen on the specified port and will synchronise with the specified linkers.
     * TODO : Ajouter les arguments (liste de linkers et port sur lequel on écoute
     */
    public void Client() {

    }

    /**
     * demarre le client
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void start() throws IOException, InterruptedException {


        DatagramSocket pointToPointSocket = new DatagramSocket(pointToPointPort);
        System.out.println("Démarrage du client");

        while (true) {
            Linker lieur = linkers[ThreadLocalRandom.current().nextInt(0, linkers.length)];
            System.out.println("Le client va demander le service" + idService + " au lieur:");
            System.out.println(lieur);

            byte[] demandeServiceBuffer = new byte[2];
            demandeServiceBuffer[0] = (byte) Protocol.DEMANDE_DE_SEVICE.ordinal();
            demandeServiceBuffer[1] = idService;
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

            pointToPointSocket.setSoTimeout(0);
            if (reponseDemandeDeServicePaquet.getData()[0] == Protocol.SERVICE_EXISTE_PAS.ordinal() || reponseDemandeDeServicePaquet.getData()[0] == Protocol.REPONSE_DEMANDE_DE_SERVICE.ordinal()) {
                System.out.println("Reponse du lieur recue");

                // Service not found
                if (reponseDemandeDeServicePaquet.getData()[0] == Protocol.SERVICE_EXISTE_PAS.ordinal()) {
                    System.out.println("le service demandé n'a pas été trouvé");
                    break;
                }
                // Service found - use the service
                else if (reponseDemandeDeServicePaquet.getData()[0] == Protocol.REPONSE_DEMANDE_DE_SERVICE.ordinal()) {
                    // Get the ip and port
                    InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(reponseDemandeDeServicePaquet.getData(), 2, 6));
                    byte[] portByte = new byte[2];
                    portByte[0] = reponseDemandeDeServicePaquet.getData()[7];
                    portByte[1] = reponseDemandeDeServicePaquet.getData()[6];
                    int port = new BigInteger(portByte).intValue();

                    System.out.println("Le service a été trouvé il est joignable a l'adresse: " + ip.getHostAddress() + ":" + port);

                    // Send the echo message
                    byte[] messageAEnvoyer = {(byte) Protocol.CONTACT_SERVICE.ordinal(), 4, 1, 1, 1, 1};

                    DatagramPacket contactServicePaquet = new DatagramPacket(messageAEnvoyer, messageAEnvoyer.length, ip, port);

                    System.out.println("Message envoyé au service");
                    pointToPointSocket.send(contactServicePaquet);

                    buffer = new byte[8];
                    DatagramPacket reponseServicePaquet = new DatagramPacket(buffer, buffer.length);

                    try {
                        pointToPointSocket.setSoTimeout(2000);
                        pointToPointSocket.receive(reponseServicePaquet);
                        if (reponseServicePaquet.getData()[0] == Protocol.REPONSE_AU_SERVICE.ordinal()) {
                            System.out.println("Reponse du serveur reçue");
                        }
                    } catch (SocketTimeoutException e) {
                        byte[] serviceExistePasBuffer = {(byte) Protocol.SERVICE_EXISTE_PAS.ordinal(), (byte) idService,
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
            // Wait and ask again for the service
            Thread.sleep(10000);
        }
    }
}
