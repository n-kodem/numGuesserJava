package nkodem.numguesser;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nkodem.numguesser.network.NetworkClient;
import nkodem.numguesser.network.NetworkServer;



public class Main {
    private static final ExecutorService bg = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // ====== Player login ======
        System.out.print("Enter your nickname: ");
        String nickname = scanner.nextLine();
        Player player = new Player(nickname);
        AIPlayer ai = new AIPlayer("programtest");

        boolean running = true;

        while (running) {
            System.out.println("\n=== MAIN MENU ===");
            System.out.println("1. Single Game (you guess)");
            System.out.println("2. Reverse Game (computer guesses)");
            System.out.println("3. Mixed Game (take turns)");
            System.out.println("4. Host game");
            System.out.println("5. Join game");
            System.out.println("0. Exit");
            System.out.print("Choose mode: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // clear buffer

            switch (choice) {
                case 1 -> { // Single
                    int[] range = chooseDifficulty(scanner);
                    SingleGame game = new SingleGame(player, range[0], range[1], getDifficultyName(range));
                    game.start();
                }
                case 2 -> { // Reverse
                    int[] range = chooseDifficulty(scanner);
                    ReverseGame game = new ReverseGame(player, range[0], range[1], getDifficultyName(range));
                    game.start();
                }
                case 3 -> { // Mixed
                    int[] range = chooseDifficulty(scanner);
                    MixedGame game = new MixedGame(player, ai, range[0], range[1]);
                    game.start();
                }
                case 4 -> hostGame(scanner);
                case 5 -> joinGame(scanner);
                case 0 -> {
                    System.out.println("Saving data and exiting...");
                    player.saveData();
                    ai.saveData();
                    running = false;
                }
                default -> System.out.println("Invalid choice!");
            }
        }
    }

    private static void joinGame(Scanner scanner) {
        try {
            System.out.print("Host (default localhost): ");
            String host = scanner.nextLine().trim();
            if (host.isEmpty()) host = "localhost";

            System.out.print("Port (default 5000): ");
            String portStr = scanner.nextLine().trim();
            int port = portStr.isEmpty() ? 5000 : Integer.parseInt(portStr);

            try {
                new NetworkClient(host, port).start();
            } catch (IOException e) {
                System.out.println("NetworkClient error: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number input.");
        }
    }

    private static void hostGame(Scanner scanner) {
        try {
            System.out.print("Port (default 5000): ");
            String portStr = scanner.nextLine().trim();
            int port = portStr.isEmpty() ? 5000 : Integer.parseInt(portStr);

            System.out.print("Low bound (default 0): ");
            String lowStr = scanner.nextLine().trim();
            int low = lowStr.isEmpty() ? 0 : Integer.parseInt(lowStr);

            System.out.print("High bound (default 100): ");
            String highStr = scanner.nextLine().trim();
            int high = highStr.isEmpty() ? 100 : Integer.parseInt(highStr);

            System.out.print("Specify player number: (default 2): ");
            String playerNumStr = scanner.nextLine().trim();
            int playerCount = playerNumStr.isEmpty()? 2 : Integer.parseInt(playerNumStr);
            System.out.println("Starting server in on port " + port + ". Waiting for clients...");
            try {
                new NetworkServer(port, low, high, playerCount).start();
            } catch (IOException e) {
                System.out.println("NetworkServer error: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number input.");
        }
    }


    private static int[] chooseDifficulty(Scanner scanner) {
        while (true) {
            System.out.println("\n=== Difficulty Levels ===");
            System.out.println("1. Easy (0–100)");
            System.out.println("2. Normal (0–10,000)");
            System.out.println("3. Hard (0–1,000,000)");
            System.out.println("4. Custom (specify min and max)");
            System.out.print("Choose difficulty: ");

            String line = scanner.nextLine().trim();
            int choice;
            try {
                choice = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number between 1 and 4.");
                continue;
            }

            switch (choice) {
                case 1 -> { return new int[]{0, 100}; }
                case 2 -> { return new int[]{0, 10000}; }
                case 3 -> { return new int[]{0, 1000000}; }
                case 4 -> {
                    Integer min = null;
                    Integer max = null;
                    while (min == null) {
                        System.out.print("Enter minimum value: ");
                        String minLine = scanner.nextLine().trim();
                        try {
                            min = Integer.parseInt(minLine);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid integer. Try again.");
                        }
                    }
                    while (max == null) {
                        System.out.print("Enter maximum value: ");
                        String maxLine = scanner.nextLine().trim();
                        try {
                            max = Integer.parseInt(maxLine);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid integer. Try again.");
                        }
                    }
                    if (min >= max) {
                        System.out.println("Minimum must be less than maximum. Choose difficulty again.");
                        continue;
                    }
                    return new int[]{min, max};
                }
                default -> System.out.println("Invalid choice. Enter 1-4.");
            }
        }
    }

    private static String getDifficultyName(int[] range) {
        if (range[1] == 100 && range[0] == 0) return "easy";
        if (range[1] == 10000 && range[0] == 0) return "normal";
        if (range[1] == 1000000 && range[0] == 0) return "hard";
        return "custom";
    }
}
