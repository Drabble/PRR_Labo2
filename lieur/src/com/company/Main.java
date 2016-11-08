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

    public void main(String[] args) throws InterruptedException, IOException {
        // Create point to point socket to send messages to the linker
        final int pointToPointPort = 1234;
        DatagramSocket pointToPointSocket = new DatagramSocket(pointToPointPort);
        System.out.println("Started the socket!");

        Service s1 = new Service(1,"192.186.1.1",7777);
        Service s2 = new Service(2,"192.186.1.1",7778);
        Service s3 = new Service(2,"192.186.1.1",7779);
        serviceList.add(s1);
        serviceList.add(s2);
        serviceList.add(s3);

        start(pointToPointSocket);
        while (true) {
            byte[]  buffer = new byte[1];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            pointToPointSocket.receive(receivePacket);
            int type = receivePacket.getData()[0];
            if(type == (byte)Protocol.ASK_SERVIVE_LIST.ordinal())
            {
                sendServiceList(receivePacket);
            }
            else if(type == (byte)Protocol.ADD_SERVICE.ordinal()){
                addService(receivePacket);
            }
        }
	// write your code here
    }

    //fonction lancé au demmarage pour se sycroniser avec les autres lieur
    private void start(DatagramSocket pointToPointSocket) throws InterruptedException, IOException
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
    private void sendServiceList(DatagramPacket serviceAddressPacket)throws InterruptedException, IOException
    {

            DatagramPacket serviceListPacket = new DatagramPacket(new byte[]{(byte)Protocol.RETURN_SERVICES.ordinal()}, 702, InetAddress.getByName(serviceAddressPacket.getAddress().getHostName()),  serviceAddressPacket.getPort());
            byte[] IDservice = new byte[1];
            byte[] IP = new byte[4];
            byte[] port = new byte[2];
            int i = 0;
            serviceListPacket.setData(intToBytes(serviceList.size(),1),1,1);
            serviceAddressPacket.setLength(2+(7*serviceList.size()));
            for(Service service : serviceList)
            {
                IDservice = intToBytes(service.getIDservice(),IDservice.length);
                IP = InetAddress.getByName(service.getIp()).getAddress();
                port = intToBytes(service.getPort(),port.length);

                serviceListPacket.setData(IDservice, 2 + 7*i, IDservice.length);
                serviceListPacket.setData(IP, 3 + 7*i, IP.length);
                serviceListPacket.setData(port,8 + 7*i,port.length);
                i++;
            }

    }
    public void sendServiceToClient()
    {

    }
    //efface le Service de la liste et envoie l'info au autre lieure que le Service n'existe plus
    public void deletService()
    {

    }
    //ajoute le Service à la liste et envoie l'info au autre lieure que le Service est nouveau
    public void addService(DatagramPacket addServicePacket)
    {
        
    }
    public void verifExist()
    {}
    public void confirmSub()
    {}

    public static byte[] intToBytes(int x, int n) {
        byte[] bytes = new byte[n];
        for (int i = 0; i < n; i++, x >>>= 8)
            bytes[i] = (byte) (x & 0xFF);
        return bytes;
    }
}
