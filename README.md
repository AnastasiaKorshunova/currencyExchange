# Currency Exchange  
**Multithreaded Java Educational Project**

---
## Project Overview

This project is a **multithreaded Java application** that simulates a **currency exchange**.  
Participants trade **USD** and **EUR** against **MNT (Mongolian Tugrik)**.

The project was created as a **study assignment** to demonstrate:

- work with shared resources  
- thread safety  
- usage of `java.util.concurrent`  
- application of the **State** design pattern  
- correct multithreaded architecture  

---

## Task Description (Short)

- Several participants trade currencies on a shared exchange  
- Each participant works in its **own thread**  
- Exchange rates change after completed transactions  
- A participant can execute **multiple transactions at the same time**  
- A transaction can be **rejected** (e.g. insufficient funds)  
- The system periodically prints a **balance table** for all participants  

---

## Technologies Used

- **Java 21**
- **java.util.concurrent**
  - `ExecutorService`
  - `Callable / Future`
  - `AtomicInteger / AtomicReference`
  - `ConcurrentHashMap`
- **java.util.concurrent.locks**
  - `ReadWriteLock`
  - `ReentrantReadWriteLock`
- **Log4J2** for logging  

### Restrictions
-  `synchronized`  
-  `volatile`  
-  `BlockingQueue`  

---

## Multithreading Model

- Each participant is executed as a **Callable**
- All participants are managed by an **ExecutorService**
- Exchange rates are protected by **ReadWriteLock**
- Balances use **ReentrantReadWriteLock**
- Counters and states use **Atomic** classes
- Reporting runs in a **separate background thread**

---

## Data Initialization

Participants are loaded from a file:

**`data/participants.txt`**

All input data is assumed to be **correct**, as required by the task.

---

## Logging

- All important events are logged:
  - successful transactions
  - rejected transactions
  - state changes
  - errors
- Logging is implemented using **Log4J2**
- Configuration file: `src/resources/log4j2.xml`
- Logs are saved to the **logs/** directory

---

## Exchange (Singleton)

The `Exchange` class:

- stores exchange rates  
- executes transactions  
- manages shared resources  

Implemented as a **thread-safe Singleton** using an **initialization-on-demand
holder** (Enum Singleton is **not used**, as required)