package GUI;

import AIS.Antigen;
import AIS.Antibody;
import AIS.Controller;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

public class GUI2 extends BorderPane {

    public final static HashMap<String, Integer> dataSetLabelIndexes = new HashMap<>();
    static {
        dataSetLabelIndexes.put("FAKENEWSNET", 0);
        dataSetLabelIndexes.put("FAKEDDIT", 0);
    }
    private final ChoiceBox<String> dataSetBox = new ChoiceBox<>(FXCollections.observableArrayList("FAKENEWSNET", "FAKEDDIT"));
    private int labelIndex;

    private final Stage primaryStage;
    private final Controller controller;
    //private StatisticGraph statisticGraph;
    //public ArrayList<StatisticGraph> graphs = new ArrayList<>();
    //private SolutionGraph solutionGraph;
    public final Button startButton = new Button("Start");
    public final Button stopButton = new Button("Stop");

    private final TextField inputIterations = new TextField("1000");
    private final TextField inputPopulationSize = new TextField("1000");
    private final TextField inputDataSetSplit = new TextField("0.1");
    private final TextField inputValidationSplit = new TextField("0.3");
    public final TextField iterationTextField = new TextField();
    private final TextField inputK = new TextField("0");
    private final TextField radiusMultiplier = new TextField("0.0");
    private final TextField pca = new TextField("0");

    private CheckBox radiusCheckBox = new CheckBox("Plot radius");
    private CheckBox plotSolutionCheckBox = new CheckBox("Plot solution");
    private CheckBox globalSharingFactorCheckBox = new CheckBox("Global Sharing Factor");
    private CheckBox masterValidationCheckBox = new CheckBox("Master validation");

    private final FlowPane graphPane = new FlowPane();
    private final ScrollPane scrollPane = new ScrollPane();

    private final int sceneWidth = 700;
    private final int sceneHeight = 500;
    private final int solutionGraphWidth = 200;
    private final int solutionGraphHeight = 200;
    private final int statisticGraphWidth = 250;
    private final int statisticGraphHeight = 250;

    private HBox menu;
    private VBox menuWrapper;
    public ChoiceBox<String> setBox = new ChoiceBox<>();

    public GUI2(Controller controller){

        super();
        this.primaryStage = new Stage();
        this.controller = controller;
        final Scene scene = new Scene(this, sceneWidth, sceneHeight);
        primaryStage.setScene(scene);
        primaryStage.setTitle("AIS");

        //make sure when selecting the data sets, the index of the label on each row is also set
        dataSetBox.getSelectionModel().selectedIndexProperty().addListener((observableValue, number, number2) -> {
            labelIndex = dataSetLabelIndexes.getOrDefault(dataSetBox.getItems().get((int) number2), 0);
        });

        iterationTextField.setPrefWidth(40);
        menuWrapper = new VBox(5);
        menuWrapper.setPadding(new Insets(5,0,5,0));
        dataSetBox.setValue("FAKENEWSNET");
        dataSetBox.setPrefWidth(150);
        plotSolutionCheckBox.setSelected(true);
        radiusCheckBox.setSelected(true);
        masterValidationCheckBox.setSelected(true);
        menu = new HBox(5);
        //menu.setPadding(new Insets(5,0,10,0));
        menu.setAlignment(Pos.CENTER);
        menu.getChildren().addAll(startButton, stopButton);
        menuWrapper.getChildren().addAll(menu);
        setTop(menuWrapper);
        radiusCheckBox.setMinWidth(100);
        plotSolutionCheckBox.setMinWidth(100);
        VBox options = new VBox(10);
        options.setPadding(new Insets(5, 5, 5, 10));
        options.setAlignment(Pos.TOP_LEFT);
        options.getChildren().addAll(new Text("Iterations:"), inputIterations,
                new Text("Population size:"), inputPopulationSize,
                new Text("Name of dataset:"),dataSetBox,
                new Text("Dataset split:"),inputDataSetSplit,
                new Text("Validation split:"),inputValidationSplit,
                new Text("k-fold cross validation:"),inputK,
                new Text("PCA projection"),pca,
                radiusCheckBox,
                plotSolutionCheckBox,
                globalSharingFactorCheckBox,
                masterValidationCheckBox);
        setLeft(options);

        startButton.setOnAction((e) -> {
            try {
                controller.run();/*Integer.valueOf(inputIterations.getText()),
                        Integer.valueOf(inputPopulationSize.getText()),
                        Double.valueOf(inputMutationRate.getText()),
                        Integer.valueOf(inputNumberOfTournaments.getText()),
                        dataSetBox.getValue(),
                        labelIndex,
                        Double.valueOf(inputDataSetSplit.getText()),
                        Double.valueOf(inputMigrationFrequency.getText()),
                        Integer.valueOf(inputNumberOfIslands.getText()),
                        Double.valueOf(inputMigrationRate.getText()),
                        masterIslandCheckBox.isSelected(),
                        Integer.valueOf(inputK.getText()),
                        Integer.valueOf(islandIntegrationCount.getText()),
                        Integer.valueOf(pca.getText()),
                        radiusCheckBox.isSelected(),
                        Double.valueOf(inputValidationSplit.getText()),
                        masterValidationCheckBox.isSelected(),
                        plotSolutionCheckBox.isSelected(),
                        globalSharingFactorCheckBox.isSelected());*/
            } catch (FileNotFoundException | URISyntaxException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            }
        });
        stopButton.setOnAction(event -> controller.stopRunning());
        stopButton.setDisable(true);
        primaryStage.show();
    }

