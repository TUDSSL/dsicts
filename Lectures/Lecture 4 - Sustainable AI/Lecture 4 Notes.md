`Version: 2.0`
`Contributors: Liwia Padowska
`Publication date: 18.09.2026`


# 1) Central Processing Unit Scaling and Performance Limits

## 1.1) Instruction Level Parallelism Saturation Wall

### What is Instruction Level Parallelism?

**Instruction Level Parallelism (ILP)** refers to executing multiple instructions at the same time within a single Central Processing Unit (CPU) core (Fisher & Rau, 1991, Instruction-Level Parallel Processing). The CPU scans its upcoming instruction stream, looks for instructions that do not depend on each other, and overlaps their execution instead of running them strictly one after another (Fisher & Rau, 1991, Instruction-Level Parallel Processing). For example, if one instruction adds two numbers and another loads data from memory, and neither needs the other's result, the CPU can carry out both at once instead of waiting. This scanning and overlapping happens entirely inside the hardware and is invisible to the programmer.

ILP is limited mainly by two things: **dependencies** and **uncertainty** (Fisher & Rau, 1991, Instruction-Level Parallel Processing; Wall, 1991, Limits of Instruction-Level Parallelism).

**Dependencies**: when instruction B needs the result of instruction A, B simply has to wait. There is no way around this.

**Uncertainty**: branches (`if`/`else` statements) and memory accesses create outcomes the CPU cannot know in advance, which makes it hard to plan ahead and safely schedule work it is not yet sure about (Wall, 1991, Limits of Instruction-Level Parallelism).

### Dependency example

Instruction 2 below **depends** on instruction 1 (a read after write dependency), so they cannot truly run in parallel (Wall, 1991, Limits of Instruction-Level Parallelism):

```nasm
; r1 = r2 + r3
ADD r1, r2, r3

; r4 = r1 * r5   (needs r1 from the ADD)
MUL r4, r1, r5
```

The `MUL` cannot start until the `ADD` produces `r1`. That dependency chain limits ILP regardless of how much execution hardware the core has available (Wall, 1991, Limits of Instruction-Level Parallelism).

### Uncertainty example: branches (control dependency)

With a branch, the CPU does not know which path will execute until the condition is resolved, so it has to **predict** (Fisher & Rau, 1991, Instruction-Level Parallel Processing; Wall, 1991, Limits of Instruction-Level Parallelism):

```c
if (x > 0) {
  y = a + b;
} else {
  y = a - b;
}
z = y * 3;
```

In assembly like terms:

```nasm
CMP x, 0
JLE else_path
ADD y, a, b
JMP join
else_path:
SUB y, a, b
join:
MUL z, y, 3
```

The CPU speculatively executes one path based on a branch prediction (Fisher & Rau, 1991, Instruction-Level Parallel Processing). If the guess is wrong, it **throws away** that work in what is called a pipeline flush, which costs cycles and energy, since the CPU has to re fetch and re execute the correct path from scratch (Aragón, González, & González, 2006, Control Speculation for Energy-Efficient Next-Generation Superscalar Processors; Wall, 1991, Limits of Instruction-Level Parallelism).

### Uncertainty example: memory

Loads can be slow (a cache miss) and the CPU sometimes cannot be sure whether a later store affects an earlier load, a problem known as **aliasing** (Wall, 1991, Limits of Instruction-Level Parallelism).

**(a) Load latency and cache misses:**

```nasm
LOAD r1, [p]        ; could be fast (cache hit) or very slow (miss)
ADD  r2, r1, 1       ; depends on r1, stalls if LOAD is slow
```

The CPU tries to stay busy by executing other instructions that *do not* depend on `r1`, but if too much of the program depends on that one load, ILP collapses (Wall, 1991, Limits of Instruction-Level Parallelism).

**(b) Aliasing and ordering uncertainty (load versus store):**

```nasm
LOAD r1, [p]        ; read memory at p
STORE [q], r2        ; write memory at q
ADD  r3, r1, 5
```

If the CPU cannot prove that `p` and `q` are different addresses, it has to be conservative about reordering the load and store, which reduces ILP. Modern cores use memory disambiguation and speculation to relax this in practice, but a misspeculation can trigger a costly replay (Wall, 1991, Limits of Instruction-Level Parallelism).

### Micro architectural techniques that implement ILP

Modern CPUs implement ILP with a set of micro architectural techniques (Fisher & Rau, 1991, Instruction-Level Parallel Processing; Smith & Sohi, 1995, The Microarchitecture of Superscalar Processors).

- **Pipelining**: instruction execution is split into stages, for example fetch → decode → execute → write back, so that *different instructions occupy different stages at the same time* (Smith & Sohi, 1995, The Microarchitecture of Superscalar Processors). This raises *throughput* (instructions completed per cycle) even though each individual instruction still needs to pass through all four stages to finish.

---

**1. Fetch**: *"go get the instruction"*

The CPU reads the instruction from memory without yet knowing what it means, only retrieving the raw bytes.

```
Memory address 0x04: [ADD instruction bytes]  ← CPU fetches this
```

---

**2. Decode**: *"figure out what it means"*

The CPU interprets the instruction: this is an `ADD`, the inputs are the registers holding `a` and `b`, and the result goes into the register for `x`.

```
ADD  R1, R2, R3
      ↑   ↑   ↑
      x   a   b     ← CPU now knows: add R2 + R3, store in R1
