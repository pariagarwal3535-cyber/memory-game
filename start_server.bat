@echo off
echo ============================================
echo  Memory Game - Multiplayer Server
echo ============================================
echo Starting server on port 55555...
echo Press Ctrl+C to stop.
echo.
java -cp bin network.GameServer
pause