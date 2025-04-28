# CPU Scheduling Simulator (SRTF & HRRN)
For KSU OS-3502

An interactive JavaFX application that demonstrates two CPU‑scheduling strategies—**Shortest Remaining Time First (SRTF)** and **Highest Response Ratio Next (HRRN)**—through real‑time visual feedback. The tool lets you load or create process sets, run the chosen algorithm, and inspect a generated Gantt chart alongside performance metrics.

---

## ✨ Key Features

| Feature | Description |
|---------|-------------|
| **Dual‑algorithm support** | Switch between pre‑emptive SRTF and non‑pre‑emptive HRRN at the press of a button. |
| **Live Gantt chart** | Quick‑view canvas embedded in the main window plus a detachable, scroll‑able pop‑out for full‑length timelines. |
| **CSV import** | Load any .csv files **`id, arrival, burst`** columns. Invalid or incomplete rows are automatically skipped. |
| **Interactive table** | Add or remove processes easily; double‑click a row to delete. |
| **Performance log** | View detailed start / interrupt / resume / end events, average waiting time, average turnaround time, CPU utilisation, and throughput. |
| **One‑click reset** | A refresh‑icon button clears the table, log, and charts without needing to restarting the application. |

---

## 🛠 Requirements

| Dependency        | Minimum Version     |
|-------------------|---------------------|
| **Azul Zulu JDK** | **23.0.1 or newer** |
| **JavaFX SDK**    | 21                  |

> **Why Azul Zulu?**  Since Oracle has discontinued the use of JavaFX in later releases I have relied upon this build from Azul to make this application a reality.

---

## 🚀 Getting Started

### 1 · Clone or download the source

```bash
git clone https://github.com/jporti10/CPU.Scheduler.OS.Pjkt.git
cd CPU.Scheduler.OS.Pjkt
```

```.jar
From releases
```

### 2 · Compile & run

```IDE or .jar file
Download the Azul Zulu JDK 23.0.1 or newer.

https://www.azul.com/downloads/?version=java-23&show-old-builds=true#zulu

Open the Jar or project in an IDE with the correct JDK selected to run.
```

---

## 📂 .CSV File Format

A generic dataset looks like:

```csv
id,arrival,burst (excluding this line right here, just numbers)
1,0,7
2,2,4
3,4,1
```

*   **id** – identifying the process.
*   **arrival** – arrival time unit.
*   **burst** – CPU burst length.

Rows with fewer than three numeric columns are ignored; malformed numbers are skipped as well.

---

## ⚠ Known Limitations

* HRRN is non‑pre‑emptive; therefore each process appears as a single contiguous bar when that algorithm is selected.
* The GUI currently supports exactly three columns (ID, Arrival, Burst). Priority or I/O burst modelling has not been implemented.
* JavaFX runtime must be present at execution time. You cannot run the application with a standard Java SE runtime environment.

## 📄 License

This project is released under the MIT License. See License for details.
