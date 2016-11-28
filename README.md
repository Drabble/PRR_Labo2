# Laboratoire de programmation repartie PRR


## Objectifs

- Concevoir et symplifier une architecture repartie.
- Analyser la problématique à un lieur de service.


### Enoncé

Dans ce laboratoire il est question e réaliser un lieur redondant entre des clients et serveurs d'application.
L'objectif de lieur étant d'associer un client à un service fourni par l'un des serveurs d'application disponible.


### Comportement des clients

Le client doit obténir l'adresse et le port du service fournit par un serveur pour la toute prémière fois qu'il nécessite ce service.
Par la suite il pourra alors utiliser l'adresse obténu pour reclamer directement le service auprès du serveur.

Les arguements pour le lancment d'un client doivent etre les suivants :

* 1er argument = port d'ecoute du client
* 2eme argument = type de service
* 3eme arguement = ip du lieur
* 4eme arguement = port d'ecoute du lieur
les arguments 3 et 4 pevent etre répété si nous avons plus d'un lieur

</br>

Exemple de parametres minimal pour le lancement d'un client
 > 2226 1 127.0.0.1 2222


### Comportement des serveurs

Lors du demarrage, tout serveur s'inscrit auprès d'un lieur en lui transmettant son adresse IP, son port de service
ainsi que le type du service rendu.

### Protocole
![alt tag](prr.png)

##### CONTACT_SERVICE
Ce paquet est envoyé quand nous voulons faire une requête à un service.
il est constitué de la manière suivante : [type de paquet][longueur du message][message]

##### REPONSE_AU_SERVICE
Réponse du service questionné.
Envoyé après réception de <strong>« CONTACT_SERVICE »</strong>
il est constitué de la manière suivante : [type de paquet][longueur du message][message]

##### DEMANDE_DE_SEVICE
Ce parquet est envoyé quand un client veux accéder à un service.
Il est constitué de la manière suivante : [type de paquet][type de service demandé]

##### REPONSE_DEMANDE_DE_SERVICE
Réponse du service de la demande du client.
Envoyé après réception de <strong>« DEMANDE_DE_SEVICE »</strong>
Il est constitué de la manière suivante : type de paquet][IP du service][port du service]

##### SERVICE_EXISTE_PAS
Cas client → lieur : Paquet envoyé au lieur si un service n'a pas été attient par le client.
Il est constitué de la manière suivante : [type de paquet][type du service][IP du service][port du service]
Cas lieur → client : Paquet envoyé si le lieur ne connais pas le type de service demandé.
Envoyé après réception de <strong>« DEMANDE_DE_SEVICE »</strong>
Il est constitué de la manière suivante : [type de paquet]

##### ABONNEMENT
Demande d’adhésion d'un service à un lieur.
Il est constitué de la manière suivante :[type de paquet][type de service]

##### CONFIRMATION_ABONNEMENT
Ce parquet est envoyé comme confirmation d’adhésion d'un service à un lieur, une fois ce paquet reçu par le service, ce dernier tournera dans une boucle infinie.
Envoyé après réception de <strong>« ABONNEMENT »</strong>
Il est constitué de la manière suivante :[type de paquet]

##### DEMANDE_DE_LISTE_DE_SERVICES
Ce parquet est envoyé quand à un lieur quand un lieur démarre pour mettre à jour ça liste de service.
Il est constitué de la manière suivante : [type de paquet]

##### REPONSE_DEMANDE_LISTE_DE_SERVICES
Ce parquet est envoyé en réponse à la demande de mise à jour d'un lieur.
Envoyé après réception de <strong>«DEMANDE_DE_LISTE_DE_SERVICES»</strong>  
Il est constitué de la manière suivante : [type de paquet][nombre de service][type de service][IP du service][port du service]

##### AJOUT_SERVICE
Ce parquet est envoyé par un lieur vers les autre lieur après réception du paquet <strong>« ABONNEMENT »</strong>
Il est constitué de la manière suivante :[type de paquet][type de service][IP du service][port du service]

