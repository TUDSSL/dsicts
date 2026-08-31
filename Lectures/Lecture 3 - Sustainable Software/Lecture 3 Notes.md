`Version: 1.0`
`Contributors: Radosław Majer, Alexandru Postu`
`Publication date: 18.09.2025`

# Introduction to sustainable software
Through green or sustainable software, we [understand](https://learn.greensoftware.foundation/introduction/) designing solutions that emit the least carbon possible. This efficiency can be achieved at an energy, carbon and hardware level. Thus, when discussing sustainable engineering, we refer to software engineering practices that help to achieve minimum carbon emissions.

The Green Software Foundation came up with [6 base principles](https://learn.greensoftware.foundation/introduction/) that green software should adhere to, namely:
1. **Carbon Efficiency**: Emit the least amount of carbon possible.
2. **Energy Efficiency**: Use the least amount of energy possible.
3. **Carbon Awareness**: Do more when the electricity is cleaner and do less when the electricity is dirtier.
4. **Hardware Efficiency**: Use the least amount of embodied carbon possible.
5. **Measurement**: What you can't measure, you can't improve.
6. **Climate Commitments**: Understand the exact mechanism of carbon reduction. 

# Sustainable software metrics
The base assumption is that we should strive for software applications that minimize the **carbon footprint**. However, carbon footprint is affected by other variables that a developer is not able to reliably control, such as how clean the energy production sources are. In other words, carbon footprint as a measurement is something that can be used at a wider project-level, but not at a purely software one.

When discussing the software engineering part of such a project,  we must switch our focus to more granular metrics. A sufficiently relevant measure is the energy consumed by software itself, as minimizing energy consumption implies reducing carbon footprint as well. Minimizing energy consumption must happen at two different levels. Firstly, developers should strive to minimize energy consumption during the code development phase itself. However, they should also develop applications with that minimize the energy consumption when used by customers.

## Metrics
To design energy efficient software, we must first be able to quantify the energy consumption of software. Thus, we need to introduce metrics that we can reliably use to measure energy consumption. Herein, we distinguish between **electrical energy** and **power**. 
### Energy
Electrical energy, understood as the energy associated with the flow of charged particles through a conductor, is what we need to develop and run a software application. It is most commonly measured in joules (J) and kilowatts-hour (kWh). This is measured by the energy profilers that you use, but more on that later.
### Power
Power can be understood as the amount of energy used per unit of time. It is commonly measured in watts (W).  This is measured by the energy profilers that you use, but more on that later.
#### Average Power
This can be used when power is not consistent over time. You average the power in a time interval and divide that by the length of the time interval, leading to the formula:

Average power can easily be converted to energy via the formula:
$$ E = P_{\text{average}}\Delta t$$
#### Energy-Delay Product (EDP)
Next to energy consumption, this metric additionally considers the time taken to run an application. Consequently, it favors applications which minimize energy consumption **and** the runtime.
$$EDP = E  t =\Delta P  t^2 $$

## Measurement 
Measuring the power consumption of your software may be done in multiple ways. On the one hand, we have hardware-based solutions, which are the most reliable. On the other hand, there are the energy profilers which, though an estimate, provide more customizability.

### Hardware Solution
The simplest and most accurate solution is to measure power directly through the socket, with the help of a [watt meter](https://en.wikipedia.org/wiki/Wattmeter). You can simply plug in the device that you want to run the application on. Measure the power output when the device is in an idle state and compare it against the power output of your device when running the application.

While this approach is simple and reliable, it does not provide us with a lot of special information, aside from power consumption. Therefore, we turn to energy profilers.
### Energy Profilers
**Energy profilers**  are software tools that [estimate](https://dl.acm.org/doi/10.1145/2695664.2695825) "the energy consumption of a system based on the computational resources used by applications, and by monitoring the hardware resources".  The **advantage** is that you can use these tools on your own devices, with no additional hardware modifications being required. However, these tools have a **disadvantage**: they present an **estimation** of energy consumption, meaning that their output cannot completely reflect reality.

There exist multiple [energy profiling tools](https://luiscruz.github.io/2021/07/20/measuring-energy.html). Picking the right one is a difficult choice to make, but one aspect to first consider is your device specifications, as different computer architectures leverage different approaches for their energy estimations.  For instance, some profilers may not be available on all operating systems or not have M1 support.

#### Energy Profilers for developing Mobile Applications
| Tool                                                                                                                         | Description                                                                                                                                                                                                                                                                   | Availability                                                                                                                                                                              | **Mobile OS**                                                                                                                                                                                 |
| ---------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [**Android Studio's Power Profiler**](https://developer.android.com/studio/profile/power-profiler)                           | Can be used for Android applications development. Records power consumption data and is part of the CPU profiler. Information is available at a device level only.                                                                                                            | Available beginning with [Android Hedgehog](https://developer.android.com/studio/releases/past-releases/as-hedgehog-release-notes#new-power-profiler]release), released in November 2023. | Android. This is only available on devices  whose hardware supports On-Demand Power Management (ODPM). This is available on Pixel 6 and subsequent devices that run on Android 10 and higher. |
| [Xcode's Power Profiler](https://developer.apple.com/documentation/Xcode/measuring-your-app-s-power-use-with-power-profiler) | Can be specifically used for iOS application development and is available in the Xcode environment. You may use it to obtain power traces of your target Apple devices. A brief tutorial may be found [here](https://developer.apple.com/videos/play/wwdc2025/226/?time=112). | Should be available from Xcode 15 onwards.                                                                                                                                                | iOS. [Here](https://developer.apple.com/support/xcode/), you may find an overview of iOS versions, depending on Xcode version.                                                                |

#### Energy Profilers for Desktop Development
| Tool                                                                                                                                  | Description                                                                                                                                                                                                                                                     | **OS**                                                                                                                                                                                            | CPU                       |
| ------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------- |
| [Intel Performance Counter Monitor](https://www.intel.com/content/www/us/en/developer/articles/tool/performance-counter-monitor.html) | Provides in-depth processor related statistics.                                                                                                                                                                                                                 | Linux, Windows, macOS. You may refer to the [Github](https://github.com/intel/pcm?tab=readme-ov-file#downloading-pre-compiled-pcm-tools) for information about each operating system.             | Intel                     |
| [Powerstat](https://github.com/ColinIanKing/powerstat)                                                                                | Simple tool that can be used to measure power usage through the command line. Works by using battery statistics or the Intel RAPL interface. An overview of the commands may be found [here](https://manpages.ubuntu.com/manpages/bionic/man8/powerstat.8.htm). | Linux. A specific overview of versions can be seen on the Github page.                                                                                                                            | Intel                     |
| [AMD μProf](https://www.amd.com/en/developer/uprof.html)                                                                              | Tool that, among others, also contains power profiling. Next to its GUI monitoring, it can also generate csv and html reports.                                                                                                                                  | Supports 64-bit versions of Windows 10 & 11, also of Linux distributions such as Ubuntu 22.04 and later. For a more detailed overview, check [here](https://www.amd.com/en/developer/uprof.html). | AMD                       |
| [EnergiBridge](https://github.com/tdurieux/EnergiBridge)                                                                              | Tool that measures the energy/power usage of commands and exports it in a csv file. Its advantage is that it supports multiple architectures and operating systems.                                                                                             | Linux, Windows, Mac                                                                                                                                                                               | Intel, AMD, Apple ARM CPU |

#### Measuring code directly
| Tool                                                                                                                                  | Description                                                                                                                                                                                                                                                     | **Programming Language**                                                                                                                                                                                           |
| ------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | 
| [CodeCarbon](https://codecarbon.io/) |  Popular tool that measures power consumption, but also carbon intensity, which is useful for AI solutions.                                                                                                                                                                                                             | Python             |
| [pyEnergiBridge](https://github.com/luiscruz/pyEnergiBridge) | Energibridge wrapper that allows for power measurement statistics. | Python | 
|  [JoularJX](https://github.com/joular/joularjx) | a power monitoring tool that working at a source code level. | Java |

## Conducting experiments
An energy profiler estimates the energy usage of your device while running a specific application (or software code). However, different runs of the same application may have different behavior, making it necessary to develop a consistent and reproducible measurement methodology.  

During your measurements, confounding variables related to your system will be at play.  This is because energy profilers do **not** measure the energy consumption of an **application or piece of code**, but the energy consumption of the **system on which you are running those**. For instance, there is a significant difference between running your profiling tool when your laptop is charging compared to when it is not. Experiments will also contain confounding variables related to the system that you are running them on. For instance, running an experiment on a Linux OS vs Windows can lead to different results. To take this into account, ensure that your experiments are run on the same machine.  

To minimize confounding variables:
1. Any unnecessary processes should be terminated. Notifications and background applications should be turned off, internet should be turned off or, if needed for the experiment, one should pick cable over wireless internet.
2. All external hardware should be disconnected. This includes extra displays, USB sticks and so on. 
3. Your system settings should stay the same throughout the entire experiment. For instance, your internet connection should stay the same, your laptop should be constantly charging, and your screen brightness should stay the same.
4. Keep hardware cool. If not possible, you should also measure temperature, so that you have additional data about the power or energy consumption recorded during a certain run.
5. If you are trying to compare different setups to each other, keep in mind that executing the same setup consequently may lead to low-level energy optimizations via, for instance, system caching that you cannot avoid. To decrease the extent to which this happens, it would thus be best to shuffle your setups.  

To ensure that data is representative, you need to run your experiments multiple times. To ensure that different executions do not affect each other, it is important to pause between and old and new execution. 

You may visualize your results through methods such as boxplots. Some data may seem abnormal, for instance if it measures a run that was abruptly cancelled due to factors outside your control, or if you forgot to plug in your laptop charger. Next to visual inspection, there is also the [Shapiro Wilk test](https://en.wikipedia.org/wiki/Shapiro%E2%80%93Wilk_test). This can help you investigate if the measured data has a normal distribution or not.

# Sustainable software development practices
We presented the means to measure the energy consumed by software, but the question remains how do we approach [reducing that footprint as engineers](https://doi.org/10.1016/j.suscom.2011.06.004), developers or designers. For that, we will take a closer look at a software development life cycle, and identify the practices towards reducing environmental impact of software.

In general, in software engineering we can identify the following lifecycle [stages](https://en.wikipedia.org/wiki/Waterfall_model):
- requirements definition,
- design,
- implementation,
- testing,
- deployment and maintenance.

While in modern software engineering this [waterfall model](https://doi.org/10.1145/3337773) is not that relevant anymore, and developers are often working with constantly evolving codebases and requirements, many of its stages, like testing, software implementation are nevertheless very relevant for us to identify possible improvements in the process.

## Requirements
The energy efficiency of a system is a non-functional requirement, we read in the book *Green Software Engineering: Exploring Green Technology for Sustainable IT Solutions* (see further reading). To understand what can be done in terms of minimizing the carbon footprint of a program, we have to understand that it is not a software that consumes energy, but the hardware components that are being utilized. For example, an algorithm can be more efficient in terms of its complexity, allowing for less CPU operations, or a mobile app can compress data before sending it online, allowing the network component to send less data. In both scenarios, an environmental impact can be reduced by using less resources. The latter example also enhances experience for users with limited data plans or slower internet connections.

Regarding the requirements towards energy efficient resources use, we can outline four main requirement types:
- computational efficiency,
- data management - reducing I/O operations to storage,
- data communication - network use,
- energy consumption awareness - provide visibility into energy impact of the software.

The requirements can as well point to the specific implementation decisions, such as concurrent programming, or system design decisions, such as offloading larger calculations to the cloud, or edge computing. 

## Design patterns
Let's recall an example of minimizing network usage. If you visit YouTube website, you can see multiple videos suggested for you. Scrolling through the main page shows the auto-playing snippets of the videos, so that you can preview the content. A designer could identify this as a potentially extensive use of data, as playing these in high-resolution, or downloading the whole video is extremely costly, and potentially very bad for user experience. Because of that, the snippets downloaded to a users device are short fragments of the videos, not containing sound, and being compressed for very efficient data use.

Software design is not only about user experience, but also, what's even more important, [design patterns](https://tudelft.on.worldcat.org/oclc/1449692601). Most of the software infrastructure in our world is currently written in an object oriented manner, and it seems that it will remain that way for a long time, since the refactoring all the codebases in a different manner is not a feasible option at scale. The paradigm is oriented around conventional design patterns, which you may have encountered in your studies already. If not, it is worth knowing that a design pattern is a reusable solution (like a blueprint of classes organization) to a common problem in software design. 

You can learn all about different design patterns from the book "[Design Patterns: Elements of Reusable Object-Oriented Software](https://github.com/GunterMueller/Books-3/blob/master/Design%20Patterns%20Elements%20of%20Reusable%20Object-Oriented%20Software.pdf)", which is famous for introducing most of the widely-used ones. The energy use decrease by reimplementing a piece of code with a specific pattern has been tested in the "[Initial explorations on design pattern energy usage](https://doi.org/10.1109/GREENS.2012.6224257)" paper. The table shows the exact numerical results of this experiment.

![Pasted image 20250528220358](Figures/Pasted%20image%2020250528220358.png)
[Experiment results](https://doi.org/10.1109/GREENS.2012.6224257)

The main outcome of the research is not that the impact of patterns like decorator or abstract factory is necessarily negative, as it is rather infeasible to estimate the energy usage based solely on low-level architecture of the code. The main outcome is that there exist a positive correlation between a number of objects and number of messages passed between objects, and an energy consumption. Below you can find an outline of three patterns which can particularly reduce the number of objects in the system, and may be therefore considered "green".

### Flyweight
[Flyweight pattern](https://en.wikipedia.org/wiki/Flyweight_pattern) aims to reduce memory usage by sharing data between similar object. An example can be a word processor. Instead of keeping an object for each character in the document, containing the font metrics and data, each character can have a reference to a glyph object, shared by all the instances of the character over the text.

![Pasted image 20250528222657](Figures/Pasted%20image%2020250528222657.png)
[Flyweight pattern](https://en.wikipedia.org/wiki/Flyweight_pattern)

### Proxy
[Proxy pattern](https://en.wikipedia.org/wiki/Proxy_pattern) provides a placeholder or substitute for another object to control access or add functionality. It can be used to keep a protection proxy object that restricts access to a subject based on access rights, or to keep a virtual proxy in place of a heavy object, to provide a simple skeleton representation.

![Pasted image 20250528225636](Figures/Pasted%20image%2020250528225636.png)
[Proxy pattern](https://en.wikipedia.org/wiki/Proxy_pattern)

### Observer
[Observer pattern](https://en.wikipedia.org/wiki/Observer_pattern) is a pattern in which an object called a subject, maintains list of objects called observers, and updates them each time a certain event happens. It is an often used pattern for event-driven software, such as web clients, or applications that interact with users through keyboard, mouse, touchscreen, etc. Its advantage is that it does not require tight coupling between the objects, as the subjects can simply notify its observers by calling a certain observer function.

![Pasted image 20250528225206](Figures/Pasted%20image%2020250528225206.png)
[Observer pattern](https://en.wikipedia.org/wiki/Observer_pattern)

## Implementation
Now we will outline a couple of coding paradigms that has proven to optimize code's energy consumption and how to apply them effectively.

### Work Stealing - parallel computing
Work stealing is an approach to constructing multithreaded program runtimes of parallel programming languages. It is a work-balancing scheduler for multi-threaded programs. Here we explain the basic idea of the algorithm, and an optimization called [Hermes](https://dl.acm.org/doi/10.1145/2654822.2541971).

The concept of work stealing is that a program consists of *workers* - threads, executing on a host CPU. Each *worker* maintains a dequeue of tasks. If the queue is empty, the worker *steals* a task from a *victim*.

**Work-first principle** - when spawning a new thread, execute the thread and enqueue the further instructions (put them as the most immediate).

Below an example of work-first principle in Cilk - a C like language designed for parallel programming. In the example, when the program reaches the second like, it places *other statements* from ```f``` onto the dequeue, and carries on the execution of ```f1```.

```c
cilk int f() { 
	int n1 = spawn f1();
	... // other statements
}  

cilk int f1() {
	// other statements
}
```

When a queue of a worker has been emptied, it retrieves a task from the *head* of the other worker's queue, so the least immediate task.

![Pasted image 20250604214217](Figures/Pasted%20image%2020250604214217.png)
[Work stealing illustration](https://dl.acm.org/doi/10.1145/2654822.2541971)

Now, this algorithm can be optimized for better energy use. Hermes introduces the following optimizations, involving control of the working tempo:

1. Workpath-Sensitive Tempo Control - at the beginning of thief-victim relationship, thief's tempo is slowed down (thief procrastination); once the relationship ends because victim has run out of tasks, thief's tempo is again raised up (immediacy relay).

![Pasted image 20250604220544](Figures/Pasted%20image%2020250604220544.png)
[Workpath-Sensitive Tempo Control](https://dl.acm.org/doi/10.1145/2654822.2541971)

2. Workload-Sensitive Tempo Control - worker's tempo is dependent on the number of tasks to be completed.

![Pasted image 20250604220559](Figures/Pasted%20image%2020250604220559.png)
[Workload-Sensitive Tempo Control](https://dl.acm.org/doi/10.1145/2654822.2541971)

### Other coding practices
Here we present other low-level small code changes that usually improve the performance and energy consumption. These are usually implemented on the compiler lever, and thus it is very important to be mindful about how the compiler works, and what are the best practices adviced by the creators of a programming languages.

#### Macros vs Function Calls
The difference between function and macro is, that for macro the code is rewritten before runtime. This causes an increase in the memory use, and in that sense function calls are preferred, also for the sustainability reasons. Keep in mind that for small operations macros can be more efficient in terms of time, and therefore cause less energy use. If you want to learn more about the topic a good exercise would be to compare the code generated for both cases using the energy profilers, also introduced in this module.

Consider the following pieces of pseudocode.

```
macro square(x) = x * x;
return square(3 + 2);
---
function square(x) = x * x;
return square(3 + 2);
```

Here, for the macro, compiler replaces ```square(3 + 2)``` with ```(3 + 2) * (3 + 2)``` before runtime.

#### Loop unrolling
Loop unrolling on the compilation lever allows for increased instruction-level parallelism and reduces the runtime computations of the loop header. Encryption and decryption methods often contain multiple nested loops, often iterating from 0 to 9. Compiler unroll such loop replacing it with 10 instructions.

## Infrastructure / DevOps
Finally, we will talk about the infrastructure, that allows for a sustainable maintenance of the code.

### Tests
While there are different approaches to [testing](https://tudelft.on.worldcat.org/oclc/1310466110), we can safely say that more energy is consumed when there are multiple tests that run the same piece of code with similar inputs. That being said, keep in mind, that more isolated tests will always allow for less executions of the code, and also will allow for potentially running less tests in the pipeline when a module is updated. The conclusion would be, that more modular approach in testing and in designing may be often better for the sustainability of the codebase. What's even more important, redundancy in the tests does not allow you to catch many more errors, so it's best to avoid similar test cases. Try to keep that in mind while designing a test suite, and what's most important, be mindful of the specification of the tools you are using.

What's more, tests can include benchmarking the code to see if it is energy efficient (see [CodeCarbon](https://codecarbon.io)). A lot of code quality assessment tools provide functionality for that, also it may be a good idea to include some efficiency tests in a CI/CD pipeline.

### [Maintenance](https://www.academia.edu/download/49247409/A_Review_on_Software_Maintenance_Issues_and_How_to_Reduce_Maintenance_Efforts.pdf) & DevOps
Once the software is deployed, it must be maintained. Maintenance activities involve correcting faults, improving performance or other attributes, and adapting the product to a modified environment. Maintenance cost time and engineering resources, *example*: when a system is being continuously developed, a CI/CD pipeline is run often to run the tests and deploy the new version in all environments, the pipeline can also be optimized for energy efficiency. Practices to reduce maintenance cost include: Proper documentation Consistency in testing Eliminating dead code and repeated code.

In greenifying the DevOps, you often have to think yourself if the infrastructure you are implementing makes sense from the sustainability point of view. There is no one golden standard for [green DevOps](https://medium.com/@tajinder.singh1985/green-devops-sustainable-way-of-doing-devops-e69429b01933), and in most cases a tailored and carefully selected approach is needed.

# Programming languages and energy efficiency
This submodule focuses on the efficiency of different programming languages and explains why deciding on a single best language is not a simple task. Secondly, it iterates over some system-level improvements introduced by the operating systems and platform developers, explaining how do they work, and how a software engineer can make use of them to ensure development of an efficient and green software.

## Memory Use and Execution Time Impact on the Energy Consumption 
It is commonly assumed that the energy efficiency is proportional to the execution time of a programme. While this is true that there exist a correlation between these two, the fact is that energy-time relation can be expressed by the formula **Energy = Power x Time**, where [power cannot be assumed as a constant](https://dl.acm.org/doi/10.1145/3136014.3136031), and also has an impact on the Energy. Studies suggest that both execution time and energy efficiency may very well vary between languages, platforms and applications. 

### Overview of the Performance of Selected Languages
A [study](https://dl.acm.org/doi/10.1145/3136014.3136031) conducted by Pereira R. et al. offers a benchmark list detailing the energy consumption of a machine when executing standardised solutions to well-known programming problems across various programming languages, providing insight into the energy efficiency of each language. The core findings of the study are summarised in the following table:

![Pasted image 20250722195955](Figures/Pasted%20image%2020250722195955.png)
[Languages performance](https://dl.acm.org/doi/10.1145/3136014.3136031)

The overview suggests the efficiency advantage of the lower-level and compiled languages like C, C++ or Rust over interpretable languages like JavaScript and Python. The main result of the overview is, that a developer concerned only for energy efficiency and execution time of the problem can very well select a language that suits their use case, however if memory is also a concern, it is impossible to decide for a single language automatically.

### Overview of the Performance Improvements for Selected Platforms
To ensure more sustainable use of the device resources, the operating system designers make effort to introduce system-level improvements. These vary depending on the type of a device (personal computer, smartwatch, etc.). Different usage patterns are expected for different devices, for example, on smartphones there are lots of services running in the background, that have to be managed. The OS-level improvements enforce developers to approach the green optimisation of their software differently depending on the platform, and it is absolutely crucial to be mindful of their existence. Here we present the overview of the worked examples of these platform-dependent improvements and energy tools, together with their consequences for developers.

## Task Schedulers

### CPU Power Management Fundamentals
**Processor's Power Consumption**
It may seem like the energy consumed by a processor depends solely on the number of the operations executed, so every operation has a certain cost. This is not exactly the case in computing. Let's look closer at what contributes to the CPU's energy consumption.
In general, power consumption composition can be expressed using the following formula.

$$P_{\text{cpu}} = P_{\text{dyn}} + P_{\text{sc}} + P_{\text{leak}}$$

Where power consumed by a processor is a sum of:
- short-circuit power loss, which happens on a logic gate level is hard to model on a macroscopic level, 
- leakage power, which is a power leakage on a transistor level and is also not the subject of this module, 
- dynamic power, originating from the logic gates activity. 
The value of dynamic power can be expressed further by the following formula.

$$P_{\text{dyn}} = CV^2f$$

Where *C* is switched load capacitance, *V* is voltage and *f* stands for the operation frequency.
From this we can derive the cost of an operation.

$$E_{\text{dyn}} = P_{\text{dyn}}t$$

First we compute time required to complete an operation.

$$t = \frac{1}{f}$$

From that we get

$$E_{\text{dyn}}=CV^2$$

Therefore it still may seem like ultimately the energy does not depend on frequency, however in practice, higher voltage is required with higher frequencies for the CPU to retain its stability. This leads to the [dynamic power](https://en.wikipedia.org/wiki/Dynamic_frequency_scaling) of the CPU increasing nonlinearly with frequency, and energy consumed by a certain number of operations being higher when executed with higher frequency.


**Operating Performance Points**

Complex CPUs today consist of multiple domains of cores, allowing some of them to work on lower voltage and frequency, while others work in higher performance mode. This allows the operating systems to use different parts of the processor for a different parts. Note that not all tasks executed on the processor have to be executed with higher available performance. In fact, this approach would lead to very quick energy drain and very inefficient use of resources.
The use of CPU is implemented differently in the system kernels. In this section we focus on [Operation Performance Points (OPPs)](https://www.kernel.org/doc/Documentation/power/opp.rst) introduced in the Linux Kernel. OPPs are sets of tuples consisting of frequency and voltage, that the device will support per domain.
```
cpu0: cpu@0 {
    compatible = "arm,cortex-a53";
    device_type = "cpu";
    reg = <0x0>;
    capacity-dmips-mhz = <300>;
    clocks = <&cpu0_clk>;
    operating-points-v2 = <&cpu0_opp_table>;
    cpu-idle-states = <&CPU_SLEEP>;
    #cooling-cells = <2>;
    dynamic-power-coefficient = <240>;
    ...
};

cpu0_opp_table: opp-table {
    compatible = "operating-points-v2";

    opp-shared;

    opp00 {
        opp-hz = /bits/ 64 <500000000>;  // 500 MHz
        opp-microvolt = <800000>;
        opp-microwatt = <120>;          // energy estimate
    };
    opp01 {
        opp-hz = /bits/ 64 <1000000000>; // 1 GHz
        opp-microvolt = <850000>;
        opp-microwatt = <200>;
    };
    opp02 {
        opp-hz = /bits/ 64 <1500000000>; // 1.5 GHz
        opp-microvolt = <950000>;
        opp-microwatt = <400>;
    };
};
```
Here we have a snippet of a device tree, presenting OPPs for *cpu0*. If we look closely at OPP table, we see three performance points: (500MHz @ 800000μV), (1GHz @ 850000μV), (1.5GHz @ 950000μV). Here you may notice that *opp01* seem to be more efficient than slower *opp00*. However, *opp00* is still needed by the Kernel, in order to avoid generating extensive amounts of heat for the tasks that can be run on slower frequency.

### Consequences For Task Scheduling
As you saw in the previous sections, a computer can save a substantial amount of energy by assigning less urgent tasks to slower, but more efficient core clusters. This is a responsibility of system's [task scheduler](https://www-sciencedirect-com.tudelft.idm.oclc.org/science/article/pii/S1877750313000173#sec0050).

### Example of Task Schedulers and Related Tools

#### Linux Kernel - Energy Aware Scheduler
In an attempt to optimize battery use by the background tasks the developers of the Linux Kernel introduced so called [*Energy Aware Scheduler*](https://www.kernel.org/doc/html/latest/scheduler/sched-energy.html) (EAS). It is designed for heterogenous CPU topologies, such as Arm big.LITTLE, where the potential for saving energy is the highest. In traditional task scheduling, the scheduler does not take the efficiency aspects of a core into account. EAS decides which core should run a certain task using the least energy.

The scheduler addresses an important CPU issue, that the power consumption of a processor grows nearly-exponentially with frequency growth. Scheduling less relevant jobs on lower-frequency cores can therefore reduce the overall energy consumed while running a certain job.
Energy Aware Scheduler introduces to major concepts to the Kernel:

**Energy Models** - describes how much energy each CPU or cluster consumes at different loads and frequencies (see Operating Performance Points).

**Energy cost calculation** - for every task, the kernel estimates the cost of placing each task on each CPU.

#### Windows - EcoQoS
[*Windows EcoQoS*](https://devblogs.microsoft.com/performance-diagnostics/introducing-ecoqos/) is a Quality of Service level that developers can utilize to ensure more energy efficiency in the Windows apps. It uses the same property as described above, that the power consumption of a processor grows nearly-exponentially with frequency growth. The developers can decide to identify certain workloads as workloads that do not require a significant performance or latency. This is done with using Windows API. A potential example of a situation in which the QoS level may ensure more efficiency without compromising the performance is periodical removal of cache files by an application. An example of a situation in which the functionality should not be used is application in UI threads, which need to be running with higher priority. In terms of performance, Microsoft claims that initial tests resulted in up to 90% reduction in CPU power consumption.

### Other Tools for Energy Optimization Examples
#### Windows - E3
[E3 - *Windows Energy Estimation Engine*](https://devblogs.microsoft.com/sustainable-software/measuring-your-application-power-and-carbon-impact-part-1/) provides a heuristic estimate of energy use per process, tracking CPU usage, GPU usage, disk IO and network use. A tool for developers allows them to include carbon efficiency in their tests pipeline. E3 produces an energy report listing background processes with high energy cost, devices preventing sleep and a number of other insightful entries.

#### Android - Doze and App Standby
Android introduces [*Doze* and *App Standby*](https://developer.android.com/training/monitoring-device-state/doze-standby) modes to limits applications work in the background and safe energy. Doze prevents access to energy intensive resources outside of a regular time window, causing background threads to be potentially delayed. The best developer practice in line with these optimizations is to use Job Scheduler instead of background threads, letting the system optimize the energy use.

#### Android - Battery Historian and Batterystats
Another example of a feature introduced by the platform developers are [*Battery Historian* and *Batterystats*](https://developer.android.com/topic/performance/power/setup-battery-historian). The first is a development tool that serves as an energy profiler for Android, the latter is a tool included in Android framework that collects battery data on a device. The data collected by the stats can be analyzed with battery historian. That is a crucial and strongly advised testing stage in mobile development.

#### MacOS / iOS - Low Power Mode
The systems introduce a global [*Low Power Mode*](https://developer.apple.com/documentation/foundation/processinfo/islowpowermodeenabled) that reduces CPU frequency, animations and background activities. Users who wish to prolong the device's battery life can enable the mode in the system settings. The developers can make the application access the information whether the mode is enabled with an API call, or register to receive a notification when the mode changes. Apple encourages the app developers to make use of the mode status information to adjust the application's behavior to the users preference, and include more battery-oriented solutions in case the mode is activated. 

#### Dynamic Compilation, JAVA Virtual Machine Example 
[Dynamic compilation](https://en.wikipedia.org/wiki/Dynamic_compilation) is a compilation approach that is essentially used to gain performance during the program execution, by allowing the efficiency optimizations not available to statically-compiled languages implementations. First introduced in an implementation of the [Smalltalk](https://dl.acm.org/doi/10.1145/800017.800542) language.

Under normal conditions, static, ahead-of-time (AOT) compilation, most of the code compilation happens before executing. A native executable is created based on the source code file. Modern Java VMs however use different approach, [Just-In-Time (JIT) compilation](https://cr.openjdk.org/~vlivanov/talks/2015_JIT_Overview.pdf). In that case, most compilation happens during the execution. Source code is compiled into a bytecode, which is later interpreted with use of JIT.

Profiling - gathers information about the code during execution. The information include information about types, constants, as well as branches and calls statistics. Then profiling uses the data to make educated guesses.

The consequences of JIT are the following:
- **\[JIT\]** It's hard to guess the applications actual behavior and this can only be archived in JIT.
- **\[JIT\]** In a multi-platform application it's impossible to utilize specific platform features in AOT compilation.
- **\[AOT\]** JIT makes use of extensive profiling data, while the resources are limited.
- **\[AOT\]** The application is expected to be slower on the startup, due to the optimizations being still applied.

The consequences of JIT can be both beneficial and undesirable, making static compilation still very relevant and desired in certain cases.

# Feedback
We are happy to receive any feedback you may have on this lecture. Is there too much information in the slides/notes, or would you like to know more about a certain topic? Please let us know by [**filling in this form**](https://forms.cloud.microsoft/e/FEML1A22Pb).

# Further reading
Want to know more about the topics in this lecture? Here are some sources that didn't quite make the cut.

## Books about sustainable software engineering
- [Green Software Engineering: Exploring Green Technology for Sustainable IT Solutions](https://app.knovel.com/hotlink/toc/id:kpGSEEGTS3/green-software-engineering/green-software-engineering): a book that covers the topic of sustainable software engineering in a comprehensive manner. It discusses the environmental impact of software, how to measure it and how to reduce it. It also contains chapters on green software design patterns and green software development practices.

## Sustainable software engineering organizations
- [The Green Software Foundation](https://directory.greensoftware.foundation/projects/): a non-profit formed under the Linux Foundation. They have working groups related to hardware standards, software standards and policy making. They are also actively working on building a database of research and tools that can contribute to building more environmentally friendly applications.
- [The Sustainable Web Interest Group](https://www.w3.org/groups/ig/sustainableweb/tools/): an international group preoccupied with improving digital sustainability. They do so by developing the [Web Sustainability Guidelines (WSG)](https://w3c.github.io/sustainableweb-wsg/), which are constantly evolving.
- [ClimateAction.tech](https://climateaction.tech/): an international volunteer-led community of tech workers with four different focus areas, out of which one is green software engineering. They organize events centered around learning, networking, advocacy and also provide small grants for initiatives that aligns with their overall mission.
- [Netherlands Knowledge Network Green Software](https://www.rvo.nl/onderwerpen/kennisnetwerken-industrie-en-chemie/knowledge-network-green-software): a now-defunct knowledge network focused on sharing knowledge and raising awareness about the potential of green software. Nonetheless, a list of events or initiatives that they helped start can be found [here](http://kngs.wikidot.com/).

## Energy profilers
- **[PowerJoular and JoularJX: Multi-Platform Software Power Monitoring Tools](https://hal.archives-ouvertes.fr/hal-03608223v1)**. Adel Noureddine. In the 18th International Conference on Intelligent Environments (IE2022). Biarritz, France, 2022. This paper details the development of two software power monitoring tools. Most interestingly, JoularJX is a **source-code** power monitoring tool that runs based on PowerJoular.
- Erik Jagroep, Jan Martijn E. M. van der Werf, Slinger Jansen, Miguel Ferreira, and Joost Visser. 2015. Profiling energy profilers. In Proceedings of the 30th Annual ACM Symposium on Applied Computing (SAC '15). Association for Computing Machinery, New York, NY, USA, 2198–2203. https://doi.org/10.1145/2695664.2695825 This 2015 paper offers an overview of (software) energy profilers that were available at the time. Nonetheless, it highlights the challenges that are associated with using these tools.

## Design patterns for sustainable software
- [Green Software Patterns](https://patterns.greensoftware.foundation/): a course developed by the *Green Software Foundation*. Contains a catalog of good practices for AI, Cloud and Web engineers.
- [Green Software Practitioner](https://learn.greensoftware.foundation/): a course developed by the *Green Software Foundation*. It offers an introduction to the topic of green software for all types of software practitioners. It is relevant for designing, maintaining and running green applications regardless of the platform.
- [The Principles of Sustainable Software Engineering](https://learn.microsoft.com/en-us/training/modules/sustainable-software-engineering-overview/): a course covering fundamental concepts related to SSE. Developed by *Microsoft* for the company's learning platform.
- [CS4575 Sustainable Software Engineering](https://luiscruz.github.io/course_sustainableSE/2025/): a master elective has a lecture on [measurement metrics]() and on [how to conduct energy measurement experiments](https://surfdrive.surf.nl/files/index.php/s/V8f66pd7V7sQYx6)
