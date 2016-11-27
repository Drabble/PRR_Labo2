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

### Comportement des serveurs

Lors du demarrage, tout serveur s'inscrit auprès d'un lieur en lui transmettant son adresse IP, son port de service
ainsi que le type du service rendu.


### Tests à effectuer

Le tableau suivant présente les tests qui seront effectués.

| **Tests** | **Resutats** | **Commentaires**<br/> |  
| --- | --- | --- |
| Un lieur doit être obténu aléatoirement à partir d'une liste | succés | Aucun |
| Un lieur ou un serveur doit être redemarré aussitôt qu'il est tombé en panne  | - | Non pris en compte, redemarrage manuel |
| Après rédémarrage d'un lieur, il doit se mettre à jours par rapport aux autres lieurs disponibles   | succes | lancement du 1er lieur, lancment du serveur, lancment du second lieur. Message : <p> Nouveau service reçu:
Service: id 1,ip 127.0.0.1, port 12347 |
| Le client doit s'arrêter ou attendre un délai après qu'il redemande un service inconnu auprès d'un lieur   |  |  |
| Le lieur doit repondre uniquement aux service existants   |  |  |
| Un client doit notifier un lieu d'un service non disponible   |  |  |
| Après notification d'un client lieur doit mettre à jous sa table de service et la sychroniser avec les autres lieurs   |  |  |
|  Le lieur doit distribuer les services de même type de façon cyclique entre les serveurs   |  |  |
| Deux clients se connectent l'un apres l'autre (1 sec d'intervalle ) à un lieur pour l'informer que le service X est down (crash probable du au fait que le port d'ecoute du lieur sera occupé)   |  |  |
| client fait une demande de service au lieur alors que celui si est en verification d'existance d'un autre service   |  |  |
