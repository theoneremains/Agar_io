# Agar_io
AIU Hackathon group project — Agar.io clone made with Java
Group Members : Kamil Yunus Özkaya, Mert Efe Sevim, Ahmet Kurt

## How to Play
You are a circular cell that can eat other cells smaller than you, and you get bigger. Score increases as player eats other cells.
Aim is to get the highest score!

Controls: **WASD** or **Arrow Keys** to move, **ESC** to return to the main menu.

## Features
- Camera follows the player cell across a large scrolling world (3840 × 2160)
- Highscore is tracked and displayed on the main menu between games
- Options menu:
  - **SOUND** — toggle all game sounds on/off
  - **COLOR** — cycle through available player cell colors

## How to Run
Open the project in **IntelliJ IDEA** (`Agar_io.iml`) and run `MainClass`.

Or from the command line:
```bash
javac -d out/production/Agar_io src/*.java
java -cp out/production/Agar_io MainClass
```
Note: copy the `.png` and `.wav` files from `src/` into `out/production/Agar_io/` if running from the command line.

## Known Limitations
- Single player only (no multiplayer)

If you see any errors or have an idea to improve the code, please make comments or open an issue.
Enjoy!!!
