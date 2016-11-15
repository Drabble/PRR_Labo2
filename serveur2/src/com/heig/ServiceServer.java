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
 * A linker server that will handle subscriptions from the services and maintain a list of these services.
 * It will synchronise with the other linkers and answer the clients requests.
 */
public class ServiceServer {

    // List of the other linkers TODO : remove the values and set it with the args
    final Linker[] linkers = {
            new Linker("127.0.0.1", 7780),
            new Linker("127.0.0.1", 7781),
            new Linker("127.0.0.1", 7782)
    };

    byte idService = 1;

    final int pointToPointPort = 1234; // TODO : Make this an argument

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
        DatagramPacket linkerSubscribePacket = new DatagramPacket(new byte[]{idService, (byte)Protocol.SUB.ordinal()}, 2, InetAddress.getByName(linkers[linkerNumber].getIp()),  linkers[linkerNumber].getPort());
        pointToPointSocket.send(linkerSubscribePacket);

        // Wait for confirmation
        byte[] buffer = new byte[1];
        DatagramPacket linkerConfirmationPacket = new DatagramPacket(buffer, buffer.length);
        pointToPointSocket.receive(linkerConfirmationPacket);

        if(linkerConfirmationPacket.getData()[1] == Protocol.CONFIRM_SUB.ordinal()) {
            // Do service forever
            while (true) {
                // Wait for client request
                byte[] buffer2 = new byte[4];
                DatagramPacket clientPacket = new DatagramPacket(buffer2, buffer2.length);
                pointToPointSocket.receive(clientPacket);

                // Answer to client request
                DatagramPacket clientResponsePacket = new DatagramPacket(clientPacket.getData(), clientPacket.getData().length, clientPacket.getAddress(), clientPacket.getPort());
                pointToPointSocket.send(clientResponsePacket);
            }
        }
    }

}