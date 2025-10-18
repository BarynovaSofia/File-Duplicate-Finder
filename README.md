# File Duplicate Finder

A console application for finding duplicate files. My first big Java project!

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Tests](https://img.shields.io/badge/Tests-114-brightgreen.svg)](#tests)

---

## About the Project

This is my first full project for portfolio. Initially I didn't know which topic to choose, but working with files turned out to be really interesting!

The program scans a folder and finds identical files (compares by content, not by name). Uses multithreading to work fast.

**Honestly:** I'm partially self-taught, this is the first time I'm writing a README and making such a big project. Tried to do everything right and follow best practices

---

## What It Can Do

- Scans folders and finds duplicate files
- Uses multithreading (all CPU cores work)
- You can choose hashing algorithm (MD5, SHA-1, SHA-256)
- Has filters by extension and file size
- CLI interface — runs from command line
- Shows how much space can be freed
- Wrote 114 tests to make sure everything works correctly!

---

## How to Run

### Requirements
- Java 21
- Maven

### Commands

```bash
# Clone the project
git clone https://github.com/BarynovaSofia/File-Duplicate-Finder
cd file-duplicate-finder

# Build
mvn clean package

# Run
java -jar target/file-duplicate-finder-1.0.0.jar --help
```

---

## Examples

### Basic run
```bash
java -jar file-duplicate-finder.jar
```

### Specify folder
```bash
java -jar file-duplicate-finder.jar --directory ~/Documents
```

### Find duplicates only for images
```bash
java -jar file-duplicate-finder.jar -d ~/Photos -e jpg,png,gif
```

### With more threads
```bash
java -jar file-duplicate-finder.jar -d ~/Downloads -t 8
```

### Parameters

```
-d, --directory     Which folder to scan
-t, --threads       How many threads to use
-s, --min-size      Minimum file size (in bytes)
-e, --extensions    Which extensions to search (txt,pdf,jpg)
-a, --algorithm     MD5, SHA-1 or SHA-256
-h, --help          Show help
```

---

## Technologies

What I used:
- **Java 21** — modern Java version
- **Maven** — for building the project
- **JUnit 5** — wrote 114 tests
- **ExecutorService** — for multithreading
- **ConcurrentHashMap** — thread-safe index
- **SLF4J + Logback** — logging

---

## Project Structure

```
src/main/java/
├── app/           - main application class
├── cli/           - command line parameters handling
├── config/        - configuration loading
├── concurrent/    - multithreaded processing
├── hash/          - hash calculation
├── index/         - file index
├── model/         - data model
└── scanner/       - directory scanning

test/java/         - all tests here (114 of them!)
```

---

## Configuration

You can set defaults in `application.properties` file:

```properties
threads=4
hash.algorithm=MD5
file.min-size=0
directory=.
```

Command line parameters override settings from the file.

---

## Tests

Wrote **114 tests** to check everything:

- `HashCalculatorTest` — 26 tests (file hashing)
- `SimpleFileIndexTest` — 28 tests (index and duplicate finding)
- `CommandLineParserTest` — 44 tests (CLI parameters)
- `MultiThreadHashCalculatorTest` — 16 tests (multithreading!)

Run tests:
```bash
mvn test
```

Spent most time on multithreading tests — needed to verify that no files are lost and results are correct.

---

## What I Learned

- What **ConcurrentHashMap** is and why you can't just use HashMap + synchronized
- How to write tests for multithreaded code (turns out it's hard!)
- How to make a CLI interface
- How to organize a project (I struggle with properly distributing classes so as not to get lost as the project grows)
- Lots of practice with Stream API

Overall the project took about a month (including learning all the new stuff).

---

## Performance

Tested on 1000 files:

| Threads | Time   |
|---------|--------|
| 1       | 45 sec |
| 2       | 24 sec |
| 4       | 13 sec |
| 8       | 7 sec  |

---

## Future Ideas

- Export results to JSON
- Automatic duplicate deletion (with confirmation)
- Maybe make a GUI version

Not sure if I'll have time, but the ideas are there.

---

## Contact

GitHub: [@Barynova](https://github.com/BarynovaSofia)

Email: sofiabarynova@gmail.com

---