    private boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
/*
    public void createSolutionGraph(HashMap<String, double[][]> featureMap, HashMap<String,ArrayList<Antibody>> antibodyMap) {
        this.solutionGraph = new SolutionGraph(solutionGraphWidth, solutionGraphHeight, featureMap,antibodyMap);
        setCenter(solutionGraph);
    }
*/
    /*
    public void drawSolution(HashMap<String, ArrayList<Antigen>> antigenMap, HashMap<String, ArrayList<Antibody>> antibodyMap, double accuracy, boolean radiusPlot){
        solutionGraph.drawSolutionGraph(antigenMap,antibodyMap,accuracy, radiusPlot);
    }*/
/*
    public void setAccuracy(double accuracy) {
        solutionGraph.setAccuracy(accuracy);
    }*/
/*
    public void createStatisticGraph(int iterations,int graphCount, boolean master) {
        graphs = new ArrayList<>();
        graphPane.setAlignment(Pos.TOP_CENTER);
        graphPane.setVgap(50);
        graphPane.setHgap(50);
        graphPane.setPrefWrapLength(1200);
        //setCenter(graphPane);
        if(graphCount ==1){
            graphs.add(new StatisticGraph(statisticGraphWidth, statisticGraphHeight, iterations,true,true,0));
        }else{
            for(int i=0;i <graphCount;i++){
                StatisticGraph graph;
                if(master && i == graphCount-1){
                    graph = new StatisticGraph(550, 300, iterations,false,false,i);
                }else{
                    graph = new StatisticGraph(550, 300, iterations,false,true,i);
                }
                //graph.setPadding(new Insets(100, 0, 100, 100));
                graphs.add(graph);
            }
        }
        //StatisticGraph statisticGraph2 = new StatisticGraph(statisticGraphWidth, statisticGraphHeight, iterations);
        //primaryStage.setHeight(700);
        graphPane.getChildren().clear();
        graphPane.getChildren().addAll(graphs);
        graphPane.setPadding(new Insets(100, 0, 100, 100));
        //graphPane.setMinWidth(1100);
        scrollPane.setContent(graphPane);
        //scrollPane.setPadding(new Insets(100, 100, 200, 200));
        setCenter(scrollPane);
        //scrollPane.setMinWidth(1100);

        scrollPane.setContent(graphPane);
        setCenter(scrollPane);
        scrollPane.setPadding(new Insets(100, 100, 100, 100));
        //setCenter(statisticGraph);
    }*/
/*
    public void addIteration(double fitness, boolean migration, int graphIndex) {
        graphs.get(graphIndex).addIteration(fitness,migration);
        //statisticGraph.addIteration(fitness, migration);
    }

    public void setBestAccuracy(double accuracy,int graphIndex) {
        graphs.get(graphIndex).setBestAccuracy(accuracy);
    }
    public void setAverageAccuracy(double accuracy,int graphIndex) {
        graphs.get(graphIndex).setAverageAccuracy(accuracy);
    }
    public void setCurrentIteration(int iteration, int graphIndex) {
        graphs.get(graphIndex).setIteration(iteration);
    }
    public void setBestAccuracyIteration(double accuracy, int iteration) {
        solutionGraph.setBestAccuracyIteration(accuracy, iteration);
    }
*/
}
