package java1;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.Random;

public class TicTacToe {
    private JFrame frame;
    private JButton[][] buttons;
    private String currentPlayer;
    private JLabel statusLabel;
    private JLabel xScoreLabel, oScoreLabel, drawLabel;
    private int xWins, oWins, draws;
    private boolean vsComputer;
    private String computerSymbol;
    private String humanSymbol;
    private JRadioButton twoPlayerRadio;
    private JRadioButton vsComputerRadio;
    private ButtonGroup gameModeGroup;
    private int difficultyLevel; // 1: Easy, 2: Medium, 3: Hard
    private JComboBox<String> difficultyComboBox;

    // JDBC Database connection variables
    private Connection connection;
    private String playerName = "Player1"; // Default player name

    public TicTacToe() {
        initializeGame();
        initializeDatabase();
        createGUI();
    }

    private void initializeGame() {
        buttons = new JButton[3][3];
        currentPlayer = "X";
        xWins = 0;
        oWins = 0;
        draws = 0;
        vsComputer = false;
        computerSymbol = "O";
        humanSymbol = "X";
        difficultyLevel = 2; // Default to medium
    }

    private void initializeDatabase() {
        try {
            // Load JDBC driver (for MySQL)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish connection
            String url = "jdbc:mysql://localhost:3306/tictactoe_db";
            String username = "root";
            String password = "password"; // Change this to your actual password

            connection = DriverManager.getConnection(url, username, password);

            // Create table if it doesn't exist
            createTableIfNotExists();

            // Load player statistics
            loadPlayerStats();

        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(frame,
                    "MySQL JDBC Driver not found. Please add the driver to your classpath.\n" +
                            "Continuing without database functionality.",
                    "Database Error",
                    JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame,
                    "Cannot connect to database. Please check your database connection.\n" +
                            "Error: " + e.getMessage() + "\n" +
                            "Continuing without database functionality.",
                    "Database Connection Error",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void createTableIfNotExists() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS game_stats (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_name VARCHAR(50) NOT NULL," +
                "game_mode VARCHAR(20) NOT NULL," +
                "difficulty VARCHAR(10)," +
                "player_symbol CHAR(1)," +
                "result VARCHAR(10) NOT NULL," +
                "play_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        String createPlayerStatsSQL = "CREATE TABLE IF NOT EXISTS player_stats (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_name VARCHAR(50) UNIQUE NOT NULL," +
                "total_games INT DEFAULT 0," +
                "wins INT DEFAULT 0," +
                "losses INT DEFAULT 0," +
                "draws INT DEFAULT 0," +
                "last_played TIMESTAMP" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            stmt.execute(createPlayerStatsSQL);
        }
    }

    private void loadPlayerStats() {
        if (connection == null) return;

        try {
            String sql = "SELECT * FROM player_stats WHERE player_name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    xWins = rs.getInt("wins");
                    draws = rs.getInt("draws");
                    // For two-player mode, we treat both X and O wins as player stats
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading player stats: " + e.getMessage());
        }
    }

    private void saveGameResult(String gameMode, String result) {
        if (connection == null) return;

        try {
            // Save to game_stats table
            String gameStatsSQL = "INSERT INTO game_stats (player_name, game_mode, difficulty, player_symbol, result) " +
                    "VALUES (?, ?, ?, ?, ?)";

            String difficulty = vsComputer ?
                    (difficultyLevel == 1 ? "Easy" : difficultyLevel == 2 ? "Medium" : "Hard") :
                    "Two Player";

            try (PreparedStatement pstmt = connection.prepareStatement(gameStatsSQL)) {
                pstmt.setString(1, playerName);
                pstmt.setString(2, gameMode);
                pstmt.setString(3, difficulty);
                pstmt.setString(4, humanSymbol);
                pstmt.setString(5, result);
                pstmt.executeUpdate();
            }

            // Update player_stats table
            updatePlayerStats(result);

        } catch (SQLException e) {
            System.err.println("Error saving game result: " + e.getMessage());
        }
    }

    private void updatePlayerStats(String result) {
        if (connection == null) return;

        try {
            // Check if player exists
            String checkSQL = "SELECT * FROM player_stats WHERE player_name = ?";
            String updateSQL = "UPDATE player_stats SET total_games = total_games + 1, " +
                    "wins = wins + ?, losses = losses + ?, draws = draws + ?, " +
                    "last_played = CURRENT_TIMESTAMP WHERE player_name = ?";
            String insertSQL = "INSERT INTO player_stats (player_name, total_games, wins, losses, draws, last_played) " +
                    "VALUES (?, 1, ?, ?, ?, CURRENT_TIMESTAMP)";

            try (PreparedStatement checkStmt = connection.prepareStatement(checkSQL)) {
                checkStmt.setString(1, playerName);
                ResultSet rs = checkStmt.executeQuery();

                int winsInc = 0, lossesInc = 0, drawsInc = 0;

                if (vsComputer) {
                    if (result.equals("Win")) {
                        winsInc = 1;
                    } else if (result.equals("Loss")) {
                        lossesInc = 1;
                    } else {
                        drawsInc = 1;
                    }
                } else {
                    // For two-player mode, track differently
                    if (result.equals("X Win")) {
                        winsInc = 1;
                    } else if (result.equals("O Win")) {
                        lossesInc = 1;
                    } else {
                        drawsInc = 1;
                    }
                }

                if (rs.next()) {
                    // Player exists, update
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
                        updateStmt.setInt(1, winsInc);
                        updateStmt.setInt(2, lossesInc);
                        updateStmt.setInt(3, drawsInc);
                        updateStmt.setString(4, playerName);
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Player doesn't exist, insert
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                        insertStmt.setString(1, playerName);
                        insertStmt.setInt(2, winsInc);
                        insertStmt.setInt(3, lossesInc);
                        insertStmt.setInt(4, drawsInc);
                        insertStmt.executeUpdate();
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error updating player stats: " + e.getMessage());
        }
    }

    private void createGUI() {
        frame = new JFrame("Tic Tac Toe AI with Database");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Title
        JLabel titleLabel = new JLabel("Tic Tac Toe AI with Database", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(0, 100, 200));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        frame.add(titleLabel, BorderLayout.NORTH);

        // Game mode selection panel
        JPanel modePanel = createModePanel();

        // Game board
        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        boardPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        boardPanel.setBackground(Color.DARK_GRAY);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                buttons[row][col] = createGameButton();
                final int finalRow = row;
                final int finalCol = col;

                buttons[row][col].addActionListener(e -> buttonClicked(finalRow, finalCol));
                boardPanel.add(buttons[row][col]);
            }
        }

        // Control panel
        JPanel controlPanel = createControlPanel();

        // Main content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(modePanel, BorderLayout.NORTH);
        contentPanel.add(boardPanel, BorderLayout.CENTER);
        contentPanel.add(controlPanel, BorderLayout.SOUTH);

        frame.add(contentPanel, BorderLayout.CENTER);
        frame.setSize(550, 700); // Increased height for database button
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createModePanel() {
        JPanel modePanel = new JPanel(new GridLayout(3, 1, 5, 5));
        modePanel.setBorder(BorderFactory.createTitledBorder("Game Settings"));
        modePanel.setBackground(new Color(240, 240, 240));

        // Player name input
        JPanel namePanel = new JPanel(new FlowLayout());
        JLabel nameLabel = new JLabel("Player Name:");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));

        JTextField nameField = new JTextField(playerName, 15);
        JButton nameButton = new JButton("Set Name");
        nameButton.addActionListener(e -> {
            String newName = nameField.getText().trim();
            if (!newName.isEmpty()) {
                playerName = newName;
                loadPlayerStats();
                updateScoreDisplay();
                JOptionPane.showMessageDialog(frame,
                        "Player name set to: " + playerName,
                        "Name Updated",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        namePanel.add(nameLabel);
        namePanel.add(nameField);
        namePanel.add(nameButton);

        // Game mode selection
        JPanel modeSelectionPanel = new JPanel(new FlowLayout());
        twoPlayerRadio = new JRadioButton("Two Players", true);
        vsComputerRadio = new JRadioButton("One Player vs AI");

        twoPlayerRadio.setFont(new Font("Arial", Font.PLAIN, 14));
        vsComputerRadio.setFont(new Font("Arial", Font.PLAIN, 14));

        gameModeGroup = new ButtonGroup();
        gameModeGroup.add(twoPlayerRadio);
        gameModeGroup.add(vsComputerRadio);

        twoPlayerRadio.addActionListener(e -> setGameMode(false));
        vsComputerRadio.addActionListener(e -> setGameMode(true));

        modeSelectionPanel.add(twoPlayerRadio);
        modeSelectionPanel.add(vsComputerRadio);

        // Difficulty selection
        JPanel difficultyPanel = new JPanel(new FlowLayout());
        JLabel difficultyLabel = new JLabel("AI Difficulty:");
        difficultyLabel.setFont(new Font("Arial", Font.BOLD, 12));

        String[] difficulties = {"Easy", "Medium", "Hard"};
        difficultyComboBox = new JComboBox<>(difficulties);
        difficultyComboBox.setSelectedIndex(1); // Medium
        difficultyComboBox.addActionListener(e -> {
            difficultyLevel = difficultyComboBox.getSelectedIndex() + 1;
        });

        difficultyPanel.add(difficultyLabel);
        difficultyPanel.add(difficultyComboBox);

        modePanel.add(namePanel);
        modePanel.add(modeSelectionPanel);
        modePanel.add(difficultyPanel);

        return modePanel;
    }

    private JButton createGameButton() {
        JButton button = new JButton("");
        button.setFont(new Font("Arial", Font.BOLD, 50));
        button.setFocusPainted(false);
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        return button;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Status
        statusLabel = new JLabel("Current Player: X", JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        statusLabel.setForeground(Color.BLACK);

        // Score panel
        JPanel scorePanel = new JPanel(new GridLayout(1, 3, 10, 0));

        xScoreLabel = createScoreLabel("Player X: 0", new Color(200, 0, 0));
        oScoreLabel = createScoreLabel("Player O: 0", new Color(0, 0, 200));
        drawLabel = createScoreLabel("Draws: 0", new Color(0, 150, 0));

        scorePanel.add(xScoreLabel);
        scorePanel.add(oScoreLabel);
        scorePanel.add(drawLabel);

        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 10, 0));

        JButton resetButton = new JButton("New Game");
        resetButton.setFont(new Font("Arial", Font.BOLD, 16));
        resetButton.setBackground(new Color(100, 200, 100));
        resetButton.setForeground(Color.WHITE);
        resetButton.addActionListener(e -> resetGame());

        JButton undoButton = new JButton("Undo Move");
        undoButton.setFont(new Font("Arial", Font.BOLD, 16));
        undoButton.setBackground(new Color(200, 150, 100));
        undoButton.setForeground(Color.WHITE);
        undoButton.addActionListener(e -> undoMove());

        JButton statsButton = new JButton("View Stats");
        statsButton.setFont(new Font("Arial", Font.BOLD, 16));
        statsButton.setBackground(new Color(100, 150, 200));
        statsButton.setForeground(Color.WHITE);
        statsButton.addActionListener(e -> showStatistics());

        JButton dbButton = new JButton("Database");
        dbButton.setFont(new Font("Arial", Font.BOLD, 16));
        dbButton.setBackground(new Color(150, 100, 200));
        dbButton.setForeground(Color.WHITE);
        dbButton.addActionListener(e -> showDatabaseInfo());

        buttonPanel.add(resetButton);
        buttonPanel.add(undoButton);
        buttonPanel.add(statsButton);
        buttonPanel.add(dbButton);

        controlPanel.add(statusLabel, BorderLayout.NORTH);
        controlPanel.add(scorePanel, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);

        return controlPanel;
    }

    private JLabel createScoreLabel(String text, Color color) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(color);
        label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return label;
    }

    private void setGameMode(boolean vsComputerMode) {
        this.vsComputer = vsComputerMode;
        resetGame();

        if (vsComputer) {
            Object[] options = {"Play as X (First)", "Play as O (Second)"};
            int choice = JOptionPane.showOptionDialog(frame,
                    "Choose your symbol:\nX goes first, O goes second",
                    "Choose Symbol",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == 0) {
                humanSymbol = "X";
                computerSymbol = "O";
                currentPlayer = "X";
            } else {
                humanSymbol = "O";
                computerSymbol = "X";
                currentPlayer = "X";
                // Computer makes first move
                SwingUtilities.invokeLater(this::makeComputerMove);
            }
            updateStatusLabel();
        }
    }

    private void buttonClicked(int row, int col) {
        JButton button = buttons[row][col];

        if (!button.getText().isEmpty() || checkGameOver()) {
            return;
        }

        if (vsComputer && !currentPlayer.equals(humanSymbol)) {
            return;
        }

        makeMove(row, col);
    }

    private void makeMove(int row, int col) {
        JButton button = buttons[row][col];
        button.setText(currentPlayer);
        button.setForeground(currentPlayer.equals("X") ? new Color(200, 0, 0) : new Color(0, 0, 200));

        if (checkWin()) {
            highlightWinningCells();
            showWinMessage();
            updateScores();

            // Save game result to database
            if (vsComputer) {
                saveGameResult("VS AI", currentPlayer.equals(humanSymbol) ? "Win" : "Loss");
            } else {
                saveGameResult("Two Player", currentPlayer + " Win");
            }
        } else if (checkDraw()) {
            showDrawMessage();
            draws++;
            updateScoreDisplay();

            // Save draw result to database
            saveGameResult(vsComputer ? "VS AI" : "Two Player", "Draw");
        } else {
            switchPlayer();

            // If playing against computer and it's computer's turn
            if (vsComputer && currentPlayer.equals(computerSymbol) && !checkGameOver()) {
                SwingUtilities.invokeLater(this::makeComputerMove);
            }
        }
    }

    private void makeComputerMove() {
        if (checkGameOver()) return;

        int[] move = findBestMove();
        if (move != null) {
            Timer timer = new Timer(600, e -> {
                makeMove(move[0], move[1]);
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    private int[] findBestMove() {
        // Hard AI - Minimax algorithm
        if (difficultyLevel == 3) {
            return findBestMoveMinimax();
        }
        // Medium AI - Strategic moves
        else if (difficultyLevel == 2) {
            return findBestMoveMedium();
        }
        // Easy AI - Random moves with some strategy
        else {
            return findBestMoveEasy();
        }
    }

    private int[] findBestMoveMinimax() {
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (buttons[i][j].getText().isEmpty()) {
                    buttons[i][j].setText(computerSymbol);
                    int score = minimax(0, false);
                    buttons[i][j].setText("");

                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = new int[]{i, j};
                    }
                }
            }
        }
        return bestMove;
    }

    private int minimax(int depth, boolean isMaximizing) {
        if (checkWin()) {
            if (isMaximizing) {
                return -10 + depth; // Human wins
            } else {
                return 10 - depth; // Computer wins
            }
        }
        if (checkDraw()) {
            return 0;
        }

        if (isMaximizing) {
            int bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (buttons[i][j].getText().isEmpty()) {
                        buttons[i][j].setText(computerSymbol);
                        int score = minimax(depth + 1, false);
                        buttons[i][j].setText("");
                        bestScore = Math.max(score, bestScore);
                    }
                }
            }
            return bestScore;
        } else {
            int bestScore = Integer.MAX_VALUE;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (buttons[i][j].getText().isEmpty()) {
                        buttons[i][j].setText(humanSymbol);
                        int score = minimax(depth + 1, true);
                        buttons[i][j].setText("");
                        bestScore = Math.min(score, bestScore);
                    }
                }
            }
            return bestScore;
        }
    }

    private int[] findBestMoveMedium() {
        // Try to win
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (buttons[i][j].getText().isEmpty()) {
                    buttons[i][j].setText(computerSymbol);
                    if (checkWin()) {
                        buttons[i][j].setText("");
                        return new int[]{i, j};
                    }
                    buttons[i][j].setText("");
                }
            }
        }

