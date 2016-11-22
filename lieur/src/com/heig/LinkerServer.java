/**
 * Project: Labo002
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
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
            new Linker("127.0.0.1",12349)
    };

    final int pointToPointPort = 12349; // TODO : Make this an argument to the main

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
        /*Service s1 = new Service(1, "192.186.1.1", 7777);
        Service s2 = new Service(2, "192.186.1.1", 7778);
        Service s3 = new Service(2, "192.186.1.1", 7779);
        serviceList.add(s1);
        serviceList.add(s2);
        serviceList.add(s3);*/

        // démarrage du lieur et syncronisation avec les autre lieur
        getServiceList(pointToPointSocket);


        while (true) {

            //paquet reçu, taille max de 702 (le message le plus grand est celui d'envoi de la liste de services
            // un byte pour le type de message
            // un pour le nombre de service,
            // 7 par service avec un max de 100 services)
            System.out.println("while");

            if(!serviceList.isEmpty()) {
                System.out.println("IP" + serviceList.get(0).getIp());
                System.out.println("service id " + serviceList.get(0).getIdService());
                System.out.println("port d'ecoute " + serviceList.get(0).getPort());
            }
            byte[] buffer = new byte[702];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            pointToPointSocket.receive(receivePacket);
            System.out.println("while2");



            //byte deffinissant le type de message
            byte messageType = receivePacket.getData()[0];
            byte service = receivePacket.getData()[1];
            System.out.println(" message type " + messageType);
            System.out.println("service id " + service);


            //si le message reçu est une demande de liste de services d'un lieur (lieur -> lieur)
            if (messageType == Protocol.DEMANDE_DE_LISTE_DE_SERVICES.ordinal()) {
                sendServiceList(receivePacket, pointToPointSocket);
            }
            //si le message est une demande de service d'un client (client -> lieur)
            else if(messageType == Protocol.DEMANDE_DE_SEVICE.ordinal()){
                sendServiceToClient(receivePacket, pointToPointSocket);
            }
            // ajout d'un nouveau service de la part d'un lieur (lieur -> lieur)
            else if (messageType == Protocol.AJOUT_SERVICE.ordinal()) {
                addService(receivePacket);
            }
            // suppression d'un service (lieur -> lieur)
            else if (messageType == Protocol.DELETE_SERVICE.ordinal()) {
                deleteService(receivePacket);
            }
            // si un client n'a pas trouvé le service ( client -> lieur )
            else if (messageType == Protocol.SERVICE_EXISTE_PAS.ordinal()) {
                verifExists(receivePacket, pointToPointSocket);
            }
            // si un service veut s'abonner à un lieur
            else if (messageType == Protocol.ABONNEMENT.ordinal()) {
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
            DatagramPacket linkerPacket = new DatagramPacket(new byte[]{(byte) Protocol.DEMANDE_DE_LISTE_DE_SERVICES.ordinal()}, 1, InetAddress.getByName(linker.getIp()), linker.getPort());
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
            if (type == (byte) Protocol.REPONSE_DEMANDE_LISTE_DE_SERVICES.ordinal()) {
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
        System.out.println("waiting");
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
        DatagramPacket serviceListPacket = new DatagramPacket(new byte[]{(byte) Protocol.REPONSE_DEMANDE_LISTE_DE_SERVICES.ordinal()}, 702, InetAddress.getByName(serviceAddressPacket.getAddress().getHostName()), serviceAddressPacket.getPort());

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
     * Envoie l'IP et le port d'un service au client qui a effectué une requete
     *
     * @param serviceNumberPacket
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    public void sendServiceToClient(DatagramPacket serviceNumberPacket, DatagramSocket pointToPointSocket) throws InterruptedException, IOException {


        DatagramPacket servicePacket;

        // recupère le service qui a été utilisé le moin récemment
        // TODO : Récupérer le service avec l'id de service correspondant
        if(!serviceList.isEmpty()) {
            Service service = serviceList.stream().min((a, b) -> a.getLastUse() == null ? -1 : b.getLastUse() == null ? 1 : a.getLastUse().compareTo(b.getLastUse())).get();
            if (service == null) {
                servicePacket = new DatagramPacket(new byte[]{(byte) Protocol.SERVICE_EXISTE_PAS.ordinal()}, 1, InetAddress.getByName(serviceNumberPacket.getAddress().getHostName()), serviceNumberPacket.getPort());

            } else {
                byte[] idService = Util.intToBytes(service.getIdService(), 1);
                byte[] ip = InetAddress.getByName(service.getIp()).getAddress();
                byte[] port = Util.intToBytes(service.getPort(), 2);

                byte[] tosend = new byte[8];
                tosend[0] = (byte) Protocol.REPONSE_DEMANDE_DE_SERVICE.ordinal();
                tosend[1] = (byte) service.getIdService();
                tosend[2] = ip[0];
                tosend[3] = ip[1];
                tosend[4] = ip[2];
                tosend[5] = ip[3];

                System.out.println("send p" + port[0]);
                System.out.println("send p" + port[1]);
                tosend[6] = port[0];
                tosend[7] = port[1];

                System.out.println("send" + service.getPort());
                servicePacket = new DatagramPacket(tosend, 8, InetAddress.getByName(serviceNumberPacket.getAddress().getHostName()), serviceNumberPacket.getPort());
                System.out.println("send datagram");
                service.use();
            }
        }
        else
        {
            servicePacket = new DatagramPacket(new byte[]{(byte) Protocol.SERVICE_EXISTE_PAS.ordinal()}, 1, InetAddress.getByName(serviceNumberPacket.getAddress().getHostName()), serviceNumberPacket.getPort());
        }
        // envoi du paquet
        pointToPointSocket.send(servicePacket);
    }

    /**
     * Methode permetant d'effacer un service de la liste (lieur -> lieur)
     *
     * @param deleteServicePacket
     * @throws InterruptedException
     * @throws IOException
     */
    public void deleteService(DatagramPacket deleteServicePacket) throws InterruptedException, IOException {
        int IDService = deleteServicePacket.getData()[1];
        InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(deleteServicePacket.getData(), 2, 6));
        byte[] portByte = new byte[2];
        portByte[0] = deleteServicePacket.getData()[7];
        portByte[1] = deleteServicePacket.getData()[6];
        int port = new BigInteger(portByte).intValue();

        Service newService = new Service(IDService, ip.getHostAddress(), port);
        serviceList.remove(newService);
    }

    /**
     * Methode qui ajoute un service à la liste (lieur -> lieur)
     *
     * @param addServicePacket
     * @throws InterruptedException
     * @throws IOException
     */
    public void addService(DatagramPacket addServicePacket) throws InterruptedException, IOException {
        // Retrieve data from packet
        int idService = addServicePacket.getData()[1];
        InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(addServicePacket.getData(), 2, 6));
        byte[] portByte = new byte[2];
        portByte[0] = addServicePacket.getData()[7];
        portByte[1] = addServicePacket.getData()[6];
        int port = new BigInteger(portByte).intValue();

        // Ajoute le service à la liste s'il n'existe pas deja

        System.out.println("ajout d'un service");
        Service newService = new Service(idService, ip.getHostAddress(), port);
        // TODO : Verifier si contains marche bien
        if(!serviceList.contains(newService)) {
            serviceList.add(newService);
        }
    }

    /**
     * Methode qui va verifier si un service et bien indisponible, si c'est le cas, il le supprime et informe les autres
     * lieur de supprimer ce service.
     *
     * @param serviceNotExistPacket
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    public void verifExists(DatagramPacket serviceNotExistPacket, DatagramSocket pointToPointSocket) throws InterruptedException, IOException {

        System.out.println("verif");
        // Retrieve service from packet
        int idService = serviceNotExistPacket.getData()[1];
        InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(serviceNotExistPacket.getData(), 2, 6));
        byte[] portByte = Arrays.copyOfRange(serviceNotExistPacket.getData(), 6, 8);
        int port = ((portByte[0] & 0xff) << 8) | (portByte[1] & 0xff);

        // Envoie un paquet au service que le client n'a pas pu joindre
        Service serviceNotReachable = new Service(idService, ip.getHostAddress(), port);
        DatagramPacket checkPacket = new DatagramPacket(new byte[]{(byte) Protocol.VERIFIE_N_EXISTE_PAS.ordinal()}, 1, InetAddress.getByName(serviceNotReachable.getIp()), serviceNotReachable.getPort());
        pointToPointSocket.send(checkPacket);

        byte[] bufferResponse = new byte[1];
        try{
            // si nous avons eu une reponse dans les deux seconde, le service existe toujours, si non
            // on le supprime et notifie les autres lieurs
            // TODO : UTILISER UN AUTRE PORT
            DatagramPacket serviceResponsePacket = new DatagramPacket(bufferResponse, bufferResponse.length);
            pointToPointSocket.setSoTimeout(2000);
            pointToPointSocket.receive(serviceResponsePacket);

            // If wrong type, delete service
            int messageType = serviceResponsePacket.getData()[0];
            if (messageType != (byte) Protocol.J_EXISTE.ordinal()) {
                System.out.println("throw");
                DatagramPacket sayNotExist = new DatagramPacket(new byte[]{(byte) Protocol.SERVICE_EXISTE_PAS.ordinal()}, 1, serviceNotExistPacket.getAddress(), serviceNotExistPacket.getPort());
                removeServiceAndNotifyLinkers(serviceNotReachable, pointToPointSocket);
                pointToPointSocket.send(sayNotExist);
            }
        } catch (SocketTimeoutException e) {
            System.out.println("throw");
            DatagramPacket sayNotExist = new DatagramPacket(new byte[]{(byte) Protocol.SERVICE_EXISTE_PAS.ordinal()}, 1, serviceNotExistPacket.getAddress(), serviceNotExistPacket.getPort());
            removeServiceAndNotifyLinkers(serviceNotReachable, pointToPointSocket);
            pointToPointSocket.send(sayNotExist);
        }

        // redefinition du time out à l'infinie
        pointToPointSocket.setSoTimeout(0);

    }

    /**
     * methode qui supprime le service de la liste de service et informe les autres lieurs
     *
     * @param service,
     * @param pointToPointSocket
     * @throws IOException
     */
    public void removeServiceAndNotifyLinkers(Service service, DatagramSocket pointToPointSocket) throws IOException {
        serviceList.remove(service);
        byte tosend[] = new byte[8];

        byte[] ip = InetAddress.getByName(service.getIp()).getAddress();
        byte[] port = Util.intToBytes(service.getPort(), 2);

        tosend[0] = (byte) Protocol.DELETE_SERVICE.ordinal();
        tosend[1] = (byte) service.getIdService();
        tosend[2] = ip[0];
        tosend[3] = ip[1];
        tosend[4] = ip[2];
        tosend[5] = ip[3];
        tosend[6] = port[0];
        tosend[7] = port[1];

        for(Linker linker : linkers) {
            // creation du paquet
            DatagramPacket servicePacket = new DatagramPacket(tosend, 8, InetAddress.getByName(linker.getIp()), linker.getPort());
            // envoi du paquet
            pointToPointSocket.send(servicePacket);
        }
    }

    /**
     * Method used to add a service when it subscribes
     * Methode d'ajout d'un nouveau service, information aux autres lieurs de l'existance de ce nouveau service,
     * confirmation au service qu'il a bien été ajouté.
     *
     * @param subscribeServicePacket
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    public void serviceSubscribe(DatagramPacket subscribeServicePacket, DatagramSocket pointToPointSocket) throws InterruptedException, IOException {
        // recuperation des datas du parquet
        int idService = subscribeServicePacket.getData()[1];
        InetAddress ip = subscribeServicePacket.getAddress();
        int port = subscribeServicePacket.getPort();

        // creation du service et ajout a la liste
        Service newService = new Service(idService, ip.getHostAddress(), port);
        serviceList.add(newService);

        byte tosend[] = new byte[8];

        byte[] ipByte = InetAddress.getByName(newService.getIp()).getAddress();
        byte[] portbyte = Util.intToBytes(newService.getPort(), 2);

        tosend[0] = (byte) Protocol.AJOUT_SERVICE.ordinal();
        tosend[1] = (byte) newService.getIdService();
        tosend[2] = ipByte[0];
        tosend[3] = ipByte[1];
        tosend[4] = ipByte[2];
        tosend[5] = ipByte[3];
        tosend[6] = portbyte[0];
        tosend[7] = portbyte[1];
        // information aux autres lieurs
        for(Linker linker : linkers) {
            // creation du paquet
            DatagramPacket servicePacket = new DatagramPacket(tosend, 8, InetAddress.getByName(linker.getIp()), linker.getPort());
            // envoi du paquet
            System.out.println("notfy all other linker");
            pointToPointSocket.send(servicePacket);
        }

        // creation du paquet de confirmation d'abonnement
        DatagramPacket confirmSubPacket = new DatagramPacket(new byte[]{(byte) Protocol.CONFIRMATION_ABONNEMENT.ordinal()}, 1, InetAddress.getByName(subscribeServicePacket.getAddress().getHostAddress()), subscribeServicePacket.getPort());

        // envoi du paquet
        pointToPointSocket.send(confirmSubPacket);
    }
}
