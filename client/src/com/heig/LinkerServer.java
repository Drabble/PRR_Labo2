/**
 * Project: Labo002
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A linker server that will handle subscriptions from the services and maintain a list of these services.
 * It will synchronise with the other linkers and answer the clients requests.
 */
public class LinkerServer {
    // List of the services
    List<Service> serviceList = new ArrayList<>();

    // List of the other linkers TODO : remove the values and set it with the args
    final Linker[] linkers = {
            new Linker("127.0.0.1", 7780),
            new Linker("127.0.0.1", 7781),
            new Linker("127.0.0.1", 7782)

    };

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
        // Create point to point socket to send messages to the linker
        final int pointToPointPort = 1234;
        DatagramSocket pointToPointSocket = new DatagramSocket(pointToPointPort);
        System.out.println("Started the socket!");

        // Setup the service TODO : remove this !
        Service s1 = new Service(1, "192.186.1.1", 7777);
        Service s2 = new Service(2, "192.186.1.1", 7778);
        Service s3 = new Service(2, "192.186.1.1", 7779);
        serviceList.add(s1);
        serviceList.add(s2);
        serviceList.add(s3);

        // Start the linker by synchronising with the other linkers if there are any
        getServiceList(pointToPointSocket);

        // Provide the linker service forever
        while (true) {

            // Receive a packet
            byte[] buffer = new byte[1];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            pointToPointSocket.receive(receivePacket);

            // Forward the packet to the right controller depending on the protocol value
            byte messageType = receivePacket.getData()[0];

            // In case another linker ask for the service list to synchronise
            if (messageType == Protocol.ASK_SERVICE_LIST.ordinal()) {
                sendServiceList(receivePacket, pointToPointSocket);
            }
            else if(messageType == Protocol.ASK_SERVICE.ordinal()){
                sendServiceToClient(receivePacket, pointToPointSocket);
            }
            // In case another linker found out that a service was added
            else if (messageType == Protocol.ADD_SERVICE.ordinal()) {
                addService(receivePacket, pointToPointSocket);
            }
            // In case another linker found out that a service was down
            else if (messageType == Protocol.DELETE_SERVICE.ordinal()) {
                deleteService(receivePacket, pointToPointSocket);
            }
            // In case a client found out that a service was down
            else if (messageType == Protocol.SERVICE_DONT_EXIST.ordinal()) {
                verifExist(receivePacket, pointToPointSocket);
            }
        }
    }

    /**
     * Method executed at start to synchronise with the other linkers.
     *
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    private void getServiceList(DatagramSocket pointToPointSocket) throws InterruptedException, IOException {
        for (Linker linker : linkers) {
            // Ask the linked for service number 0
            DatagramPacket linkerPacket = new DatagramPacket(new byte[]{(byte) Protocol.ASK_SERVICE_LIST.ordinal()}, 1, InetAddress.getByName(linker.getIp()), linker.getPort());
            pointToPointSocket.send(linkerPacket);

            // Receive the service ip and address
            // 1 byte pour le type, 1 pour le nombre , 100 services max
            byte[] buffer = new byte[702];
            DatagramPacket serviceAddressPacket = new DatagramPacket(buffer, buffer.length);
            pointToPointSocket.receive(serviceAddressPacket);
            int type = serviceAddressPacket.getData()[0];
            if (type == (byte) Protocol.RETURN_SERVICES.ordinal()) {
                int nbServices = serviceAddressPacket.getData()[1];
                for (int i = 0; i < nbServices; i++) {
                    int idService = serviceAddressPacket.getData()[2 + 7 * i];
                    InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(serviceAddressPacket.getData(), 3 + i * 7, 7 + i * 7));
                    byte[] portByte = Arrays.copyOfRange(serviceAddressPacket.getData(), 8 + i * 7, 9 + i * 7);
                    int port = ((portByte[0] & 0xff) << 8) | (portByte[1] & 0xff);

                    Service service = new Service(idService, ip.getHostAddress(), port);
                    serviceList.add(service);
                }
            }

            InetAddress serviceAddress = InetAddress.getByAddress(Arrays.copyOfRange(serviceAddressPacket.getData(), 0, 4));

        }
    }

    /**
     * Method used to respond to a linker that asked for the service list.
     *
     * @param serviceAddressPacket
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    private void sendServiceList(DatagramPacket serviceAddressPacket, DatagramSocket pointToPointSocket) throws InterruptedException, IOException {

        DatagramPacket serviceListPacket = new DatagramPacket(new byte[]{(byte) Protocol.RETURN_SERVICES.ordinal()}, 702, InetAddress.getByName(serviceAddressPacket.getAddress().getHostName()), serviceAddressPacket.getPort());
        byte[] IDservice = new byte[1];
        byte[] IP = new byte[4];
        byte[] port = new byte[2];
        int i = 0;
        serviceListPacket.setData(Util.intToBytes(serviceList.size(), 1), 1, 1);
        serviceAddressPacket.setLength(2 + (7 * serviceList.size()));
        for (Service service : serviceList) {
            IDservice = Util.intToBytes(service.getIdService(), IDservice.length);
            IP = InetAddress.getByName(service.getIp()).getAddress();
            port = Util.intToBytes(service.getPort(), port.length);

            serviceListPacket.setData(IDservice, 2 + 7 * i, IDservice.length);
            serviceListPacket.setData(IP, 3 + 7 * i, IP.length);
            serviceListPacket.setData(port, 8 + 7 * i, port.length);
            i++;
        }
        pointToPointSocket.send(serviceListPacket);

    }

    /**
     * Send the service's IP and port to the client when he asks for the service
     *
     * @param serviceNumberPacket
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    public void sendServiceToClient(DatagramPacket serviceNumberPacket, DatagramSocket pointToPointSocket) throws InterruptedException, IOException {
        // Create the packet
        DatagramPacket servicePacket = new DatagramPacket(new byte[]{(byte) Protocol.ADDRESS_SERVICE.ordinal()}, 702, InetAddress.getByName(serviceNumberPacket.getAddress().getHostName()), serviceNumberPacket.getPort());

        // Create the buffers that will be sent
        byte[] idService = new byte[1];
        byte[] ip = new byte[4];
        byte[] port = new byte[2];

        // Set the packet length to
        servicePacket.setLength(8);

        // Get the last used service in the list which corresponds to the service type
        Service service = serviceList.stream().min((a, b) -> a.getLastUse() == null ? -1 : b.getLastUse() == null ? 1 : a.getLastUse().compareTo(b.getLastUse())).get();

        // Convert the service data to bytes
        idService = Util.intToBytes(service.getIdService(), idService.length);
        ip = InetAddress.getByName(service.getIp()).getAddress();
        port = Util.intToBytes(service.getPort(), port.length);

        // Add the service data to the packet
        serviceNumberPacket.setData(idService, 1, idService.length);
        serviceNumberPacket.setData(ip, 2, ip.length);
        serviceNumberPacket.setData(port, 6, port.length);

        // Use the service so next time another service will be chosen if there is another one
        service.use();

        // Send the packet
        pointToPointSocket.send(serviceNumberPacket);
    }

    /**
     * Method used to delete a service from the service list
     *
     * @param addServicePacket
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    public void deleteService(DatagramPacket addServicePacket, DatagramSocket pointToPointSocket) throws InterruptedException, IOException {
        byte[] buffer = new byte[8];
        DatagramPacket recivedNewService = new DatagramPacket(buffer, buffer.length);
        pointToPointSocket.receive(recivedNewService);
        int IDService = recivedNewService.getData()[1];
        byte[] IP = new byte[4];
        byte[] portByte = new byte[2];

        InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(recivedNewService.getData(), 3, 7));
        portByte = Arrays.copyOfRange(recivedNewService.getData(), 8, 9);
        int port = ((portByte[0] & 0xff) << 8) | (portByte[1] & 0xff);

        Service newService = new Service(IDService, ip.getHostAddress(), port);
        //TODO verifier si le remove marche si l'objet passé n'est pas une copie de l'objet a supprimer
        serviceList.remove(newService);
    }

    /**
     * Method used to add a service to the service list. It also sends the info to the other linkers
     * @param addServicePacket
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    public void addService(DatagramPacket addServicePacket, DatagramSocket pointToPointSocket) throws InterruptedException, IOException {
        byte[] buffer = new byte[8];
        DatagramPacket receivedNewService = new DatagramPacket(buffer, buffer.length);
        pointToPointSocket.receive(receivedNewService);
        int IDService = receivedNewService.getData()[1];
        byte[] IP = new byte[4];
        byte[] portByte = new byte[2];

        InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(receivedNewService.getData(), 3, 7));
        portByte = Arrays.copyOfRange(receivedNewService.getData(), 8, 9);
        int port = ((portByte[0] & 0xff) << 8) | (portByte[1] & 0xff);

        Service newService = new Service(IDService, ip.getHostAddress(), port);
        serviceList.add(newService);


    }

    /**
     * This method checks if another service exists, if it doesn't it is deleted from the service list and the info is
     * sent to the other linkers
     *
     * @param addServicePacket
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    public void verifExist(DatagramPacket addServicePacket, DatagramSocket pointToPointSocket) throws InterruptedException, IOException {
        //on lis le packet nous disant que le service XX n'estsite pas
        //on lui send un message

        byte[] buffer = new byte[8];
        DatagramPacket recivedNewService = new DatagramPacket(buffer, buffer.length);
        pointToPointSocket.receive(recivedNewService);
        int IDService = recivedNewService.getData()[1];
        byte[] IP = new byte[4];
        byte[] portByte = new byte[2];

        InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(recivedNewService.getData(), 3, 7));
        portByte = Arrays.copyOfRange(recivedNewService.getData(), 8, 9);
        int port = ((portByte[0] & 0xff) << 8) | (portByte[1] & 0xff);

        Service serviceLost = new Service(IDService, ip.getHostAddress(), port);


        pointToPointSocket.setSoTimeout(10000);
        DatagramPacket checkPacket = new DatagramPacket(new byte[]{(byte) Protocol.CHECK_DONT_EXIST.ordinal()}, 1, InetAddress.getByName(serviceLost.getIp()), serviceLost.getPort());
        pointToPointSocket.send(checkPacket);


        byte[] bufferResponse = new byte[1];
        while (true) {
            DatagramPacket getack = new DatagramPacket(bufferResponse, bufferResponse.length);
            try {
                pointToPointSocket.receive(getack);
                int messageType = getack.getData()[0];
                if (messageType == (byte) Protocol.I_EXIST.ordinal()) {
                    //le service existe. Envoi de la fausse alerte ?
                }
            } catch (SocketTimeoutException e) {
                //TODO delete the service and send the info
                serviceList.remove(serviceLost);
            }

        }


    }

    /**
     *
     */
    public void confirmSub() {
    }


}
