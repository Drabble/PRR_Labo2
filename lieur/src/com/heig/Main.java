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

        if(args.length < 3){ // ex args = "2222" , "127.0.0.1", "1111", "127.0.0.1", "1112"
            System.out.println("You must privide at least 3 parametters");
            System.exit (1);
        }

        List<Pair<String, Integer>> linkers = new ArrayList<Pair<String, Integer>>();
        int port = Integer.parseInt(args[0]);
        for(int i = 1; i<args.length - 1; i++) {
            linkers.add(Pair.of(args[i], args[i + 1]));
            i++; // positionner i à la prochaine adress ip (voir exemple ci dessus)
        }

        LinkerServer linkerServer = new LinkerServer(linkers, port);
        linkerServer.demarrer();
    }
}
