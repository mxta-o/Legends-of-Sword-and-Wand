# CSSD-2203
cssd 2203 project
rpg-style java system encompassing CRUD functions and database usage

**person a - core logic**
- Hero model
- Class system
- Battle engine
- Spell system
- Related UML + sequence diagrams
- Related test cases

**person b - system logic**
- Profile creation
- PvE campaign logic
- Inn logic
- Database integration
- Gantt chart + backlog
- Related diagrams + test cases

## Running tests and demo

Quick commands to run the project tests and the demo.

- Run the full test suite (all unit tests):

```bash
mvn test
```

- Run only the curated acceptance tests (grader-facing):

```bash
mvn -Dtest=service.AcceptanceTests test
```

- Run only development tests (dev-only tests are under `src/test/java/development`):

```bash
mvn -Dtest="development.*" test
```

- Run a single test class or method:

```bash
# class
mvn -Dtest=ClassName test
# single method
mvn -Dtest=ClassName#methodName test
```

- Run the demo `DemoMain` class from the IDE, or via Maven (no pom plugin required):

```bash
mvn org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=app.DemoMain
```

Notes:
- The acceptance tests are located at `src/test/java/service/AcceptanceTests.java`.
- Development-only tests are grouped under `src/test/java/development/` and are intended for local development and debugging; graders can ignore that folder when evaluating the curated suite.
- If you use an IDE (IntelliJ/Eclipse), you can run tests or the `app.DemoMain` class via the Run menu/context actions.
