package Modular;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GameLogger {
    private static final String LOG_DIRECTORY = "game_logs";
    private static final String LOG_FILE = "game_history.txt";

    public static void logGameResult(String gameMode, String player1, String player2, long gameStartTime,
            int searchedNodes, int blackCount, int whiteCount, String winner) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String logFileName = LOG_DIRECTORY + "/" + LOG_FILE;
            DecimalFormat formatter = new DecimalFormat("#,###");
            String formattedNumber = formatter.format(searchedNodes);
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFileName, true))) {
                writer.println("\nGame Log - " + timestamp);
                writer.println("----------------------------------------");
                writer.println("Game Mode: " + gameMode);
                writer.println("Game Duration: " + formatDuration(System.currentTimeMillis() - gameStartTime));
                writer.println("Searched Nodes: " + formattedNumber);
                writer.println("Black Player: " + player1);
                writer.println("White Player: " + player2);
                writer.println("Final Score:");
                writer.println("  Black: " + blackCount);
                writer.println("  White: " + whiteCount);
                writer.println("Winner: " + winner);
                writer.println("----------------------------------------");
                writer.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d minutes, %d seconds", minutes, seconds);
    }
}