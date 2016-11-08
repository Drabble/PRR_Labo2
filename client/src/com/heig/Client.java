/**
 * Project: Labo002
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Random;

/**
 * A linker server that will handle subscriptions from the services and maintain a list of these services.
 * It will synchronise with the other linkers and answer the clients requests.
 */
public class Client {

    // List of the other linkers TODO : remove the values and set it with the args
    final Linker[] linkers = {
            new Linker("127.0.0.1", 7780),
            new Linker("127.0.0.1", 7781),
            new Linker("127.0.0.1", 7782)
    };

    byte idService = 0;

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

        while (true) {
            // Ask the linked for service number 0
            int linkerNumber = rand.nextInt(linkers.length);
            DatagramPacket linkerPacket = new DatagramPacket(new byte[]{idService, (byte)Protocol.ASK_SERVICE.ordinal()}, 2, InetAddress.getByName(linkers[linkerNumber].getIp()),  linkers[linkerNumber].getPort());
            pointToPointSocket.send(linkerPacket);

            // Receive the service ip and port
            byte[] buffer = new byte[7];
            DatagramPacket serviceAddressPacket = new DatagramPacket(buffer, buffer.length);
            pointToPointSocket.receive(serviceAddressPacket);

            // Service not found
            if(serviceAddressPacket.getData()[0] == Protocol.SERVICE_DONT_EXIST.ordinal()){
                System.out.println("Service not found");
                break;
            }
            // Service found - use the service
            else if(serviceAddressPacket.getData()[0] == Protocol.ADDRESS_SERVICE.ordinal()){
                // Get the ip and port
                InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(serviceAddressPacket.getData(), 1, 4));
                byte[] portByte = Arrays.copyOfRange(serviceAddressPacket.getData(), 5, 6);
                int port = ((portByte[0] & 0xff) << 8) | (portByte[1] & 0xff);

                // Send the echo message
                byte[] echoMessage = {1,1,1,1};
                DatagramPacket servicePacket = new DatagramPacket(echoMessage, echoMessage.length, ip, port);
                try {
                    pointToPointSocket.send(servicePacket);
                    System.out.println("Echo message sent to the service");
                } catch (IOException e) {
                    System.out.println("Service not available");
                    // TODO Notify linker
                }
                try {
                    pointToPointSocket.receive(servicePacket);
                    System.out.println("Echo message received from the service");
                } catch (IOException e) {
                    System.out.println("Service not available");
                    // TODO Notify linker
                }

                // Wait and ask again for the service
                Thread.sleep(10000);
            }
            /*
             * Transform INetIPAddress to byte array
              * InetAddress ip = InetAddress.getByName("192.168.2.1");
                byte[] bytes = ip.getAddress();
                for (byte b : bytes) {
                    System.out.println(b & 0xFF);
                }
             */

        }
    }

}
