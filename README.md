# Legends of Sword and Wand

## Run Tests

Run all tests:

```bash
mvn test
```

Run only use-case tests:

```bash
mvn -Dtest=service.UseCaseTests test
```

## Run Demo

Run the Swing demo:

```bash
./run.sh
```

Optional console demo:

```bash
mvn org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=app.DemoMain
```
