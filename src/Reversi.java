import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;

public class Reversi extends JFrame {
    private final GameEngine gameEngine;
    private JButton[][] boardButtons;
    private JLabel turnLabel, scoreLabel, depthLabel, searchedNodesLabel, lastMoveLabel;


    public Reversi() {
        super("Reversi");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Game mode selection
        String[] gameModes = {"Human vs Human", "Human vs AI", "AI vs AI"};
        int gameModeChoice = JOptionPane.showOptionDialog(this,
                "Select Game Mode",
                "Reversi",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                gameModes,
                gameModes[0]);

        GameMode gameMode = GameMode.values()[gameModeChoice];

        AIDifficulty blackAIDifficulty = null;
        AIDifficulty whiteAIDifficulty = null;

        if (gameMode == GameMode.HumanvsAI || gameMode == GameMode.AIvsAI) {
            // AI difficulty selection
            String[] difficulties = {"Easy", "Medium", "Hard", "Expert"};
            int difficultyChoiceBlack;
            int difficultyChoiceWhite;

            if (gameMode == GameMode.HumanvsAI) {
                // Player chooses color
                String[] colors = {"Black", "White"};
                int colorChoice = JOptionPane.showOptionDialog(this,
                        "Choose your color",
                        "Color Selection",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        colors,
                        colors[0]);

                if (colorChoice == 0) {
                    // Player is Black, AI is White
                    difficultyChoiceWhite = JOptionPane.showOptionDialog(this,
                            "Select AI Difficulty for White",
                            "AI Difficulty",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            difficulties,
                            difficulties[0]);
                    whiteAIDifficulty = AIDifficulty.values()[difficultyChoiceWhite];
                } else {
                    // Player is White, AI is Black
                    difficultyChoiceBlack = JOptionPane.showOptionDialog(this,
                            "Select AI Difficulty for Black",
                            "AI Difficulty",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            difficulties,
                            difficulties[0]);
                    blackAIDifficulty = AIDifficulty.values()[difficultyChoiceBlack];
                }
            } else if (gameMode == GameMode.AIvsAI) {
                // AI vs AI
                difficultyChoiceBlack = JOptionPane.showOptionDialog(this,
                        "Select AI Difficulty for Black",
                        "AI Difficulty",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        difficulties,
                        difficulties[0]);
                blackAIDifficulty = AIDifficulty.values()[difficultyChoiceBlack];

                difficultyChoiceWhite = JOptionPane.showOptionDialog(this,
                        "Select AI Difficulty for White",
                        "AI Difficulty",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        difficulties,
                        difficulties[0]);
                whiteAIDifficulty = AIDifficulty.values()[difficultyChoiceWhite];
            }
        }
        if (gameMode == GameMode.AIvsAI) {
            setTitle("Reversi - AI(" + blackAIDifficulty + ") vs AI(" + whiteAIDifficulty + ")");
        } else if (gameMode == GameMode.HumanvsAI) {
            setTitle("Reversi - Human vs AI(" + (blackAIDifficulty == null ? whiteAIDifficulty : blackAIDifficulty)
                    + ")");
        } else {
            setTitle("Reversi - Human vs Human");
        }
        gameEngine = new GameEngine(this, gameMode, blackAIDifficulty, whiteAIDifficulty);

        initializeUI();
        gameEngine.startGame();
    }

