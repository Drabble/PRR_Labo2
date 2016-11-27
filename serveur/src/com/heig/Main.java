/**
 * Project: Labo02
 * Authors: Antoine Drabble & Simon Baehler
 * Date: 08.11.2016
 */
package com.heig;

import java.io.IOException;

/**
 * This class creates a new service server and starts it. The port of the service must be passed as an
 * argument to the program. As well as the list of the linkers.
 */
public class Main {

    /**
     * Creates a new service server which will listen on the specified port and will subscribe to a linker.
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

        ServiceServer serviceServer = new ServiceServer(linkers, port);
        serviceServer.start();
    }
}
