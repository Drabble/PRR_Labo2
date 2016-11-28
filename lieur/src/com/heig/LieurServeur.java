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
 *
 * Le LieurServer va utiliser le port précisé pour toutes les requêtes et va utiliser le port précisé + 1 pour
 * effectuer le test d'existence d'un service.
 *
 * Il aurait été plus performant d'utiliser le multicast pour l'envoi et la réception d'ajout/suppression
 * de services pour les lieurs mais il aurait fallu créer un thread supplémentaire pour l'écoute de ces requêtes et ça
 * aurait créer des problèmes de concurrence au niveau de la liste des services. Nous avons donc décider de ne pas le
 * faire pour garder la classe LieurServeur plus simple.
 *
 * // TODO : NE PAS HARDCODER LE 702 !!!
 */
public class LieurServeur {
    private List<Service> services = new ArrayList<>(); // Liste des services
    private final Lieur[] lieurs; // Liste des autres lieurs
    private final int port;
    private final int portVerification;
    private final int tailleMax = 702;
    final int timeout = 2000;     //temps avant de throw SocketTimeoutException


    /**
     * Création d'un nouveau lieur avec un port principal, un port pour la vérification de l'existence d'un serveur
     * et une liste de lieurs
     *
     * @throws InterruptedException
     * @throws IOException
     */
    LieurServeur(int port, int portVerification, Lieur[] lieurs){
        this.port = port;
        this.portVerification = portVerification;
        this.lieurs = lieurs;
    }

