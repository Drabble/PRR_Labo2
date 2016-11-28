# Laboratoire de programmation repartie PRR


## Objectifs

- Concevoir et simplifier une architecture répartie.
- Analyser la problématique à un lieur de service.


### Enoncé

Dans ce laboratoire il est question de réaliser un lieur redondant entre des clients et des serveurs d'application.
L'objectif du lieur étant d'associer un client à un service fourni par l'un des serveurs d'application disponible.


### Comportement des clients

Le client doit obtenir l'adresse et le port du service fournit par un serveur auprès d'un lieur.
Par la suite il pourra alors utiliser l'adresse obtenue pour réclamer directement le service auprès du serveur.
Le client va choisir un lieur aléatoire et lui formuler sa demande. Si le lieur ne répond pas, le client va s'arrêter.
Si le service est inatteignable, le client le fera savoir au lieur et se terminera.

### Comportement des serveurs

Lors du demarrage, tout serveur s'inscrit auprès d'un lieur en lui transmettant son adresse IP, son port de service
ainsi que le type du service rendu.

### Protocole
![alt tag](prr.png)

##### CONTACT_SERVICE
Ce paquet est envoyé quand nous voulons faire une requête à un service.
il est constitué de la manière suivante : 
 > [type de paquet][longueur du message][message]

##### REPONSE_DU_SERVICE
Réponse du service questionné.
Envoyé après réception de <strong>« CONTACT_SERVICE »</strong>
il est constitué de la manière suivante : 
 > [type de paquet][longueur du message][message]

##### DEMANDE_DE_SERVICE
Ce paquet est envoyé à un lieur quand un client veux accéder à un service.
Il est constitué de la manière suivante : 
 > [type de paquet][type de service demandé]

##### REPONSE_DEMANDE_DE_SERVICE
Réponse envoyée par le lieur après une demande de service d'un client.
Envoyé après réception de <strong>« DEMANDE_DE_SERVICE »</strong>
Il est constitué de la manière suivante : 
 > [type de paquet][IP du service][port du service]

##### SERVICE_EXISTE_PAS
Cas client → lieur : Paquet envoyé au lieur si un service n'a pas été atteint par le client.
Il est constitué de la manière suivante : 
 > [type de paquet][type du service][IP du service][port du service]

Cas lieur → client : Paquet envoyé si le lieur ne connait pas le type de service demandé.
Envoyé après réception de <strong>« DEMANDE_DE_SERVICE »</strong>
Il est constitué de la manière suivante : 
 > [type de paquet]

##### ABONNEMENT
Demande d’adhésion d'un service à un lieur.
Il est constitué de la manière suivante :
 > [type de paquet][type de service]

##### CONFIRMATION_ABONNEMENT
Ce paquet est envoyé comme confirmation d’adhésion d'un service à un lieur, une fois ce paquet reçu par le service, ce dernier tournera dans une boucle infinie.
Envoyé après réception de <strong>« ABONNEMENT »</strong>
Il est constitué de la manière suivante :
 > [type de paquet]

##### DEMANDE_DE_LISTE_DE_SERVICES
Ce paquet est envoyé à un lieur quand un autre lieur démarre et met à jour sa liste de service.
Il est constitué de la manière suivante :  
 >[type de paquet]

##### REPONSE_DEMANDE_LISTE_DE_SERVICES
Ce paquet est envoyé en réponse à la demande de mise à jour d'un lieur.
Envoyé après réception de <strong>«DEMANDE_DE_LISTE_DE_SERVICES»</strong>  
Il est constitué de la manière suivante : 
 > [type de paquet][nombre de service][type de service][IP du service][port du service]

##### AJOUT_SERVICE
Ce paquet est envoyé par un lieur aux les autres lieurs après réception du paquet <strong>« ABONNEMENT »</strong>
Il est constitué de la manière suivante :
 > [type de paquet][type de service][IP du service][port du service]

##### SUPPRESSION_SERVICE
Ce paquet est envoyé arpès l’émission du paquet <strong>« VERIFIE_N_EXISTE_PAS »</strong> qui n'a pas donné de réponse <strong>« J_EXISTE »</strong>. Ce paquet est envoyé aux autres lieurs pour leur dire de supprimer le service incriminé.
Il est constitué de la manière suivante :
 > [type de paquet][type de service][IP du service][port du service]

#####  VERIFIE_N_EXISTE_PAS
Ce paquet est envoyé par le lieur après réception du <strong>« SERVICE_EXISTE_PAS »</strong>. Il est envoyé du lieur vers le service incriminé pour vérifier si ce dernier est bien injoignable. Il est constitué de la manière suivante : 
 >[type de paquet]

#####  J_EXISTE
Ce paquet est envoyé par le service après réception du paquet  <strong><strong>« VERIFIE_N_EXISTE_PAS »</strong></strong> il permet de confirmer son existence. Il est constitué de la manière suivante :
 >[type de paquet]



### Tests effectués

Le tableau suivant présente les tests qui seront effectués.

| **Tests** | **Resutats** | **Commentaires**<br/> |  
| --- | --- | --- |
| Un serveur doit se souscrire à un lieur de la liste des lieurs quand il démarre | succès | Ok |
| Un client doit demander un service à un lieur quand il démarre puis utiliser le service |  |  |
| Un client doit informer le lieur quand un service donné ne répond pas |  |  |
| Un lieur doit envoyer une confirmation aux services quand il s'inscrit | succès | Ok |
| Un lieur doit notifier les autres lieurs quand un service s'inscrit à lui et ils doivent se mettre à jour |  |  |
| Un lieur doit vérifier l'existence d'un service si un client lui indique qu'il ne répond pas et doit le supprimer de sa liste de services |  |  |
| Un lieur doit notifier les autres lieurs quand un service est indisponible et ils doivent se mettre à jour |  |  |
| le serveur doit fournir son service d'echo aux clients après s'être souscrit au lieur |  |  |
| les serveurs doivent réponse aux demande d'existence des lieurs après s'être souscrit à un lieur |  |  |
| Un lieur doit être obténu aléatoirement à partir d'une liste dans le client et le serveur | succès | Aucun |
| Un lieur ou un serveur doit être redemarré aussitôt qu'il est tombé en panne  | - | Non pris en compte, redemarrage manuel |
| Au démarrage et au redémarrage, un lieur doit se mettre à jour par rapport à un autre lieur disponible   | succès | lancement du 1er lieur, lancment du serveur, lancment du second lieur. Message : <p> Nouveau service reçu:Service: id 1,ip 127.0.0.1, port 12347|
| Un lieur doit envoyer envoyer sa liste de service quand un autre lieur la lui demande | succès | Ok |
| Le client doit s'arrêter ou attendre un délai après qu'il redemande un service inconnu auprès d'un lieur   | succès | le client s'arrête si le lieur ne connais pas le service demandé |
| Après notification d'un client qu'un service ne répond pas, un lieur doit le vérifier et notifier les autres lieurs  |  |  |
| Le lieur doit distribuer les services de même type de façon cyclique entre les clients   |  |  |
| Deux clients notifient presque simultanément qu'un service ne répond pas | Succès | Le lieur utilise un autre port pour l'envoi et la récéption du message de vérification d'un service, il ne recevra donc pas le 2ème message du client dans le receive de la confirmation d'existence du service. |
