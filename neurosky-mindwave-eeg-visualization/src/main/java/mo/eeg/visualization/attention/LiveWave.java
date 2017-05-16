package mo.eeg.visualization.attention;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JPanel;

public class LiveWave extends JPanel {
    private BufferedImage image;
    private Graphics2D graphics;
    
    private int width = 500;
    private int height = 200;
    
    private ArrayList<Variable> variables;
    
    private int whiteSpaceWidth = 50;
    private int pointWidth = 1;
    private int pointHeight = 2;
    
    private int pointDistance = 10;
    
    private int prevX;
    private int prevY;
    
    private long lastTimestamp = 0;

    public LiveWave() {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        graphics = image.createGraphics();
        graphics.setBackground(Color.white);
        graphics.fillRect(0, 0, width, height);
        variables = new ArrayList<>();
        setDoubleBuffered(true);
    }
    
    private static int next(int last) {
        Random r = new Random();
        int n = r.nextInt();
        if (n % 3 == 0) {
            last++;
        } else if (n % 2 == 0) {
            last--;
        }
        return last;
    }
    
    private static int randInt(int min, int max) {

        // Usually this can be a field rather than a method variable
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }
    
    public void addData(String variableName, long timestamp, double value) {
        for (Variable variable : variables) {
            if (variable.name.equals(variableName)) {
                draw(variable, timestamp, value);
            }
        }
    }
    
    private void draw(Variable v, long timestamp, double value) {
        
        if (timestamp > lastTimestamp) {
            graphics.copyArea(0, 0, width, height, -pointDistance, 0);
            graphics.setColor(graphics.getBackground());
            graphics.fillRect(width-whiteSpaceWidth, 0, height-whiteSpaceWidth, height);
        }
        
        lastTimestamp = timestamp;
        
        int mappedValue =  (int) ((int) (value - v.min) / (v.max - v.min) * height) ;
        int inverted = height - mappedValue;
        
        graphics.setColor(v.color);

        int x = width-whiteSpaceWidth;
        int y = inverted;
        
        if (prevX == 0 && prevY == 0) {
            prevX = x;
            prevY = y;
        }
        
        graphics.drawLine(prevX, prevY, x, y);
        prevX = x - pointDistance;
        prevY = y;
        //graphics.drawRect(width-whiteSpaceWidth, inverted, pointWidth, pointHeight);

        FontMetrics m = graphics.getFontMetrics();
        String val = value + "";
        int valWidth = m.stringWidth(val);
        graphics.drawString(val, width - valWidth, height - 40);
        
        String time = timestamp + "";
        int timeWidth = m.stringWidth(time);
        if (timeWidth < whiteSpaceWidth) {
            graphics.drawString(time, width - timeWidth, height - 20);
        } 

        repaint();
    }

    public void addVariable(String name, double min, double max, Color color) {
        for (Variable variable : variables) {
            if (variable.name.equals(name)) {
                //todo log
                return;
            }
        }
        
        Variable v = new Variable();
        v.name = name;
        v.min = min;
        v.max = max;
        if (color != null) {
            v.color = color;
        }
        
        variables.add(v);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        int w = this.getWidth();
        int h = this.getHeight();
        
        g.drawImage(image,
                1, 1,
                w - 0,
                h - 0,
                0, 0,
                image.getWidth(), image.getHeight(),
                null);
    }
    
    private class Variable {
        Color color = Color.BLACK;
        String name;
        double min = 0, max = height;
    }
    
    public void clear() {
        Color prevBakground = graphics.getBackground();
        Color prevColor = graphics.getColor();
        graphics.setBackground(Color.white);
        graphics.setColor(Color.white);
        graphics.fillRect(0, 0, width, height);
        graphics.setBackground(prevBakground);
        graphics.setColor(prevColor);
        
    }
}