```

---

**3. Execute**: *"actually do the work"*

The ALU (Arithmetic Logic Unit) performs the addition.

```
R2 = 5   (value of a)
R3 = 3   (value of b)

ALU: 5 + 3 = 8
```

---

**4. Write back**: *"save the result"*

The CPU writes the result (`8`) back into the destination register `R1`, which now holds `x`. This is the stage that commits the answer so later instructions can use it.

```
R1 ← 8    (x is now 8, available for the next instruction)
```

---

### Why pipelining matters

Without pipelining, the CPU would finish all four stages of one instruction before starting the next. With pipelining, each stage works on a *different* instruction at the same time once the pipeline has filled:

```
Cycle:       1       2       3       4       5       6
Instr. 1:  Fetch   Decode  Execute Write
Instr. 2:          Fetch   Decode  Execute Write
Instr. 3:                  Fetch   Decode  Execute Write
```

Each instruction still takes four cycles end to end, but once the pipeline is full the CPU completes close to one instruction every cycle instead of one every four. It is worth being precise here: during the very first cycles the pipeline is still filling, so not every stage is occupied yet (in cycle 1 above, only the fetch stage is doing useful work). The throughput gain is a steady state effect that appears once the pipeline has enough instructions in flight to occupy every stage every cycle (Smith & Sohi, 1995, The Microarchitecture of Superscalar Processors).

- **Multiple issue (superscalar)**: the core can start more than one instruction in the same cycle, provided the instructions are independent of each other and a free execution unit of the right type is available, such as a spare ALU for integer arithmetic, a floating point unit, or a load and store unit for memory access (Smith & Sohi, 1995, The Microarchitecture of Superscalar Processors).

Consider the CPU executing this code:

```c
x = a + b;
y = c * d;
z = e - f;
```

### Why these instructions can run in parallel

Each line uses completely different variables, so none of them depend on each other's result and the CPU can run all three in the same cycle:

```
x = a + b;   →   uses R1, R2        ✓ independent
y = c * d;   →   uses R3, R4        ✓ independent
z = e - f;   →   uses R5, R6        ✓ independent
```

### What a superscalar core does

A three way superscalar core has multiple execution units running in parallel, for example a separate ALU for each operation:

```
Cycle 1:
┌─────────────────┬─────────────────┬─────────────────┐
│   ALU Unit 1    │   ALU Unit 2    │   ALU Unit 3    │
│   a + b → x     │   c * d → y     │   e - f → z     │
└─────────────────┴─────────────────┴─────────────────┘
```

All three instructions complete in one cycle instead of three, but only because three independent instructions and three free ALUs happened to line up at the same time.

- **Out of order execution**: the CPU is allowed to **temporarily reorder** instructions internally so that independent ones can run earlier, **without changing the program's final results** (Smith & Sohi, 1995, The Microarchitecture of Superscalar Processors).

Consider the CPU executing this code:

```c
x = load(memory[100]);   // instruction 1, slow, fetches from RAM
y = x + 1;                // instruction 2, depends on x, must wait
z = a + b;                // instruction 3, independent, no reason to wait
w = c * d;                // instruction 4, independent, no reason to wait
```

### Without out of order execution

The CPU executes strictly in program order. Instructions 3 and 4 sit idle even though they have everything they need:

```
Cycle 1:   Instr 1 starts  → fetching x from RAM...
Cycle 2:   Instr 1 waiting → still fetching (RAM is slow, about 100 cycles)
Cycle 3:   Instr 1 waiting → still fetching...
...
Cycle 100: Instr 1 done    → x is ready
Cycle 101: Instr 2 runs    → y = x + 1
Cycle 102: Instr 3 runs    → z = a + b
Cycle 103: Instr 4 runs    → w = c * d

Total: about 103 cycles
```

### With out of order execution

The CPU looks ahead, spots that instructions 3 and 4 are independent, and runs them while waiting for the slow memory fetch:

```
Cycle 1:   Instr 1 starts  → fetching x from RAM...
Cycle 2:   Instr 3 runs    → z = a + b   ✓ (no need to wait)
Cycle 3:   Instr 4 runs    → w = c * d   ✓ (no need to wait)
...
Cycle 100: Instr 1 done    → x is ready
Cycle 101: Instr 2 runs    → y = x + 1

Total: about 101 cycles
```

Instructions 3 and 4 executed *before* instruction 2, out of the original program order, but the final values of `x`, `y`, `z`, and `w` are identical to what strict in order execution would have produced.

### What makes this possible

The CPU uses a structure called a **reorder buffer (ROB)**: a queue that tracks every in flight instruction in its original program order. Instructions can execute internally in any order, but their results only become visible to the rest of the program once they reach the front of the ROB queue, which is what guarantees the final outcome is always correct (Smith & Sohi, 1995, The Microarchitecture of Superscalar Processors).

**Speculative execution**: the CPU **guesses** what will happen next, most importantly which way an `if` branch will go, and starts executing down that path early; if the guess turns out to be wrong, the speculative results are discarded and execution restarts from the correct path (Fisher & Rau, 1991, Instruction-Level Parallel Processing; Wall, 1991, Limits of Instruction-Level Parallelism).

Consider the CPU executing this code:

```c
x = load(memory[100]);   // slow memory fetch (about 100 cycles)

if (x > 0) {
    y = a + b;             // branch A, taken if x is positive
} else {
    y = c * d;              // branch B, taken if x is zero or negative
}

