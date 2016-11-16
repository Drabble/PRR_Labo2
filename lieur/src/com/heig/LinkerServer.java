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
 * Le lieur permet de faire le lien entre les services et les client. Il contient la liste des services actifs
 * Lors du démarage il va demander à une lieur de sa liste la liste de services actifs de ce dernier.
 * Une fois démarré, il serra possible aux services qui démarré d'indiquer leur existance au lieur, le lieur
 * informera par la suite l'existance de ce service aux autres lieurs.
 * Il a aussi pour tache de verfier si un service est toujours actif si un client se pleind.
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
     * demarrage du lieur. Au démarrage le lieur va questionner les autres lieur un à un pour obtenir une liste de
     * service. Il va aussi répondre aux requetes des clients
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void start() throws IOException, InterruptedException {
        // Creration d'une connexion pointToPoint
        DatagramSocket pointToPointSocket = new DatagramSocket(pointToPointPort);
        System.out.println("Started the socket!");

        // Setup the service TODO : remove this !
        Service s1 = new Service(1, "192.186.1.1", 7777);
        Service s2 = new Service(2, "192.186.1.1", 7778);
        Service s3 = new Service(2, "192.186.1.1", 7779);
        serviceList.add(s1);
        serviceList.add(s2);
        serviceList.add(s3);

        // démarrage du lieur et syncronisation avec les autre lieur
        getServiceList(pointToPointSocket);


        while (true) {

            //paquet reçu, taille max de 702 (le message le plus grand est celui d'envoi de la liste de services
            // un byte pour le type de message
            // un pour le nombre de service,
            // 7 par service avec un max de 100 services)
            byte[] buffer = new byte[702];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            pointToPointSocket.receive(receivePacket);

            //byte deffinissant le type de message
            byte messageType = receivePacket.getData()[0];

            //si le message reçu est une demande de liste de services d'un lieur (lieur -> lieur)
            if (messageType == Protocol.ASK_SERVICE_LIST.ordinal()) {
                sendServiceList(receivePacket, pointToPointSocket);
            }
            //si le message est une demande de service d'un client (client -> lieur)
            else if(messageType == Protocol.ASK_SERVICE.ordinal()){
                sendServiceToClient(receivePacket, pointToPointSocket);
            }
            // ajout d'un nouveau service de la part d'un lieur (lieur -> lieur)
            else if (messageType == Protocol.ADD_SERVICE.ordinal()) {
                addService(receivePacket);
            }
            // suppression d'un service (lieur -> lieur)
            else if (messageType == Protocol.DELETE_SERVICE.ordinal()) {
                deleteService(receivePacket);
            }
            // si un client n'a pas trouvé le service ( client -> lieur )
            else if (messageType == Protocol.SERVICE_DONT_EXIST.ordinal()) {
                verifExists(receivePacket, pointToPointSocket);
            }
            // si un service veut s'abonner à un lieur
            else if (messageType == Protocol.SUB.ordinal()) {
                serviceSubscribe(receivePacket, pointToPointSocket);
            }
        }
    }

    /**
     * Methode qui permet la sycronisation du nouveau lieur
     *
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    private void getServiceList(DatagramSocket pointToPointSocket) throws IOException {
        for (Linker linker : linkers) {
            // creation du parquet de demande
            DatagramPacket linkerPacket = new DatagramPacket(new byte[]{(byte) Protocol.ASK_SERVICE_LIST.ordinal()}, 1, InetAddress.getByName(linker.getIp()), linker.getPort());
            pointToPointSocket.send(linkerPacket);

            // 1 byte pour le type, 1 pour le nombre , 100 services max
            byte[] buffer = new byte[702];
            DatagramPacket serviceListAddressPacket = new DatagramPacket(buffer, buffer.length);

            // Set timeout and receive service list
            // definition d'un time out et reception de la liste des services. Si un lieur met plus de 2sec pour
            // répondre on passe au lieur suivant
            try {
                // TODO : UTILISER UN AUTRE PORT
                pointToPointSocket.setSoTimeout(2000);
                pointToPointSocket.receive(serviceListAddressPacket);
            } catch (SocketTimeoutException e) {
                continue;
            }

            // verification si le parquet est bien une réponse contant la liste des services
            int type = serviceListAddressPacket.getData()[0];
            if (type == (byte) Protocol.RETURN_SERVICES.ordinal()) {
                // ajout des nouveaux services dans la liste
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
        //redefinition du time out
        pointToPointSocket.setSoTimeout(0);
    }

    /**
     * Methode de réponse à un lieur qui a demandé une liste de service
     *
     * @param serviceAddressPacket
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    private void sendServiceList(DatagramPacket serviceAddressPacket, DatagramSocket pointToPointSocket) throws InterruptedException, IOException {
        // construction du paquet
        DatagramPacket serviceListPacket = new DatagramPacket(new byte[]{(byte) Protocol.RETURN_SERVICES.ordinal()}, 702, InetAddress.getByName(serviceAddressPacket.getAddress().getHostName()), serviceAddressPacket.getPort());

        // definition de la taille du paquet (2 + (le nombre de service * 7))
        serviceAddressPacket.setLength(2 + (7 * serviceList.size()));
        serviceListPacket.setData(Util.intToBytes(serviceList.size(), 1), 1, 1);

        // ajout des services au paquet
        int i = 0;
        for (Service service : serviceList) {
            // Retrieve service data
            byte[] idService = Util.intToBytes(service.getIdService(), 1);
            byte[] ip = InetAddress.getByName(service.getIp()).getAddress();
            byte[] port = Util.intToBytes(service.getPort(), 2);

            serviceListPacket.setData(idService, 2 + 7 * i, idService.length);
            serviceListPacket.setData(ip, 3 + 7 * i, ip.length);
            serviceListPacket.setData(port, 8 + 7 * i, port.length);

            i++;
        }
        // envoi du paquet
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
        // creation du paquet
        DatagramPacket servicePacket = new DatagramPacket(new byte[]{(byte) Protocol.ADDRESS_SERVICE.ordinal()}, 8, InetAddress.getByName(serviceNumberPacket.getAddress().getHostName()), serviceNumberPacket.getPort());

        // recupère le service qui a été utilisé le moin récemment
        // TODO : Récupérer le service avec l'id de service correspondant
        Service service = serviceList.stream().min((a, b) -> a.getLastUse() == null ? -1 : b.getLastUse() == null ? 1 : a.getLastUse().compareTo(b.getLastUse())).get();
        if(service == null)
        {
            servicePacket = new DatagramPacket(new byte[]{(byte) Protocol.SERVICE_EXISTE_PAS.ordinal()}, 1, InetAddress.getByName(serviceNumberPacket.getAddress().getHostName()), serviceNumberPacket.getPort());

        }else {
            byte[] idService = Util.intToBytes(service.getIdService(), 1);
            byte[] ip = InetAddress.getByName(service.getIp()).getAddress();
            byte[] port = Util.intToBytes(service.getPort(), 2);

            servicePacket.setData(idService, 1, idService.length);
            servicePacket.setData(ip, 2, ip.length);
            servicePacket.setData(port, 6, port.length);

            // Use the service so next time another service will be chosen if there is another one
            // met a jour la date de dernière utilisation du service
            service.use();
        }
        // envoi du paquet
        pointToPointSocket.send(servicePacket);
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
