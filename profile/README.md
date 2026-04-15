# Profile

* In which data format store profile data?

## Data Capture

> When profiling a function, you want to capture all the relevant data that allows you to **characterize its behavior, resource usage, and context**. Based on what you listed, here’s a systematic view of the data typically captured:

### Thread / Process Context

* **Process ID (PID)** – the process in which the function is running.
* **Thread ID (TID)** – the thread executing the function.
* **Thread group / parent thread** – optional, helps understand concurrency hierarchies.

### Function Identification

* **Function name / symbol** – uniquely identifies the routine.
* **Module / library** – the binary or library the function belongs to.
* **Call site / caller** – which function invoked this one (important for call graph profiling).

### Execution Context

* **Stack trace / call stack** – the path of function calls leading to this function.
* **Parameters / arguments** – optional, useful if behavior depends on input.
* **Return value / output** – optional, sometimes used to correlate with performance.

### Timing / Performance Metrics

* **Start timestamp / entry time** – when execution begins.
* **End timestamp / exit time** – when execution finishes.
* **Execution duration / elapsed time** – derived from start and end.
* **CPU usage / cycles** – if measuring CPU cost specifically.

### Resource Usage (optional but useful)

* **Memory allocation / deallocation** during the function.
* **I/O operations** – files read/written, network calls.
* **Lock contention / synchronization events** – for multithreaded functions.

### Other Contextual Metadata

* **Sampling frequency** – if using sampling-based profiling.
* **Instrumentation flags** – e.g., whether the function was traced, sampled, or counted.