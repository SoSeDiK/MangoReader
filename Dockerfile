FROM eclipse-temurin:21-alpine

WORKDIR /workspace

COPY build/libs/MangoReader-*.jar /workspace/MangoReader.jar

CMD ["java", "-jar", "MangoReader.jar", "--enable-preview", "--enable-native-access=ALL-UNNAMED", "-Djava.library.path=/usr/lib"]
