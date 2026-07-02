# Exercise 1 — System Design Basics

**Course:** System Design Lab  
**Subject Code:** CS3461  

## Question

Create a GitHub repository, configure a Java project in Visual Studio Code, and upload the project to GitHub.

## Project Description

A simple Java project demonstrating basic folder structure, VS Code Java settings, and GitHub repository upload workflow.

## Folder Structure

```text
.
|-- .vscode/
|   `-- settings.json
|-- bin/
|   `-- App.class
|-- src/
|   `-- App.java
`-- README.md
```

> **Note:** The `bin/` folder contains compiled bytecode and should normally be excluded from version control (via `.gitignore`). It is included here only to demonstrate the full VS Code Java project structure for this assignment.

## System Design Concepts

- **Version Control (Git/GitHub):** Tracks changes, enables collaboration, and maintains history of the codebase.
- **Project Structure:** Organizing source code (`src/`), compiled output (`bin/`), and configuration (`.vscode/`) into separate directories for maintainability.
- **Build & Run Workflow:** Compiling Java source files into bytecode and executing them via the JVM.
- **`.gitignore`:** A file that tells Git which files/folders to exclude from version control (e.g., `bin/`, `target/`, `.idea/`). Compiled artifacts should not be pushed to GitHub since they can be regenerated from source.

## Requirements

- Java Development Kit (JDK)
- Visual Studio Code
- Extension Pack for Java in Visual Studio Code

## How to Run

```powershell
javac -d bin src/App.java
java -cp bin App
```
