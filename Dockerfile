FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY src ./src

RUN mkdir build && javac -d build src/*.java

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/build ./build
COPY ["Residential_Energy_Dataset_UK- 2014-2020.csv", "./"]

CMD ["java", "-cp", "build", "Main"]
