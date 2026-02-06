# Java Agent

> Note check the Byte Code Compatiblity of the ASM Lib Version. I Have tested for Java 8 and 11; failed for 25.


## Compile

```bash
mvn install
```

## Run

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=42693 \
     -javaagent:target/java-agent-1.0-SNAPSHOT.jar \
     -jar target/java-agent-1.0-SNAPSHOT.jar
```

```bash
java -javaagent:target/java-agent-1.0-SNAPSHOT.jar
     -jar target/java-agent-1.0-SNAPSHOT.jar
```

## **Step 4: Package and Run the Agent**

> ...

### Compile the Java agent

```bash
javac -cp asm-9.5.jar:. MyAgent.java MyLogger.java
```

### Create `MANIFEST.MF`

```
Premain-Class: MyAgent
```

### Create the JAR

```bash
jar cmf MANIFEST.MF myagent.jar MyAgent.class MyLogger.class
```

### Run with the Agent

```bash
java -javaagent:myagent.jar -jar YourApplication.jar
```

## Debug

```json
{
    // Use IntelliSense to learn about possible attributes.
    // Hover to view descriptions of existing attributes.
    // For more information, visit: https://go.microsoft.com/fwlink/?linkid=830387
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Debug Java Agent",
            "request": "attach",
            "hostName": "localhost",
            "port": 42693
        }
    ]
}
```

TODO:
- Separar el agente del código.
- Enviar la info a un servidor python en formato json - guardar en sqlite.
