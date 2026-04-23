# Memory Game

A Java Swing-based memory game with single-player, multiplayer, quiz, and brain training modes.

## Overview

This project is a complete memory game application built in Java. It includes:

- User authentication with MongoDB Atlas integration and a local fallback storage option
- Single-player card matching game with level progression
- Multiplayer support via a Java socket server
- Quiz mode with subject selection and scoring
- Brain training mini-games such as path memory, spot the difference, and jigsaw puzzles
- A polished Swing UI with multiple views and custom styling

## Features

- Login and registration flow
- Home screen with mode selection
- Category-based single-player memory game
- Multiplayer rooms and real-time board updates
- Quiz mode with randomized question sets
- Brain training screens for additional challenges
- User stats tracking (high score and games played)
- Local and cloud-backed authentication handling

## Project Structure

- `src/` - Java source files
- `bin/` - Compiled classes output directory
- `lib/` - External libraries and dependencies
- `resources/` - Image assets for cards and UI
- `data/` - Local fallback storage for `users.txt`
- `run.bat` - Windows compile and launch script
- `start_server.bat` - Windows multiplayer server launcher

## Prerequisites

- Java JDK 8 or later
- `javac` and `java` available on the system PATH
- `lib/` must contain the required libraries for MongoDB and any additional dependencies

## Build and Run

### Windows

1. Open a terminal in the project root folder.
2. Run:
   ```bat
   run.bat
   ```

This script compiles the source files into `bin/` and launches the application.

### Manual build

From the project root:
```bat
javac -d bin -cp "lib/*" -sourcepath src src/Main.java
java -cp "bin;lib/*" Main
```

## Multiplayer Server

To run the multiplayer server separately:

```bat
start_server.bat
```

The server listens on port `55555` and is required for multiplayer mode.

## MongoDB Configuration

Authentication is handled by `src/auth/AuthManager.java`.

- The default connection string points to MongoDB Atlas.
- If the MongoDB connection fails, the application falls back to local storage in `data/users.txt`.

If you want to use your own MongoDB Atlas details, update the constants in `AuthManager.java`:

- `CONNECTION_STRING`
- `DATABASE_NAME`
- `COLLECTION_NAME`

## Notes

- The application uses Java Swing for the UI.
- The main entrypoint is `src/Main.java`.
- Multiplayer mode requires the server to be running before joining a room.
- Local fallback storage uses `data/users.txt` if MongoDB is unavailable.

## License

This project is provided as-is for learning and demonstration purposes.
