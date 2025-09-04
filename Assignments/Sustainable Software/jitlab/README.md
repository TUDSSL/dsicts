# JITLab: JVM Optimization Testbed

This project is a lightweight Spring Boot server designed to be stressed by a client benchmark in order to study JVM just-in-time (JIT) optimizations and their impact on performance and energy efficiency. The server exposes simple endpoints that perform controlled workloads, while a companion **Bench client** can repeatedly poll these endpoints under configurable load. Together, they provide a reproducible environment for experimenting with JVM flags, system design choices, and runtime metrics to answer questions about efficiency and architecture-aware optimization.

## How to run 

### Requirements 
Java 21 (OpenJDK)
``` 
java -version
# should print "openjdk 21..."
```

Maven 3.9+
```
mvn -v
# should print "Apache Maven 3.9..."
```

Python 3.9+
With the following packages: ```psutil httpx pandas matplotlib```

### Build the project
From the project root:
```
mvn clean package -DskipTests
```
### Run the server
```
java -jar target/jitlab-0.0.1-SNAPSHOT.jar
```
Check if it's alive:
```
curl http://localhost:8080/ping
# -> pong
```

### Monitor
Now that you ran the server, let's monitor it's usage: 
```
PID=$(pgrep -f 'jitlab-0.0.1-SNAPSHOT.jar' | head -n1)
python3 tools/monitor.py --pid $PID --interval 1 --duration 140 --out runs/monitor_cpu.csv
```

### Generate load
After you started the server, and the monitoring tool, put some load on the server:
Compute load:
```
python3 tools/load_py.py \
  --url http://localhost:8080/work/cpu \
  --body '{"iterations":2000,"payloadSize":20000}' \
  --concurrency 8 --warmupSec 10 --runSec 120 \
  --out runs/load_cpu.csv
```

File creation and sending load:
```
python3 tools/load_py.py \
  --url http://localhost:8080/work/files \
  --body '{"fileCount":10,"fileSizeBytes":262144,"prefix":"sample"}' \
  --concurrency 4 --warmupSec 5 --runSec 120 \
  --out runs/load_files.csv
```

### Plot 
To visualize your data use the provided plot tool:
```
# Force non-GUI plotting backend just in case
MPLBACKEND=Agg python3 tools/plot.py \
  --load runs/load_cpu.csv \
  --monitor runs/monitor_cpu.csv \
  --title "CPU endpoint"
```

### Routes
**POST /work/cpu → CPU-bound loop**

Body: { "iterations": <int>, "payloadSize": <int> }

**POST /work/files → create N files, stream them back as a ZIP, clean up**

Body: { "fileCount": <int>, "fileSizeBytes": <int>, "prefix": "<str>" }

### Common issues
You might see on your first run that this will appear in the output: ```[monitor] no energy_uj found; power/energy will be 0```. You will have to search online for a fix based on your CPU manufacturer and Linux distro. Most issues can be fixed by running the monitor script in ```sudo``` mode.

## Experiment 
Here lies the actualy heart of the assignment. Creating the experiment is (almost) entirely up to you. You will have to extend the codebase with any type of stress a sever may encounter. For example you may decite to simulate normal workload a server may encouter with daily and assess the carbon footprint of that.

Keep in mind that whatever the environment, the goal is to test out the abilities of the JIT compiler to reduce the workload.

Here are some flags that when set activate a certain part of the JIT:
```
# Baseline
java -jar target/jitlab-0.0.1-SNAPSHOT.jar

# Interpret-only (no JIT) — slow, great as a control
java -Xint -jar target/jitlab-0.0.1-SNAPSHOT.jar

# C2-only (disable tiered compilation)
java -XX:-TieredCompilation -jar target/jitlab-0.0.1-SNAPSHOT.jar

# C1-only (no C2 JIT): stop tiering at level 1
java -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -jar target/jitlab-0.0.1-SNAPSHOT.jar

# Lower compile threshold (compile sooner)
java -XX:CompileThreshold=1000 -jar target/jitlab-0.0.1-SNAPSHOT.jar

# Single compiler thread (slower warmup)
java -XX:CICompilerCount=1 -jar target/jitlab-0.0.1-SNAPSHOT.jar

# Heap sizing (stabilize GC effects)
java -Xms1g -Xmx1g -jar target/jitlab-0.0.1-SNAPSHOT.jar
```