# Use Java 17
FROM openjdk:17-slim

# Set working directory
WORKDIR /app

# Copy everything
COPY . .

# Create bin directory
RUN mkdir -p bin

# Compile all Java files with MongoDB JARs
RUN javac -d bin -cp "lib/*" -sourcepath src src/Main.java

# Expose the game server port
EXPOSE 55555

# Start the multiplayer server
CMD ["java", "-cp", "bin:lib/*", "network.GameServer"]
