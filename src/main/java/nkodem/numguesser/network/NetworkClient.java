// java
package nkodem.numguesser.network;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class NetworkClient {
    private final String host;
    private final int port;
    private static final int WAIT_TIMEOUT_MS = 30_000;

    public NetworkClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws IOException {
        Scanner scanner = new Scanner(System.in);
        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Connected to " + host + ":" + port);
            String serverLine = in.readLine();
            if (serverLine == null || !serverLine.startsWith("START:")) {
                System.out.println("Protocol error or server closed.");
                return;
            }
            String[] parts = serverLine.split(":");
            int low = Integer.parseInt(parts[1]);
            int high = Integer.parseInt(parts[2]);
            System.out.println("Game started. Guess a number between " + low + " and " + high);

            boolean waiting = false;
            boolean done = false;

            while (!done) {
                if (!waiting) {
                    try { socket.setSoTimeout(0); } catch (SocketException ignored) {}

                    System.out.print("Your guess (or 'q' to quit): ");
                    String input;
                    try {
                        input = scanner.nextLine().trim();
                    } catch (Exception e) {
                        break;
                    }
                    if (input.equalsIgnoreCase("q")) {
                        out.println("QUIT");
                        break;
                    }
                    try {
                        int guess = Integer.parseInt(input);
                        out.println("GUESS:" + guess);
                    } catch (NumberFormatException e) {
                        System.out.println("Enter a valid integer.");
                        continue;
                    }
                } else {
                    try { socket.setSoTimeout(WAIT_TIMEOUT_MS); } catch (SocketException ignored) {}
                }

                String response;
                try {
                    response = in.readLine();
                } catch (SocketTimeoutException ste) {
                    System.out.println("No response from server for " + (WAIT_TIMEOUT_MS/1000) + "s. Server may have crashed. Exiting.");
                    break;
                }

                if (response == null) {
                    System.out.println("Server disconnected.");
                    break;
                }

                if (response.startsWith("RESULT:")) {
                    String res = response.substring(7);
                    switch (res) {
                        case "LOW" -> System.out.println("Too low.");
                        case "HIGH" -> System.out.println("Too high.");
                        case "CORRECT" -> {
                            System.out.println("Correct! Waiting for other players...");
                            waiting = true;
                            try { socket.setSoTimeout(WAIT_TIMEOUT_MS); } catch (SocketException ignored) {}
                        }
                        default -> System.out.println("Server response: " + res);
                    }
                } else if (response.equals("WAIT")) {
                    System.out.println("Waiting for remaining players...");
                    waiting = true;
                    try { socket.setSoTimeout(WAIT_TIMEOUT_MS); } catch (SocketException ignored) {}
                } else if (response.equals("RANKING_START")) {

                    try { socket.setSoTimeout(0); } catch (SocketException ignored) {}
                    System.out.println("\n--- Ranking ---");
                    while (true) {
                        String rankLine;
                        try {
                            rankLine = in.readLine();
                        } catch (SocketTimeoutException ste) {
                            System.out.println("Server stopped responding while sending ranking. Exiting.");
                            done = true;
                            break;
                        }
                        if (rankLine == null) {
                            System.out.println("Server disconnected while sending ranking.");
                            done = true;
                            break;
                        }
                        if (rankLine.equals("RANKING_END")) {
                            break;
                        }
                        if (rankLine.startsWith("RANK:")) {
                            String[] rparts = rankLine.split(":", 4);
                            if (rparts.length >= 4) {
                                String pos = rparts[1];
                                String nick = rparts[2];
                                String result = rparts[3];
                                if ("-".equals(result)) {
                                    System.out.printf("%s) %s - did not guess%n", pos, nick);
                                } else {
                                    System.out.printf("%s) %s - %s guesses%n", pos, nick, result);
                                }
                            } else {
                                System.out.println(rankLine);
                            }
                        } else {
                            System.out.println(rankLine);
                        }
                    }
                    System.out.println("--- End ranking ---\n");
                } else if (response.equals("BYE")) {
                    System.out.println("Server ended the game.");
                    break;
                } else {
                    System.out.println("Unknown server message: " + response);
                }
            }
        }
    }

    // for tests
    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        new NetworkClient(host, port).start();
    }
}
