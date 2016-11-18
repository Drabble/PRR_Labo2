/**
 * Project: Labo02
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

/**
 * Defines different messages of the protocol to exchange UDP packets between the client, the server and the linker
 */
public enum Protocol {
    CONTACT_SERVICE,
    REPONSE_AU_SERVICE,
    DEMANDE_DE_SEVICE,
    REPONSE_DEMANDE_DE_SERVICE,
    SERVICE_EXISTE_PAS,
    ABONNEMENT,
    CONFIRMATION_ABONNEMENT,
    DEMANDE_DE_LISTE_DE_SERVICES,
    REPONSE_DEMANDE_LISTE_DE_SERVICES,
    AJOUT_SERVICE,
    DELETE_SERVICE,
    VERIFIE_N_EXISTE_PAS,
    J_EXISTE
}