z = y * 2;
```

### Without speculative execution

The CPU cannot touch the `if` block until `x` arrives from RAM. Everything stalls:

```
Cycle 1:    fetch x from RAM...
Cycle 2:    waiting...
Cycle 3:    waiting...
...
Cycle 100:  x = 42 arrives  → condition (x > 0) is TRUE
Cycle 101:  run: y = a + b
Cycle 102:  run: z = y * 2

Total: about 102 cycles, of which about 99 are wasted stalling
```

### With speculative execution

The branch predictor has seen this code run many times before and noticed that `x > 0` is almost always true. It guesses **branch A** and starts executing immediately, without waiting for `x`:

```
Cycle 1:    fetch x from RAM...  (in the background)
Cycle 2:    GUESS: x > 0 is likely true → speculatively run y = a + b
Cycle 3:    speculatively run z = y * 2
...
Cycle 100:  x = 42 arrives → condition confirmed TRUE ✓
Cycle 101:  commit results, y and z become visible to the program

Total: about 101 cycles
```

The CPU ran roughly 98 cycles ahead of the confirmation, with almost no stall at all.

### When the guess is wrong

Now imagine `x` comes back as `-5`:

```
Cycle 1:    fetch x from RAM...
Cycle 2:    GUESS: x > 0 → speculatively run y = a + b
Cycle 3:    speculatively run z = y * 2
...
Cycle 100:  x = -5 arrives → condition is FALSE ✗  wrong branch!
Cycle 101:  discard speculative y and z (never committed)
Cycle 102:  restart from the else branch → y = c * d
Cycle 103:  run: z = y * 2

