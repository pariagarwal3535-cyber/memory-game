# Use Java 17 with full JDK
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy all project files
COPY . .

# Create bin directory
RUN mkdir -p bin

# Compile - find all java files and compile them
RUN find src -name "*.java" > sources.txt && \
    javac -d bin -cp "lib/*" @sources.txt

# Expose port
EXPOSE 55555

# Start the multiplayer server
CMD ["java", "-cp", "bin:lib/*", "network.GameServer"]
