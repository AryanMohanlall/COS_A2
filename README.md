## COS 710

### Artificial Intelligence 1

Assignment 2: Structure-Based Genetic Programming for Regression

This project implements a Structure-Based Genetic Programming (SBGP) approach for predicting residential electricity load from historical energy and weather data. The main entry point is `Main.java`.

[Data files](https://drive.google.com/drive/folders/1q9JxS7Gy-Fyk0dAl6zWEquMjl1F0fhXM?usp=sharing)

## Prerequisites

- JDK 17 or newer installed
- `java` and `javac` available on your `PATH`
- Run commands from the project root so the program can find `Residential_Energy_Dataset_UK- 2014-2020.csv`

## Project Structure

- `src/` contains the Java source files
- `bin/` contains compiled class files
- `Residential_Energy_Dataset_UK- 2014-2020.csv` is the dataset loaded by the program at runtime
- `Assignment2.pdf` is the assignment brief
- `u23565536.tex` is the report source
- `log.txt` contains the latest experiment output
- `Main.java` is the application entry point

## Usage on Windows

The following commands use PowerShell from the repository root.

### Compile

```powershell
New-Item -ItemType Directory -Force build | Out-Null
javac -d build .\src\*.java
```

### Run

```powershell
java -cp build Main
```

### Save Output to a Log

```powershell
java -cp build Main | Tee-Object -FilePath log.txt
```

## Usage on Linux

The following commands use a POSIX shell such as `bash`, run from the repository root.

### Compile

```bash
mkdir -p build
javac -d build src/*.java
```

### Run

```bash
java -cp build Main
```

### Save Output to a Log

```bash
java -cp build Main | tee log.txt
```

## Build a Single Runnable JAR (Windows and Linux)

This project has no external dependencies, so you can package all compiled classes into one executable JAR. The examples below use `build/` as the compile-output folder.

## Usage with Docker

Build the image from the repository root:

```bash
docker build -t cos710-a2 .
```

Run the experiment:

```bash
docker run --rm cos710-a2
```

Save the output to `log.txt` on the host machine:

```bash
docker run --rm cos710-a2 > log.txt
```

### Windows (PowerShell)

Run from the repository root:

```powershell
New-Item -ItemType Directory -Force build | Out-Null
New-Item -ItemType Directory -Force dist | Out-Null
javac -d .\build .\src\*.java
jar --create --file .\dist\cos710-a2.jar --main-class Main -C .\build .
```

Run the JAR from the repository root:

```powershell
java -jar .\dist\cos710-a2.jar
```

### Linux (bash)

Run from the repository root:

```bash
mkdir -p build dist
javac -d build src/*.java
jar --create --file dist/cos710-a2.jar --main-class Main -C build .
```

Run the JAR from the repository root:

```bash
java -jar dist/cos710-a2.jar
```

### Optional: Run JAR from another folder

The dataset path is currently relative to the working directory. If you want to run the JAR from `dist/` or any other directory, copy the CSV there first.

Windows:

```powershell
Copy-Item ".\Residential_Energy_Dataset_UK- 2014-2020.csv" ".\dist\"
```

Linux:

```bash
cp "Residential_Energy_Dataset_UK- 2014-2020.csv" dist/
```

## Expected Behavior

When the program starts, it loads the dataset, runs multiple SBGP configurations over 15 repeated runs each, and prints summary statistics such as:

- mean training RMSE
- mean test RMSE
- training and test RMSE standard deviation
- best training and test RMSE
- average execution time per configuration
- runtime standard deviation
- fastest and slowest runtime per configuration
- best evolved program per configuration

The current experiment sweep includes:

- baseline parameters
- smaller and larger population sizes
- shallower and deeper maximum tree depths
- different crossover, mutation, and hoist-rate balances

## Notes

- If `javac` or `java` is not recognized, install a JDK and reopen the terminal
- If the dataset file is missing or renamed, the program will fail to load data correctly
- Because the dataset path is hard-coded as a relative path, running the program from another directory may cause file loading errors
- The algorithm is stochastic, so repeated executions may produce different RMSE values and runtimes
- The report compares these SBGP results against the Assignment 1 canonical GP results
