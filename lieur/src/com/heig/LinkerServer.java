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

    // TODO : TRANSLATE TO FRENCH !!

    // TODO: FAIRE DE PROTOCOL UTIL LINKER SERVICE UN LIBRAIRIE PARTAGée

    // List of the other linkers TODO : remove the values and set it with the args
    final Linker[] linkers = {
            new Linker("127.0.0.1", 7780),
            new Linker("127.0.0.1", 7781),
            new Linker("127.0.0.1", 7782)
    };

    final int pointToPointPort = 1234; // TODO : Make this an argument to the main

    /**
     * Creates a new linker which will listen on the specified port and will synchronise with the specified linkers.
     * TODO : Ajouter les arguments (liste de linkers et port sur lequel on écoute
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void LinkerServer(){

    }

    /**
     * Starts the linker. It will begin by synchronising with another linker than it will answer to requests
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void start() throws IOException, InterruptedException {
        // Create point to point socket to send messages to the linker
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
            byte[] buffer = new byte[702];
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
                addService(receivePacket);
            }
            // In case another linker found out that a service was down
            else if (messageType == Protocol.DELETE_SERVICE.ordinal()) {
                deleteService(receivePacket);
            }
            // In case a client found out that a service was down
            else if (messageType == Protocol.SERVICE_DONT_EXIST.ordinal()) {
                verifExists(receivePacket, pointToPointSocket);
            }
            // In case a server tries to subscribe to the client
            else if (messageType == Protocol.SUB.ordinal()) {
                serviceSubscribe(receivePacket, pointToPointSocket);
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
    private void getServiceList(DatagramSocket pointToPointSocket) throws IOException {
        for (Linker linker : linkers) {
            // Ask the linked for service number 0
            DatagramPacket linkerPacket = new DatagramPacket(new byte[]{(byte) Protocol.ASK_SERVICE_LIST.ordinal()}, 1, InetAddress.getByName(linker.getIp()), linker.getPort());
            pointToPointSocket.send(linkerPacket);

            // Receive the service ip and address
            // 1 byte pour le type, 1 pour le nombre , 100 services max
            byte[] buffer = new byte[702];
            DatagramPacket serviceListAddressPacket = new DatagramPacket(buffer, buffer.length);

            // Set timeout and receive service list
            try {
                // TODO : UTILISER UN AUTRE PORT
                pointToPointSocket.setSoTimeout(2000);
                pointToPointSocket.receive(serviceListAddressPacket);
            } catch (SocketTimeoutException e) {
                continue;
            }

            // Check the type of the response
            int type = serviceListAddressPacket.getData()[0];
            if (type == (byte) Protocol.RETURN_SERVICES.ordinal()) {

                // Add the received services to the service list
                int nbServices = serviceListAddressPacket.getData()[1];
                for (int i = 0; i < nbServices; i++) {
                    int idService = serviceListAddressPacket.getData()[2 + 7 * i];
                    InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(serviceListAddressPacket.getData(), 3 + i * 7, 7 + i * 7));
                    byte[] portByte = Arrays.copyOfRange(serviceListAddressPacket.getData(), 8 + i * 7, 9 + i * 7);
                    int port = ((portByte[0] & 0xff) << 8) | (portByte[1] & 0xff);

                    Service service = new Service(idService, ip.getHostAddress(), port);
                    serviceList.add(service);
                }
                break;
            }
        }
        // Set the timeout back to infinite
        pointToPointSocket.setSoTimeout(0);
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
        // Prepare packet containing the service list
        DatagramPacket serviceListPacket = new DatagramPacket(new byte[]{(byte) Protocol.RETURN_SERVICES.ordinal()}, 702, InetAddress.getByName(serviceAddressPacket.getAddress().getHostName()), serviceAddressPacket.getPort());

        // Set length and number of services for the packet
        serviceAddressPacket.setLength(2 + (7 * serviceList.size()));
        serviceListPacket.setData(Util.intToBytes(serviceList.size(), 1), 1, 1);

        // Add every service to the packet
        int i = 0;
        for (Service service : serviceList) {
            // Retrieve service data
            byte[] idService = Util.intToBytes(service.getIdService(), 1);
            byte[] ip = InetAddress.getByName(service.getIp()).getAddress();
            byte[] port = Util.intToBytes(service.getPort(), 2);

            // Set the service data in the packet
            serviceListPacket.setData(idService, 2 + 7 * i, idService.length);
            serviceListPacket.setData(ip, 3 + 7 * i, ip.length);
            serviceListPacket.setData(port, 8 + 7 * i, port.length);

            i++;
        }

        // Send the packet
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
        DatagramPacket servicePacket = new DatagramPacket(new byte[]{(byte) Protocol.ADDRESS_SERVICE.ordinal()}, 8, InetAddress.getByName(serviceNumberPacket.getAddress().getHostName()), serviceNumberPacket.getPort());

        // Set the packet length to
        servicePacket.setLength(8);

        // Get the last used service in the list which corresponds to the service type
        // TODO : Récupérer le service avec l'id de service correspondant
        // TODO : Si le service existe pas retourner service_not_exist
        Service service = serviceList.stream().min((a, b) -> a.getLastUse() == null ? -1 : b.getLastUse() == null ? 1 : a.getLastUse().compareTo(b.getLastUse())).get();

        // Convert the service data to bytes
        byte[] idService = Util.intToBytes(service.getIdService(), 1);
        byte[] ip = InetAddress.getByName(service.getIp()).getAddress();
        byte[] port = Util.intToBytes(service.getPort(), 2);

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
     * @param deleteServicePacket
     * @throws InterruptedException
     * @throws IOException
     */
    public void deleteService(DatagramPacket deleteServicePacket) throws InterruptedException, IOException {
        // Retrieve data from packet
        int IDService = deleteServicePacket.getData()[1];
        InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(deleteServicePacket.getData(), 3, 7));
        byte[] portByte = Arrays.copyOfRange(deleteServicePacket.getData(), 8, 9);
        int port = ((portByte[0] & 0xff) << 8) | (portByte[1] & 0xff);

        // Delete the service from the service list if it exists
        Service newService = new Service(IDService, ip.getHostAddress(), port);
        //TODO verifier si le remove marche si l'objet passé n'est pas une copie de l'objet a supprimer
        serviceList.remove(newService);
    }

    /**
     * Method used to add a service to the service list. It also sends the info to the other linkers
     * @param addServicePacket
     * @throws InterruptedException
     * @throws IOException
     */
    public void addService(DatagramPacket addServicePacket) throws InterruptedException, IOException {
        // Retrieve data from packet
        int idService = addServicePacket.getData()[1];
        InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(addServicePacket.getData(), 3, 7));
        byte[] portByte = Arrays.copyOfRange(addServicePacket.getData(), 8, 9);
        int port = ((portByte[0] & 0xff) << 8) | (portByte[1] & 0xff);

        // Add the new service if it doesn't exist
        Service newService = new Service(idService, ip.getHostAddress(), port);
        // TODO : Verifier si contains marche bien
        if(!serviceList.contains(newService)) {
            serviceList.add(newService);
        }
    }

    /**
     * This method checks if another service exists, if it doesn't it is deleted from the service list and the info is
     * sent to the other linkers
     *
     * @param serviceNotExistPacket
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    public void verifExists(DatagramPacket serviceNotExistPacket, DatagramSocket pointToPointSocket) throws InterruptedException, IOException {
        // Retrieve service from packet
        int idService = serviceNotExistPacket.getData()[1];
        InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(serviceNotExistPacket.getData(), 2, 6));
        byte[] portByte = Arrays.copyOfRange(serviceNotExistPacket.getData(), 6, 8);
        int port = ((portByte[0] & 0xff) << 8) | (portByte[1] & 0xff);

        // Send a packet to the service to check if it exists
        Service serviceNotReachable = new Service(idService, ip.getHostAddress(), port);
        DatagramPacket checkPacket = new DatagramPacket(new byte[]{(byte) Protocol.CHECK_DONT_EXIST.ordinal()}, 1, InetAddress.getByName(serviceNotReachable.getIp()), serviceNotReachable.getPort());
        pointToPointSocket.send(checkPacket);

        byte[] bufferResponse = new byte[1];
        try{
            // Receive the response from the service
            // TODO : UTILISER UN AUTRE PORT
            DatagramPacket serviceResponsePacket = new DatagramPacket(bufferResponse, bufferResponse.length);
            pointToPointSocket.setSoTimeout(2000);
            pointToPointSocket.receive(serviceResponsePacket);

            // If wrong type, delete service
            int messageType = serviceResponsePacket.getData()[0];
            if (messageType != (byte) Protocol.I_EXIST.ordinal()) {
                removeServiceAndNotifyLinkers(serviceNotReachable, pointToPointSocket);
            }
        } catch (SocketTimeoutException e) {
            removeServiceAndNotifyLinkers(serviceNotReachable, pointToPointSocket);
        }

        // Reset the timeout to infinite
        pointToPointSocket.setSoTimeout(0);

    }

    public void removeServiceAndNotifyLinkers(Service service, DatagramSocket pointToPointSocket) throws IOException {
        serviceList.remove(service);

        for(Linker linker : linkers) {
            // Create the packet to add the service
            DatagramPacket servicePacket = new DatagramPacket(new byte[]{(byte) Protocol.DELETE_SERVICE.ordinal()}, 8, InetAddress.getByName(linker.getIp()), linker.getPort());

            // Set the packet length to
            servicePacket.setLength(8);

            // Add the service data to the packet
            servicePacket.setData(Util.intToBytes(service.getIdService(), 1), 1, 1);
            servicePacket.setData(InetAddress.getByName(service.getIp()).getAddress(), 2, 4);
            servicePacket.setData(Util.intToBytes(service.getPort(), 2), 6, 2);

            // Send the packet
            pointToPointSocket.send(servicePacket);
        }
    }

    /**
     * Method used to add a service when it subscribes
     *
     * @param subscribeServicePacket
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    public void serviceSubscribe(DatagramPacket subscribeServicePacket, DatagramSocket pointToPointSocket) throws InterruptedException, IOException {
        // Retrieve the data from the packet
        int idService = subscribeServicePacket.getData()[1];
        InetAddress ip = subscribeServicePacket.getAddress();
        int port = subscribeServicePacket.getPort();

        // Create the service and add it to the list
        Service newService = new Service(idService, ip.getHostAddress(), port);
        serviceList.add(newService);

        // Add the service on every other linkers
        for(Linker linker : linkers) {
            // Create the packet to add the service
            DatagramPacket servicePacket = new DatagramPacket(new byte[]{(byte) Protocol.ADD_SERVICE.ordinal()}, 8, InetAddress.getByName(linker.getIp()), linker.getPort());

            // Set the packet length to
            servicePacket.setLength(8);

            // Add the service data to the packet
            servicePacket.setData(Util.intToBytes(idService, 1), 1, 1);
            servicePacket.setData(ip.getAddress(), 2, 4);
            servicePacket.setData(Util.intToBytes(port, 2), 6, 2);

            // Send the packet
            pointToPointSocket.send(servicePacket);
        }

        // Confirm subscription of service
        DatagramPacket confirmSubPacket = new DatagramPacket(new byte[]{(byte) Protocol.CONFIRM_SUB.ordinal()}, 1, InetAddress.getByName(subscribeServicePacket.getAddress().getHostAddress()), subscribeServicePacket.getPort());

        // Send the packet
        pointToPointSocket.send(confirmSubPacket);
    }
}
