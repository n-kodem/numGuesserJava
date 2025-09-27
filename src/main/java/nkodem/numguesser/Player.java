package nkodem.numguesser;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Player {
    private String nickname;
    private Map<String, Integer> bestScores; // difficulty -> best score
    private Map<String, Integer> bestBotScores; // difficulty -> best score
    private int wins;
    private int losses;
    private int botGames;
    private int timesBotFooled;

    public Player(String nickname) {
        this.nickname = nickname;
        this.bestScores = new HashMap<>();
        this.bestBotScores = new HashMap<>();
        this.wins = 0;
        this.losses = 0;
        this.botGames = 0;
        this.timesBotFooled = 0;
        loadData();
    }

    public String getNickname() {
        return nickname;
    }

    public int getBestScore(String difficulty) {
        return bestScores.getOrDefault(difficulty, Integer.MAX_VALUE);
    }

    public void setBestScore(String difficulty, int score) {
        int current = bestScores.getOrDefault(difficulty, Integer.MAX_VALUE);
        if (score < current) {
            bestScores.put(difficulty, score);
        }
    }

    public int getBestBotScore(String difficulty) {
        return bestBotScores.getOrDefault(difficulty, Integer.MAX_VALUE);
    }

    public void setBestBotScore(String difficulty, int score) {
        int current = bestBotScores.getOrDefault(difficulty, Integer.MAX_VALUE);
        if (score < current) {
            bestBotScores.put(difficulty, score);
        }
    }

    public void addWin() { wins++; }
    public void addLoss() { losses++; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int addBotGames() { return botGames++; }
    public int addBotFooled() { return timesBotFooled++; }


    private void loadData() {
        File file = new File(nickname + ".txt");
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(":");
                switch (data[0]) {
                    case "easy":
                    case "normal":
                    case "hard":
                        bestScores.put(data[0], Integer.parseInt(data[1]));
                        break;
                    case "wins":
                        wins = Integer.parseInt(data[1]);
                        break;
                    case "losses":
                        losses = Integer.parseInt(data[1]);
                        break;
                    case "botEasy":
                    case "botNormal":
                    case "botHard":
                        bestBotScores.put(data[0].substring(3).toLowerCase(), Integer.parseInt(data[1]));
                        break;
                    case "botGames":
                        botGames = Integer.parseInt(data[1]);
                        break;
                    case "botFooled":
                        timesBotFooled = Integer.parseInt(data[1]);
                        break;

                }
            }
        } catch (IOException e) {
            System.out.println("Error loading player data.");
        }
    }

    public void saveData() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(nickname + ".txt"))) {
            for (String difficulty : bestScores.keySet()) {
                bw.write(difficulty + ":" + bestScores.get(difficulty));
                bw.newLine();
            }
            bw.write("wins:" + wins);
            bw.newLine();
            bw.write("losses:" + losses);
            bw.newLine();
            for (String difficulty : bestBotScores.keySet()) {
                bw.write("bot"+difficulty.substring(0, 1).toUpperCase() + difficulty.substring(1) + ":" + bestBotScores.get(difficulty));
                bw.newLine();
            }
            bw.write("botGames:" + botGames);
            bw.newLine();
            bw.write("botFooled:" + timesBotFooled);
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error saving player data.");
        }
    }
}

