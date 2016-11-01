package com.company;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    static List<Service> serviceList = new ArrayList<Service>();
    static final Linker[] linkers = {
            new Linker("127.0.0.1", 7780),
            new Linker("127.0.0.1", 7781),
            new Linker("127.0.0.1", 7782)

    };

    public static void main(String[] args) throws InterruptedException, IOException {
        // Create point to point socket to send messages to the linker
        final int pointToPointPort = 1234;
        DatagramSocket pointToPointSocket = new DatagramSocket(pointToPointPort);
        System.out.println("Started the socket!");

        /*Service s1 = new Service(1,"192.186.1.1",7777);
        Service s2 = new Service(2,"192.186.1.1",7778);
        Service s3 = new Service(2,"192.186.1.1",7779);
        serviceList.add(s1);
        serviceList.add(s2);
        serviceList.add(s3);*/

        start(pointToPointSocket);
        while (true) {

        }
	// write your code here
    }

    //fonction lancé au demmarage pour se sycroniser avec les autres lieur
    public static void start(DatagramSocket pointToPointSocket) throws InterruptedException, IOException
    {
        for(Linker linker : linkers){
            // Ask the linked for service number 0
            DatagramPacket linkerPacket = new DatagramPacket(new byte[]{(byte)Protocol.ASK_SERVIVE_LIST.ordinal()}, 1, InetAddress.getByName(linker.getIp()),  linker.getPort());
            pointToPointSocket.send(linkerPacket);

            // Receive the service ip and address
            // 1 byte pour le type, 1 pour le nombre , 100 services max
            byte[] buffer = new byte[702];
            DatagramPacket serviceAddressPacket = new DatagramPacket(buffer, buffer.length);
            pointToPointSocket.receive(serviceAddressPacket);
            int type = serviceAddressPacket.getData()[0];
            int nbServices = serviceAddressPacket.getData()[1];
            for(int i = 0; i < nbServices; i++){
                int idService = serviceAddressPacket.getData()[2 + 7 * i];
                InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(serviceAddressPacket.getData(), 3 + i * 7, 7 + i * 7));
                byte[] portByte = Arrays.copyOfRange(serviceAddressPacket.getData(), 8 + i * 7, 9 + i * 7);
                int port = ((portByte[0] & 0xff) << 8) | (portByte[1] & 0xff);

                Service service = new Service(idService, ip.getHostAddress(), port);
                serviceList.add(service);
            }

            InetAddress serviceAddress = InetAddress.getByAddress(Arrays.copyOfRange(serviceAddressPacket.getData(), 0, 4));
        }
    }
    public void sendServiceList()
    {

    }
    public void sendServiceToClient()
    {

    }
    //efface le Service de la liste et envoie l'info au autre lieure que le Service n'existe plus
    public void deletService()
    {}
    //ajoute le Service à la liste et envoie l'info au autre lieure que le Service est nouveau
    public void addService()
    {}
    public void verifExist()
    {}
    public void confirmSub()
    {}
}
