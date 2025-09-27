package nkodem.numguesser;

import java.util.Scanner;

public class ReverseGame {
    private int min;
    private int max;
    private String difficulty;
    private Scanner scanner;
    private Player player;

    public ReverseGame(Player player, int min, int max, String difficulty) {
        this.min = min;
        this.max = max;
        this.player = player;
        this.difficulty = difficulty;
        this.scanner = new Scanner(System.in);
    }

    public boolean start() {
        System.out.println("Think of a number between " + min + " and " + max + ".");
        System.out.println("Answer with: 'higher', 'lower', 'correct'.");

        int attempts = 0;
        while (min <= max) {
            int guess = (min + max) / 2; // binary search strategy
            System.out.println("Is your number " + guess + "?");
            attempts++;

            String answer = scanner.nextLine().trim().toLowerCase();

            if (answer.equals("correct")) {
                System.out.println("I guessed it in " + attempts + " attempts!");
                player.addBotGames();
                player.setBestBotScore(difficulty, attempts);
                return true;
            } else if (answer.equals("higher")) {
                min = guess + 1;
            } else if (answer.equals("lower")) {
                max = guess - 1;
            } else {
                System.out.println("Please type: 'higher', 'lower' or 'correct'.");
            }
        }
        System.out.println("Something went wrong, maybe you cheated 😉");
        player.addBotFooled();
        return false;
    }
}
