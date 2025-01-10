#### **Overview**
This project implements a Reversi (Othello) game with various game modes: Human vs Human, Human vs AI, and AI vs AI. The AI is available in four difficulty levels: Easy, Medium, Hard, and Expert. The game features a graphical user interface (GUI) built using Java Swing.

---

#### **Prerequisites**

1. **Java Development Kit (JDK):**
   - Ensure JDK 8 or above is installed on your system.
   - [Download JDK](https://www.oracle.com/java/technologies/javase-jdk-downloads.html)

2. **Environment:**
   - The program is written in Java and can run on Windows, macOS, and Linux.

3. **IDE (Optional):**
   - Use any Java IDE, such as IntelliJ IDEA, Eclipse, or NetBeans, to view and run the source code.

---

#### **Files in the Project**

- `AI.java`: Implements the AI logic for move decision-making.
- `AIDifficulty.java`: Enum for AI difficulty levels.
- `Board.java`: Contains logic for board state and move validation.
- `GameEngine.java`: Core game logic, including move execution and turn management.
- `GameLogger.java`: Handles logging of game results.
- `GameMode.java`: Enum for game modes (Human vs AI, AI vs AI, etc.).
- `Move.java`: Represents a single move with row and column.
- `MoveHistory.java`: Tracks the game state for undo functionality.
- `Reversi.java`: Main class with GUI implementation.
- `WeightType.java`: Defines evaluation weights for AI difficulty.

---

#### **How to Run**

1. **Option 1: Using an IDE**
   - Import the project into your preferred IDE.
   - Locate the `Reversi.java` file.
   - Run the `main` method in `Reversi.java`.

2. **Option 2: Using Command Line**
   - Compile the program:
     ```bash
     javac *.java
     ```
   - Run the program:
     ```bash
     java Reversi
     ```

---

#### **Gameplay Instructions**

1. **Launch the Program:**
   - Upon running the program, a dialog box will prompt you to select the game mode:
     - Human vs Human
     - Human vs AI
     - AI vs AI

2. **For Human vs AI Mode:**
   - Choose your color (Black or White).
   - Select the difficulty level for the AI.

3. **For AI vs AI Mode:**
   - Select the difficulty level for both Black and White AIs.

4. **Game Controls:**
   - **New Game**: Starts a new game.
   - **Undo**: Reverts the last move.
   - **Exit**: Closes the application.

5. **Board Interaction:**
   - Click on the valid cells (highlighted in green) to make a move.

---

#### **Game Logs**

- Game results are logged in the `game_logs/game_history.txt` file.
- Log details include:
  - Game mode
  - Duration
  - Searched nodes (AI performance)
  - Final scores
  - Winner

