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

    // For network/multiplayer features
    private boolean isOnlineMode = false;
    private String gameId;
    private String opponentName = "Opponent";
    private JButton onlineModeButton;

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
        isOnlineMode = false;
        gameId = generateGameId();
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
                "play_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "game_id VARCHAR(20)," +
                "opponent_name VARCHAR(50)," +
                "moves TEXT" +
                ")";

        String createPlayerStatsSQL = "CREATE TABLE IF NOT EXISTS player_stats (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_name VARCHAR(50) UNIQUE NOT NULL," +
                "total_games INT DEFAULT 0," +
                "wins INT DEFAULT 0," +
                "losses INT DEFAULT 0," +
                "draws INT DEFAULT 0," +
                "online_wins INT DEFAULT 0," +
                "online_losses INT DEFAULT 0," +
                "last_played TIMESTAMP" +
                ")";

        String createOnlineGamesSQL = "CREATE TABLE IF NOT EXISTS online_games (" +
                "game_id VARCHAR(20) PRIMARY KEY," +
                "player1 VARCHAR(50) NOT NULL," +
                "player2 VARCHAR(50)," +
                "current_player CHAR(1) DEFAULT 'X'," +
                "board_state VARCHAR(9) DEFAULT '         '," +
                "status VARCHAR(20) DEFAULT 'waiting'," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            stmt.execute(createPlayerStatsSQL);
            stmt.execute(createOnlineGamesSQL);
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
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading player stats: " + e.getMessage());
        }
    }

    private void saveGameResult(String gameMode, String result, String moves) {
        if (connection == null) return;

        try {
            // Save to game_stats table
            String gameStatsSQL = "INSERT INTO game_stats (player_name, game_mode, difficulty, player_symbol, result, game_id, opponent_name, moves) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            String difficulty = vsComputer ?
                    (difficultyLevel == 1 ? "Easy" : difficultyLevel == 2 ? "Medium" : "Hard") :
                    (isOnlineMode ? "Online" : "Two Player");

            String opponent = isOnlineMode ? opponentName : (vsComputer ? "AI" : "Local Player");

            try (PreparedStatement pstmt = connection.prepareStatement(gameStatsSQL)) {
                pstmt.setString(1, playerName);
                pstmt.setString(2, gameMode);
                pstmt.setString(3, difficulty);
                pstmt.setString(4, humanSymbol);
                pstmt.setString(5, result);
                pstmt.setString(6, gameId);
                pstmt.setString(7, opponent);
                pstmt.setString(8, moves);
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
                    "online_wins = online_wins + ?, online_losses = online_losses + ?, " +
                    "last_played = CURRENT_TIMESTAMP WHERE player_name = ?";
            String insertSQL = "INSERT INTO player_stats (player_name, total_games, wins, losses, draws, online_wins, online_losses, last_played) " +
                    "VALUES (?, 1, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

            try (PreparedStatement checkStmt = connection.prepareStatement(checkSQL)) {
                checkStmt.setString(1, playerName);
                ResultSet rs = checkStmt.executeQuery();

                int winsInc = 0, lossesInc = 0, drawsInc = 0;
                int onlineWinsInc = 0, onlineLossesInc = 0;

                if (isOnlineMode) {
                    if (result.equals("Win")) {
                        winsInc = 1;
                        onlineWinsInc = 1;
                    } else if (result.equals("Loss")) {
                        lossesInc = 1;
                        onlineLossesInc = 1;
                    } else {
                        drawsInc = 1;
                    }
                } else if (vsComputer) {
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
                        updateStmt.setInt(4, onlineWinsInc);
                        updateStmt.setInt(5, onlineLossesInc);
                        updateStmt.setString(6, playerName);
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Player doesn't exist, insert
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                        insertStmt.setString(1, playerName);
                        insertStmt.setInt(2, winsInc);
                        insertStmt.setInt(3, lossesInc);
                        insertStmt.setInt(4, drawsInc);
                        insertStmt.setInt(5, onlineWinsInc);
                        insertStmt.setInt(6, onlineLossesInc);
                        insertStmt.executeUpdate();
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error updating player stats: " + e.getMessage());
        }
    }

    private void createGUI() {
        frame = new JFrame("Tic Tac Toe AI with Database & Online Features");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Title
        JLabel titleLabel = new JLabel("Tic Tac Toe AI with Database & Online", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
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
        frame.setSize(600, 750);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createModePanel() {
        JPanel modePanel = new JPanel(new GridLayout(4, 1, 5, 5));
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
        JRadioButton onlineRadio = new JRadioButton("Online Multiplayer");

        twoPlayerRadio.setFont(new Font("Arial", Font.PLAIN, 14));
        vsComputerRadio.setFont(new Font("Arial", Font.PLAIN, 14));
        onlineRadio.setFont(new Font("Arial", Font.PLAIN, 14));

        gameModeGroup = new ButtonGroup();
        gameModeGroup.add(twoPlayerRadio);
        gameModeGroup.add(vsComputerRadio);
        gameModeGroup.add(onlineRadio);

        twoPlayerRadio.addActionListener(e -> setGameMode(false, false));
        vsComputerRadio.addActionListener(e -> setGameMode(true, false));
        onlineRadio.addActionListener(e -> setGameMode(false, true));

        modeSelectionPanel.add(twoPlayerRadio);
        modeSelectionPanel.add(vsComputerRadio);
        modeSelectionPanel.add(onlineRadio);

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

        // Online controls
        JPanel onlinePanel = new JPanel(new FlowLayout());
        onlineModeButton = new JButton("Start Online Game");
        onlineModeButton.setEnabled(false);
        onlineModeButton.addActionListener(e -> startOnlineGame());

        JButton joinGameButton = new JButton("Join Game");
        joinGameButton.setEnabled(false);
        joinGameButton.addActionListener(e -> joinOnlineGame());

        onlinePanel.add(onlineModeButton);
        onlinePanel.add(joinGameButton);

        modePanel.add(namePanel);
        modePanel.add(modeSelectionPanel);
        modePanel.add(difficultyPanel);
        modePanel.add(onlinePanel);

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
        JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 10, 0));

        JButton resetButton = new JButton("New Game");
        resetButton.setFont(new Font("Arial", Font.BOLD, 14));
        resetButton.setBackground(new Color(100, 200, 100));
        resetButton.setForeground(Color.WHITE);
        resetButton.addActionListener(e -> resetGame());

        JButton undoButton = new JButton("Undo");
        undoButton.setFont(new Font("Arial", Font.BOLD, 14));
        undoButton.setBackground(new Color(200, 150, 100));
        undoButton.setForeground(Color.WHITE);
        undoButton.addActionListener(e -> undoMove());

        JButton statsButton = new JButton("Stats");
        statsButton.setFont(new Font("Arial", Font.BOLD, 14));
        statsButton.setBackground(new Color(100, 150, 200));
        statsButton.setForeground(Color.WHITE);
        statsButton.addActionListener(e -> showStatistics());

        JButton dbButton = new JButton("DB Info");
        dbButton.setFont(new Font("Arial", Font.BOLD, 14));
        dbButton.setBackground(new Color(150, 100, 200));
        dbButton.setForeground(Color.WHITE);
        dbButton.addActionListener(e -> showDatabaseInfo());

        JButton serverButton = new JButton("Server");
        serverButton.setFont(new Font("Arial", Font.BOLD, 14));
        serverButton.setBackground(new Color(200, 100, 150));
        serverButton.setForeground(Color.WHITE);
        serverButton.addActionListener(e -> startServer());

        buttonPanel.add(resetButton);
        buttonPanel.add(undoButton);
        buttonPanel.add(statsButton);
        buttonPanel.add(dbButton);
        buttonPanel.add(serverButton);

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

    private void setGameMode(boolean vsComputerMode, boolean onlineMode) {
        this.vsComputer = vsComputerMode;
        this.isOnlineMode = onlineMode;
        resetGame();

        if (onlineMode) {
            onlineModeButton.setEnabled(true);
            difficultyComboBox.setEnabled(false);
            vsComputerRadio.setEnabled(false);
            twoPlayerRadio.setEnabled(false);
            statusLabel.setText("Online Mode - Create or Join a Game");
        } else if (vsComputer) {
            onlineModeButton.setEnabled(false);
            difficultyComboBox.setEnabled(true);
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
                SwingUtilities.invokeLater(this::makeComputerMove);
            }
            updateStatusLabel();
        } else {
            onlineModeButton.setEnabled(false);
            difficultyComboBox.setEnabled(false);
            currentPlayer = "X";
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

        if (isOnlineMode) {
            // In online mode, check if it's current player's turn
            makeOnlineMove(row, col);
        } else {
            makeMove(row, col);
        }
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
            String moves = getMovesHistory();
            if (isOnlineMode) {
                saveGameResult("Online", currentPlayer.equals(humanSymbol) ? "Win" : "Loss", moves);
                updateOnlineGameStatus("completed");
            } else if (vsComputer) {
                saveGameResult("VS AI", currentPlayer.equals(humanSymbol) ? "Win" : "Loss", moves);
            } else {
                saveGameResult("Two Player", currentPlayer + " Win", moves);
            }
        } else if (checkDraw()) {
            showDrawMessage();
            draws++;
            updateScoreDisplay();

            // Save draw result to database
            String moves = getMovesHistory();
            if (isOnlineMode) {
                saveGameResult("Online", "Draw", moves);
                updateOnlineGameStatus("draw");
            } else {
                saveGameResult(vsComputer ? "VS AI" : "Two Player", "Draw", moves);
            }
        } else {
            switchPlayer();

            // If playing against computer and it's computer's turn
            if (vsComputer && currentPlayer.equals(computerSymbol) && !checkGameOver()) {
                SwingUtilities.invokeLater(this::makeComputerMove);
            }

            // If online mode, update game state in database
            if (isOnlineMode) {
                updateOnlineGameState();
            }
        }
    }

    private String getMovesHistory() {
        StringBuilder moves = new StringBuilder();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                String text = buttons[row][col].getText();
                moves.append(text.isEmpty() ? "-" : text);
            }
        }
        return moves.toString();
    }

    private void makeOnlineMove(int row, int col) {
        // Check if it's player's turn by checking database
        if (checkOnlineTurn()) {
            makeMove(row, col);
        } else {
            JOptionPane.showMessageDialog(frame,
                    "It's not your turn!",
                    "Wait",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private boolean checkOnlineTurn() {
        if (connection == null) return false;

        try {
            String sql = "SELECT current_player FROM online_games WHERE game_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, gameId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getString("current_player").equals(humanSymbol);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void updateOnlineGameState() {
        if (connection == null) return;

        try {
            String sql = "UPDATE online_games SET board_state = ?, current_player = ? WHERE game_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, getMovesHistory());
                pstmt.setString(2, currentPlayer);
                pstmt.setString(3, gameId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateOnlineGameStatus(String status) {
        if (connection == null) return;

        try {
            String sql = "UPDATE online_games SET status = ? WHERE game_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, status);
                pstmt.setString(2, gameId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void startOnlineGame() {
        if (connection == null) {
            JOptionPane.showMessageDialog(frame,
                    "Database not connected. Cannot start online game.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        gameId = generateGameId();

        try {
            String sql = "INSERT INTO online_games (game_id, player1, status) VALUES (?, ?, 'waiting')";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, gameId);
                pstmt.setString(2, playerName);
                pstmt.executeUpdate();
            }

            humanSymbol = "X";
            opponentName = "Waiting for opponent...";
            statusLabel.setText("Game ID: " + gameId + " - Waiting for opponent...");

            // Start polling for opponent
            startGamePolling();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame,
                    "Error creating game: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void joinOnlineGame() {
        String inputGameId = JOptionPane.showInputDialog(frame,
                "Enter Game ID to join:",
                "Join Game",
                JOptionPane.QUESTION_MESSAGE);

        if (inputGameId != null && !inputGameId.trim().isEmpty()) {
            gameId = inputGameId.trim();

            if (connection == null) {
                JOptionPane.showMessageDialog(frame,
                        "Database not connected.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                String sql = "SELECT * FROM online_games WHERE game_id = ? AND status = 'waiting'";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, gameId);
                    ResultSet rs = pstmt.executeQuery();

                    if (rs.next()) {
                        // Join the game
                        String updateSql = "UPDATE online_games SET player2 = ?, status = 'active' WHERE game_id = ?";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                            updateStmt.setString(1, playerName);
                            updateStmt.setString(2, gameId);
                            updateStmt.executeUpdate();
                        }

                        opponentName = rs.getString("player1");
                        humanSymbol = "O";
                        currentPlayer = "X"; // First player starts

                        statusLabel.setText("Playing against: " + opponentName);
                        startGamePolling();

                    } else {
                        JOptionPane.showMessageDialog(frame,
                                "Game not found or already started.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(frame,
                        "Error joining game: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void startGamePolling() {
        Timer pollingTimer = new Timer(2000, e -> pollGameUpdates());
        pollingTimer.start();
    }

    private void pollGameUpdates() {
        if (connection == null || gameId == null) return;

        try {
            String sql = "SELECT * FROM online_games WHERE game_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, gameId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String status = rs.getString("status");
                    String boardState = rs.getString("board_state");
                    String currentPlayerFromDB = rs.getString("current_player");

                    if (status.equals("active")) {
                        // Update board
                        updateBoardFromState(boardState);
                        currentPlayer = currentPlayerFromDB;
                        updateStatusLabel();
                    } else if (status.equals("completed") || status.equals("draw")) {
                        // Game ended
                        JOptionPane.showMessageDialog(frame,
                                "Game Over!",
                                "Game Ended",
                                JOptionPane.INFORMATION_MESSAGE);
                        resetGame();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateBoardFromState(String boardState) {
        if (boardState == null || boardState.length() != 9) return;

        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            char symbol = boardState.charAt(i);

            if (symbol != ' ') {
                buttons[row][col].setText(String.valueOf(symbol));
                buttons[row][col].setForeground(symbol == 'X' ?
                        new Color(200, 0, 0) : new Color(0, 0, 200));
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
        if (vsComputer || isOnlineMode) {
            JOptionPane.showMessageDialog(frame,
                    "Undo not available in AI or Online mode. Starting new game.",
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
                (isOnlineMode ? (currentPlayer.equals(humanSymbol) ? "You" : opponentName) :
                        "Player " + currentPlayer);

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
        if (isOnlineMode) {
            statusLabel.setText("Online Game - " +
                    (currentPlayer.equals(humanSymbol) ? "Your turn" : opponentName + "'s turn"));
        } else if (vsComputer) {
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
        isOnlineMode = false;
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
                    stats.append("Overall Statistics:\n");
                    stats.append("Total Games: ").append(rs.getInt("total_games")).append("\n");
                    stats.append("Wins: ").append(rs.getInt("wins")).append("\n");
                    stats.append("Losses: ").append(rs.getInt("losses")).append("\n");
                    stats.append("Draws: ").append(rs.getInt("draws")).append("\n");

                    stats.append("\nOnline Statistics:\n");
                    stats.append("Online Wins: ").append(rs.getInt("online_wins")).append("\n");
                    stats.append("Online Losses: ").append(rs.getInt("online_losses")).append("\n");

                    int totalGames = rs.getInt("total_games");
                    if (totalGames > 0) {
                        double winRate = (rs.getInt("wins") * 100.0) / totalGames;
                        stats.append(String.format("Overall Win Rate: %.1f%%\n", winRate));
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
                            .append(rs.getString("result")).append(" vs ")
                            .append(rs.getString("opponent_name")).append(" - ")
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

                countSQL = "SELECT COUNT(*) as active FROM online_games WHERE status = 'waiting'";
                try (Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery(countSQL);
                    if (rs.next()) {
                        info.append("Waiting online games: ").append(rs.getInt("active")).append("\n");
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

    private void startServer() {
        JOptionPane.showMessageDialog(frame,
                "Starting HTTP Server on port 8080...\n" +
                        "Access the web interface at: http://localhost:8080/tictactoe",
                "Server Starting",
                JOptionPane.INFORMATION_MESSAGE);

        // Start the HTTP server in a separate thread
        new Thread(() -> {
            try {
                SimpleHttpServer server = new SimpleHttpServer(8080);
                server.start();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame,
                        "Failed to start server: " + e.getMessage(),
                        "Server Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private String generateGameId() {
        Random rand = new Random();
        return String.format("%04d", rand.nextInt(10000));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TicTacToe());
    }
}

// Simple HTTP Server for web interface
class SimpleHttpServer {
    private com.sun.net.httpserver.HttpServer server;
    private int port;

    public SimpleHttpServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(port), 0);

        // Serve static files
        server.createContext("/tictactoe", new WebHandler());
        server.createContext("/api", new ApiHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}

// Web interface handler
class WebHandler implements com.sun.net.httpserver.HttpHandler {
    @Override
    public void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
        String response = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Tic Tac Toe Web Interface</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; }
                    .board { display: grid; grid-template-columns: repeat(3, 100px); gap: 5px; margin: 20px 0; }
                    .cell { width: 100px; height: 100px; font-size: 40px; text-align: center; 
                            line-height: 100px; border: 2px solid #333; cursor: pointer; }
                    .status { font-size: 20px; margin: 20px 0; }
                    button { padding: 10px 20px; font-size: 16px; margin: 5px; }
                </style>
            </head>
            <body>
                <h1>Tic Tac Toe Web Interface</h1>
                <div id="status" class="status">Loading...</div>
                <div class="board" id="board"></div>
                <button onclick="newGame()">New Game</button>
                <button onclick="joinGame()">Join Game</button>
                <div id="gameId"></div>
                
                <script>
                    let currentPlayer = 'X';
                    let gameId = null;
                    
                    function createBoard() {
                        const board = document.getElementById('board');
                        board.innerHTML = '';
                        for (let i = 0; i < 9; i++) {
                            const cell = document.createElement('div');
                            cell.className = 'cell';
                            cell.dataset.index = i;
                            cell.onclick = () => makeMove(i);
                            board.appendChild(cell);
                        }
                    }
                    
                    async function makeMove(index) {
                        const response = await fetch('/api/move', {
                            method: 'POST',
                            headers: {'Content-Type': 'application/json'},
                            body: JSON.stringify({gameId, index, player: currentPlayer})
                        });
                        updateBoard();
                    }
                    
                    async function newGame() {
                        const response = await fetch('/api/newgame');
                        const data = await response.json();
                        gameId = data.gameId;
                        document.getElementById('gameId').textContent = 'Game ID: ' + gameId;
                        currentPlayer = 'X';
                        createBoard();
                    }
                    
                    async function joinGame() {
                        const inputId = prompt('Enter Game ID:');
                        if (inputId) {
                            gameId = inputId;
                            const response = await fetch('/api/joingame/' + gameId);
                            updateBoard();
                        }
                    }
                    
                    async function updateBoard() {
                        if (!gameId) return;
                        const response = await fetch('/api/board/' + gameId);
                        const data = await response.json();
                        const board = document.getElementById('board');
                        
                        data.board.forEach((cell, index) => {
                            if (cell !== ' ') {
                                board.children[index].textContent = cell;
                            }
                        });
                        
                        document.getElementById('status').textContent = 
                            'Current Player: ' + data.currentPlayer;
                    }
                    
                    // Poll for updates every 2 seconds
                    setInterval(updateBoard, 2000);
                    createBoard();
                </script>
            </body>
            </html>
            """;

        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, response.length());
        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}

// API handler for game operations
class ApiHandler implements com.sun.net.httpserver.HttpHandler {
    @Override
    public void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        String response = "{}";

        try {
            if (path.equals("/api/newgame") && method.equals("GET")) {
                // Create new game
                response = "{\"gameId\":\"1234\",\"status\":\"created\"}";
            } else if (path.startsWith("/api/joingame/") && method.equals("GET")) {
                // Join existing game
                String gameId = path.substring("/api/joingame/".length());
                response = "{\"gameId\":\"" + gameId + "\",\"status\":\"joined\"}";
            } else if (path.startsWith("/api/board/") && method.equals("GET")) {
                // Get board state
                String gameId = path.substring("/api/board/".length());
                response = "{\"board\":[\" \",\" \",\" \",\" \",\" \",\" \",\" \",\" \",\" \"],\"currentPlayer\":\"X\"}";
            } else if (path.equals("/api/move") && method.equals("POST")) {
                // Make a move
                response = "{\"status\":\"move_made\"}";
            }
        } catch (Exception e) {
            response = "{\"error\":\"" + e.getMessage() + "\"}";
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