    /**
     * Démarrage du lieur. Au démarrage le lieur va questionner les autres lieur un à un pour obtenir une liste de
     * service. Il va aussi répondre aux requêtes des clients
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void demarrer() throws IOException, InterruptedException {
        // Création d'une connexion pointToPoint
        DatagramSocket pointAPointSocket = new DatagramSocket(port);
        System.out.println("Démarrage du lieur");

        // Démarrage du lieur et syncronisation avec les autre lieur
        recupererListeServices(pointAPointSocket);

        // Traitement de toutes les requêtes reçues
        while (true) {
            System.out.println("Attente d'une nouvelle demande...");

            // Réception de la requête, taille max de 702 (le message le plus grand est celui d'envoi de la liste de services,
            // un byte pour le type de message, un pour le nombre de service et 7 par service avec un max de 100 services)
            // dans la méthode recupererListeService
            byte[] buffer = new byte[tailleMax];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            pointAPointSocket.receive(receivePacket);


            System.out.println("Liste actuelle");
            for (Service service : services) {
                System.out.println(service);
            }


            System.out.println("Nouvelle demande recue");

            // Récupération du type de message
            byte messageType = receivePacket.getData()[0];
            System.out.println("Type de message: " + Protocole.getByOrdinale(messageType));

            // Si le message reçu est une demande de liste de services d'un lieur (lieur -> lieur)
            if (messageType == Protocole.DEMANDE_DE_LISTE_DE_SERVICES.ordinal()) {
                envoiListeServices(receivePacket, pointAPointSocket);
            }
            // Si le message est une demande de service d'un client (client -> lieur)
            else if(messageType == Protocole.DEMANDE_DE_SERVICE.ordinal()){
                envoiServiceAuClient(receivePacket, pointAPointSocket);
            }
            // Ajout d'un nouveau service de la part d'un lieur (lieur -> lieur)
            else if (messageType == Protocole.AJOUT_SERVICE.ordinal()) {
                ajoutService(receivePacket);
            }
            // Suppression d'un service (lieur -> lieur)
            else if (messageType == Protocole.SUPPRESSION_SERVICE.ordinal()) {
                suppressionService(receivePacket);
            }
            // Si un client n'a pas trouvé le service ( client -> lieur )
            else if (messageType == Protocole.SERVICE_EXISTE_PAS.ordinal()) {
                verifServiceExiste(receivePacket, pointAPointSocket);
            }
            // Si un service veut s'abonner à un lieur
            else if (messageType == Protocole.ABONNEMENT.ordinal()) {
                souscriptionService(receivePacket, pointAPointSocket);
            }
        }
    }

    /**
     * Méthode qui permet la synchronisation du nouveau lieur
     *
     * @param pointAPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    private void recupererListeServices(DatagramSocket pointAPointSocket) throws IOException {
        System.out.println("Reception de la liste des services");

        // Parcourir la liste des lieurs jusqu'à trouver un Lieur up
        for (Lieur Lieur : lieurs) {
            // Création du paquet de demande
            DatagramPacket LieurPacket = new DatagramPacket(new byte[]{(byte) Protocole.DEMANDE_DE_LISTE_DE_SERVICES.ordinal()}, 1, InetAddress.getByName(Lieur.getIp()), Lieur.getPort());
            pointAPointSocket.send(LieurPacket);

            // Création du paquet pour la récéption de la liste des services
            // 1 byte pour le type, 1 pour le nombre de services, 100 services max
            byte[] buffer = new byte[702];
            DatagramPacket serviceListAddressPacket = new DatagramPacket(buffer, buffer.length);

            // On définit un timeout (si le Lieur n'est pas up) et on reçoit le paquet.
            // definition d'un time out et reception de la liste des services. Si un lieur met plus de 2sec pour
            // répondre on passe au lieur suivant
            try {
                pointAPointSocket.setSoTimeout(timeout);
                pointAPointSocket.receive(serviceListAddressPacket);
            } catch (SocketTimeoutException e) {
                // Dans le cas d'un timeout, on passe au suivant Lieur
                continue;
            }

            // Vérification si le paquet est bien une réponse contenant la liste des services
            // Si un client ou un serveur a envoyé une requête, elle sera ignorée car le lieur est entrain de démarrer
            // C'est au rôle du client d'essayer un autre lieur si celui là ne répond pas
            int type = serviceListAddressPacket.getData()[0];
            if (type == (byte) Protocole.REPONSE_DEMANDE_LISTE_DE_SERVICES.ordinal()) {
                // Ajout des nouveaux services dans la liste
                int nbServices = serviceListAddressPacket.getData()[1];
                for (int i = 0; i < nbServices; i++) {
                    int idService = serviceListAddressPacket.getData()[2 + 7 * i];
                    InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(serviceListAddressPacket.getData(), 3 + i * 7, 7 + i * 7));
                    byte[] portByte = Arrays.copyOfRange(serviceListAddressPacket.getData(), 7 + i * 7, 9 + i * 7);
                    int port = ((portByte[1] & 0xff) << 8) | (portByte[0] & 0xff);

                    Service service = new Service(idService, ip.getHostAddress(), port);
                    System.out.println("Nouveau service reçu:");
                    System.out.println(service);
                    services.add(service);
                }
                break;
            }
        }

        // On remet le timeout à 0 (infini)
        pointAPointSocket.setSoTimeout(0);
        System.out.println("La liste des services est à jour");
    }

    /**
     * Méthode de réponse à un lieur qui a demandé la liste des services
     *
     * @param serviceAddressPacket
     * @param pointAPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    private void envoiListeServices(DatagramPacket serviceAddressPacket, DatagramSocket pointAPointSocket) throws InterruptedException, IOException {
        System.out.println("Nouvelle demande de la liste des services");

        // Définition de la taille du paquet (2 + (le nombre de service * 7))
        byte[] listeServiceData = new byte[2 + (7 * services.size())];
        listeServiceData[0] = (byte) Protocole.REPONSE_DEMANDE_LISTE_DE_SERVICES.ordinal();
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
            listeServiceData[7 + 7 * i] = port[0];
            listeServiceData[8 + 7 * i] = port[1];

            System.out.println("Envoi du service:");
            System.out.println(service);

            i++;
        }

        // Construction du paquet
        DatagramPacket serviceListPacket = new DatagramPacket(listeServiceData, listeServiceData.length, InetAddress.getByName(serviceAddressPacket.getAddress().getHostName()), serviceAddressPacket.getPort());

        // Envoi du paquet
        pointAPointSocket.send(serviceListPacket);
    }

    /**
     * Envoie l'IP et le port d'un service au client qui a effectué une demande de service
     *
     * @param serviceNumberPacket
     * @param pointAPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    private void envoiServiceAuClient(DatagramPacket serviceNumberPacket, DatagramSocket pointAPointSocket) throws InterruptedException, IOException {
        System.out.println("Envoi du service au client");
        
        DatagramPacket servicePacket;

        // Récupère le service qui a été utilisé le moin récemment si la liste des services n'est pas vide
        if(!services.isEmpty()) {
            Service service;
            try {
                // On récupère le service qui a été utilisé il y a le plus longtemps et qui a le bon id
                 service = services.stream().filter(s -> s.getIdService() == serviceNumberPacket.getData()[1])
                        .min((a, b) -> a.getDerniereUtilisation() == null ? -1 : b.getDerniereUtilisation() == null ? 1 : a.getDerniereUtilisation()
                                .compareTo(b.getDerniereUtilisation())).get();
            }
            catch (NoSuchElementException e)
            {
                service = null;
            }
            // Si on a trouvé aucun services correspondant on l'annonce au client
            if (service == null) {
                servicePacket = new DatagramPacket(new byte[]{(byte) Protocole.SERVICE_EXISTE_PAS.ordinal()}, 1, InetAddress.getByName(serviceNumberPacket.getAddress().getHostName()), serviceNumberPacket.getPort());
            }
            // Sinon on lui retourne le service trouvé
            else {
                byte[] ip = InetAddress.getByName(service.getIp()).getAddress();
                byte[] port = Util.intToBytes(service.getPort(), 2);
                byte[] serviceBuffer = {(byte) Protocole.REPONSE_DEMANDE_DE_SERVICE.ordinal(), (byte) service.getIdService(),
                                         ip[0], ip[1], ip[2], ip[3], port[0], port[1]};

                servicePacket = new DatagramPacket(serviceBuffer, serviceBuffer.length, InetAddress.getByName(serviceNumberPacket.getAddress().getHostName()), serviceNumberPacket.getPort());
                service.utiliser();

                System.out.println("Service envoyé au client:");
                System.out.println(service);
            }
        }
        else
        {
            System.out.println("Aucun service avec cet id n'a été trouvé");
            servicePacket = new DatagramPacket(new byte[]{(byte) Protocole.SERVICE_EXISTE_PAS.ordinal()}, 1, InetAddress.getByName(serviceNumberPacket.getAddress().getHostName()), serviceNumberPacket.getPort());
        }
        // envoi du paquet
        pointAPointSocket.send(servicePacket);
    }

    /**
     * Methode permetant d'effacer un service de la liste (lieur -> lieur)
     *
     * @param deleteServicePacket
     * @throws InterruptedException
     * @throws IOException
     */
    private void suppressionService(DatagramPacket deleteServicePacket) throws InterruptedException, IOException {
        // Récupération du service
        int IDService = deleteServicePacket.getData()[1];
        InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(deleteServicePacket.getData(), 2, 6));
        byte[] portByte = {deleteServicePacket.getData()[7], deleteServicePacket.getData()[6]};
        int port = new BigInteger(portByte).intValue();
        Service newService = new Service(IDService, ip.getHostAddress(), port);

