package GUI;

import AIS.Antibody;
import AIS.Antigen;
import AIS.Controller;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;


public class GUI extends Application {
    ArrayList<Antibody> antibodies;
    ArrayList<Antigen> antigens;
    Controller controller;
    private final int width = 1250;
    private final int height = 650;
    private final int horizontal_offset = 700;
    private final int vertical_offset = 300;
    private final int scale = 300;


    public GUI() throws Exception {
        super();
        this.controller = new Controller();
        this.controller.run();
        this.antibodies = controller.antibodies;
        this.antigens = controller.testing_antigens;
    }

    public void getArgs(ArrayList<Antibody> antibodies, ArrayList<Antigen> antigens) {
        this.antibodies = antibodies;
        this.antigens = antigens;
    }

    private Parent createContent() {
        return new StackPane();
    }

    @Override
    public void start(Stage primaryStage) throws Exception{

        /*
        primaryStage.setTitle("Solution Plot");
        Pane pane = new Pane();
        Scene scene = new Scene(pane, this.width, this.height, true);

        HashMap<String, Color> ab_colors = new HashMap<String, Color>();
        HashMap<String, Color> ag_colors = new HashMap<String, Color>();

        float[] r = {255, 0, 0, 153, 204, 255};
        float[] g = {153, 51, 153, 51, 0, 255};
        float[] b = {0, 204, 51, 153, 0, 0};

        for (int color=0; color<this.antigens.get(0).number_of_classes; color++) {
            Color ab_col = new Color(r[color]/255, g[color]/255, b[color]/255, 0.1);
            Color ag_col = new Color(r[color]/255, g[color]/255, b[color]/255, 1.0);

            ab_colors.put(this.antigens.get(0).classes[color], ab_col);
            ag_colors.put(this.antigens.get(0).classes[color], ag_col);
        }

        for (Antibody ab : this.antibodies) {
            double x = ((ab.feature_list[0]*scale) + horizontal_offset);
            double y = ((ab.feature_list[1]*scale) + vertical_offset);

            Circle circle = new Circle();
            circle.setCenterX(x);
            circle.setCenterY(y);
            circle.setRadius(ab.RR_radius*scale);
            circle.setFill(ab_colors.get(ab.true_class));

            pane.getChildren().add(circle);
        }

        for (Antigen ag : this.antigens) {
            double x = ((ag.feature_list[0]*scale) + horizontal_offset);
            double y = ((ag.feature_list[1]*scale) + vertical_offset);

            Rectangle rect = new Rectangle();
            rect.setX(x);
            rect.setY(y);
            rect.setHeight(3);
            rect.setWidth(3);
            rect.setStroke(Color.BLACK);
            rect.setStrokeWidth(0.5);
            rect.setFill(ag_colors.get(ag.true_class));

            pane.getChildren().add(rect);
        }

        Text txt11 = new Text();
        txt11.setText("k: " + controller.k);
        txt11.setStyle("-fx-font: 16 arial;");
        txt11.setX(100);
        txt11.setY(vertical_offset-20);
        pane.getChildren().add(txt11);

        Text txt1 = new Text();
        txt1.setText("Dataset: " + controller.dataset);
        txt1.setStyle("-fx-font: 16 arial;");
        txt1.setX(100);
        txt1.setY(vertical_offset+5);
        pane.getChildren().add(txt1);

        Text txt2 = new Text();
        txt2.setText("Total number of features: " + controller.number_of_features);
        txt2.setStyle("-fx-font: 16 arial;");
        txt2.setX(100);
        txt2.setY(vertical_offset+30);
        pane.getChildren().add(txt2);

        Text txt3 = new Text();
        txt3.setText("Number of classes: " + antigens.get(0).number_of_classes);
        txt3.setStyle("-fx-font: 16 arial;");
        txt3.setX(100);
        txt3.setY(vertical_offset+55);
        pane.getChildren().add(txt3);

        Text txt4 = new Text();
        txt4.setText("Accuracy: " + (Math.round(controller.total_accuracy * 100.0) / 100.0));
        txt4.setStyle("-fx-font: 16 arial;");
        txt4.setX(100);
        txt4.setY(vertical_offset+80);
        pane.getChildren().add(txt4);

        Text txt5 = new Text();
        txt5.setText("Generations: " + controller.generations);
        txt5.setStyle("-fx-font: 16 arial;");
        txt5.setX(100);
        txt5.setY(vertical_offset+105);
        pane.getChildren().add(txt5);

        Text txt6 = new Text();
        txt6.setText("Antibody ratio: " + controller.antibody_ratio);
        txt6.setStyle("-fx-font: 16 arial;");
        txt6.setX(100);
        txt6.setY(vertical_offset+130);
        pane.getChildren().add(txt6);

        Text txt7 = new Text();
        txt7.setText("Max antibody replacement ratio: " + controller.max_antibody_replacement_ratio);
        txt7.setStyle("-fx-font: 16 arial;");
        txt7.setX(100);
        txt7.setY(vertical_offset+155);
        pane.getChildren().add(txt7);

        Text txt8 = new Text();
        txt8.setText("Feature vector mutation probability: " + controller.feature_vector_mutation_probability);
        txt8.setStyle("-fx-font: 16 arial;");
        txt8.setX(100);
        txt8.setY(vertical_offset+180);
        pane.getChildren().add(txt8);

        Text txt9 = new Text();
        txt9.setText("RR radius mutation probability: " + controller.RR_radius_mutation_probability);
        txt9.setStyle("-fx-font: 16 arial;");
        txt9.setX(100);
        txt9.setY(vertical_offset+205);
        pane.getChildren().add(txt9);

        Text txt10 = new Text();
        txt10.setText("Antibody removal (fitness) threshold: " + controller.antibody_removal_threshold);
        txt10.setStyle("-fx-font: 16 arial;");
        txt10.setX(100);
        txt10.setY(vertical_offset+230);
        pane.getChildren().add(txt10);
        */


        ///// Accuracy plot //////
        NumberAxis xAxis = new NumberAxis("Generation", 1, this.controller.generations, 1);
        NumberAxis yAxis = new NumberAxis("Accuracy", 0.5, 1.0, 0.01);

        LineChart<Number, Number> graph = new LineChart<>(xAxis, yAxis);
        graph.setTitle("Accuracy per generation");

        final XYChart.Series<Number, Number> series = new XYChart.Series<>();

        for (int i=1; i<this.controller.generations; i++) {
            series.getData().add(new XYChart.Data<Number, Number>(i, this.controller.final_generation_accuracies[i-1]));
        }

        graph.getData().add(series);
        Scene scene2 = new Scene(graph, this.width, this.height, true);




        //primaryStage.setScene(scene);
        primaryStage.setScene(scene2);
        primaryStage.show();
    }
}


