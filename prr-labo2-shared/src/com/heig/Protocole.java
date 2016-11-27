/**
 * Project: Labo02
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

/**
 * Défini les différents types de message du protocole d'échange de paquet UDP entre le client, le serveur et le lieur
 */
public enum Protocole {
    CONTACT_SERVICE,
    REPONSE_DU_SERVICE,
    DEMANDE_DE_SERVICE,
    REPONSE_DEMANDE_DE_SERVICE,
    SERVICE_EXISTE_PAS,
    ABONNEMENT,
    CONFIRMATION_ABONNEMENT,
    DEMANDE_DE_LISTE_DE_SERVICES,
    REPONSE_DEMANDE_LISTE_DE_SERVICES,
    AJOUT_SERVICE,
    SUPPRESSION_SERVICE,
    VERIFIE_N_EXISTE_PAS,
    J_EXISTE;

    /**
     * Retourne le nom du message de type protocole à partir de l'ordinale
     *
     * @param ordinale
     * @return
     */
    public static String getByOrdinale(int ordinale) {
        for(Protocole e : values()) {
            if(e.ordinal() == ordinale) return e.getName();
        }
        return null;
    }

    /**
     * Retourne le nom du protocole
     *
     * @return
     */
    public String getName() { return name(); }
}
