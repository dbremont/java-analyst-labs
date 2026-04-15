# Profile Spec

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

### Timing / Performance Metrics

* **Start timestamp / entry time** – when execution begins.
* **End timestamp / exit time** – when execution finishes.
* **Execution duration / elapsed time** – derived from start and end.
* **CPU usage / cycles** – if measuring CPU cost specifically.

## Data Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "FunctionProfile",
  "type": "object",
  "properties": {
    "processContext": {
      "type": "object",
      "properties": {
        "pid": { "type": "integer", "description": "Process ID" },
        "tid": { "type": "integer", "description": "Thread ID" },
        "threadGroup": { "type": "integer", "description": "Optional parent thread or group ID" }
      },
      "required": ["pid", "tid"]
    },
    "functionIdentification": {
      "type": "object",
      "properties": {
        "functionName": { "type": "string", "description": "Function name or symbol" },
        "module": { "type": "string", "description": "Module or library the function belongs to" },
        "caller": { "type": "string", "description": "Caller function name or symbol" }
      },
      "required": ["functionName"]
    },
    "executionContext": {
      "type": "object",
      "properties": {
        "stackTrace": {
          "type": "array",
          "items": { "type": "string" },
          "description": "Call stack frames leading to this function"
        },
        "parameters": {
          "type": "array",
          "items": {},
          "description": "Function arguments (optional, can be any type)"
        },
        "returnValue": {},
        "description": "Return value or output (optional)"
      },
      "required": ["stackTrace"]
    },
    "performanceMetrics": {
      "type": "object",
      "properties": {
        "startTime": { "type": "number", "description": "Function entry timestamp (ms or ns)" },
        "endTime": { "type": "number", "description": "Function exit timestamp" },
        "duration": { "type": "number", "description": "Execution duration (derived or recorded)" },
        "cpuCycles": { "type": "number", "description": "CPU cycles consumed (optional)" }
      },
      "required": ["startTime", "endTime", "duration"]
    },
    "resourceUsage": {
      "type": "object",
      "properties": {
        "memoryAllocated": { "type": "number", "description": "Memory allocated during execution (bytes)" },
        "memoryFreed": { "type": "number", "description": "Memory freed during execution (bytes)" },
        "ioOperations": {
          "type": "array",
          "items": { "type": "string" },
          "description": "List of I/O operations performed"
        },
        "locks": {
          "type": "array",
          "items": { "type": "string" },
          "description": "Locks or synchronization events"
        }
      }
    },
    "metadata": {
      "type": "object",
      "properties": {
        "samplingFrequency": { "type": "number", "description": "Sampling frequency in Hz (optional)" },
        "instrumentationFlags": {
          "type": "array",
          "items": { "type": "string" },
          "description": "E.g., traced, sampled, counted"
        }
      }
    }
  },
  "required": ["processContext", "functionIdentification", "performanceMetrics"]
}
```

## Capturing Mechanism

* Design a Java agent to instrument and capture every function call.
* Append the captured data to a write-only file and serve it via the web—supporting descriptions of up to ~100 MB for viewing.

## References

* ...
