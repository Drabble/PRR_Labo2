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
import java.util.Random;

/**
 * A linker server that will handle subscriptions from the services and maintain a list of these services.
 * It will synchronise with the other linkers and answer the clients requests.
 */
public class Client {

    // List of the other linkers TODO : remove the values and set it with the args
    final Linker[] linkers = {
            new Linker("127.0.0.1", 1234)
    };

    byte idService = 1;

    final int pointToPointPort = 12347; // TODO : Make this an argument

    /**
     * Creates a new linker which will listen on the specified port and will synchronise with the specified linkers.
     * TODO : Ajouter les arguments (liste de linkers et port sur lequel on écoute
     *
     */
    public void Client() {

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
            int linkerNumber = 0;

            byte[] tosend =new byte[2];
            tosend[0] = (byte) Protocol.DEMANDE_DE_SEVICE.ordinal();
            tosend[1] = idService;
            //DatagramPacket linkerPacket = new DatagramPacket(new byte[]{idService, (byte)Protocol.DEMANDE_DE_SEVICE.ordinal()}, 2, InetAddress.getByName(linkers[linkerNumber].getIp()),  linkers[linkerNumber].getPort());
            DatagramPacket linkerPacket = new DatagramPacket(tosend, 2, InetAddress.getByName(linkers[linkerNumber].getIp()), linkers[linkerNumber].getPort());


            pointToPointSocket.send(linkerPacket);


            byte[] buffer = new byte[8];
            DatagramPacket serviceAddressPacket = new DatagramPacket(buffer, buffer.length);
            pointToPointSocket.receive(serviceAddressPacket);

            System.out.println(serviceAddressPacket.getData()[0]);
            // Service not found
            if(serviceAddressPacket.getData()[0] == Protocol.SERVICE_EXISTE_PAS.ordinal()){
                System.out.println("Service not found");
                break;
            }
            // Service found - use the service
            else if(serviceAddressPacket.getData()[0] == Protocol.REPONSE_DEMANDE_DE_SERVICE.ordinal()){
                // Get the ip and port
                System.out.println("REPONSE_DEMANDE_DE_SERVICE");
                InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(serviceAddressPacket.getData(), 2, 6));

                byte[] portByte = new byte[2];
                portByte[0] = serviceAddressPacket.getData()[7];
                portByte[1] = serviceAddressPacket.getData()[6];
                int port = new BigInteger(portByte).intValue();

                // Send the echo message
                byte[] echoMessage = {1,1,1,1};
                tosend =new byte[echoMessage.length+2];
                tosend[0] = (byte) Protocol.CONTACT_SERVICE.ordinal();
                tosend[1] = (byte) echoMessage.length;
                tosend[2] = echoMessage[0];
                tosend[3] = echoMessage[1];
                tosend[4] = echoMessage[2];
                tosend[5] = echoMessage[3];

                System.out.println(portByte[0]);
                System.out.println(portByte[1]);
                System.out.println(ip.getHostAddress());
                System.out.println(port);

                port = 14604;

                DatagramPacket servicePacket = new DatagramPacket(tosend, echoMessage.length+2, ip, port);



                try {
                    pointToPointSocket.send(servicePacket);
                    System.out.println("Echo message sent to the service");
                } catch (IOException e) {
                    System.out.println("Service not available");
                    // TODO Notify linker
                    DatagramPacket servieNoAttient = new DatagramPacket(new byte[]{(byte) Protocol.SERVICE_EXISTE_PAS.ordinal()}, 8, InetAddress.getByName(linkers[linkerNumber].getIp()), linkers[linkerNumber].getPort());
                    servieNoAttient.setData(Util.intToBytes(idService, 1), 1, 1);
                    servieNoAttient.setData(ip.getAddress(),2,4);
                    servieNoAttient.setData(Util.intToBytes(port, 2), 6, 2);

                    pointToPointSocket.send(servieNoAttient);

                }

                try {
                    // TODO : UTILISER UN AUTRE PORT
                    pointToPointSocket.setSoTimeout(2000);
                    pointToPointSocket.receive(servicePacket);
                    System.out.println("Echor reviceed id packet" + servicePacket.getData()[0]);
                    if(servicePacket.getData()[0] == Protocol.REPONSE_AU_SERVICE.ordinal()) {
                        System.out.println("Echo message received from the service");
                    }
                } catch (SocketTimeoutException e) {
                    // TODO Notify linker
                    tosend = new byte[8];
                    tosend[0] = (byte) Protocol.SERVICE_EXISTE_PAS.ordinal();
                    tosend[1] = (byte) idService;
                    tosend[2] = ip.getAddress()[0];
                    tosend[3] = ip.getAddress()[1];
                    tosend[4] = ip.getAddress()[2];
                    tosend[5] = ip.getAddress()[3];

                    tosend[6] = serviceAddressPacket.getData()[6];
                    tosend[7] = serviceAddressPacket.getData()[7];

                    System.out.print("send verif");
                    DatagramPacket servieNoAttient = new DatagramPacket(tosend, 8, InetAddress.getByName(linkers[linkerNumber].getIp()), linkers[linkerNumber].getPort());

                    pointToPointSocket.send(servieNoAttient);
                }

                // Wait and ask again for the service
                Thread.sleep(10000);
            }
        }
    }

}
