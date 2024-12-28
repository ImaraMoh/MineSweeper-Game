import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;

public class MiniSweeper {
    public static void main(String[] args) {
        new StartWindow();
    }
}

class StartWindow extends JFrame {
    public StartWindow() {
        setTitle("MiniSweeper");
        setSize(400, 200);
        setLayout(new FlowLayout());

        // Create the main panel for the game window
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Create and set the font for the label
        JLabel titleLabel = new JLabel("MiniSweeper Game");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(Color.BLACK);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT); // Center the label

        // Create "New Game" button and set its size
        JButton newGameButton = new JButton("New Game");
        newGameButton.setPreferredSize(new Dimension(200, 60)); // Larger size for New Game
        newGameButton.setAlignmentX(CENTER_ALIGNMENT); // Center the button

        // Create "Exit" button and set its size
        JButton exitButton = new JButton("Exit");
        exitButton.setPreferredSize(new Dimension(100, 40)); // Smaller size for Exit
        exitButton.setAlignmentX(CENTER_ALIGNMENT); // Center the button

        newGameButton.addActionListener(e -> {
            // Show level selection dialog
            String[] options = { "10x10", "15x15" };
            int choice = JOptionPane.showOptionDialog(this, "Select a game level:", "Game Level",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

            // Based on selection, start the game
            if (choice == 0) {
                new GameBoard(10, 10, 10); // 10x10 grid with 10 mines
            } else if (choice == 1) {
                new GameBoard(15, 15, 20); // 15x15 grid with 15 mines
            }

            dispose(); // Close the start window after game starts
        });

        exitButton.addActionListener(e -> System.exit(0)); // Exit the game

        // Add components to the panel
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(20)); // Add space between label and button
        panel.add(newGameButton);
        panel.add(Box.createVerticalStrut(10)); // Space between buttons
        panel.add(exitButton);

        // Add the panel to the frame
        add(panel);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        setLocationRelativeTo(null); // Center the window
    }
}

class GameBoard extends JFrame {
    private int rows, cols, mines;
    private JButton[][] grid;
    private boolean[][] mineLocations;
    private boolean[][] flags;
    private int[][] adjacentMines;
    private int remainingFlags; // To track remaining flags/mine count
    private boolean firstClick = true;
    private Timer timer;
    private int seconds = 0;
    private JLabel timerLabel, mineCounterLabel;
    private static final String SAVE_FILE = "minisweeper_save.dat";
    private static final String HIGH_SCORE_FILE = "minisweeper_high_scores.dat";
    private int highScore10x10 = Integer.MAX_VALUE;
    private int highScore15x15 = Integer.MAX_VALUE;

