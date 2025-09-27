package nkodem.numguesser;

import java.util.Scanner;

public class Main {
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

    private static int[] chooseDifficulty(Scanner scanner) {
        System.out.println("\n=== Difficulty Levels ===");
        System.out.println("1. Easy (0–100)");
        System.out.println("2. Normal (0–10,000)");
        System.out.println("3. Hard (0–1,000,000)");
        System.out.print("Choose difficulty: ");

        int choice = scanner.nextInt();
        scanner.nextLine();

        return switch (choice) {
            case 2 -> new int[]{0, 10000};
            case 3 -> new int[]{0, 1000000};
            default -> new int[]{0, 100};
        };
    }

    private static String getDifficultyName(int[] range) {
        if (range[1] == 100) return "easy";
        if (range[1] == 10000) return "normal";
        return "hard";
    }
}
