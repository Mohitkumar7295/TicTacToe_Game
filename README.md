🎮 Tic Tac Toe – Java Swing, AI, MySQL & Server Support

A fully-featured Tic Tac Toe game developed using Java Swing, enhanced with AI opponents, multiple difficulty levels, player statistics, MySQL database integration, and a Servlet-like HTTP server for web access.

This project demonstrates GUI development, database handling, AI algorithms, and server-side concepts in a single application.

🚀 Features

✔️ Two Player (Offline) Mode

🤖 Player vs AI Mode

Easy

Medium

Hard (Minimax Algorithm)

🌐 Online / Web Mode

Lightweight HTTP server

Browser-based Tic Tac Toe

REST-style APIs

💾 MySQL Database Support

Game results stored

Player statistics saved

Recent matches history

📊 Scoreboard & Statistics Window

🔄 Undo & New Game Options

🎨 Clean UI

Highlighted winning moves

Smooth Swing interface

👤 Player Name Support

🛠️ Tech Stack

Java (Swing, AWT)

MySQL

JDBC (MySQL Connector/J)

Minimax Algorithm

Java HTTP Server (Servlet-like)

⚙️ MySQL Configuration
1️⃣ Create Database
CREATE DATABASE tictactoe_db;


Required tables are created automatically by the application.

2️⃣ Update Database Credentials (Inside Code)
String url = "jdbc:mysql://localhost:3306/tictactoe_db";
String username = "root";
String password = "your_password";

3️⃣ Add JDBC Driver

Download mysql-connector-j.jar

Add it to the project classpath

▶️ Run the Project
javac TicTacToe.java
java TicTacToe


(Or run directly using any Java IDE)

📁 Project Structure
/src
 ├── TicTacToe.java
 ├── SimpleHttpServer.java
 ├── WebHandler.java
 └── ApiHandler.java
README.md

🧠 AI Logic
Difficulty	Description
Easy	Random moves
Medium	Win & block strategy
Hard	Minimax algorithm (perfect play)
🛡️ Database Features

Stores every game played

Tracks wins, losses & draws

Maintains recent 10 matches

Stores player statistics

Supports online & offline data

🌐 Servlet / Server Explanation

This project includes a built-in HTTP server that follows the Servlet request–response concept without using Tomcat.

🔧 Server Overview

Runs on port 8080

Uses com.sun.net.httpserver.HttpServer

Acts like a mini servlet container

Enables web-based Tic Tac Toe

🧩 Server Components
1️⃣ SimpleHttpServer

Starts HTTP server

Registers URL routes (contexts)

Equivalent to a servlet container

server.createContext("/tictactoe", new WebHandler());
server.createContext("/api", new ApiHandler());

2️⃣ WebHandler (UI Servlet)

Serves HTML, CSS & JavaScript

Displays Tic Tac Toe board in browser

Access URL:

http://localhost:8080/tictactoe

3️⃣ ApiHandler (Backend Servlet Logic)

Handles API requests

Sends and receives JSON

Works like doGet() and doPost()

Endpoint	Method	Purpose
/api/newgame	GET	Create new game
/api/joingame/{id}	GET	Join game
/api/board/{id}	GET	Fetch board
/api/move	POST	Make move
🔄 Request–Response Flow

Browser sends HTTP request

ApiHandler processes request

Game logic executed

JSON response returned

Browser updates UI

➡️ This is conceptually identical to Java Servlets, without deploying Tomcat.

📡 Real-Time Updates

Browser polls server every 2 seconds

Keeps game state synchronized

Enables multiplayer interaction

🆚 Servlet vs This Project
Feature	Java Servlet	This Project
Request handling	✔	✔
Response handling	✔	✔
JSON APIs	✔	✔
External server	Required	❌ Not required
Easy local run	❌	✔
🗄️ SQL Code (Minimal Version)
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

🚀 Future Enhancements

Convert server to HttpServlet + Tomcat

WebSocket-based real-time multiplayer

Login & authentication

Game replay system

Mobile UI
