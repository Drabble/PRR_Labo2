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

            if(!serviceList.isEmpty())
            System.out.print(serviceList.get(0).getIp());
            byte[] buffer = new byte[702];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            pointToPointSocket.receive(receivePacket);
            System.out.println("while2");



            //byte deffinissant le type de message
            byte messageType = receivePacket.getData()[0];

            System.out.println(messageType);



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
        // creation du paquet
        DatagramPacket servicePacket = new DatagramPacket(new byte[]{(byte) Protocol.REPONSE_DEMANDE_DE_SERVICE.ordinal()}, 8, InetAddress.getByName(serviceNumberPacket.getAddress().getHostName()), serviceNumberPacket.getPort());

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

            // met a jour la date de dernière utilisation du service
            service.use();
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
        InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(deleteServicePacket.getData(), 3, 7));
        byte[] portByte = Arrays.copyOfRange(deleteServicePacket.getData(), 8, 9);
        int port = ((portByte[0] & 0xff) << 8) | (portByte[1] & 0xff);

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
        InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(addServicePacket.getData(), 3, 7));
        byte[] portByte = Arrays.copyOfRange(addServicePacket.getData(), 8, 9);
        int port = ((portByte[0] & 0xff) << 8) | (portByte[1] & 0xff);

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
                removeServiceAndNotifyLinkers(serviceNotReachable, pointToPointSocket);
            }
        } catch (SocketTimeoutException e) {
            removeServiceAndNotifyLinkers(serviceNotReachable, pointToPointSocket);
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

        for(Linker linker : linkers) {
            // creation du paquet
            DatagramPacket servicePacket = new DatagramPacket(new byte[]{(byte) Protocol.DELETE_SERVICE.ordinal()}, 8, InetAddress.getByName(linker.getIp()), linker.getPort());

            servicePacket.setLength(8);

            // Ajout de data
            servicePacket.setData(Util.intToBytes(service.getIdService(), 1), 1, 1);
            servicePacket.setData(InetAddress.getByName(service.getIp()).getAddress(), 2, 4);
            servicePacket.setData(Util.intToBytes(service.getPort(), 2), 6, 2);

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

        // information aux autres lieurs
        for(Linker linker : linkers) {
            // creation du paquet
            DatagramPacket servicePacket = new DatagramPacket(new byte[]{(byte) Protocol.AJOUT_SERVICE.ordinal()}, 8, InetAddress.getByName(linker.getIp()), linker.getPort());

            // ajout des datas
            servicePacket.setData(Util.intToBytes(idService, 1), 1, 1);
            servicePacket.setData(ip.getAddress(), 2, 4);
            servicePacket.setData(Util.intToBytes(port, 2), 6, 2);

            // envoi du paquet
            pointToPointSocket.send(servicePacket);
        }

        // creation du paquet de confirmation d'abonnement
        DatagramPacket confirmSubPacket = new DatagramPacket(new byte[]{(byte) Protocol.CONFIRMATION_ABONNEMENT.ordinal()}, 1, InetAddress.getByName(subscribeServicePacket.getAddress().getHostAddress()), subscribeServicePacket.getPort());

        // envoi du paquet
        pointToPointSocket.send(confirmSubPacket);
    }
}
