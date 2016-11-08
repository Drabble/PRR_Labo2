/**
 * Project: Labo02
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 * Rapport: We defined a protocol......
 *          We limit to 100 the number of service per linker (buffer size is 702 = 100 services + protocol type + number of services)
 */
package com.heig;

import java.io.IOException;

/**
 * This class creates a new linker server and starts it. The list of the others linkers must be passed
 * as arguments when running the program. The port on which the linker will listen must be specified too.
 */
public class Main {

    /**
     * Creates a new linker which will listen on the specified port and will synchronise with the specified linkers.
     *
     * @param args
     * @throws InterruptedException
     * @throws IOException
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        // TODO : Parse arguments
        LinkerServer linkerServer = new LinkerServer();
        linkerServer.start();
    }
}
