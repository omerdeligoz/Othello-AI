import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;

public class Reversi extends JFrame {
    private final GameEngine gameEngine; // The game engine that handles the game logic
    private JButton[][] boardButtons; // 2D array of buttons representing the game board
    private JLabel turnLabel, scoreLabel, depthLabel, searchedNodesLabel, lastMoveLabel; // Labels for game status

    // Constructor to initialize the Reversi game
    public Reversi() {
        super("Reversi"); // Set the title of the window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close the application when the window is closed
        setLayout(new BorderLayout()); // Use BorderLayout for the main layout

        // Game mode selection dialog
        String[] gameModes = {"Human vs Human", "Human vs AI", "AI vs AI"}; // Available game modes
        int gameModeChoice = JOptionPane.showOptionDialog(this,
                "Select Game Mode", // Dialog title
                "Reversi", // Dialog header
                JOptionPane.DEFAULT_OPTION, // Option type
                JOptionPane.QUESTION_MESSAGE, // Message type
                null, // Icon
                gameModes, // Options
                gameModes[0]); // Default option

        GameMode gameMode = GameMode.values()[gameModeChoice]; // Convert the choice to a GameMode enum

        AIDifficulty blackAIDifficulty = null; // Difficulty for the black AI (if applicable)
        AIDifficulty whiteAIDifficulty = null; // Difficulty for the white AI (if applicable)

        // If the game mode involves AI, prompt for AI difficulty
        if (gameMode == GameMode.HumanvsAI || gameMode == GameMode.AIvsAI) {
            String[] difficulties = {"Easy", "Medium", "Hard", "Expert"}; // Available AI difficulties
            int difficultyChoiceBlack; // Difficulty choice for black AI
            int difficultyChoiceWhite; // Difficulty choice for white AI

            if (gameMode == GameMode.HumanvsAI) {
                // Player chooses color in Human vs AI mode
                String[] colors = {"Black", "White"}; // Available colors
                int colorChoice = JOptionPane.showOptionDialog(this,
                        "Choose your color", // Dialog title
                        "Color Selection", // Dialog header
                        JOptionPane.DEFAULT_OPTION, // Option type
                        JOptionPane.QUESTION_MESSAGE, // Message type
                        null, // Icon
                        colors, // Options
                        colors[0]); // Default option

                if (colorChoice == 0) {
                    // Player is Black, AI is White
                    difficultyChoiceWhite = JOptionPane.showOptionDialog(this,
                            "Select AI Difficulty for White", // Dialog title
                            "AI Difficulty", // Dialog header
                            JOptionPane.DEFAULT_OPTION, // Option type
                            JOptionPane.QUESTION_MESSAGE, // Message type
                            null, // Icon
                            difficulties, // Options
                            difficulties[0]); // Default option
                    whiteAIDifficulty = AIDifficulty.values()[difficultyChoiceWhite]; // Set white AI difficulty
                } else {
                    // Player is White, AI is Black
                    difficultyChoiceBlack = JOptionPane.showOptionDialog(this,
                            "Select AI Difficulty for Black", // Dialog title
                            "AI Difficulty", // Dialog header
                            JOptionPane.DEFAULT_OPTION, // Option type
                            JOptionPane.QUESTION_MESSAGE, // Message type
                            null, // Icon
                            difficulties, // Options
                            difficulties[0]); // Default option
                    blackAIDifficulty = AIDifficulty.values()[difficultyChoiceBlack]; // Set black AI difficulty
                }
            } else if (gameMode == GameMode.AIvsAI) {
                // AI vs AI mode: select difficulty for both AIs
                difficultyChoiceBlack = JOptionPane.showOptionDialog(this,
                        "Select AI Difficulty for Black", // Dialog title
                        "AI Difficulty", // Dialog header
                        JOptionPane.DEFAULT_OPTION, // Option type
                        JOptionPane.QUESTION_MESSAGE, // Message type
                        null, // Icon
                        difficulties, // Options
                        difficulties[0]); // Default option
                blackAIDifficulty = AIDifficulty.values()[difficultyChoiceBlack]; // Set black AI difficulty

                difficultyChoiceWhite = JOptionPane.showOptionDialog(this,
                        "Select AI Difficulty for White", // Dialog title
                        "AI Difficulty", // Dialog header
                        JOptionPane.DEFAULT_OPTION, // Option type
                        JOptionPane.QUESTION_MESSAGE, // Message type
                        null, // Icon
                        difficulties, // Options
                        difficulties[0]); // Default option
                whiteAIDifficulty = AIDifficulty.values()[difficultyChoiceWhite]; // Set white AI difficulty
            }
        }

        // Set the window title based on the game mode
        if (gameMode == GameMode.AIvsAI) {
            setTitle("Reversi - AI(" + blackAIDifficulty + ") vs AI(" + whiteAIDifficulty + ")");
        } else if (gameMode == GameMode.HumanvsAI) {
            setTitle("Reversi - Human vs AI(" + (blackAIDifficulty == null ? whiteAIDifficulty : blackAIDifficulty) + ")");
        } else {
            setTitle("Reversi - Human vs Human");
        }

        // Initialize the game engine with the selected mode and AI difficulties
        gameEngine = new GameEngine(this, gameMode, blackAIDifficulty, whiteAIDifficulty);

        initializeUI(); // Set up the user interface
        gameEngine.startGame(); // Start the game
    }

