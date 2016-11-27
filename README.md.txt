# Laboratoire de programmation repartie PRR


## Objectifs

- Concevoir et symplifier une architecture repartie.
- Analyser la probl�matique � un lieur de service.

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Enonc�

Dans ce laboratoire il est question e r�aliser un lieur redondant entre des clients et serveurs d'application. 
L'objectif de lieur �tant d'associer un client � un service fourni par l'un des serveurs d'application disponible.

```
Give examples
```

### Comportement des clients

Le client doit obt�nir l'adresse et le port du service fournit par un serveur pour la toute pr�mi�re fois qu'il n�cessite ce service.
Par la suite il pourra alors utiliser l'adresse obt�nu pour reclamer directement le service aupr�s du serveur.

### Comportement des serveurs

Lors du demarrage, tout serveur s'inscrit aupr�s d'un lieur en lui transmettant son adresse IP, son port de service 
ainsi que le type du service rendu.


### Tests � effectuer

Pour tester le bon fonctionnement de l'application obt�nue, les tests suivantes doivent �tre r�aliser avec succ�s.

* Un lieur doit �tre obt�nu al�atoirement � partir d'une liste;
* Un lieur ou un serveur doit �tre redemarr� aussit�t qu'il est tomb� en panne;
* Apr�s r�d�marrage d'un lieur, il doit se mettre � jours par rapport aux autres lieurs disponibles;
* Le client doit s'arr�ter ou attendre un d�lai apr�s qu'il redemande un service inconnu aupr�s d'un lieur;
* Le lieur doit repondre uniquement aux service existants;
* Un client doit notifier un lieu d'un service non disponible;
* Apr�s notification d'un client lieur doit mettre � jous sa table de service et la sychroniser avec les autres lieurs
* Le lieur doit distribuer les services de m�me type de fa�on cyclique entre les serveurs;