    private void initializeUI() {
        // Create board buttons
        boardButtons = new JButton[Board.BOARD_SIZE][Board.BOARD_SIZE];
        JPanel boardPanel = new JPanel(new GridLayout(Board.BOARD_SIZE, Board.BOARD_SIZE));
        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                JButton button = getJButton(i, j);
                boardButtons[i][j] = button;
                boardPanel.add(button);
            }
        }

        // Create control buttons
        JButton newGameButton = new JButton("New Game");
        newGameButton.addActionListener(_ -> {
            dispose();
            new Reversi().setVisible(true);
        });
        JButton undoButton = new JButton("Undo");
        undoButton.addActionListener(_ -> gameEngine.undoMove());
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(_ -> System.exit(0));

        // Create control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        controlPanel.add(newGameButton);
        controlPanel.add(undoButton);
        controlPanel.add(exitButton);


        // Create status labels
        turnLabel = new JLabel("Black's turn");
        scoreLabel = new JLabel("Black: 2  White: 2");
        depthLabel = new JLabel("Game Depth: 4  Remaining: 60");
        searchedNodesLabel = new JLabel("Searched Nodes: 0");
        lastMoveLabel = new JLabel("Last Move: None");

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding around the info panel
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        depthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchedNodesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        lastMoveLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(turnLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Add spacing between labels
        infoPanel.add(scoreLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Add spacing between labels
        infoPanel.add(depthLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Add spacing between labels
        infoPanel.add(searchedNodesLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Add spacing between labels
        infoPanel.add(lastMoveLabel);

        add(controlPanel, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    private JButton getJButton(int i, int j) {
        JButton button = new JButton(i + "," + j);
        button.setPreferredSize(new Dimension(Board.CELL_SIZE, Board.CELL_SIZE));
        button.setBackground(new Color(0, 120, 0));
        final int row = i;
        final int col = j;
        button.addActionListener(_ -> gameEngine.makePlayerMove(row, col));
        return button;
    }

    public void updateBoard(int[][] boardState, boolean isBlackTurn, int lastMoveRow, int lastMoveCol) {
        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                boardButtons[i][j].setBackground(new Color(0, 120, 0)); // Reset background
                boardButtons[i][j].setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1)); // Default border

                if (boardState[i][j] == 1) {
                    boardButtons[i][j].setIcon(createDiskIcon(Color.BLACK));
                    boardButtons[i][j].setText("");
                } else if (boardState[i][j] == 2) {
                    boardButtons[i][j].setIcon(createDiskIcon(Color.WHITE));
                    boardButtons[i][j].setText("");
                } else {
                    boardButtons[i][j].setIcon(null);
                    boardButtons[i][j].setText(i + "," + j);
                    if (Board.isValidMove(i, j, isBlackTurn, boardState)) {
                        boardButtons[i][j].setBackground(new Color(0, 180, 0)); // Highlight valid moves
                    } else {
                        boardButtons[i][j].setBackground(new Color(0, 120, 0)); // Default background
                    }
                }
                if (i == lastMoveRow && j == lastMoveCol) {
                    boardButtons[i][j].setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3)); // Yellow border
                }
            }
        }
        lastMoveLabel.setText("Last Move: " + lastMoveRow + "," + lastMoveCol);
    }

    public void updateStatus(String status, String score) {
        turnLabel.setText(status);
        scoreLabel.setText(score);
        depthLabel.setText("Placed Tiles: " + gameEngine.placedTiles + "  Remaining: " + (Board.BOARD_SIZE * Board.BOARD_SIZE - gameEngine.placedTiles));
        searchedNodesLabel.setText("Searched Nodes: " + NumberFormat.getInstance().format(gameEngine.searchedNodes));
    }

    public void showEndGameDialog(String winner, int blackCount, int whiteCount) {
        String message = String.format("Game Over!\nBlack: %d\nWhite: %d\n%s wins!", blackCount, whiteCount, winner);
        int option = JOptionPane.showOptionDialog(this,
                message,
                "Game Over",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new String[]{"New Game", "Exit"},
                "New Game");

        if (option == 0) {
            // Start a new game
            dispose();
            new Reversi().setVisible(true);
        } else {
            // Exit the game
            System.exit(0);
        }
    }

    public void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private ImageIcon createDiskIcon(Color color) {
        int size = Board.CELL_SIZE - 10;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);
        g2d.fillOval(0, 0, size, size);
        g2d.dispose();
        return new ImageIcon(image);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Reversi().setVisible(true));
    }
}