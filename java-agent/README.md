# Java Monitoring Infrastructure

- Agent: `/home/dvictoriano/Code/cs-analyst-labs/java-agent/agent/target/java-agent-1.0-SNAPSHOT.jar`

- Monitoring System: `/home/dvictoriano/Code/cs-analyst-labs/java-agent/sample/target/sample-1.0-SNAPSHOT.jar`

```java
java -javaagent:agent/target/java-agent-1.0-SNAPSHOT.jar  -jar sample/target/sample-1.0-SNAPSHOT.jar
```

> {...}/bin/standalone.conf

## Server

- `python3 server.py`