    // Initialize the user interface
    private void initializeUI() {
        // Create board buttons
        boardButtons = new JButton[Board.BOARD_SIZE][Board.BOARD_SIZE]; // Initialize the 2D array of buttons
        JPanel boardPanel = new JPanel(new GridLayout(Board.BOARD_SIZE, Board.BOARD_SIZE)); // Create a grid layout for the board
        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                JButton button = getJButton(i, j); // Create a button for each cell
                boardButtons[i][j] = button; // Add the button to the array
                boardPanel.add(button); // Add the button to the board panel
            }
        }

        // Create control buttons
        JButton newGameButton = new JButton("New Game"); // Button to start a new game
        newGameButton.addActionListener(_ -> {
            dispose(); // Close the current window
            new Reversi().setVisible(true); // Open a new game window
        });

        JButton undoButton = new JButton("Undo"); // Button to undo the last move
        undoButton.addActionListener(_ -> gameEngine.undoMove()); // Trigger undo functionality

        JButton exitButton = new JButton("Exit"); // Button to exit the game
        exitButton.addActionListener(_ -> System.exit(0)); // Exit the application

        // Create control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Panel for control buttons
        controlPanel.add(newGameButton); // Add new game button
        controlPanel.add(undoButton); // Add undo button
        controlPanel.add(exitButton); // Add exit button

        // Create status labels
        turnLabel = new JLabel("Black's turn"); // Label to show whose turn it is
        scoreLabel = new JLabel("Black: 2  White: 2"); // Label to show the current score
        depthLabel = new JLabel("Game Depth: 4  Remaining: 60"); // Label to show game depth and remaining tiles
        searchedNodesLabel = new JLabel("Searched Nodes: 0"); // Label to show the number of nodes searched by the AI
        lastMoveLabel = new JLabel("Last Move: None"); // Label to show the last move made

        // Create info panel for status labels
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS)); // Use vertical layout
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding around the info panel
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center align the turn label
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center align the score label
        depthLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center align the depth label
        searchedNodesLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center align the searched nodes label
        lastMoveLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center align the last move label
        infoPanel.add(turnLabel); // Add turn label to info panel
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Add spacing between labels
        infoPanel.add(scoreLabel); // Add score label to info panel
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Add spacing between labels
        infoPanel.add(depthLabel); // Add depth label to info panel
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Add spacing between labels
        infoPanel.add(searchedNodesLabel); // Add searched nodes label to info panel
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Add spacing between labels
        infoPanel.add(lastMoveLabel); // Add last move label to info panel

        // Add panels to the main window
        add(controlPanel, BorderLayout.NORTH); // Add control panel to the top
        add(boardPanel, BorderLayout.CENTER); // Add board panel to the center
        add(infoPanel, BorderLayout.SOUTH); // Add info panel to the bottom

        pack(); // Pack the components to fit the window
        setLocationRelativeTo(null); // Center the window on the screen
    }

    // Helper method to create a JButton for a specific cell on the board
    private JButton getJButton(int i, int j) {
        JButton button = new JButton(i + "," + j); // Create a button with initial text (row, col)
        button.setPreferredSize(new Dimension(Board.CELL_SIZE, Board.CELL_SIZE)); // Set button size
        button.setBackground(new Color(0, 120, 0)); // Set button background color
        final int row = i;
        final int col = j;
        button.addActionListener(_ -> gameEngine.makePlayerMove(row, col)); // Add action listener for player moves
        return button;
    }

    // Update the board UI based on the current game state
    public void updateBoard(int[][] boardState, boolean isBlackTurn, int lastMoveRow, int lastMoveCol) {
        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                boardButtons[i][j].setBackground(new Color(0, 120, 0)); // Reset background color
                boardButtons[i][j].setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1)); // Set default border

                if (boardState[i][j] == 1) {
                    // Black piece
                    boardButtons[i][j].setIcon(createDiskIcon(Color.BLACK)); // Set black disk icon
                    boardButtons[i][j].setText(""); // Clear text
                } else if (boardState[i][j] == 2) {
                    // White piece
                    boardButtons[i][j].setIcon(createDiskIcon(Color.WHITE)); // Set white disk icon
                    boardButtons[i][j].setText(""); // Clear text
                } else {
                    // Empty cell
                    boardButtons[i][j].setIcon(null); // Clear icon
                    boardButtons[i][j].setText(i + "," + j); // Show cell coordinates
                    if (Board.isValidMove(i, j, isBlackTurn, boardState)) {
                        boardButtons[i][j].setBackground(new Color(0, 180, 0)); // Highlight valid moves
                    } else {
                        boardButtons[i][j].setBackground(new Color(0, 120, 0)); // Default background for invalid moves
                    }
                }
                if (i == lastMoveRow && j == lastMoveCol) {
                    boardButtons[i][j].setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3)); // Highlight last move
                }
            }
        }
    }

    // Update the status labels (turn, score, depth, searched nodes)
    public void updateStatus(String status, String score, int lastMoveRow, int lastMoveCol) {
        turnLabel.setText(status); // Update turn label
        scoreLabel.setText(score); // Update score label
        depthLabel.setText("Placed Tiles: " + gameEngine.placedTiles + "  Remaining: " + (Board.BOARD_SIZE * Board.BOARD_SIZE - gameEngine.placedTiles)); // Update depth label
        searchedNodesLabel.setText("Searched Nodes: " + NumberFormat.getInstance().format(gameEngine.searchedNodes)); // Update searched nodes label
        lastMoveLabel.setText("Last Move: " + lastMoveRow + "," + lastMoveCol); // Update last move label
    }

    // Show the end game dialog with the winner and final score
    public void showEndGameDialog(String winner, int blackCount, int whiteCount) {
        String message = String.format("Game Over!\nBlack: %d\nWhite: %d\n%s wins!", blackCount, whiteCount, winner); // End game message
        int option = JOptionPane.showOptionDialog(this,
                message, // Dialog message
                "Game Over", // Dialog title
                JOptionPane.DEFAULT_OPTION, // Option type
                JOptionPane.INFORMATION_MESSAGE, // Message type
                null, // Icon
                new String[]{"New Game", "Exit"}, // Options
                "New Game"); // Default option

        if (option == 0) {
            // Start a new game
            dispose(); // Close the current window
            new Reversi().setVisible(true); // Open a new game window
        } else {
            // Exit the game
            System.exit(0); // Exit the application
        }
    }

    // Show an error message dialog
    public void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE); // Show error dialog
    }

    // Helper method to create a disk icon for the board
    private ImageIcon createDiskIcon(Color color) {
        int size = Board.CELL_SIZE - 10; // Set the size of the disk
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB); // Create a buffered image
        Graphics2D g2d = image.createGraphics(); // Get graphics context
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Enable anti-aliasing
        g2d.setColor(color); // Set the color of the disk
        g2d.fillOval(0, 0, size, size); // Draw the disk
        g2d.dispose(); // Release graphics resources
        return new ImageIcon(image); // Return the disk as an ImageIcon
    }

    // Main method to start the application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Reversi().setVisible(true)); // Launch the game on the event dispatch thread
    }
}