package exercise;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;


public class Entrypoint {

    private static final int port = 8310;

    public static void main(String[] args) {
        try {

            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/process", new FilePostHandler());
            server.start();

            System.out.println("Running back-end on :" + port);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}