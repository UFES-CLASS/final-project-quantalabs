JamuSync — Installation & Running Instructions

============================================================
DEFAULT LOGIN CREDENTIALS
============================================================
The database is created and pre-seeded automatically on the
first run, so these accounts work immediately — no setup.

Owner account:
  Username: admin
  Password: admin123

Staff account:
  Username: staff1
  Password: staff123

Guest access:
  Click "Browse Products as Guest" on the login screen (no login needed)
============================================================

REQUIREMENTS
- Java 17 or higher (JDK)
- Maven 3.6+ (only needed if running from source)

HOW TO RUN

Option 1 — Run from source with Maven
1. Open a terminal in the project folder
2. cd jamusync
3. Run: mvn javafx:run

Option 2 — Run the JAR (easiest, no setup)
1. Ensure Java is installed. Check with: java -version
2. Open a terminal in the folder containing jamusync.jar
3. Run: java -jar jamusync.jar
4. The application window opens automatically.

NOTES
- The database (jamusync.db) is created automatically on first launch and
  pre-filled with sample products, sales, and pending orders.
- The AI chatbot requires an internet connection (uses Google Gemini API).
- Tested on macOS (Apple Silicon) and Windows x64.

TEAM QUANTALABS
- Hassan Waleed Hassan Abdo Al-Awadhi — 25523274
- Laraib Arshad — 25523275
- Nada Taufik Shahbal — 25523266
- Rama Walta Alinta Elsaprike — 25523085
