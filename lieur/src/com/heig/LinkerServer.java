/**
 * Project: Labo002
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Le lieur permet de faire le lien entre les services et les client. Il contient la liste des services actifs
 * Lors du démarage il va demander à une lieur de sa liste la liste de services actifs de ce dernier.
 * Une fois démarré, il serra possible aux services qui démarré d'indiquer leur existance au lieur, le lieur
 * informera par la suite l'existance de ce service aux autres lieurs.
 * Il a aussi pour tache de verfier si un service est toujours actif si un client se pleind.
 */
public class LinkerServer {
    // List of the services
    List<Service> services = new ArrayList<>();

    // TODO : TRANSLATE TO FRENCH !!

    // TODO: FAIRE DE PROTOCOL UTIL LINKER SERVICE UN LIBRAIRIE PARTAGée

    // List of the other linkers TODO : remove the values and set it with the args
    final Linker[] linkers = {
            new Linker("127.0.0.1", 1234)
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
     * Démarrage du lieur. Au démarrage le lieur va questionner les autres lieur un à un pour obtenir une liste de
     * service. Il va aussi répondre aux requetes des clients
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void demarrer() throws IOException, InterruptedException {
        // Création d'une connexion pointToPoint
        DatagramSocket pointToPointSocket = new DatagramSocket(pointToPointPort);
        System.out.println("Started the socket!");

        // Démarrage du lieur et syncronisation avec les autre lieur
        recupererListeServices(pointToPointSocket);

        // Traitement de toutes les requêtes reçues
        while (true) {
            System.out.println("Attente d'une nouvelle demande...");

            // TODO : supprimer ce bout
            if(!services.isEmpty()) {
                System.out.println("IP" + services.get(0).getIp());
                System.out.println("service id " + services.get(0).getIdService());
                System.out.println("port d'ecoute " + services.get(0).getPort());
            }

            // Réception de la requête, taille max de 702 (le message le plus grand est celui d'envoi de la liste de services,
            // un byte pour le type de message, un pour le nombre de service et 7 par service avec un max de 100 services)
            // TODO : est-ce que ça sert à quelque chose le 702 ? comme c'est juste pour la récupération de la liste des services
            // dans la méthode recupererListeService
            byte[] buffer = new byte[702];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            pointToPointSocket.receive(receivePacket);
            System.out.println("Nouvelle demande recue");

            // Récupération du type de message
            byte messageType = receivePacket.getData()[0];
            byte service = receivePacket.getData()[1]; // TODO : what if service not specified ?? à enlever
            System.out.println("Message type " + messageType);
            System.out.println("Service id " + service);

            // Si le message reçu est une demande de liste de services d'un lieur (lieur -> lieur)
            if (messageType == Protocol.DEMANDE_DE_LISTE_DE_SERVICES.ordinal()) {
                envoiListeServices(receivePacket, pointToPointSocket);
            }
            // Si le message est une demande de service d'un client (client -> lieur)
            else if(messageType == Protocol.DEMANDE_DE_SEVICE.ordinal()){
                envoiServiceAuClient(receivePacket, pointToPointSocket);
            }
            // Ajout d'un nouveau service de la part d'un lieur (lieur -> lieur)
            else if (messageType == Protocol.AJOUT_SERVICE.ordinal()) {
                addService(receivePacket);
            }
            // Suppression d'un service (lieur -> lieur)
            else if (messageType == Protocol.DELETE_SERVICE.ordinal()) {
                deleteService(receivePacket);
            }
            // Si un client n'a pas trouvé le service ( client -> lieur )
            else if (messageType == Protocol.SERVICE_EXISTE_PAS.ordinal()) {
                verifExists(receivePacket, pointToPointSocket);
            }
            // Si un service veut s'abonner à un lieur
            else if (messageType == Protocol.ABONNEMENT.ordinal()) {
                serviceSubscribe(receivePacket, pointToPointSocket);
            }
        }
    }

    /**
     * Méthode qui permet la synchronisation du nouveau lieur
     *
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    private void recupererListeServices(DatagramSocket pointToPointSocket) throws IOException {
        System.out.println("Reception de la liste des services");

        // Parcourir la liste des linkers jusqu'à trouver un linker up
        for (Linker linker : linkers) {
            // Création du paquet de demande
            DatagramPacket linkerPacket = new DatagramPacket(new byte[]{(byte) Protocol.DEMANDE_DE_LISTE_DE_SERVICES.ordinal()}, 1, InetAddress.getByName(linker.getIp()), linker.getPort());
            pointToPointSocket.send(linkerPacket);

            // Création du paquet pour la récéption de la liste des services
            // 1 byte pour le type, 1 pour le nombre de services, 100 services max
            byte[] buffer = new byte[702];
            DatagramPacket serviceListAddressPacket = new DatagramPacket(buffer, buffer.length);

            // On définit un timeout (si le linker n'est pas up) et on reçoit le paquet.
            // definition d'un time out et reception de la liste des services. Si un lieur met plus de 2sec pour
            // répondre on passe au lieur suivant
            try {
                // TODO : UTILISER UN AUTRE PORT
                pointToPointSocket.setSoTimeout(2000);
                pointToPointSocket.receive(serviceListAddressPacket);
            } catch (SocketTimeoutException e) {
                // Dans le cas d'un timeout, on passe au suivant linker
                continue;
            }

            // TODO : est-ce qu'on envoie une réponse par exemple ERROR si c'est pas le bon type de requête ?
            // Vérification si le paquet est bien une réponse contenant la liste des services
            int type = serviceListAddressPacket.getData()[0];
            if (type == (byte) Protocol.REPONSE_DEMANDE_LISTE_DE_SERVICES.ordinal()) {
                // Ajout des nouveaux services dans la liste
                int nbServices = serviceListAddressPacket.getData()[1];
                for (int i = 0; i < nbServices; i++) {
                    int idService = serviceListAddressPacket.getData()[2 + 7 * i];
                    InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(serviceListAddressPacket.getData(), 3 + i * 7, 7 + i * 7));
                    byte[] portByte = Arrays.copyOfRange(serviceListAddressPacket.getData(), 7 + i * 7, 9 + i * 7);
                    int port = ((portByte[1] & 0xff) << 8) | (portByte[0] & 0xff);

                    /**
                     * byte[] portByte = new byte[2];
                     portByte[0] = serviceAddresspacket.getData()[7];
                     portByte[1] = serviceListAddressPacket.getData()[6];
                     int port = new BigInteger(portByte).intValue();
                     */

                    Service service = new Service(idService, ip.getHostAddress(), port);
                    services.add(service);
                }
                break;
            }
        }

        // On remet le timeout à 0 (infini)
        pointToPointSocket.setSoTimeout(0);
        System.out.println("La liste des services est à jour");
    }

    /**
     * Méthode de réponse à un lieur qui a demandé la liste des services
     *
     * @param serviceAddressPacket
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    private void envoiListeServices(DatagramPacket serviceAddressPacket, DatagramSocket pointToPointSocket) throws InterruptedException, IOException {

        // Définition de la taille du paquet (2 + (le nombre de service * 7))
        byte[] listeServiceData = new byte[2 + (7 * services.size())];
        listeServiceData[0] = (byte) Protocol.REPONSE_DEMANDE_LISTE_DE_SERVICES.ordinal();
        listeServiceData[1] = (byte)services.size();

        // Ajout des services au paquet
        int i = 0;
        for (Service service : services) {
            // Retrieve service data
            byte[] ip = InetAddress.getByName(service.getIp()).getAddress();
            byte[] port = Util.intToBytes(service.getPort(), 2);

            listeServiceData[2 + 7 * i] = (byte)service.getIdService();
            listeServiceData[3 + 7 * i] = ip[0];
            listeServiceData[4 + 7 * i] = ip[1];
            listeServiceData[5 + 7 * i] = ip[2];
            listeServiceData[6 + 7 * i] = ip[3];
            System.out.println("TEST : " + service.getPort());
            listeServiceData[7 + 7 * i] = port[0];
            listeServiceData[8 + 7 * i] = port[1];

            i++;
        }

        // Construction du paquet
        DatagramPacket serviceListPacket = new DatagramPacket(listeServiceData, listeServiceData.length, InetAddress.getByName(serviceAddressPacket.getAddress().getHostName()), serviceAddressPacket.getPort());

        // Envoi du paquet
        pointToPointSocket.send(serviceListPacket);
    }

    /**
     * Envoie l'IP et le port d'un service au client qui a effectué une demande de service
     *
     * @param serviceNumberPacket
     * @param pointToPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    public void envoiServiceAuClient(DatagramPacket serviceNumberPacket, DatagramSocket pointToPointSocket) throws InterruptedException, IOException {
        DatagramPacket servicePacket;

        // recupère le service qui a été utilisé le moin récemment
        // TODO : Récupérer le service avec l'id de service correspondant

        // Récupère le service qui a été utilisé le moin récemment si la liste des services n'est pas vide
        if(!services.isEmpty()) {
            Service service;
            try {
                // On récupère le service qui a été utilisé il y a le plus longtemps et qui a le bon id
                 service = services.stream().filter(s -> s.getIdService() == serviceNumberPacket.getData()[1])
                        .min((a, b) -> a.getLastUse() == null ? -1 : b.getLastUse() == null ? 1 : a.getLastUse()
                                .compareTo(b.getLastUse())).get();
            }
            catch (NoSuchElementException e)
            {
                service = null;
            }
            // Si on a trouvé aucun services correspondant on l'annonce au client
            if (service == null) {
                servicePacket = new DatagramPacket(new byte[]{(byte) Protocol.SERVICE_EXISTE_PAS.ordinal()}, 1, InetAddress.getByName(serviceNumberPacket.getAddress().getHostName()), serviceNumberPacket.getPort());
            }
            // Sinon on lui retourne le service trouvé
            else {
                // Récupère la liste des
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
        services.remove(newService);
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
        if(!services.contains(newService)) {
            services.add(newService);
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

        byte tosend[] = new byte[8];
        services.remove(service);

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
        services.add(newService);

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