        // Block human from winning
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (buttons[i][j].getText().isEmpty()) {
                    buttons[i][j].setText(humanSymbol);
                    if (checkWin()) {
                        buttons[i][j].setText("");
                        return new int[]{i, j};
                    }
                    buttons[i][j].setText("");
                }
            }
        }

        // Strategic moves
        int[][] priorities = {
                {1, 1}, // Center
                {0, 0}, {0, 2}, {2, 0}, {2, 2}, // Corners
                {0, 1}, {1, 0}, {1, 2}, {2, 1}  // Edges
        };

        for (int[] pos : priorities) {
            if (buttons[pos[0]][pos[1]].getText().isEmpty()) {
                return pos;
            }
        }

        return getRandomMove();
    }

    private int[] findBestMoveEasy() {
        Random rand = new Random();

        // 30% chance to make a random move
        if (rand.nextInt(100) < 30) {
            return getRandomMove();
        }

        // Otherwise use medium strategy
        return findBestMoveMedium();
    }

    private int[] getRandomMove() {
        Random rand = new Random();
        java.util.List<int[]> emptyCells = new java.util.ArrayList<>();

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (buttons[i][j].getText().isEmpty()) {
                    emptyCells.add(new int[]{i, j});
                }
            }
        }

        if (!emptyCells.isEmpty()) {
            return emptyCells.get(rand.nextInt(emptyCells.size()));
        }
        return null;
    }

    private void undoMove() {
        // Simple undo - just reset the game
        if (vsComputer) {
            JOptionPane.showMessageDialog(frame,
                    "Undo not available in AI mode. Starting new game.",
                    "Undo",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        resetGame();
    }

    private boolean checkWin() {
        String[][] board = getBoardState();

        for (int i = 0; i < 3; i++) {
            if (checkLine(board[i][0], board[i][1], board[i][2]) ||
                    checkLine(board[0][i], board[1][i], board[2][i])) {
                return true;
            }
        }

        return checkLine(board[0][0], board[1][1], board[2][2]) ||
                checkLine(board[0][2], board[1][1], board[2][0]);
    }

    private boolean checkLine(String a, String b, String c) {
        return !a.isEmpty() && a.equals(b) && b.equals(c);
    }

    private boolean checkDraw() {
        for (JButton[] row : buttons) {
            for (JButton button : row) {
                if (button.getText().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkGameOver() {
        return checkWin() || checkDraw();
    }

    private String[][] getBoardState() {
        String[][] board = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = buttons[i][j].getText();
            }
        }
        return board;
    }

    private void highlightWinningCells() {
        String[][] board = getBoardState();

        for (int i = 0; i < 3; i++) {
            if (checkLine(board[i][0], board[i][1], board[i][2])) {
                highlightCells(new int[][]{{i, 0}, {i, 1}, {i, 2}});
                return;
            }
        }

        for (int i = 0; i < 3; i++) {
            if (checkLine(board[0][i], board[1][i], board[2][i])) {
                highlightCells(new int[][]{{0, i}, {1, i}, {2, i}});
                return;
            }
        }

        if (checkLine(board[0][0], board[1][1], board[2][2])) {
            highlightCells(new int[][]{{0, 0}, {1, 1}, {2, 2}});
            return;
        }

        if (checkLine(board[0][2], board[1][1], board[2][0])) {
            highlightCells(new int[][]{{0, 2}, {1, 1}, {2, 0}});
        }
    }

    private void highlightCells(int[][] cells) {
        for (int[] cell : cells) {
            buttons[cell[0]][cell[1]].setBackground(new Color(255, 255, 150));
        }
    }

    private void showWinMessage() {
        String winner = vsComputer ?
                (currentPlayer.equals(humanSymbol) ? "You" : "Computer") :
                "Player " + currentPlayer;

        JOptionPane.showMessageDialog(frame,
                winner + " wins!",
                "Game Over",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showDrawMessage() {
        JOptionPane.showMessageDialog(frame,
                "It's a draw!",
                "Game Over",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void switchPlayer() {
        currentPlayer = currentPlayer.equals("X") ? "O" : "X";
        updateStatusLabel();
    }

    private void updateStatusLabel() {
        if (vsComputer) {
            if (currentPlayer.equals(humanSymbol)) {
                statusLabel.setText("Your turn (" + humanSymbol + ") - AI Level: " +
                        difficultyComboBox.getSelectedItem());
            } else {
                statusLabel.setText("Computer's turn (" + computerSymbol + ") - Thinking...");
            }
        } else {
            statusLabel.setText("Current Player: " + currentPlayer);
        }
    }

    private void updateScores() {
        if (currentPlayer.equals("X")) {
            xWins++;
        } else {
            oWins++;
        }
        updateScoreDisplay();
    }

    private void updateScoreDisplay() {
        xScoreLabel.setText("Player X: " + xWins);
        oScoreLabel.setText("Player O: " + oWins);
        drawLabel.setText("Draws: " + draws);
    }

    private void resetGame() {
        for (JButton[] row : buttons) {
            for (JButton button : row) {
                button.setText("");
                button.setBackground(Color.WHITE);
            }
        }
        currentPlayer = "X";
        updateStatusLabel();
    }

    private void showStatistics() {
        if (connection == null) {
            JOptionPane.showMessageDialog(frame,
                    "Database not connected. Statistics unavailable.",
                    "Database Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            StringBuilder stats = new StringBuilder();
            stats.append("Statistics for: ").append(playerName).append("\n\n");

            // Get player stats
            String playerStatsSQL = "SELECT * FROM player_stats WHERE player_name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(playerStatsSQL)) {
                pstmt.setString(1, playerName);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    stats.append("Total Games: ").append(rs.getInt("total_games")).append("\n");
                    stats.append("Wins: ").append(rs.getInt("wins")).append("\n");
                    stats.append("Losses: ").append(rs.getInt("losses")).append("\n");
                    stats.append("Draws: ").append(rs.getInt("draws")).append("\n");

                    int totalGames = rs.getInt("total_games");
                    if (totalGames > 0) {
                        double winRate = (rs.getInt("wins") * 100.0) / totalGames;
                        stats.append(String.format("Win Rate: %.1f%%\n", winRate));
                    }

                    Date lastPlayed = rs.getDate("last_played");
                    if (lastPlayed != null) {
                        stats.append("Last Played: ").append(lastPlayed.toString()).append("\n");
                    }
                }
            }

            // Get recent games
            stats.append("\nRecent Games:\n");
            String recentGamesSQL = "SELECT * FROM game_stats WHERE player_name = ? ORDER BY play_date DESC LIMIT 10";
            try (PreparedStatement pstmt = connection.prepareStatement(recentGamesSQL)) {
                pstmt.setString(1, playerName);
                ResultSet rs = pstmt.executeQuery();

                int count = 1;
                while (rs.next()) {
                    stats.append(count).append(". ")
                            .append(rs.getString("game_mode")).append(" - ")
                            .append(rs.getString("difficulty")).append(" - ")
                            .append(rs.getString("result")).append(" - ")
                            .append(rs.getTimestamp("play_date").toString().substring(0, 16))
                            .append("\n");
                    count++;
                }
            }

            JOptionPane.showMessageDialog(frame,
                    stats.toString(),
                    "Game Statistics",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame,
                    "Error retrieving statistics: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showDatabaseInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Database Information\n\n");

        if (connection == null) {
            info.append("Status: NOT CONNECTED\n");
            info.append("Please check your database configuration.\n\n");
            info.append("Required setup:\n");
            info.append("1. Install MySQL database\n");
            info.append("2. Create database: tictactoe_db\n");
            info.append("3. Update connection details in code:\n");
            info.append("   - URL: jdbc:mysql://localhost:3306/tictactoe_db\n");
            info.append("   - Username: your_username\n");
            info.append("   - Password: your_password\n");
            info.append("4. Add MySQL JDBC driver to classpath\n");
        } else {
            info.append("Status: CONNECTED\n");
            info.append("Player: ").append(playerName).append("\n\n");

            try {
                // Get database stats
                String countSQL = "SELECT COUNT(*) as total FROM game_stats";
                try (Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery(countSQL);
                    if (rs.next()) {
                        info.append("Total games recorded: ").append(rs.getInt("total")).append("\n");
                    }
                }

                countSQL = "SELECT COUNT(DISTINCT player_name) as players FROM player_stats";
                try (Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery(countSQL);
                    if (rs.next()) {
                        info.append("Total players: ").append(rs.getInt("players")).append("\n");
                    }
                }

            } catch (SQLException e) {
                info.append("Error getting database info: ").append(e.getMessage());
            }
        }

        JOptionPane.showMessageDialog(frame,
                info.toString(),
                "Database Information",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TicTacToe());
    }
}
