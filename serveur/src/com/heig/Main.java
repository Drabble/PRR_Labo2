package com.heig;

import java.io.IOException;
import java.net.*;
import java.util.Random;

public class Main {



    public static void main(String[] args) throws InterruptedException, IOException {
        final String[] linkers = {"127.0.0.1", "127.0.0.1"};
        final int[] linkersPorts = {4444, 5555};
        final int pointToPointPort = 1235;
        byte[] serviceNumber = {0};

        Random rand = new Random();

        // Create point to point socket to send messages to the linker
        DatagramSocket pointToPointSocket = new DatagramSocket(pointToPointPort);
        System.out.println("Started the socket!");

        // Subscribe to the linker
        int linkerNumber = rand.nextInt(linkers.length);
        DatagramPacket linkerSubscribePacket = new DatagramPacket(serviceNumber, serviceNumber.length, InetAddress.getByName(linkers[linkerNumber]),  linkersPorts[linkerNumber]);
        pointToPointSocket.send(linkerSubscribePacket);

        // Wait for confirmation
        byte[] buffer = new byte[1];
        DatagramPacket linkerConfirmationPacket = new DatagramPacket(buffer, buffer.length);
        pointToPointSocket.receive(linkerConfirmationPacket);

        // Do service forever
        while(true){
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
