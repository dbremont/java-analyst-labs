# Sample

- mvn compile exec:java -Dexec.mainClass="com.sample.app.App"
- java -cp "target/sample-1.0-SNAPSHOT.jar:/home/dvictoriano/Code/hacienda/toolbox/libs/ojdbc8.jar com.sample.app.App

Agents:

- /home/dvictoriano/Code/java-analyst-labs/java-agent-jdbc-interposing/agent/target/java-agent-1.0-SNAPSHOT.jar

Attach:

```bash
java \
  -cp "target/sample-1.0-SNAPSHOT.jar:/home/dvictoriano/Code/hacienda/toolbox/libs/ojdbc8.jar" \
  -javaagent:/home/dvictoriano/Code/java-analyst-labs/java-agent-jdbc-interposing/agent/target/java-agent-1.0-SNAPSHOT.jar \
  com.sample.app.App
```

```bash
java \
  --add-modules jdk.httpserver \
  -cp "target/sample-1.0-SNAPSHOT.jar:/home/dvictoriano/Code/hacienda/toolbox/libs/ojdbc8.jar" \
  com.sample.app.App
```

```bash
String url = "jdbc:oracle:thin:@srvoracdbf.sigef.gov.do:15253/des_sigef_3.sigef.gov.do";
        String dbUser = "SIGEF_PRD";
        String dbPass = "refresh_des_sigef_3";
```

## Paths

-> http://localhost:9090/query?conn=1&q=SELECT 1 AS ABC FROM DUAL