# Java Agent - Method Timing and Null Pointer Detection

This Java agent provides two main capabilities:
1. **Method Timing**: Measures execution time of all methods and writes to `measurements.csv`
2. **Null Pointer Detection**: Detects and logs NullPointerExceptions

## Method Timing Feature

The agent measures the execution time of every method in transformed classes and writes the measurements to a CSV file called `measurements.csv`.

### Components

- **MeasurementBuffer**: Thread-safe in-memory buffer that stores method measurements and periodically flushes them to CSV
- **TimerHelper**: Static helper methods for timing method execution
- **TimingClassVisitor**: ASM ClassVisitor that wraps all methods with timing instrumentation
- **TimingMethodVisitor**: ASM MethodVisitor that injects timing code at method entry and exit
- **TimingTransformer**: ClassFileTransformer that applies timing instrumentation to classes

### CSV Format

The `measurements.csv` file contains the following columns:
- `className`: Fully qualified class name
- `methodName`: Method name
- `descriptor`: JVM method descriptor (e.g., `()V`, `(I)Ljava/lang/String;`)
- `timestamp`: Unix timestamp in milliseconds when measurement was recorded
- `executionTimeNanos`: Execution time in nanoseconds
- `executionTimeMs`: Execution time in milliseconds (for readability)

Example output:
```csv
className,methodName,descriptor,timestamp,executionTimeNanos,executionTimeMs
com.sample.app.App,fastMethod,()V,1770425060645,5657,0.005657
com.sample.app.App,slowMethod,()V,1770425060745,100209253,100.209253
```

### Configuration

- **Buffer Capacity**: 10,000 measurements (configurable in `MeasurementBuffer.java`)
- **Flush Interval**: 5 seconds (configurable in `MeasurementBuffer.java`)
- **CSV File Location**: Current working directory as `measurements.csv`

### Class Filtering

The timing transformer automatically skips:
- System classes (`java.*`, `javax.*`, `sun.*`, `jdk.*`, `com.sun.*`)
- Agent classes (`com.mycompany.app.*`)
- Native and abstract methods
- Class initialization methods (`<clinit>`)

## Usage

### Building the Agent

```bash
cd agent
mvn clean package
```

### Running with the Agent

```bash
java -javaagent:agent/target/java-agent-1.0-SNAPSHOT.jar -jar your-application.jar
```

### Testing

A sample application is provided to demonstrate the timing functionality:

```bash
cd sample
mvn clean package
java -javaagent:../agent/target/java-agent-1.0-SNAPSHOT.jar -jar target/sample-1.0-SNAPSHOT.jar
```

After running, check the `measurements.csv` file for the timing data.

## Architecture

### Method Timing Flow

1. **Agent.premain()** - Agent entry point that registers transformers
2. **TimingTransformer.transform()** - Transforms class bytecode
3. **TimingClassVisitor.visitMethod()** - Creates method visitor for each method
4. **TimingMethodVisitor** - Injects timing code:
   - At method entry: Calls `TimerHelper.startTimer()`
   - At method exit: Calls `TimerHelper.endTimerAndRecord()`
5. **TimerHelper** - Records measurements to MeasurementBuffer
6. **MeasurementBuffer** - Stores measurements and periodically flushes to CSV

### Bytecode Injection

The timing instrumentation uses ASM's `AdviceAdapter` to inject code at method boundaries:

```java
// At method entry:
long startTime = TimerHelper.startTimer();

// At method exit (before return):
TimerHelper.endTimerAndRecord(startTime, "com/sample/app/App", "fastMethod", "()V");
```

## Null Pointer Detection

The agent also detects NullPointerExceptions and logs them with stack traces. This is implemented via:
- **NullPointerTransformer**: Transforms `java.lang.NullPointerException`
- **NullPointerClassVisitor**: Modifies constructor
- **NullPointerMethodVisitor**: Injects logging on return

See the existing implementation for more details.

## Technical Details

- **ASM Version**: 9.5
- **Java Version**: 8+
- **Thread Safety**: Uses `LinkedBlockingQueue` for concurrent measurement collection
- **Precision**: Uses `System.nanoTime()` for high-precision timing
- **CSV Writing**: Uses `FileWriter` with append mode for periodic flushes