# Use correct Java 17 image that exists on Docker Hub
FROM eclipse-temurin:17-jdk-jammy

# Set working directory
WORKDIR /app

# Copy all project files
COPY . .

# Create bin directory
RUN mkdir -p bin

# Compile all Java files
RUN find src -name "*.java" > sources.txt && \
    javac -d bin -cp "lib/*" @sources.txt

# Expose game server port
EXPOSE 55555

# Start the multiplayer server
CMD ["java", "-cp", "bin:lib/*", "network.GameServer"]