Total: about 103 cycles, slightly worse than no speculation at all
```

This is called a **branch misprediction penalty**: the cycles and energy spent on the wrong path before it is discarded (Aragón, González, & González, 2006, Control Speculation for Energy-Efficient Next-Generation Superscalar Processors). It is why modern CPUs invest heavily in branch predictors: even though every wrong guess costs cycles and energy, the gains from the much more numerous correct guesses far outweigh the occasional penalty from wrong ones (Fisher & Rau, 1991, Instruction-Level Parallel Processing).

---

### Bernstein's conditions: when "parallel" is safe

A classic way to formalise *when two computations can be safely overlapped* is **Bernstein's conditions** (Bernstein, 1966, Analysis of Programs for Parallel Processing).

The computation, or "process," $p$ operates on two sets of data items (Bernstein, 1966, Analysis of Programs for Parallel Processing):

- $I$: the set of data items it **reads** (its *inputs*),
- $O$: the set of data items it **writes** (its *outputs*).

Two processes $a$ and $b$ can run in parallel without changing the result if, over their entire execution, neither process reads a value the other writes, and neither process writes to the same variable as the other (Bernstein, 1966, Analysis of Programs for Parallel Processing):

$$
I_a \cap O_b = \varnothing \qquad \text{(b does not write something a reads)}
$$

$$
I_b \cap O_a = \varnothing \qquad \text{(a does not write something b reads)}
$$

$$
O_a \cap O_b = \varnothing \qquad \text{(a and b do not write to the same location)}
$$

### Dependent versus independent example

Suppose the goal is to compute $a = x^2 + y^2 + z^2$. In simplified assembly:

```nasm
mul r0, r0, r0  ; step 1: r0 = r0*r0 (x²)              ⎫
mul r1, r1, r1  ; step 2: r1 = r1*r1 (y²)              ⎬ parallel
mul r2, r2, r2  ; step 3: r2 = r2*r2 (z²)              ⎭
add r0, r0, r1  ; step 4: r0 = r0 + r1 (x²+y²)      sequential (needs steps 1 and 2)
add r3, r0, r2  ; step 5: r3 = r0 + r2 (x²+y²+z²)   sequential (needs steps 3 and 4)
```

**Step 1: three independent multiplies (good ILP).**

Instructions 1 to 3 do not rely on each other, since each one reads and writes a different register (`r0`, `r1`, `r2`). The CPU is *allowed* to execute all three at the same time, but only if the hardware has multiple execution units of the right type available. A modern high performance core typically has 2 to 4 integer or floating point multiplier units, so if three multiplier units are free, instructions 1 to 3 can genuinely fire in the same cycle:

```
Cycle 1:
┌──────────────┬──────────────┬──────────────┐
│  Multiplier1 │  Multiplier2 │  Multiplier3 │
│  r0 = r0*r0  │  r1 = r1*r1  │  r2 = r2*r2  │
└──────────────┴──────────────┴──────────────┘
```

If the core only had one multiplier, all three instructions would have to queue up and execute one per cycle, even though they are logically independent. Independence in the code is necessary but not sufficient: the physical execution units also need to be available.

**Step 2: the additions cannot run in parallel because each one depends on a previous result.**

- Instruction 4 must wait for instructions 1 and 2, since it needs both $r0 = x^2$ and $r1 = y^2$ to be ready before it can compute their sum.
- Instruction 5 must wait for instruction 4 (it needs the updated $r0$) and also for instruction 3 (it needs $r2 = z^2$).

This can be seen as a "must happen before" graph:

```
instruction 1 → instruction 4 → instruction 5
instruction 2 → instruction 4
instruction 3 → instruction 5
```

So even though the code starts with three instructions that can run simultaneously, instructions 4 and 5 are forced to execute one after the other. The CPU cannot overlap them no matter how many execution units it has, because instruction 5 cannot even start until instruction 4 finishes. This is the same dependency chain problem described above: the program's own structure, not the hardware, becomes the bottleneck (Wall, 1991, Limits of Instruction-Level Parallelism).

### Diminishing returns of ILP

**Issue width** is how many instructions a core can *start* (hand off to its execution units) per cycle. For example, a core with an issue width of four can send up to four instructions to its ALUs, multipliers, and load and store units on every clock tick. "Issuing" an instruction is the moment the CPU commits to executing it: it has been fetched, decoded, checked for dependencies, and is now being handed to the hardware unit that will run it. This is limited by the size of the **issue queue**, a buffer that holds decoded instructions waiting to be dispatched; the core can only send as many instructions per cycle as that queue is built to release at once.

If programs always had enough independent work available, speedup would scale close to linearly with issue width:

```
Ideal world (enough independent instructions every cycle):
1 issue slot   →  ~1×
2 issue slots  →  ~2×
4 issue slots  →  ~4×
8 issue slots  →  ~8×
```

Real code rarely looks like that (Wall, 1991, Limits of Instruction-Level Parallelism). A more realistic per cycle view for a core with four issue slots is:

```
4 issue slots per cycle: [ _  _  _  _ ]
```

**Case A: lots of independence (rare for long stretches)**

```
cycle 1: [ A  B  C  D ]   →  4 instructions started
cycle 2: [ E  F  G  H ]   →  4 instructions started
```

**Case B: a dependency chain (very common)**

```
cycle 1: [ A  _  _  _ ]
cycle 2: [ B  _  _  _ ]   (B must wait for A's result)
cycle 3: [ C  _  _  _ ]
```

**Case C: typical mixed code**

```
cycle 1: [ A  B  _  _ ]
cycle 2: [ C  _  _  _ ]   (branch, memory, or dependency stalls)
cycle 3: [ D  E  _  _ ]
```

So even with four issue slots built into the hardware, the program often only supplies one or two "ready" instructions at a time, which is why the measured speedup curve **flattens** well below the theoretical maximum (Wall, 1991, Limits of Instruction-Level Parallelism). A helpful mental model is **lane utilisation**: a core with four issue slots that on average only fills two of them delivers roughly two instructions per cycle on average, and widening it to eight issue slots does not help much if the program still only supplies two or three independent instructions most of the time.

This is the **ILP wall**: beyond a few instructions per cycle, the program's own dependency structure, together with branch and memory uncertainty, prevents wide cores from staying busy (Wall, 1991, Limits of Instruction-Level Parallelism). Wall (1991) measured real programs and found that even under idealised conditions, meaning perfect branch prediction, infinite registers, and unlimited hardware, most programs only expose on the order of **5 to 7 instructions** worth of parallelism on average, far below what a wide superscalar machine could theoretically consume (Wall, 1991, Limits of Instruction-Level Parallelism). This is the fundamental ceiling of ILP: the limit lives in the code's own dependency structure, not in the chip.

### Modern CPU example: Apple M4 Pro

---

The Apple M4 Pro is a high performance ARM based system on chip built on TSMC's 3 nanometre process, with 14 cores (10 performance cores plus 4 efficiency cores) and support for up to 64 GB of unified memory (Apple, 2024, Apple Introduces M4 Pro and M4 Max). Like other modern high performance cores, it relies on the ILP techniques covered above: an out of order execution engine with wide superscalar dispatch, deep pipelining that keeps execution units fed across branch mispredictions, register renaming, dynamic scheduling, and speculative execution, all working together so that independent instructions can execute out of program order while results are still committed correctly (Smith & Sohi, 1995, The Microarchitecture of Superscalar Processors).

Apple does not officially publish the exact issue width or branch predictor accuracy of the M4 Pro's core. Independent microarchitectural analyses place modern high end cores like this one among the widest currently shipping, but those specific numbers should be treated as third party estimates rather than confirmed specifications, so they are intentionally left out of the table below.

| Property | Value |
| --- | --- |
| Cores | 14 (10 performance + 4 efficiency) |
| Process node | 3 nm (TSMC) |
| Max unified memory | 64 GB |
| Announced | October 2024 |

## 1.2) Transistor Count (Power Wall)

### Moore's Law kept giving more transistors, but not "free speed"

For a long time, the industry got a *double win* every generation (Sutter, 2005, The Free Lunch Is Over):

1. **More transistors** (denser chips)
2. **Higher clock speeds** *without blowing the power budget*

That "free lunch" ended in the middle of the 2000s: manufacturers could still add transistors, but could no longer keep raising frequency and voltage without hitting thermal limits (Sutter, 2005, The Free Lunch Is Over; COMSOL, 2014, Haven't CPU Clock Speeds Increased in the Last Few Years?). Herb Sutter popularised this turning point in a widely read 2005 article titled *"The Free Lunch Is Over"* (Sutter, 2005, The Free Lunch Is Over).

---

### Why frequency hit a wall

### The key relationship (why GHz gets hot fast)

A widely used approximation for **dynamic (switching) power** in CMOS is (Rabaey, Chandrakasan, & Nikolić, 2003, Digital Integrated Circuits: A Design Perspective):

$$
P \approx C \cdot V^2 \cdot f
$$

$C$: effective capacitance being switched

$V$: supply voltage

$f$: clock frequency

So (Rabaey, Chandrakasan, & Nikolić, 2003, Digital Integrated Circuits: A Design Perspective):

- doubling **frequency** tends to roughly double power
- increasing **voltage** is particularly costly because of the V^2 term: a modest voltage increase produces a disproportionately large rise in power consumption

**Concrete example (why simply raising GHz becomes thermally unsustainable):**

Assume the chip starts at:

$$
V = 1.0, \quad f = 3\text{ GHz} \quad \Rightarrow \quad P \propto 1.0^2 \cdot 3 = 3
$$

Now push the frequency to 6 GHz, which in practice needs a slightly higher voltage, say $V = 1.2$, to keep the chip electrically stable:

$$
P \propto 1.2^2 \cdot 6 = 1.44 \cdot 6 = 8.64
$$

That is a **doubling of frequency** producing close to a **threefold increase in power**: the chip now generates nearly three times as much heat for only twice the speed.

In principle more power could be supplied to the chip, but heat is the hard constraint: every watt consumed becomes a watt of heat that has to be physically removed from a piece of silicon roughly the size of a fingernail. Cooling has real limits; eventually fans, heat sinks, and even liquid cooling cannot extract heat fast enough to keep the chip below its maximum safe operating temperature. Exceeding that temperature causes transistors to malfunction, produce incorrect results, or degrade permanently. Even before that point, the cooling hardware required becomes impractically large, loud, or expensive for a consumer product, and in a data centre the electricity bill simply scales with power: running a chip at three times the power to get twice the speed is not economical at scale. This is why frequency scaling stopped being a practical path to performance: the thermal and energy cost grows faster than the performance gain (Sutter, 2005, The Free Lunch Is Over; COMSOL, 2014, Haven't CPU Clock Speeds Increased in the Last Few Years?).

---

### End of Dennard scaling

**Dennard scaling**, the classic scaling theory, said that if transistors shrink, voltage and current can also be scaled down so that power density stays roughly constant while speed improves (Dennard, Gaensslen, Yu, Rideout, Bassous, & LeBlanc, 1974, Design of Ion-Implanted MOSFET's with Very Small Physical Dimensions).

Around **2005 to 2007**, voltage scaling largely stalled and leakage current became a much bigger share of total power (COMSOL, 2014, Haven't CPU Clock Speeds Increased in the Last Few Years?). As a result, transistors kept shrinking, but chips stopped becoming proportionally more energy efficient.

### What happened in practice: clock rates plateaued

Clock speeds rose rapidly through the early 2000s and then flattened from roughly 2005 onward, with mainstream CPUs remaining in a similar GHz range since (COMSOL, 2014, Haven't CPU Clock Speeds Increased in the Last Few Years?; Sutter, 2005, The Free Lunch Is Over).

```
Before about 2005:
  smaller transistors → lower voltage possible → higher frequency
  (still fits within power and thermal limits)

After about 2005:
  smaller transistors → voltage can no longer drop much →
  pushing frequency higher makes power explode
```

---

### Dark silicon: transistors you cannot afford to switch on

If transistor count keeps growing while the chip's thermal design power (TDP) budget stays roughly bounded, then **not everything on the chip can be fully active at the same time** (Esmaeilzadeh, Blem, St. Amant, Sankaralingam, & Burger, 2011, Dark Silicon and the End of Multicore Scaling). Parts of the chip have to stay unpowered, or "dark," to keep the whole design within its power and temperature budget; the powered, active regions are conventionally drawn as "lit" and the unpowered regions as "dark" (Esmaeilzadeh, Blem, St. Amant, Sankaralingam, & Burger, 2011, Dark Silicon and the End of Multicore Scaling). This is **dark silicon**.

![dark_silicon.png](Figures/dark_silicon.png)

A well known illustration of this plots, on the horizontal axis, the year in which each successive manufacturing technology node becomes mainstream, and on the vertical axis, the number of cores that fit on a chip (Hardavellas, 2012, The Rise and Fall of Dark Silicon). One line (dashed) shows the maximum number of cores that physically fit on the die at that technology node, and another line (solid) shows the number of cores that a peak performance design actually uses according to power constrained models; the growing gap between the two lines is the dark silicon effect (Hardavellas, 2012, The Rise and Fall of Dark Silicon). At the 20 nm node, for example, as many as 1000 cores could physically fit on a single die, yet a design with roughly an order of magnitude fewer cores is closer to the power efficient optimum, because populating the chip with far more cores would require more power and bandwidth than the design can supply, forcing supply voltage down and the whole system to run slower (Hardavellas, 2012, The Rise and Fall of Dark Silicon).

*Legend: **Lit** = powered core · **Dark** = unpowered silicon.*

---

### Implication for CPU design

Once "more GHz" stopped working, designers used the extra transistor budget differently (Sutter, 2005, The Free Lunch Is Over):

- **More cores**: trading single thread GHz for parallel throughput
- **Bigger caches**: to reduce slow trips to main memory (see the memory wall section below)
- **Specialised accelerators**: carrying out specific tasks with a better performance per watt than a general purpose core

---

## 1.3) Memory Wall

### Core idea: CPU performance has improved much faster than memory performance

The **memory wall** is the widening gap between how quickly a CPU can execute operations and how quickly data can be delivered from main memory, or DRAM (Wulf & McKee, 1995, Hitting the Memory Wall: Implications of the Obvious).

A tangible way to state it:

> modern CPUs can complete a great deal of arithmetic work in the time it takes to service a single DRAM access.
> 

As a representative order of magnitude comparison (Hennessy & Patterson, 2019, Computer Architecture: A Quantitative Approach):

- about **4 cycles** for a multiply
- about **200 cycles** to access DRAM

While the CPU waits for that one DRAM access, it could in principle have completed dozens of arithmetic operations, but it cannot, simply because it is missing the data those operations need.

### A timeline illustration of what a stall feels like

```
CPU wants: load A[i] from DRAM

Cycles:
0      50     100    150    200
|------|------|------|------|
CPU:   waiting... waiting... waiting...  data arrives
```

During that wait, the core's execution units sit under used and overall performance becomes **memory bound**, meaning it is limited by memory rather than by compute (Hennessy & Patterson, 2019, Computer Architecture: A Quantitative Approach).

---

### Latency versus bandwidth: two different problems

- **Latency**: how long it takes until the first byte of requested data arrives; high latency causes stalls, because the next step cannot begin until the data shows up.
- **Bandwidth**: how many bytes per second can be streamed once a transfer is already underway; bandwidth describes a theoretical upper limit, while throughput is the rate actually achieved in practice.

(Hennessy & Patterson, 2019, Computer Architecture: A Quantitative Approach)

Different workloads are bottlenecked by one or the other of these two problems. **Graph algorithms** are typically latency bound: visiting the next node means first resolving a pointer from the current node, so each access has to wait for the previous one to finish, and the chain of dependent, unpredictable accesses is what dominates runtime. **Machine learning training**, by contrast, is typically bandwidth bound: it streams large, mostly independent batches of weights and activations, so the sustained rate of data delivery, not the delay of any single access, sets the pace (Hennessy & Patterson, 2019, Computer Architecture: A Quantitative Approach).

---

### Why caches exist, and why they use so many transistors

A **cache** is a small, fast memory placed close to the core that stores recently or frequently used data (Hennessy & Patterson, 2019, Computer Architecture: A Quantitative Approach). Caches work because most programs show **locality**:

- *temporal locality*: the same data tends to be reused again soon
- *spatial locality*: nearby addresses tend to be used again soon after

Architecturally, CPUs build this into a **cache hierarchy**:

```
Registers       (tiny, fastest)
   ↓
L1 cache        (small, very fast)
   ↓
L2 / L3 cache   (bigger, slower)
   ↓
  DRAM          (huge, slowest)
```

Performance is often summarised using the **average memory access time (AMAT)** (Hennessy & Patterson, 2019, Computer Architecture: A Quantitative Approach):

$$
AMAT = T_{\text{hit}} + (M_{\text{rate}} \times M_{\text{penalty}})
$$

where:

- $T_{\text{hit}}$ **(hit time)**: the time to access data that is already found in the cache, usually very fast, on the order of a few CPU cycles.
- $M_{\text{rate}}$ **(miss rate)**: the fraction of memory accesses that are not found in the cache; for example $M_{\text{rate}} = 0.05$ means 1 out of every 20 accesses misses.
- $M_{\text{penalty}}$ **(miss penalty)**: the extra time required to fetch the data after a miss, typically involving lower levels of memory such as L2, L3, or DRAM, which can take tens to hundreds of cycles.

This formula captures why misses that fall all the way through to DRAM are so costly: because the miss penalty term is multiplied in, even a small miss rate can significantly increase AMAT (Hennessy & Patterson, 2019, Computer Architecture: A Quantitative Approach).

---

### Mitigations help, but do not remove the wall

Architects try to hide memory latency with several complementary techniques.

**Prefetching** brings data into the cache before it is actually requested, so that by the time the CPU needs it, the data is already waiting in fast cache rather than sitting in slow RAM. A hardware prefetcher watches the pattern of memory accesses and tries to predict what will be needed next; if it observes accesses to `A[0]`, `A[1]`, `A[2]` in sequence, it will speculatively load `A[3]`, `A[4]`, and so on ahead of time in the background. This works well for predictable, sequential access patterns, but fails for irregular or pointer chasing patterns where the next address cannot be guessed until the previous load completes:

```c
// ✓ prefetcher works well: sequential, predictable
for (i ...) sum += A[i];   // prefetcher sees the pattern and runs ahead

// ✗ prefetcher cannot help: each address depends on the previous load
node = head;
while (node) {
    sum += node->value;
    node = node->next;    // next address is unknown until this load finishes
}                          // every step is a potential cache miss, with no way to prefetch
```

Traversing a linked list is one of the most difficult access patterns for a cache to help with, because each pointer dereference has to wait for the previous one to complete before the CPU even knows where to look next (Hennessy & Patterson, 2019, Computer Architecture: A Quantitative Approach).

**Out of order execution** lets the core do other independent work while a memory load is pending; this is the same mechanism described in section 1.1 above. When the CPU issues a slow memory fetch, it does not stall immediately; instead it looks ahead in the instruction stream and executes any independent instructions that do not need the result of that fetch. The memory wall is precisely what makes out of order execution valuable in practice: without long memory latencies there would be little idle time to fill.

```c
x = load(memory[100]);   // slow, triggers a cache miss, about 100 cycle wait
y = x + 1;                 // blocked, needs x, cannot proceed
z = a + b;                 // ✓ independent, out of order engine runs this now
w = c * d;                 // ✓ independent, and this
// ...if there are no more independent instructions, the core stalls anyway
```

But if the program keeps missing cache because of poor locality, the core still spends a lot of time stalled; out of order execution can only cover the gap when there is enough independent work available, and in memory bound programs there often is not enough (Hennessy & Patterson, 2019, Computer Architecture: A Quantitative Approach).

---

### Why this matters, especially for modern workloads

The memory wall is one reason performance gains increasingly come from (Hennessy & Patterson, 2019, Computer Architecture: A Quantitative Approach):

- **improving locality** (algorithm and data layout)
- **increasing parallelism** (many threads)
- **using architectures that tolerate latency**
- **moving compute closer to data**

**Improving locality** means arranging data so the CPU finds what it needs already sitting in fast cache rather than fetching it from slow RAM. Think of it like a chef: instead of walking to the storage room for every single ingredient, a good chef brings everything needed for a dish to the countertop first. The CPU does something similar: it loads a whole chunk of nearby memory, a **cache line**, at once, so if data is laid out sequentially, neighbouring values come along for free. Jumping around randomly turns every access into a separate trip to the storage room.

A classic example is matrix traversal. In C, rows are stored contiguously in memory, so:

```c
// ✓ GOOD: row by row, neighbours are loaded for free in the same cache line
for (i ...) for (j ...) sum += A[i][j];

// ✗ BAD: column by column, each step jumps to a different part of memory
for (j ...) for (i ...) sum += A[i][j];
```

Same computation, same hardware, but the column major version can be **5 to 10 times slower** purely because of cache misses (Hennessy & Patterson, 2019, Computer Architecture: A Quantitative Approach).

**Increasing parallelism** and **using architectures that tolerate latency** both attack the same problem from a different angle. Graphics Processing Units (GPUs), for example, hide memory wait times by switching almost instantly to another thread: when one thread stalls waiting for data from RAM, the GPU simply moves on to run a different thread that is already ready to go, keeping the hardware busy at all times. Because a GPU typically has thousands of threads in flight simultaneously, there is almost always another thread ready to execute, so the wait is effectively hidden behind useful work (Hennessy & Patterson, 2019, Computer Architecture: A Quantitative Approach).

Finally, **moving compute closer to data**, through near memory or processing in memory designs, attacks the problem at its root by shortening the physical and electrical distance data has to travel in the first place (Hennessy & Patterson, 2019, Computer Architecture: A Quantitative Approach).

## 1.4) Energy Efficiency Wall (Physical Limits of FLOP/J in CMOS)

Even with continued improvements in chip design, there are **physical and architectural limits** to how energy efficient *CMOS microprocessors* can become (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors). The goal of this section is to estimate an **upper bound** on energy efficiency, measured in floating point operations per joule (**FLOP/J**), for CMOS processors under the **current hardware paradigm** (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors).

### Why this matters, after the end of Dennard scaling

Historical efficiency gains in computing have been enormous, but Ho, Erdil, and Besiroglu (2023) argue that those gains are likely to slow further as the industry approaches fundamental physical limits, which makes it useful to model what the ceiling could actually be. Rather than extrapolating near term industry roadmaps, the paper takes a transparent, first principles and engineering based approach, and it explicitly reports uncertainty ranges around its estimates (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors).

### Core idea: total energy per FLOP is dominated by a small number of components

The paper's central claim is that, in an optimised CMOS processor, the energy per FLOP can be approximated as the sum of a small number of dominant components: the energy to switch transistors, the energy to charge and discharge wire capacitances, and static leakage power (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors):

$$
E_{\text{total}} \approx E_{\text{transistor}} + E_{\text{interconnect}} + E_{\text{leakage}}
$$

where $E_{\text{transistor}}$ is the dynamic energy to switch transistors and $E_{\text{interconnect}}$ is the dynamic energy to charge and discharge wire capacitances. Static leakage power is analysed in the paper as well, but the authors argue it is unlikely to be the dominant limiter in their optimistic upper bound scenario, so the two dynamic terms are the main focus (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors).

---

### A) Transistor switching limit (logic energy)

The transistor switching component is modelled as:

$$
E_{\text{transistor}} = Q_S \cdot N_T
$$

- $Q_S$: energy dissipated per transistor switch
- $N_T$: number of transistor switches needed per FLOP

(Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors)

**Landauer's principle and reliability overhead: why "near zero" is not possible**

A theoretical floor for $Q_S$ comes from **Landauer's principle**: any irreversible bit operation must dissipate at least

$$
E_{\min} = k_B \, T \, \ln 2
$$

of energy per bit erased, where $k_B$ is Boltzmann's constant and $T$ is the operating temperature (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors). At room temperature ($T = 300\text{ K}$), this floor works out to approximately

$$
E_{\min} \approx 2.9 \times 10^{-21}\text{ J per bit}
$$

In practice, however, computing needs reliability margins to avoid errors from thermal noise, which pushes real minimum switching energies **1 to 2 orders of magnitude** above the pure Landauer value (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors).

**Overhead per FLOP matters (control logic versus pure arithmetic)**

Even if an arithmetic unit itself can be built from relatively few transistors, real processors need substantial additional circuitry for control, scheduling, routing, and robustness, which increases the effective $N_T$ per FLOP well above the bare arithmetic minimum. The paper uses real hardware, including NVIDIA's H100 GPU, as a sanity check to argue that the FLOPs a chip actually delivers to a user sit on top of substantial transistor overhead beyond the arithmetic core itself (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors).

---

### B) Interconnect limit (wire energy, the cost of moving bits)

A major contribution of the paper is to stress that **interconnect energy** can come to dominate total energy as logic itself gets cheaper to switch (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors). Interconnect energy is driven by charging and discharging wire capacitance, following the standard relation:

$$
E = \tfrac{1}{2} C V^2
$$

The paper rewrites the interconnect cost per FLOP in terms of:

- $C_L$: capacitance per unit length of wire
- $L$: average wire length switched
- $N$: number of wires charged per FLOP
- $V$: supply voltage

giving a relation of the form:

$$
E_{\text{interconnect}} \propto C_L \cdot L \cdot N \cdot V^2
$$

The key takeaway is: **even if transistor switching approaches its fundamental limits, signals still have to physically travel across wires**, and that cost is hard to scale down. The paper argues that capacitance per unit length has not improved much historically, and reducing it further tends to trade off against signal delay, materials constraints, or practical layout considerations (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors).

---

### C) The paper's headline ceiling estimate

To make the estimate as optimistic and forward looking as possible, the paper focuses on low precision compute (4 bit floating point, FP4) and builds a Monte Carlo model over plausible ranges for its parameters (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors). The resulting distribution has a reported **geometric mean** maximum efficiency of:

$$
\approx 4.7 \times 10^{15}\ \text{FP4 operations per joule}
$$

with substantial uncertainty of roughly 0.7 orders of magnitude in log space (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors).

Relative to the most efficient dense GPU accelerators available at the time the paper was published in 2023 (for example NVIDIA's H100), the authors estimate substantial remaining headroom before this physical ceiling binds, on the order of **two to three orders of magnitude** at comparable precision, with a more specific figure of roughly **200 times** reported for a comparison at 16 bit precision (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors). Because hardware efficiency continues to improve year over year, any such headroom figure should be read as relative to the 2023 state of the art at the time of publication, not as a fixed constant.

---

### D) Scope and assumptions (what this ceiling does not cover)

This limit applies to CMOS operating within its **current paradigm**, and the paper explicitly assumes **irreversible** switching throughout (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors). The estimate is **not** intended to bound fundamentally different future paradigms, such as fully **reversible or adiabatic CMOS**, optical or spin based computing, or in memory computing architectures (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors).

---

### How this connects back to the CPU and GPU story

- This explains why simply adding more transistors does not keep improving efficiency indefinitely: eventually **physics and wires** dominate (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors).
- It also reinforces the memory wall lesson from section 1.3: **data movement dominates energy** just as it dominates latency, which is consistent with interconnect energy being a core limiter at scale (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors).
- Finally, it motivates specialised accelerators such as TPUs and NPUs: specialisation can reduce control and routing overhead and improve FLOP/J *within CMOS*, but any such design still operates under the same broad physical constraints described in this section (Ho, Erdil, & Besiroglu, 2023, Limits to the Energy Efficiency of CMOS Microprocessors).


## References

Apple. (2024, October 29). Apple introduces M4 Pro and M4 Max. Apple Newsroom.

Aragón, J. L., González, J., & González, A. (2006). Control speculation for energy-efficient next-generation superscalar processors. *IEEE Transactions on Computers*, 55(3), 281–291.

Bernstein, A. J. (1966). Analysis of programs for parallel processing. *IEEE Transactions on Electronic Computers*, EC-15(5), 757–763.

COMSOL. (2014). Haven't CPU clock speeds increased in the last few years? COMSOL Blog.

Dennard, R. H., Gaensslen, F. H., Yu, H.-N., Rideout, V. L., Bassous, E., & LeBlanc, A. R. (1974). Design of ion-implanted MOSFET's with very small physical dimensions. *IEEE Journal of Solid-State Circuits*, 9(5), 256–268.

Esmaeilzadeh, H., Blem, E., St. Amant, R., Sankaralingam, K., & Burger, D. (2011). Dark silicon and the end of multicore scaling. In *Proceedings of the 38th Annual International Symposium on Computer Architecture (ISCA '11)* (pp. 365–376). Free PDF: research.cs.wisc.edu.

Fisher, J. A., & Rau, B. R. (1991). Instruction-level parallel processing. *Science*, 253(5025), 1233–1241.

Hardavellas, N. (2012). The rise and fall of dark silicon. *USENIX ;login:*, 37(2), 7–17. Free PDF: usenix.org.

Hennessy, J. L., & Patterson, D. A. (2019). *Computer architecture: A quantitative approach* (6th ed.). Morgan Kaufmann.

Ho, A., Erdil, E., & Besiroglu, T. (2023). Limits to the energy efficiency of CMOS microprocessors. arXiv:2312.08595.

Rabaey, J. M., Chandrakasan, A., & Nikolić, B. (2003). *Digital integrated circuits: A design perspective* (2nd ed.). Prentice Hall.

Smith, J. E., & Sohi, G. S. (1995). The microarchitecture of superscalar processors. *Proceedings of the IEEE*, 83(12), 1609–1624. Free PDF: ftp.cs.wisc.edu.

Stanford Institute for Human-Centered AI. (2024). The 2024 AI Index report. Full report PDF: aiindex.stanford.edu.

Strubell, E., Ganesh, A., & McCallum, A. (2019). Energy and policy considerations for deep learning in NLP. In *Proceedings of the 57th Annual Meeting of the Association for Computational Linguistics* (pp. 3645–3650).

Sutter, H. (2005). The free lunch is over: A fundamental turn toward concurrency in software. *Dr. Dobb's Journal*, 30(3), 202–210.

Wall, D. W. (1991). Limits of instruction-level parallelism. *ACM SIGPLAN Notices*, 26(4), 176–188. Free technical report version: DEC WRL Research Report 93.6.

Wulf, W. A., & McKee, S. A. (1995). Hitting the memory wall: Implications of the obvious. *ACM SIGARCH Computer Architecture News*, 23(1), 20–24.