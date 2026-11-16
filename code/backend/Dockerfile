FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S approval && adduser -S approval -G approval \
    && mkdir -p /var/lib/approval/attachments \
    && chown -R approval:approval /var/lib/approval

WORKDIR /app
COPY --from=build --chown=approval:approval /workspace/target/*.jar app.jar

USER approval
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
