package nkodem.numguesser;

import java.util.Random;
import java.util.Scanner;

public class MixedGame {
    private Player player;
    private AIPlayer ai;
    private Random random;
    private Scanner scanner;
    private int min;
    private int max;

    public MixedGame(Player player, AIPlayer ai, int min, int max) {
        this.player = player;
        this.ai = ai;
        this.random = new Random();
        this.scanner = new Scanner(System.in);
        this.min = min;
        this.max = max;
    }

    public void start() {
        int secret = random.nextInt(max - min + 1) + min;
        boolean playerTurn = random.nextBoolean();

        System.out.println("Mixed game started!");
        System.out.println("Secret number is between " + min + " and " + max + ".");
        System.out.println("Coin toss: " + (playerTurn ? "Player starts!" : "Computer starts!"));

        while (true) {
            int guess;
            if (playerTurn) {
                System.out.print("Your guess: ");
                guess = scanner.nextInt();

                if (guess == secret) {
                    System.out.println("You win!");
                    player.addWin();
                    ai.addLoss();
                    break;
                } else if (guess < secret) {
                    System.out.println("Too low!");
                } else {
                    System.out.println("Too high!");
                }
            } else {
                guess = random.nextInt(max - min + 1) + min;
                System.out.println("Computer guesses: " + guess);

                if (guess == secret) {
                    System.out.println("Computer wins!");
                    ai.addWin();
                    player.addLoss();
                    break;
                } else if (guess < secret) {
                    System.out.println("Computer: too low.");
                } else {
                    System.out.println("Computer: too high.");
                }
            }

            // limit bot guess range
            if (guess < secret) {
                min = guess + 1;
            } else {
                max = guess - 1;
            }
            // switch turns
            playerTurn = !playerTurn;
        }
    }
}
