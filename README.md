# Running Axiom Game

## Building the Executable JAR

To build the game as a standalone executable JAR:

```bash
cd game
./gradlew shadowJar
```

This creates: `build/libs/axiom-game.jar` (~22MB with all dependencies)

## Running the Game

### From anywhere:
```bash
java -jar axiom-game.jar
```

### Requirements:
- Java 21+ (JVM toolchain configured for Java 21)
- No additional dependencies needed (all bundled in the JAR)

## Alternative: Run from Gradle

```bash
cd game
./gradlew run
```

## Gradle Shadow Plugin

This project uses [Gradle Shadow](https://github.com/GradleUp/shadow) to create a "fat JAR" that bundles:
- All game code
- All runtime dependencies (LibGDX, Kotlin stdlib, OkHttp, etc.)
- Native libraries (LWJGL3, FreeType)
- Proper manifest with Main-Class attribute

The fat JAR is fully self-contained and portable.
