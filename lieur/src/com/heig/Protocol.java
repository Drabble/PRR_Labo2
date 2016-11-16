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
    ASK_SERVICE,
    ADDRESS_SERVICE,
    SERVICE_EXISTE_PAS,
    SUB,
    CONFIRM_SUB,
    ASK_SERVICE_LIST,
    RETURN_SERVICES,
    ADD_SERVICE,
    DELETE_SERVICE,
    CHECK_DONT_EXIST,
    I_EXIST
}
