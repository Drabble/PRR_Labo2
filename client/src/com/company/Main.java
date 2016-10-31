package com.company;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Random;

public class Main {



    public static void main(String[] args) throws InterruptedException, IOException {
        final String[] linkers = {"127.0.0.1", "127.0.0.1"};
        final int[] linkersPorts = {4444, 5555};
        final int pointToPointPort = 1234;
        byte[] serviceNumber = {0};

        Random rand = new Random();

        // Create point to point socket to send messages to the linker
        DatagramSocket pointToPointSocket = new DatagramSocket(pointToPointPort);
        System.out.println("Started the socket!");

        while (true) {
            // Ask the linked for service number 0
            int linkerNumber = rand.nextInt(linkers.length);
            DatagramPacket linkerPacket = new DatagramPacket(serviceNumber, serviceNumber.length, InetAddress.getByName(linkers[linkerNumber]),  linkersPorts[linkerNumber]);
            pointToPointSocket.send(linkerPacket);

            // Receive the service ip and address
            // 4 premiers byte = ip , 4 suivant = 0 port ?
            byte[] buffer = new byte[4];
            DatagramPacket serviceAddressPacket = new DatagramPacket(buffer, buffer.length);
            pointToPointSocket.receive(serviceAddressPacket);
            InetAddress serviceAddress = InetAddress.getByAddress(Arrays.copyOfRange(serviceAddressPacket.getData(), 0, 4));
            // TODO ADD PORT
            int port = 111;

            // Service not found
            if(serviceAddress.getHostAddress().compareTo("0.0.0.0") == 0){
                System.out.println("Service not found");
                break;
            }
            // Service found - use the service
            else{
                byte[] echoMessage = {1,1,1,1};
                DatagramPacket servicePacket = new DatagramPacket(echoMessage, echoMessage.length, serviceAddress, port);
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