    public GameBoard(int rows, int cols, int mines) {
        this.rows = rows;
        this.cols = cols;
        this.mines = mines;
        this.remainingFlags = mines;
        grid = new JButton[rows][cols];
        mineLocations = new boolean[rows][cols];
        flags = new boolean[rows][cols];

        setTitle("Mini Sweeper");
        setSize(500, 500);
        setLayout(new BorderLayout());

        // Create game grid
        JPanel gridPanel = new JPanel(new GridLayout(rows, cols));
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                JButton button = createStyledButton();
                grid[i][j] = button;
                button.setPreferredSize(new Dimension(40, 40));
                gridPanel.add(button);

                // Add listeners
                int finalI = i, finalJ = j;
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            openCell(finalI, finalJ);
                        } else if (SwingUtilities.isRightMouseButton(e)) {
                            toggleFlag(finalI, finalJ); // Handle flag placement
                        }
                    }
                });

            }
        }

        // Timer and Mine Counter Panel
        JPanel topPanel = new JPanel(new BorderLayout());
        timerLabel = new JLabel("Time: 0s");
        mineCounterLabel = new JLabel("Mines: " + remainingFlags);

        topPanel.add(timerLabel, BorderLayout.WEST);
        topPanel.add(mineCounterLabel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Menu
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Options");
        JMenuItem mainwindow = new JMenuItem("Main Window");
        mainwindow.addActionListener(e -> {
            dispose();
            new StartWindow();
        });
        JMenuItem restart = new JMenuItem("Restart");
        restart.addActionListener(e -> restartGame());
        JMenuItem withdraw = new JMenuItem("Withdraw");
        withdraw.addActionListener(e -> {
            int option = JOptionPane.showOptionDialog(
                GameBoard.this,
                "Do you want to quit the game or start a new one?",
                "Quit or Start New Game",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, // No icon
                new Object[] {"Quit", "New Game", "Cancel"}, // Custom button texts
                "Cancel"
            );// Default option

            if (option == 0) {
                // If user chooses "Yes", quit the application
                System.exit(0);
            } else if (option == 1) {
                // If user chooses "No", start a new game
                dispose();
                new StartWindow();
            }
        });

        JMenuItem saveGame = new JMenuItem("Save Game");
        saveGame.addActionListener(e -> saveGame());
        JMenuItem loadGame = new JMenuItem("Load Game");
        loadGame.addActionListener(e -> loadGame());
        JMenuItem showHighScores = new JMenuItem("High Scores");
        showHighScores.addActionListener(e -> showHighScores());

        menu.add(mainwindow);
        menu.add(restart);
        menu.add(withdraw);
        menu.add(saveGame);
        menu.add(loadGame);
        menu.add(showHighScores);
        menuBar.add(menu);
        setJMenuBar(menuBar);

        add(gridPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        loadHighScores();
        setLocationRelativeTo(null); // Center the window
    }

    private JButton createStyledButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(36, 36));
        button.setBackground(Color.LIGHT_GRAY);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setFocusPainted(false);
        return button;
    }

    private void openCell(int x, int y) {
        // Check if this is the first click
        if (firstClick) {
            firstClick = false;
            startTimer();
            placeMines(x, y); // Ensure the first click is safe
        }

        JButton button = grid[x][y];
        button.setFont(new Font("Arial", Font.BOLD, 20)); // Set a basic font

        // If the cell is flagged or already opened, ignore the click
        if (flags[x][y] || !button.isEnabled()) {
            return;
        }

        // If the cell contains a mine, the game is over
        if (mineLocations[x][y]) {
            button.setText("M");
            button.setForeground(Color.BLACK);
            button.setBackground(Color.RED);
            gameOver(false);
            return;
        }

        // Count adjacent mines
        int numAdjacentMines = countAdjacentMines(x, y);

        // Open the cell and set styles
        //button.setEnabled(true);
        button.setBackground(Color.LIGHT_GRAY);
        button.setBorder(BorderFactory.createLoweredBevelBorder()); // Bevel effect

        if (numAdjacentMines > 0) {
            button.setText(String.valueOf(numAdjacentMines));
            // Set number color based on value
            switch (numAdjacentMines) {
                case 1 -> button.setForeground(Color.BLUE); // 1 in blue
                case 2 -> button.setForeground(new Color(0, 100, 0)); // 2 in dark green
                case 3 -> button.setForeground(Color.RED); // 3 in red
                default -> button.setForeground(Color.BLACK); // Other numbers
            }
        } else {
            button.setText(""); // Blank cell
        }

        // If no adjacent mines, recursively open surrounding cells
        if (numAdjacentMines == 0) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = x + dx;
                    int ny = y + dy;

                    if (isValidCell(nx, ny) && grid[nx][ny].isEnabled()) {
                        openCell(nx, ny);
                    }
                }
            }
        }

        // Check if the player has won
        checkWinCondition();
    }

    private int countAdjacentMines(int x, int y) {
        int count = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int nx = x + dx;
                int ny = y + dy;

                if (isValidCell(nx, ny) && mineLocations[nx][ny]) {
                    count++;
                }
            }
        }

        return count;
    }

    private boolean isValidCell(int x, int y) {
        return x >= 0 && x < rows && y >= 0 && y < cols;
    }

    private void checkWinCondition() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // If there's an unclicked cell that's not a mine, the game is not won yet
                if (!mineLocations[i][j] && grid[i][j].isEnabled()) {
                    return;
                }
            }
        }

        // Stop the timer and declare victory
        timer.stop();
        gameOver(true);
    }

    private void toggleFlag(int x, int y) {
        JButton button = grid[x][y];
        button.setFont(new Font("Arial", Font.BOLD, 20));
        // Ignore already clicked cells
        if (!button.isEnabled())
            return;

        if (flags[x][y]) {
            // Remove flag
            button.setText("");
            button.setForeground(null);
            flags[x][y] = false;
            remainingFlags++; // Increment remaining flags
        } else {
            if (remainingFlags > 0) {
                // Place flag
                button.setText("F");
                button.setForeground(Color.RED);
                flags[x][y] = true;
                remainingFlags--; // Decrement remaining flags
            }
        }

        // Update the mine counter label
        mineCounterLabel.setText("Mines: " + remainingFlags);
    }

    private void placeMines(int safeX, int safeY) {
        Random random = new Random();
        int placedMines = 0;
        while (placedMines < mines) {
            int x = random.nextInt(rows);
            int y = random.nextInt(cols);
            if (!mineLocations[x][y] && (x != safeX || y != safeY)) {
                mineLocations[x][y] = true;
                placedMines++;
            }
        }
    }

    private void startTimer() {
        timer = new Timer(1000, e -> {
            seconds++;
            timerLabel.setText("Time: " + seconds + "s");
        });
        timer.start();
    }

    private void gameOver(boolean won) {
        timer.stop();
        String message = won ? "You Win!" : "Game Over! You hit a mine.";
        JOptionPane.showMessageDialog(this, message);

        if (won) {
            updateHighScore();
        }

        dispose();
        new StartWindow();
    }

    private void updateHighScore() {
        if (rows == 10 && seconds < highScore10x10) {
            highScore10x10 = seconds;
        } else if (rows == 15 && seconds < highScore15x15) {
            highScore15x15 = seconds;
        }
        saveHighScores();
    }

    private void restartGame() {
        dispose();
        new GameBoard(rows, cols, mines);
    }

    private void saveGame() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            oos.writeObject(mineLocations); // Save mine locations
            oos.writeObject(flags); // Save flagged cells
            oos.writeObject(adjacentMines); // Save adjacent mines for each cell
            oos.writeInt(seconds); // Save the timer
            oos.writeBoolean(firstClick); // Save the state of the first click
            JOptionPane.showMessageDialog(this, "Game saved successfully!");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving game: " + e.getMessage());
        }
    }

    private void initializeAdjacentMines() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // Calculate the number of adjacent mines for each cell
                adjacentMines[i][j] = countAdjacentMines(i, j);
            }
        }
    }
    

    private void loadGame() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
            // Deserialize game state
            mineLocations = (boolean[][]) ois.readObject(); // Restore mine locations
            flags = (boolean[][]) ois.readObject(); // Restore flags
            adjacentMines = (int[][]) ois.readObject(); // Restore adjacent mines
            seconds = ois.readInt(); // Restore timer
            firstClick = ois.readBoolean(); // Restore first click state
    
            // Check if adjacentMines is null, reinitialize if necessary
            if (adjacentMines == null) {
                adjacentMines = new int[rows][cols]; // Initialize adjacentMines array
                initializeAdjacentMines();
            }
    
            // Recreate the grid layout to ensure it updates the buttons
            JPanel gridPanel = new JPanel(new GridLayout(rows, cols));
            
            // Loop through the grid to update button labels and state
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    JButton button = grid[i][j]; // Your grid buttons
                    
                    if (button == null) {
                        button = new JButton();
                        grid[i][j] = button; // Initialize the button if it's not yet initialized
                    }
    
                    button.setFont(new Font("Arial", Font.BOLD, 20));
                    if (mineLocations[i][j]) {
                        // If the cell contains a mine
                        //button.setText("M"); // Set mine label
                        //button.setEnabled(false); // Disable the button
                    } else {
                        // If the cell does not contain a mine, set the number or flag
                        if (flags[i][j]) {
                            button.setText("F"); // Set flag label
                        } else {
                            // Set number of adjacent mines for this cell
                            int adjacent = adjacentMines[i][j];
                            button.setText(adjacent > 0 ? String.valueOf(adjacent) : "");
                        }
                        button.setEnabled(true); // Enable button
                    }
    
                    // Optionally, set the button colors based on the number of adjacent mines
                    if (!mineLocations[i][j]) {
                        switch (adjacentMines[i][j]) {
                            case 1 -> button.setForeground(Color.BLUE);
                            case 2 -> button.setForeground(new Color(0, 100, 0));
                            case 3 -> button.setForeground(Color.RED);
                            default -> button.setForeground(Color.BLACK);
                        }
                    }
    
                    // Add button to the grid panel
                    gridPanel.add(button);
                }
            }
    
            // Replace the existing grid with the updated grid panel
            this.getContentPane().removeAll(); // Remove the old grid
            this.getContentPane().add(gridPanel, BorderLayout.CENTER); // Add the new grid panel
    
            JPanel topPanel = new JPanel(new BorderLayout());
            // Update the timer label to reflect the loaded time
            timerLabel.setText("Time: " + seconds + "s");
            // Update the mine counter label
            mineCounterLabel.setText("Mines: " + remainingFlags);
            topPanel.add(timerLabel, BorderLayout.WEST);
            topPanel.add(mineCounterLabel, BorderLayout.EAST);
            add(topPanel, BorderLayout.NORTH);
            
            // Notify user that the game was loaded successfully
            JOptionPane.showMessageDialog(this, "Game loaded successfully!");
    
            // Start the timer (or resume from the saved time)
            if (!firstClick) {
                // If it's not the first click, resume the timer and the game logic
                startTimer(); // This method should handle the timer start/resume
            }
            // Refresh and repaint the window to reflect the updated game state
            revalidate();
            repaint();
    
        } catch (IOException | ClassNotFoundException e) {
            // Handle errors if loading fails
            JOptionPane.showMessageDialog(this, "Error loading game: " + e.getMessage());
        }
    }

    private void saveHighScores() {
        try (PrintWriter writer = new PrintWriter(new File(HIGH_SCORE_FILE))) {
            writer.println(highScore10x10);
            writer.println(highScore15x15);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving high scores: " + e.getMessage());
        }
    }

    private void loadHighScores() {
        try (BufferedReader reader = new BufferedReader(new FileReader(HIGH_SCORE_FILE))) {
            highScore10x10 = Integer.parseInt(reader.readLine());
            highScore15x15 = Integer.parseInt(reader.readLine());
        } catch (IOException e) {
            // Default high scores will remain MAX_VALUE
        }
    }

    public void showHighScores() {
        JOptionPane.showMessageDialog(this, String.format(
                "High Scores:\n10x10: %s seconds\n15x15: %s seconds",
                highScore10x10 == Integer.MAX_VALUE ? "None" : highScore10x10,
                highScore15x15 == Integer.MAX_VALUE ? "None" : highScore15x15));
    }
}