##### DELETE_SERVICE
Ce parquet est envoyé par l’émission du paquet <strong>« VERIFIE_N_EXISTE_PAS »</strong> qui n'a pas donné de réponse <strong>« J_EXISTE »</strong>. Ce paquet est envoyé au autres lieurs pour leur dire de supprimer le service incriminé.
Il est constitué de la manière suivante :[type de paquet][type de service][IP du service][port du service]

#####  VERIFIE_N_EXISTE_PAS
Ce parquet est envoyé par le lieur après réception du <strong>« SERVICE_EXISTE_PAS »</strong>. Il est envoyé du lieur vers le service incriminé pour vérifier si ce dernier est bien injoignable. Il est constitué de la manière suivante :[type de paquet]

#####  J_EXISTE
Ce paquet est envoyé par le service après réception du paquet  <strong><strong>« VERIFIE_N_EXISTE_PAS »</strong></strong> il permet de confirmer son existence Il est constitué de la manière suivante :[type de paquet]





### Tests à effectuer

Le tableau suivant présente les tests qui seront effectués.

| **Tests** | **Resutats** | **Commentaires**<br/> |  
| --- | --- | --- |
| Un lieur doit être obténu aléatoirement à partir d'une liste | succés | Aucun |
| Un lieur ou un serveur doit être redemarré aussitôt qu'il est tombé en panne  | - | Non pris en compte, redemarrage manuel |
| Après rédémarrage d'un lieur, il doit se mettre à jours par rapport aux autres lieurs disponibles   | succes | lancement du 1er lieur, lancment du serveur, lancment du second lieur. Message : <p> Nouveau service reçu:Service: id 1,ip 127.0.0.1, port 12347|
| Le client doit s'arrêter ou attendre un délai après qu'il redemande un service inconnu auprès d'un lieur   | succes | le client s'arrête si le lieur ne connais pas le service demandé |
| Le lieur doit repondre uniquement aux service existants   |  |  |
| Un client doit notifier un lieu d'un service non disponible   | succes |  |
| Après notification d'un client le lieur doit mettre à jous sa table de service et la sychroniser avec les autres lieurs   | succes | Quand un service est injoignable il est supprimé de la liste du lieur |
|  Le lieur doit distribuer les services de même type de façon cyclique entre les serveurs   |  |  |  |

### Simulation effectuée

Nous avons fait une simulation mettant en place :
* un lieur avec les parmatres suivants
    * port principal : 2222
    * port de verification : 2223
* un client
    * port : 2226
    * type de service voulu : 1
    * ip du lieur : 127.0.0.1
    * port du lieur : 2222
* un service
    * port : 2224
    * type de service : 1
    * ip du lieur : 127.0.0.1
    * port du lieur : 2222

#### client
<blockquote>
Démarrage du client </br>
Le client va demander le service1 au lieur: </br>
Service: ip 127.0.0.1, port 2222 </br>
Reponse du lieur recue </br>
Le service a été trouvé il est joignable a l'adresse: </br> 127.0.0.1:2224 </br>
Message envoyé au service </br>
Reponse du serveur reçue </br>
taille 4 </br>
0 : 1 </br>
1 : 1 </br>
2 : 1 </br>
3 : 1 </br>
</blockquote>

### serveur
<blockquote>
Serveur démarré! </br>
Tentative de souscription au lieur: </br>
Service: ip 127.0.0.1, port 2222 </br>
Confirmation de souscription reçue </br>
Attente d'une nouvelle demande d'un client </br>
Reception d'une nouvelle demande du client 127.0.0.1 2226 </br>
</blockquote>

### lieur
<blockquote>
Démarrage du lieur </br>
Reception de la liste des services </br>
La liste des services est à jour </br>
Attente d'une nouvelle demande... </br>
Liste actuelle </br>
Nouvelle demande recue </br>
Type de message: ABONNEMENT </br>
Nouvelle souscription du service: </br>
Service: id 1, ip 127.0.0.1, port 2224 </br>
Notification aux autres lieurs de l'ajout du service </br>
Envoi de la confirmation de souscription au service </br>
Attente d'une nouvelle demande... </br>
Liste actuelle </br>
Service: id 1, ip 127.0.0.1, port 2224 </br>
Nouvelle demande recue </br>
Type de message: DEMANDE_DE_SERVICE </br>
Envoi du service au client </br>
</blockquote>
