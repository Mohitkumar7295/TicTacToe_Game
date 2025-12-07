# Tic Tac Toe game

A fully-featured Tic Tac Toe game built using Java Swing, featuring AI opponents, multiple difficulty levels, player statistics, and MySQL database integration.

🚀 Features

✔️ Two Player mode

🤖 Player vs AI mode

Easy

Medium

Hard (Minimax Algorithm)

💾 MySQL database support

Game results stored

Player statistics saved

Recent matches history

📊 Scoreboard & Statistics window

🔄 Undo & New Game options

🎨 Clean UI with highlights for winning moves

👤 Player name support

🛠️ Tech Stack

Java (Swing, AWT)

MySQL Database

JDBC (MySQL Connector/J)


2. Configure MySQL

Create database:

CREATE DATABASE tictactoe_db;


Tables are created automatically by the app:

game_stats

player_stats

3. Update Database Credentials

Inside the code:

String url = "jdbc:mysql://localhost:3306/tictactoe_db";
String username = "root";
String password = "your_password";

4. Add MySQL JDBC Driver

Download mysql-connector-j.jar
Add it to your project's classpath.

▶️ Run the Project
javac TicTacToe.java
java TicTacToe


(or run via any Java IDE)

📚 Project Structure
/src
 └── TicTacToe.java
README.md

🧠 AI Logic

Easy – random moves

Medium – win-block strategy

Hard – uses Minimax algorithm for perfect play

🛡️ Database Features

Saves each game played

Tracks wins, losses, draws

Shows recent 10 matches

Shows total players in DB
sql code
-- Minimal version
CREATE DATABASE IF NOT EXISTS tictactoe_db;
USE tictactoe_db;

CREATE TABLE IF NOT EXISTS game_stats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_name VARCHAR(50) NOT NULL,
    game_mode VARCHAR(20) NOT NULL,
    difficulty VARCHAR(10),
    player_symbol CHAR(1),
    result VARCHAR(10) NOT NULL,
    play_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_stats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_name VARCHAR(50) UNIQUE NOT NULL,
    total_games INT DEFAULT 0,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    draws INT DEFAULT 0,
    last_played TIMESTAMP
);
