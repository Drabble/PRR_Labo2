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
            //new Linker("127.0.0.1", 1234),
            new Linker("127.0.0.1", 12349)
    };


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

        int max = linkers.length - 1;
        int nombreLieurs;
        int pointToPointPort = 12347;


        DatagramSocket pointToPointSocket = new DatagramSocket(pointToPointPort);
        System.out.println("Demmarage du client");

        while (true) {
            nombreLieurs = ThreadLocalRandom.current().nextInt(0, max + 1);
            byte idService = (byte) ThreadLocalRandom.current().nextInt(1, 2 + 1);
            idService = 1;

            System.out.println("le client demande le service " + idService + " au lieur " + nombreLieurs);


            byte[] dataAEnvoyer = new byte[2];
            dataAEnvoyer[0] = (byte) Protocol.DEMANDE_DE_SEVICE.ordinal();
            dataAEnvoyer[1] = idService;
            DatagramPacket demandeDeServicePaquet = new DatagramPacket(dataAEnvoyer, 2, InetAddress.getByName(linkers[nombreLieurs].getIp()), linkers[nombreLieurs].getPort());

            pointToPointSocket.send(demandeDeServicePaquet);

            byte[] buffer = new byte[8];
            DatagramPacket reponseDemandeDeServicePaquet = new DatagramPacket(buffer, buffer.length);

            try {
                // TODO : UTILISER UN AUTRE PORT
                pointToPointSocket.setSoTimeout(2000);
                pointToPointSocket.receive(reponseDemandeDeServicePaquet);
            } catch (SocketTimeoutException e) {
                System.out.println("Aucun lieur attient");
                break;
            }

            pointToPointSocket.setSoTimeout(0);
            if (reponseDemandeDeServicePaquet.getData()[0] == Protocol.SERVICE_EXISTE_PAS.ordinal() || reponseDemandeDeServicePaquet.getData()[0] == Protocol.REPONSE_DEMANDE_DE_SERVICE.ordinal()) {
                System.out.println("le lieur repond");
                System.out.println(reponseDemandeDeServicePaquet.getData()[0]);
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

                    System.out.println("Le service a été trouvé il se trouve ici : " + ip.getHostAddress() + ":" + port);

                    // Send the echo message
                    byte[] messageAEnvoyer = {1, 1, 1, 1};
                    dataAEnvoyer = new byte[messageAEnvoyer.length + 2];
                    dataAEnvoyer[0] = (byte) Protocol.CONTACT_SERVICE.ordinal();
                    dataAEnvoyer[1] = (byte) messageAEnvoyer.length;
                    dataAEnvoyer[2] = messageAEnvoyer[0];
                    dataAEnvoyer[3] = messageAEnvoyer[1];
                    dataAEnvoyer[4] = messageAEnvoyer[2];
                    dataAEnvoyer[5] = messageAEnvoyer[3];

                    DatagramPacket contactServicePaquet = new DatagramPacket(dataAEnvoyer, messageAEnvoyer.length + 2, ip, port);

                    System.out.println("Echo message sent to the service");
                    pointToPointSocket.send(contactServicePaquet);

                    buffer = new byte[8];
                    DatagramPacket reponseServicePaquet = new DatagramPacket(buffer, buffer.length);

                    try {
                        // TODO : UTILISER UN AUTRE PORT
                        pointToPointSocket.setSoTimeout(2000);
                        pointToPointSocket.receive(reponseServicePaquet);
                        System.out.println("Echor reviceed id packet" + reponseServicePaquet.getData()[0]);
                        if (reponseServicePaquet.getData()[0] == Protocol.REPONSE_AU_SERVICE.ordinal()) {
                            System.out.println("Echo message received from the service");
                        }
                    } catch (SocketTimeoutException e) {
                        dataAEnvoyer = new byte[8];
                        dataAEnvoyer[0] = (byte) Protocol.SERVICE_EXISTE_PAS.ordinal();
                        dataAEnvoyer[1] = idService;
                        dataAEnvoyer[2] = ip.getAddress()[0];
                        dataAEnvoyer[3] = ip.getAddress()[1];
                        dataAEnvoyer[4] = ip.getAddress()[2];
                        dataAEnvoyer[5] = ip.getAddress()[3];

                        dataAEnvoyer[6] = reponseDemandeDeServicePaquet.getData()[6];
                        dataAEnvoyer[7] = reponseDemandeDeServicePaquet.getData()[7];

                        System.out.print("demande de verification");
                        DatagramPacket servieNoAttient = new DatagramPacket(dataAEnvoyer, 8, InetAddress.getByName(linkers[nombreLieurs].getIp()), linkers[nombreLieurs].getPort());
                        pointToPointSocket.send(servieNoAttient);
                        pointToPointSocket.setSoTimeout(0);

                        buffer = new byte[1];
                        DatagramPacket reponseExistePaquet = new DatagramPacket(buffer, buffer.length);
                        pointToPointSocket.receive(reponseExistePaquet);
                        if (reponseExistePaquet.getData()[0] == Protocol.SERVICE_EXISTE_PAS.ordinal()) {
                            System.out.println("le service demandé n'a pas été trouvé");
                            break;
                        }
                    }
                }
            }

            // Wait and ask again for the service
            Thread.sleep(10000);
        }
    }
}
