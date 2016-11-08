/**
 * Project: Labo02
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

import java.io.IOException;

/**
 * This class creates a new client and starts it. The port of the client must be passed as an
 * argument to the program. As well as the list of the linkers.
 */
public class Main {

    /**
     * Creates a new client which will listen on the specified port and will subscribe to a linker.
     *
     * @param args
     * @throws InterruptedException
     * @throws IOException
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        // TODO : Parse arguments
        Client client = new Client();
        client.start();
    }
}