        System.out.println("Suppression du service: " + newService);

        // Suppression du service
        for(int i = 0 ; i< services.size() ; i++)
        {
            System.out.println(services.get(i));
            if(services.get(i).getIdService() == newService.getIdService()
                    && services.get(i).getIp().equals(newService.getIp())
                    && services.get(i).getPort() == newService.getPort())
            {
                services.remove(i);
            }
        }
    }

    /**
     * Methode qui ajoute un service à la liste (lieur -> lieur)
     *
     * @param addServicePacket
     * @throws InterruptedException
     * @throws IOException
     */
    private void ajoutService(DatagramPacket addServicePacket) throws InterruptedException, IOException {
        // Retrieve data from packet
        int idService = addServicePacket.getData()[1];
        InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(addServicePacket.getData(), 2, 6));
        byte[] portByte = {addServicePacket.getData()[7], addServicePacket.getData()[6]};
        int port = new BigInteger(portByte).intValue();

        // Ajoute le service à la liste s'il n'existe pas déj$
        Service newService = new Service(idService, ip.getHostAddress(), port);
        System.out.println("Ajout du service:");
        System.out.println(newService);
        if(!services.contains(newService)) {
            services.add(newService);
        }
    }

    /**
     * Methode qui va verifier si un service et bien indisponible, si c'est le cas, il le supprime et informe les autres
     * lieur de supprimer ce service.
     *
     * @param serviceNotExistPacket
     * @param pointAPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    private void verifServiceExiste(DatagramPacket serviceNotExistPacket, DatagramSocket pointAPointSocket) throws InterruptedException, IOException {
        // Création d'une connexion point à point
        boolean check = false;
        DatagramSocket verifServiceSocket = new DatagramSocket(portVerification);

        // Récupération du service depuis le packet
        int idService = serviceNotExistPacket.getData()[1];
        InetAddress ip = InetAddress.getByAddress(Arrays.copyOfRange(serviceNotExistPacket.getData(), 2, 6));
        byte[] portByte = Arrays.copyOfRange(serviceNotExistPacket.getData(), 6, 8);
        int port = ((portByte[1] & 0xff) << 8) | (portByte[0] & 0xff);



        // Envoie un paquet au service que le client n'a pas pu joindre
        Service serviceNotReachable = new Service(idService, ip.getHostAddress(), port);
        // pour ne pas surcharger le reseau on teste si le service existe bien dans nore liste
        for(int i = 0 ; i< services.size() ; i++) {
            if (services.get(i).getIdService() == serviceNotReachable.getIdService()
                    && services.get(i).getIp().equals(serviceNotReachable.getIp())
                    && services.get(i).getPort() == serviceNotReachable.getPort()) {
                check = true ;
                System.out.println("service trouvé");
                break;
            }
        }

        if(check == true) {
            DatagramPacket checkPacket = new DatagramPacket(new byte[]{(byte) Protocole.VERIFIE_N_EXISTE_PAS.ordinal()}, 1, InetAddress.getByName(serviceNotReachable.getIp()), serviceNotReachable.getPort());
            verifServiceSocket.send(checkPacket);

            System.out.println("Verification de l'existence du service:");
            System.out.println(serviceNotReachable);

            byte[] bufferResponse = new byte[1];
            try {
                // si nous avons eu une reponse dans les deux seconde, le service existe toujours, si non
                // on le supprime et notifie les autres lieurs
                DatagramPacket serviceResponsePacket = new DatagramPacket(bufferResponse, bufferResponse.length);
                verifServiceSocket.setSoTimeout(timeout);
                verifServiceSocket.receive(serviceResponsePacket);

                // If wrong type, delete service
                int messageType = serviceResponsePacket.getData()[0];
                if (messageType != (byte) Protocole.J_EXISTE.ordinal()) {
                    System.out.println("Le service n'existe pas");
                    suppressionServiceEtNotificationLieurs(serviceNotReachable, pointAPointSocket);
                } else {
                    System.out.println("Le service existe");
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Le service n'existe pas");
                suppressionServiceEtNotificationLieurs(serviceNotReachable, pointAPointSocket);
            }
        }
        else {
            System.out.println("le service à déjà été supprimé ou ne se trouve pas dans la liste");
        }

        verifServiceSocket.close();
    }

    /**
     * methode qui supprime le service de la liste de service et informe les autres lieurs
     *
     * @param service,
     * @param pointAPointSocket
     * @throws IOException
     */
    private void suppressionServiceEtNotificationLieurs(Service service, DatagramSocket pointAPointSocket) throws IOException {

        System.out.println("Notification aux autres lieurs que ce service n'existe pas:");
        System.out.println(service);

        // Suppression du service dans la liste des services
        for(int i = 0 ; i< services.size() ; i++)
        {
            if(services.get(i).getIdService() == service.getIdService()
                    && services.get(i).getIp().equals(service.getIp())
                    && services.get(i).getPort() == service.getPort()) {
                services.remove(i);
            }
        }

        // Notification aux autres lieurs que le service a été supprimé
        byte[] ip = InetAddress.getByName(service.getIp()).getAddress();
        byte[] port = Util.intToBytes(service.getPort(), 2);
        byte[] suppressionServiceBuffer = {(byte) Protocole.SUPPRESSION_SERVICE.ordinal(), (byte) service.getIdService(),
                                            ip[0], ip[1], ip[2], ip[3], port[0], port[1]};
        for(Lieur Lieur : lieurs) {
            // Création du paquet
            DatagramPacket servicePacket = new DatagramPacket(suppressionServiceBuffer, suppressionServiceBuffer.length, InetAddress.getByName(Lieur.getIp()), Lieur.getPort());

            // Envoi du paquet
            pointAPointSocket.send(servicePacket);
        }
    }

    /**
     * Methode d'ajout d'un nouveau service, envoi de l'information aux autres lieurs de l'existance de ce nouveau service,
     * confirmation au service qu'il a bien été ajouté.
     *
     * @param subscribeServicePacket
     * @param pointAPointSocket
     * @throws InterruptedException
     * @throws IOException
     */
    private void souscriptionService(DatagramPacket subscribeServicePacket, DatagramSocket pointAPointSocket) throws InterruptedException, IOException {
        // Récuperation des données du parquet
        int idService = subscribeServicePacket.getData()[1];
        InetAddress ip = subscribeServicePacket.getAddress();
        int port = subscribeServicePacket.getPort();

        // Création du service et ajout a la liste
        Service newService = new Service(idService, ip.getHostAddress(), port);
        services.add(newService);

        System.out.println("Nouvelle souscription du service:");
        System.out.println(newService);


        byte[] ipByte = InetAddress.getByName(newService.getIp()).getAddress();
        byte[] portbyte = Util.intToBytes(newService.getPort(), 2);

        byte[] ajoutServiceBuffer = {(byte) Protocole.AJOUT_SERVICE.ordinal(), (byte) newService.getIdService(),
                                      ipByte[0], ipByte[1], ipByte[2], ipByte[3], portbyte[0], portbyte[1]};

        System.out.println("Notification aux autres lieurs de l'ajout du service");

        // Envoi de l'information aux autres lieurs
        for(Lieur Lieur : lieurs) {
            // Création du paquet
            DatagramPacket servicePacket = new DatagramPacket(ajoutServiceBuffer, ajoutServiceBuffer.length, InetAddress.getByName(Lieur.getIp()), Lieur.getPort());

            // Envoi du paquet
            pointAPointSocket.send(servicePacket);
        }

        System.out.println("Envoi de la confirmation de souscription au service");

        // Création du paquet de confirmation d'abonnement
        DatagramPacket confirmSubPacket = new DatagramPacket(new byte[]{(byte) Protocole.CONFIRMATION_ABONNEMENT.ordinal()}, 1, InetAddress.getByName(subscribeServicePacket.getAddress().getHostAddress()), subscribeServicePacket.getPort());

        // Envoi du paquet
        pointAPointSocket.send(confirmSubPacket);
    }
}
