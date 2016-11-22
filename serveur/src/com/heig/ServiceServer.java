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
    final Linker[] linkers = {
            new Linker("127.0.0.1", 12349)
    };

    byte idService = 1;

    final int pointToPointPort = 12345; // TODO : Make this an argument

    /**
     * Creates a new linker which will listen on the specified port and will synchronise with the specified linkers.
     * TODO : Ajouter les arguments (liste de linkers et port sur lequel on écoute
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void LinkerServer() throws InterruptedException, IOException {

    }

    /**
     * Starts the linker. It will begin by synchronising with another linker than it will answer to requests
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void start() throws IOException, InterruptedException {
        // Variable used to generate random values
        Random rand = new Random();

        // Create point to point socket to send messages to the linker
        DatagramSocket pointToPointSocket = new DatagramSocket(pointToPointPort);
        System.out.println("Started the socket!");

        // Subscribe to the linker
        int linkerNumber = rand.nextInt(linkers.length);
        byte[] tosend =new byte[2];
        tosend[0] = (byte) Protocol.ABONNEMENT.ordinal();
        tosend[1] = idService;
        DatagramPacket linkerSubscribePacket = new DatagramPacket(tosend, 2, InetAddress.getByName(linkers[linkerNumber].getIp()), linkers[linkerNumber].getPort());

        pointToPointSocket.send(linkerSubscribePacket);

        // Wait for confirmation
        byte[] buffer = new byte[1];
        DatagramPacket linkerConfirmationPacket = new DatagramPacket(buffer, buffer.length);

        do {
            try {
                pointToPointSocket.setSoTimeout(2000);
                pointToPointSocket.receive(linkerConfirmationPacket);
            } catch (SocketTimeoutException e) {
                System.out.print("lieur non attient, le serveur va s'arreter");
                System.exit(0);
            }
        } while (linkerConfirmationPacket.getData()[0] != Protocol.CONFIRMATION_ABONNEMENT.ordinal());
        pointToPointSocket.setSoTimeout(0);
        // Do service forever
        while (true) {
            System.out.println("run");
            // Wait for client request
            byte[] buffer2 = new byte[4];
            DatagramPacket clientPacket = new DatagramPacket(buffer2, buffer2.length);
            pointToPointSocket.receive(clientPacket);

            if(clientPacket.getData()[0] == (byte)Protocol.CONTACT_SERVICE.ordinal()) {
                System.out.println("recive client " + clientPacket.getAddress().getHostAddress() + " " + clientPacket.getPort());
                tosend = new byte[clientPacket.getData().length];
                tosend[0] = (byte) Protocol.REPONSE_AU_SERVICE.ordinal();
                // Answer to client request
                DatagramPacket clientResponsePacket = new DatagramPacket(tosend, clientPacket.getData().length, clientPacket.getAddress(), clientPacket.getPort());
                pointToPointSocket.send(clientResponsePacket);
            }
            else
            {
                System.out.print("Reception de test d'existance");
                DatagramPacket sayItExist = new DatagramPacket(new byte[]{(byte) Protocol.J_EXISTE.ordinal()}, 1, clientPacket.getAddress(), clientPacket.getPort());
                pointToPointSocket.send(sayItExist);
            }
        }
    }

}
