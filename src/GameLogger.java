import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GameLogger {
    private static final String LOG_DIRECTORY = "game_logs"; // Directory where game logs are stored
    private static final String LOG_FILE = "game_history.txt"; // File name for the game log

    // Log the result of a game
    public static void logGameResult(String gameMode, String player1, String player2, long gameStartTime,
                                     int searchedNodes, int blackCount, int whiteCount, String winner) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()); // Get the current timestamp
            String logFileName = LOG_DIRECTORY + "/" + LOG_FILE; // Full path to the log file
            DecimalFormat formatter = new DecimalFormat("#,###"); // Formatter for large numbers
            String formattedNumber = formatter.format(searchedNodes); // Format the number of searched nodes

            // Open the log file in append mode
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFileName, true))) {
                // Write the game log details
                writer.println("\nGame Log - " + timestamp); // Timestamp of the game
                writer.println("----------------------------------------"); // Separator
                writer.println("Game Mode: " + gameMode); // Game mode (e.g., Human vs AI, AI vs AI)
                writer.println("Game Duration: " + formatDuration(System.currentTimeMillis() - gameStartTime)); // Duration of the game
                writer.println("Searched Nodes: " + formattedNumber); // Number of nodes searched by the AI
                writer.println("Black Player: " + player1); // Black player type (AI or Human)
                writer.println("White Player: " + player2); // White player type (AI or Human)
                writer.println("Final Score:"); // Final score of the game
                writer.println("  Black: " + blackCount); // Number of black pieces
                writer.println("  White: " + whiteCount); // Number of white pieces
                writer.println("Winner: " + winner); // Winner of the game (Black, White, or Tie)
                writer.println("----------------------------------------"); // Separator
                writer.flush(); // Ensure all data is written to the file
            }
        } catch (Exception e) {
            e.printStackTrace(); // Handle any exceptions that occur during logging
        }
    }

    // Format the duration of the game in minutes and seconds
    private static String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000; // Convert milliseconds to seconds
        long minutes = seconds / 60; // Convert seconds to minutes
        seconds = seconds % 60; // Remaining seconds after converting to minutes
        return String.format("%d minutes, %d seconds", minutes, seconds); // Format as "X minutes, Y seconds"
    }
}