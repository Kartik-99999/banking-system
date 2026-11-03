# Backend: compiles the Java app and runs the API + web UI.
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY src ./src
COPY lib ./lib
COPY sql ./sql
RUN mkdir -p out && find src -name "*.java" > sources.txt && javac -d out @sources.txt
EXPOSE 8080
# Render injects $PORT; defaults to 8080 when unset (local docker run).
CMD java -cp "out:lib/*" WebMain $PORT
