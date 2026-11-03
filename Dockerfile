# All-in-one $0 image: Java API + MySQL 8 in a single free Render web service.
# Build stage compiles the app; runtime stage adds a JRE to the MySQL image.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY src ./src
RUN mkdir -p out && find src -name "*.java" > sources.txt && javac -d out @sources.txt

FROM mysql:8.0
RUN microdnf install -y java-17-openjdk-headless && microdnf clean all
WORKDIR /app
COPY --from=build /app/out ./out
COPY lib ./lib
COPY sql ./sql
COPY web ./web
COPY docker-entrypoint.sh mysql-lowmem.cnf ./
RUN chmod +x docker-entrypoint.sh \
    && mkdir -p /etc/mysql/conf.d \
    && cp mysql-lowmem.cnf /etc/mysql/conf.d/lowmem.cnf
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
