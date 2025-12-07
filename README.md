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

📦 Setup & Installation
1. Clone repository
git clone https://github.com/your-username/tictactoe-ai.git
cd tictactoe-ai

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
