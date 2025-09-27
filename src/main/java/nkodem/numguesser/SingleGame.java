package nkodem.numguesser;

import java.util.Random;
import java.util.Scanner;

public class SingleGame {
    private Player player;
    private Random random;
    private Scanner scanner;
    private int min;
    private int max;
    private String difficulty;

    public SingleGame(Player player, int min, int max, String difficulty) {
        this.player = player;
        this.random = new Random();
        this.scanner = new Scanner(System.in);
        this.min = min;
        this.max = max;
        this.difficulty = difficulty;
    }

    public void start() {
        int secret = random.nextInt(max - min + 1) + min;
        int attempts = 0;
        int guess;

        System.out.println("Guess the number between " + min + " and " + max + "!");

        while (true) {
            System.out.print("Your guess: ");
            guess = scanner.nextInt();
            attempts++;

            if (guess < secret) {
                System.out.println("Too low!");
            } else if (guess > secret) {
                System.out.println("Too high!");
            } else {
                System.out.println("Correct! You guessed in " + attempts + " attempts.");
                player.setBestScore(difficulty, attempts);
                break;
            }
        }
    }
}
