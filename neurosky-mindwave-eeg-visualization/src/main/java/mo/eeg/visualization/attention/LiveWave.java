package mo.eeg.visualization.attention;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class LiveWave extends BorderPane {

    private final Canvas canvas;
    private final GraphicsContext graphics;

    private int whiteSpaceWidth = 50; 
    private int pointDistance = 10; 

    private long lastTimestamp = 0;

    private double prevX = 0;          
    private double prevY = 0;

    private final List<Variable> variables;
    
    ResourceBundle dialogBundle = ResourceBundle.getBundle("properties/principal");

    public LiveWave() {
        canvas = new Canvas(500, 200); 
        graphics = canvas.getGraphicsContext2D();
        clearCanvas();

        variables = new ArrayList<>();

        this.setCenter(canvas);

        this.widthProperty().addListener((observable, oldValue, newValue) -> {
            canvas.setWidth(newValue.doubleValue());
            redraw();
        });
        this.heightProperty().addListener((observable, oldValue, newValue) -> {
            canvas.setHeight(newValue.doubleValue());
            redraw();
        });
    }

    public void addData(String variableName, long timestamp, double value) {
        for (Variable variable : variables) {
            if (variable.name.equals(variableName)) {
                draw(variable, timestamp, value);
            }
        }
    }

    private void draw(Variable variable, long timestamp, double value) {
        if (timestamp > lastTimestamp) {
            graphics.drawImage(canvas.snapshot(null, null), -pointDistance, 0);
            clearRightMargin();
        }

        lastTimestamp = timestamp;

        double scaledValue = (value - variable.min) / (variable.max - variable.min);
        double mappedValue = scaledValue * canvas.getHeight();
        double inverted = canvas.getHeight() - mappedValue;

        graphics.setStroke(variable.color);
        graphics.setLineWidth(1);

        double x = canvas.getWidth() - whiteSpaceWidth;
        double y = inverted;

        if (prevX != 0 || prevY != 0) {
            graphics.strokeLine(prevX, prevY, x, y);
        }
        prevX = x - pointDistance;
        prevY = y;

        drawValues(value, timestamp);
    }

    public void addVariable(String name, double min, double max, Color color) {
        for (Variable variable : variables) {
            if (variable.name.equals(name)) {
                return; 
            }
        }
        variables.add(new Variable(name, min, max, color));
    }

    public void clear() {
        clearCanvas(); 
        prevX = 0;
        prevY = 0;
        lastTimestamp = 0; 
    }


    private void clearCanvas() {
        graphics.setFill(Color.WHITE);
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphics.setStroke(Color.BLACK);
    }

    private void clearRightMargin() {
        graphics.setFill(Color.WHITE);
        graphics.fillRect(canvas.getWidth() - whiteSpaceWidth, 0, whiteSpaceWidth, canvas.getHeight());
    }

    private void drawValues(double value, long timestamp) {
        graphics.setFill(Color.WHITE);
        double rectWidth = 200; 
        double rectHeight = 50;
        graphics.fillRect(0, 0, rectWidth, rectHeight);

        graphics.setFill(Color.BLUE);

        String attentionText = dialogBundle.getString("attention") + String.format("%.1f", value);
        graphics.fillText(attentionText, 10, 20);

        String timeText = dialogBundle.getString("time") + String.format("%d", timestamp);
        graphics.fillText(timeText, 10, 40);
    }


    private void redraw() {
        clearCanvas(); 
    }

    private static class Variable {

        String name;
        double min;
        double max;
        Color color;

        public Variable(String name, double min, double max, Color color) {
            this.name = name;
            this.min = min;
            this.max = max;
            this.color = color;
        }
    }
}
