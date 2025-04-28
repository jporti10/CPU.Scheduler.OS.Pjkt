//*** JavaFX UI components ***//
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

//*** Import for file operations (csv import) ***//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;



public class CPUSchedulerApp extends Application
{
    private Stage popoutStage;
    private TableView<Process> table;
    private ObservableList<Process> data;
    private TextArea outputArea;
    private Canvas ganttCanvas;
    private ScheduleResult lastResult;

    //*** Entry point: launches the JavaFX application ***//
    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage)
    {
        //*** Set the window title ***//
        primaryStage.setTitle("CPU Scheduling Simulator - SRTF & HRRN");

        //*** Initialize the data list and TableView ***//
        data = FXCollections.observableArrayList();
        table = new TableView<>(data);
        table.setPrefHeight(200);
        setupTableColumns();  //*** Configure table columns ***//
        enableRowRemoval();   //*** Allow rows to be removed on double-click ***//

        //*** Create the input controls (ID, Arrival, Burst, buttons) ***//
        HBox inputBox = createInputBox(primaryStage);

        //*** Buttons to run the scheduling algorithms ***//
        HBox actionBox = gethBox();

        //*** Text area for displaying output (log, averages and throughput) ***//
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(200);

        //*** Canvas for overview Gantt chart ***//
        ganttCanvas = new Canvas(634, 150);
        drawEmptyGantt(ganttCanvas);  //*** Draw placeholder ***//

        //*** Layout all components in a vertical box ***//
        VBox root = new VBox(10, table, inputBox, actionBox, outputArea, new Label("Gantt Chart Overview:"), ganttCanvas);
        root.setPadding(new Insets(10));

        //*** Set window size ***//
        primaryStage.setScene(new Scene(root, 654, 750));
        primaryStage.show();

        //*** Ensures full Gantt window closes when main window closes ***//
        primaryStage.setOnCloseRequest(e ->
        {
            if (popoutStage != null && popoutStage.isShowing())
            {
                popoutStage.close();
            }
        });
    }

    private HBox gethBox()
    {
        Button srtfBtn = new Button("Run SRTF");
        srtfBtn.setOnAction(e -> runSRTF());  //*** Schedule using SRTF ***//

        Button hrrnBtn = new Button("Run HRRN");
        hrrnBtn.setOnAction(e -> runHRRN()); //*** Schedule using HRRN ***//

        Button popoutBtn = new Button("Show Full Gantt");
        popoutBtn.setOnAction(e -> showFullGanttWindow()); //*** Show detailed Gantt ***//

        HBox actionBox = new HBox(10, srtfBtn, hrrnBtn, popoutBtn);
        actionBox.setPadding(new Insets(10));

        return actionBox;
    }

    //*** Config three columns (ID, Arrival, Burst) in the Table ***//
    private void setupTableColumns()
    {
        TableColumn<Process, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Process, Integer> arrivalCol = new TableColumn<>("Arrival");
        arrivalCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));

        TableColumn<Process, Integer> burstCol = new TableColumn<>("Burst");
        burstCol.setCellValueFactory(new PropertyValueFactory<>("burstTime"));

        table.getColumns().setAll(idCol, arrivalCol, burstCol);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        idCol.prefWidthProperty().bind(table.widthProperty().multiply(1.0 / 3));
        arrivalCol.prefWidthProperty().bind(table.widthProperty().multiply(1.0 / 3));
        burstCol.prefWidthProperty().bind(table.widthProperty().multiply(1.0 / 3));
    }

    //*** Allow removal of processes by double-clicking a table row ***//
    private void enableRowRemoval()
    {
        table.setRowFactory(e ->
        {
            TableRow<Process> row = new TableRow<>();
            row.setOnMouseClicked(ev ->
            {
                if (ev.getClickCount() == 2 && !row.isEmpty())
                {
                    data.remove(row.getItem());  //*** Bye-bye ***//
                }
            });

            return row;
        });
    }

    //*** Create input fields and buttons for adding/importing/clearing processes ***//
    private HBox createInputBox(Stage parent)
    {
        TextField idField = new TextField(); idField.setPromptText("ID");
        TextField arrivalField = new TextField(); arrivalField.setPromptText("Arrival");
        TextField burstField = new TextField(); burstField.setPromptText("Burst");

        //*** Add button to insert a new process from input fields ***//
        Button addBtn = getButton(idField, arrivalField, burstField);

        //*** Import button to load processes from a CSV file ***//
        Button importBtn = new Button("Import CSV");
        importBtn.setOnAction(e -> importCSV(parent));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        //*** Clear button with refresh icon to reset data and UI ***//
        Button clearBtn = new Button();
        clearBtn.setGraphic(new Label("\u21BB")); //*** Unicode refresh symbol ***//
        clearBtn.setTooltip(new Tooltip("Clear all data!"));

        //*** Resets all processes, logs, and gantt chart ***//
        clearBtn.setOnAction(e ->
        {
            data.clear();
            outputArea.clear();
            drawEmptyGantt(ganttCanvas);
        });

        //*** Input box layout ***//
        HBox box = new HBox(5, idField, arrivalField, burstField, addBtn, importBtn, spacer, clearBtn);
        box.setPadding(new Insets(10));
        box.setAlignment(Pos.CENTER_LEFT);

        return box;
    }

    //*** Where the magic happens of the add and import button ***//
    private Button getButton(TextField idField, TextField arrivalField, TextField burstField)
    {
        Button addBtn = new Button("Add");
        addBtn.setOnAction(e ->
        {
            try
            {
                int id = Integer.parseInt(idField.getText());
                int arrival = Integer.parseInt(arrivalField.getText());
                int burst = Integer.parseInt(burstField.getText());
                data.add(new Process(id, arrival, burst));
                idField.clear(); arrivalField.clear(); burstField.clear();
            }

            catch (NumberFormatException ex)
            {
                //*** Ignore invalid numeric input ***//
            }
        });

        return addBtn;
    }

    //*** Import processes from a CSV file with at least three columns (ID, Arrival, Burst) ***//
    private void importCSV(Stage parent)
    {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showOpenDialog(parent);

        if (file == null) return; //*** User cancels selection ***//

        int count = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file)))
        {
            String line;

            while ((line = br.readLine()) != null)
            {
                String[] cols = line.split(",");  //*** Split CSV line ***//

                if (cols.length < 3) continue;     //*** Skip incomplete rows ***//

                try
                {
                    int id = Integer.parseInt(cols[0].trim());
                    int arrival = Integer.parseInt(cols[1].trim());
                    int burst = Integer.parseInt(cols[2].trim());
                    data.add(new Process(id, arrival, burst));
                    count++;
                }

                catch (NumberFormatException ex)
                {
                    //*** Skip rows with invalid numbers ***//
                }
            }
        }

        catch (IOException ex)
        {
            ex.printStackTrace();  //*** Log file read errors ***//
        }

        //*** Show warning if no valid entries were imported ***//
        if (count == 0)
        {
            Alert alert = new Alert(AlertType.WARNING, "No valid entries found in CSV.", ButtonType.OK);
            alert.setHeaderText("Import Warning");
            alert.showAndWait();
        }
    }

    //*** Run the Shortest Remaining Time First scheduling algorithm ***//
    private void runSRTF()
    {
        List<Process> procs = deepCopy(data);           //*** Copy input processes ***//
        lastResult = SRTFScheduler.schedule(procs);     //*** Execute SRTF ***//
        displayResult(lastResult);                      //*** Display output ***//
    }

    //*** Run the Highest Response Ratio Next scheduling algorithm ***//
    private void runHRRN()
    {
        List<Process> procs = deepCopy(data);           //*** Copy input processes ***//
        lastResult = HRRNScheduler.schedule(procs);     //*** Execute HRRN ***//
        displayResult(lastResult);                      //*** Display output ***//
    }

    //*** Create a copy of the list of processes ***//
    private List<Process> deepCopy(List<Process> orig)
    {
        List<Process> copy = new ArrayList<>();

        for (Process p : orig)
        {
            copy.add(new Process(p.getId(),
                    p.getArrivalTime(),
                    p.getBurstTime()));
        }

        return copy;
    }

    //*** Show a separate window with the full Gantt chart scaled by time units ***//
    private void showFullGanttWindow()
    {
        if (lastResult == null) return;

        popoutStage = new Stage();
        popoutStage.setTitle("Full Gantt Chart");

        int canvasWidth = Math.max(800, lastResult.totalTime * 20);
        int segmentCount = lastResult.segments.size();
        int canvasHeight = Math.max(300, segmentCount * 40 + 20);

        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        drawGantt(canvas, lastResult.segments, lastResult.totalTime);

        //*** Use ScrollPane for horizontal and vertical scrolling ***//
        ScrollPane scroll = new ScrollPane(canvas);
        scroll.setPannable(true);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);

        scroll.setPrefViewportWidth(800);
        scroll.setPrefViewportHeight(Math.min(canvasHeight, 600));

        popoutStage.setScene(new Scene(scroll));
        popoutStage.show();
    }

    //*** Display scheduling logs and draw the overview Gantt chart ***//
    private void displayResult(ScheduleResult res)
    {
        outputArea.setText(res.log + String.format("\nAvg WT=%.2f \nAvg TAT=%.2f\nCPU Util=%.2f%% \nThroughput=%.2f proc/unit", res.avgWT, res.avgTAT, res.cpuUtil * 100, res.throughput));

        drawGantt(ganttCanvas, res.segments, res.totalTime);
    }

    //*** Draw Gantt chart segments onto the given Canvas ***//
    private void drawGantt(Canvas canvas, List<ExecutionSegment> segs, int totalTime) //*** hehehe, segs ***//
    {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (totalTime == 0)
        {
            drawEmptyGantt(canvas); //*** Nothing scheduled ***//

            return;
        }

        double cellWidth = canvas.getWidth() / (double) totalTime;
        double y = 20, h = 30;

        //*** Draw each execution segment as a colored rectangle ***//
        for (ExecutionSegment s : segs)
        {
            double x = s.start * cellWidth;
            double w = s.duration * cellWidth;

            gc.setFill(Color.LIGHTBLUE);
            gc.fillRect(x, y, w, h);
            gc.setStroke(Color.BLACK);
            gc.strokeRect(x, y, w, h);
            gc.setFill(Color.BLACK);
            gc.fillText("P" + s.id, x + 5, y + h / 2);

            y += h + 10; //*** Move to next row ***//
        }
    }

    //*** Placeholder message when no Gantt chart is shown ***//
    private void drawEmptyGantt(Canvas canvas)
    {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(Color.BLACK);
        gc.fillText("Gantt chart will appear here after running a schedule.\n\nMeanwhile, subscribe to @KuroeZucredl", 10, 20);
    }








    //***** Core classes/components and scheduler implementations below *****//

    //*** Represents a process with id, arrival time, burst time, and runtime ***//
    public static class Process
    {
        private final int id, arrivalTime, burstTime;
        private int remainingTime, completionTime;

        public Process(int id, int arrival, int burst)
        {
            this.id = id;
            this.arrivalTime = arrival;
            this.burstTime = burst;
            this.remainingTime = burst;
        }

        public int getId() { return id; }
        public int getArrivalTime() { return arrivalTime; }
        public int getBurstTime() { return burstTime; }
    }

    //*** Contiguous execution segment for a process in the Gantt chart ***//
    public static class ExecutionSegment
    {
        int id, start, duration;

        public ExecutionSegment(int id, int start, int duration)
        {
            this.id = id;
            this.start = start;
            this.duration = duration;
        }
    }

    //*** Holds results for a scheduling run: segments, log, and performance metrics ***//
    public static class ScheduleResult
    {
        List<ExecutionSegment> segments;
        String log;
        double avgWT, avgTAT, cpuUtil, throughput;
        int totalTime;

        public ScheduleResult(List<ExecutionSegment> segments, String log, double avgWT, double avgTAT, double cpuUtil, double throughput, int totalTime)
        {
            this.segments = segments;
            this.log = log;
            this.avgWT = avgWT;
            this.avgTAT = avgTAT;
            this.cpuUtil = cpuUtil;
            this.throughput = throughput;
            this.totalTime = totalTime;
        }
    }

    //*** Implements Shortest Remaining Time First ***//
    public static class SRTFScheduler
    {
        public static ScheduleResult schedule(List<Process> jobs)
        {
            jobs.removeIf(p -> p.getBurstTime() <= 0); //*** Remove zero-length jobs ***//
            int n = jobs.size();

            if (n == 0)
            {
                return new ScheduleResult(Collections.emptyList(), "--- SRTF Scheduling ---\n(no jobs)\n", 0, 0, 0, 0, 0);
            }

            //*** Sort by arrival time ***//
            jobs.sort(Comparator.comparingInt(Process::getArrivalTime));

            int time = 0, completed = 0, busy = 0, nextArrivalIdx = 0;
            double totalWT = 0, totalTAT = 0;

            List<ExecutionSegment> segs = new ArrayList<>();
            StringBuilder log = new StringBuilder("--- SRTF Scheduling ---\n");

            int lastPid = -1, lastRem = 0;

            //*** Loop runs until all processes complete ***//
            while (completed < n)
            {
                boolean anyReady = false;

                for (Process p : jobs)
                {
                    if (p.getArrivalTime() <= time && p.remainingTime > 0)
                    {
                        anyReady = true; break;
                    }
                }

                if (!anyReady && nextArrivalIdx < n)
                {
                    time = Math.max(time, jobs.get(nextArrivalIdx).getArrivalTime());

                    while (nextArrivalIdx < n && jobs.get(nextArrivalIdx).getArrivalTime() <= time)
                    {
                        nextArrivalIdx++;
                    }

                    continue;
                }

                Process cur = null;
                int minRem = Integer.MAX_VALUE;

                //*** Pick job with the smallest remaining time ***//
                for (Process p : jobs)
                {
                    if (p.getArrivalTime() <= time && p.remainingTime > 0)
                    {
                        if (p.remainingTime < minRem)
                        {
                            minRem = p.remainingTime;
                            cur = p;
                        }

                        else if (p.remainingTime == minRem && cur != null && p.getId() < cur.getId())
                        {
                            //*** Tie-break via whichever has the smaller ID value ***//
                            cur = p;
                        }
                    }
                }

                if (cur == null) { time++; continue; } //*** Idle CPU ***//

                //*** Detect start, interrupt, and resume events ***//
                boolean isStart = cur.remainingTime == cur.getBurstTime();
                boolean isInterrupted = lastPid != -1 && cur.getId() != lastPid && lastRem > 0;
                boolean isResuming = !isStart && !isInterrupted && cur.getId() != lastPid;

                cur.remainingTime--; //*** Execute one unit ***//
                busy++;
                boolean wasEnd = cur.remainingTime == 0;

                //*** Log builder ***//
                log.append(String.format("t=%d -> P%d", time, cur.getId()));

                if (isStart)       log.append(" (start)");
                if (isInterrupted) log.append(String.format(" (after P%d interrupted)", lastPid));
                if (isResuming)    log.append(" (resuming)");
                if (wasEnd)        log.append(" (end)");

                log.append("\n");

                //*** Record execution segment for Gantt chart ***//
                if (segs.isEmpty() || segs.getLast().id != cur.getId())
                {
                    segs.add(new ExecutionSegment(cur.getId(), time, 1));
                }

                else
                {
                    segs.getLast().duration++;
                }

                time++;

                if (wasEnd)
                {
                    completed++;
                    cur.completionTime = time;
                    int tat = cur.completionTime - cur.getArrivalTime();
                    int wt  = tat - cur.getBurstTime();
                    totalTAT += tat;
                    totalWT  += wt;
                }

                //*** Update last executed process info ***//
                lastPid = cur.getId();
                lastRem = cur.remainingTime;
            }

            //*** Calculate metrics ***//
            double avgWT      = totalWT / n;
            double avgTAT     = totalTAT / n;
            double cpuUtil    = (double) busy / time;
            double throughput = (double) n / time;

            return new ScheduleResult(segs, log.toString(), avgWT, avgTAT, cpuUtil, throughput, time);
        }
    }

    //*** Implements Highest Response Ratio Next ***//
    public static class HRRNScheduler
    {
        public static ScheduleResult schedule(List<Process> jobs)
        {
            jobs.removeIf(p -> p.getBurstTime() <= 0); //*** Remove zero-length jobs ***//
            int n = jobs.size();

            if (n == 0)
            {
                return new ScheduleResult(Collections.emptyList(), "--- HRRN Scheduling ---\n(no jobs)\n", 0, 0, 0, 0, 0);
            }
            //*** Sort by arrival time ***//
            jobs.sort(Comparator.comparingInt(Process::getArrivalTime));

            int time = 0, completed = 0, busy = 0, nextArrivalIdx = 0;
            double totalWT = 0, totalTAT = 0;
            List<ExecutionSegment> segs = new ArrayList<>();
            StringBuilder log = new StringBuilder("--- HRRN Scheduling ---\n");

            boolean[] done = new boolean[n]; //*** Track completed jobs ***//

            //*** Main scheduling loop ***//
            while (completed < n)
            {
                boolean anyReady = false;

                for (int i = 0; i < n; i++)
                {
                    if (!done[i] && jobs.get(i).getArrivalTime() <= time)
                    {
                        anyReady = true; break;
                    }
                }

                if (!anyReady && nextArrivalIdx < n)
                {
                    time = Math.max(time, jobs.get(nextArrivalIdx).getArrivalTime());

                    while (nextArrivalIdx < n && jobs.get(nextArrivalIdx).getArrivalTime() <= time)
                    {
                        nextArrivalIdx++;
                    }

                    continue;
                }

                Process next = null;
                double maxRatio = -1;

                //*** Response ratio for each ready job ***//
                for (int i = 0; i < n; i++)
                {
                    if (done[i]) continue;

                    Process p = jobs.get(i);

                    if (p.getArrivalTime() >	time) continue;

                    int wt    = time - p.getArrivalTime();
                    int burst = p.getBurstTime();
                    double ratio = (wt + burst) / (double) burst;

                    if (ratio > maxRatio)
                    {
                        maxRatio = ratio; next = p;
                    }

                    else if (Math.abs(ratio - maxRatio) < 1e-6 && next != null && p.getId() < next.getId())
                    {
                        //*** Tie-break via whichever has the smaller ID value ***//
                        next = p;
                    }
                }

                if (next == null) { time++; continue; }

                //*** Log start/end both at same time for non-preemptive HRRN ***//
                log.append(String.format("t=%d -> P%d (burst=%d) (start,end)\n", time, next.getId(), next.getBurstTime()));

                //*** Record segment for entire burst ***//
                segs.add(new ExecutionSegment(next.getId(), time, next.getBurstTime()));
                time   += next.getBurstTime();
                busy   += next.getBurstTime();

                //*** Compute metrics, Avg WT, Avg TAT ***//
                next.completionTime = time;
                int tat = next.completionTime - next.getArrivalTime();
                int wt  = tat - next.getBurstTime();
                totalTAT += tat;
                totalWT  += wt;

                done[jobs.indexOf(next)] = true; //*** Mark job done ***//
                completed++;
            }

            //*** Display metrics ***//
            double avgWT = totalWT / n;
            double avgTAT = totalTAT / n;
            double cpuUtil = (double) busy / time;
            double throughput = (double) n / time;

            return new ScheduleResult(segs, log.toString(), avgWT, avgTAT, cpuUtil, throughput, time);
        }
    }
}

