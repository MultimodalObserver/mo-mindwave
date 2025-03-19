package mo.eeg.visualization.attention;

//import com.theeyetribe.clientsdk.data.GazeData;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import mo.visualization.Playable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import mo.eeg.data.EEGData;
import org.apache.commons.io.input.ReversedLinesFileReader;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class EEGPlayer implements Playable {

    private long start;
    private long end;
    private RandomAccessFile file;
    private EEGData current;
    private EEGData next;
    private LiveWave wave;
    private boolean stopped;
    private static final Logger logger = Logger.getLogger(EEGPlayer.class.getName());
    private Timeline playbackTimeline;
    
    /*
    //test to try different lenguages on the tab
    Locale idiom = new Locale("en", "EN");
    ResourceBundle dialogBundle = ResourceBundle.getBundle("properties/principal", idiom);
    */
    
    ResourceBundle dialogBundle = ResourceBundle.getBundle("properties/principal");

   
    public EEGPlayer(File file) {
        try {
            if (!file.exists() || file.length() == 0) {
                throw new IOException("El archivo está vacío o no existe.");
            }

            readLastTime(file);
            this.file = new RandomAccessFile(file, "r");

            current = next();
            if (current == null) {
                throw new IOException("No se encontraron datos válidos en el archivo.");
            }
            start = current.time;
            next = next();

            wave = new LiveWave();
            wave.addVariable("Att", 0, 100, Color.BLUE);

            /*Platform.runLater(() -> {
                Stage stage = new Stage();
                StackPane root = new StackPane();
                root.getChildren().add(wave.getCanvas());
                Scene scene = new Scene(root, 600, 400);
                stage.setTitle(dialogBundle.getString("title"));
                stage.setScene(scene);
                stage.show();
            });*/

        } catch (IOException ex) {
            System.err.println("Error al inicializar EEGPlayer: " + ex.getMessage());
        }
    }
    
    public Node getPaneNode() {
        if (wave != null) {
            return wave; 
        } else {
            throw new IllegalStateException("LiveWave instance is not initialized.");
        }
    }


    private void readLastTime(File file) {
        try (ReversedLinesFileReader rev = new ReversedLinesFileReader(file, Charset.defaultCharset())) {
            String lastLine;
            do {
                lastLine = rev.readLine();
            } while (lastLine == null || lastLine.trim().isEmpty());
            end = parseTimestamp(lastLine);
        } catch (IOException ex) {
            System.err.println("Error al leer el último tiempo: " + ex.getMessage());
        }
    }

    private static long parseTimestamp(String str) {
        String[] parts = str.split(" ");
        if (parts.length == 2) {
            String[] timeData = parts[0].split(":");
            if (timeData.length == 2 && "t".equals(timeData[0])) {
                return Long.parseLong(timeData[1]);
            }
        }
        return -1;
    }

    @Override
    public void play(long millis) {
        if (current == null) { 
            try {
                file.seek(0); 
                current = next(); 
                next = next();
                wave.clear(); 
            } catch (IOException ex) {
                System.err.println("Error al reiniciar el archivo: " + ex.getMessage());
                return; 
            }
        }

        if (playbackTimeline == null || stopped) {
            if ((millis >= start) && (millis <= end)) {
                seek(millis);

                playbackTimeline = new Timeline(new KeyFrame(Duration.millis(100), event -> {
                   
                    if (wave == null || current == null) {
                        stopPlayback();
                        return;
                    }

                    Platform.runLater(() -> {
                        if (current != null) {
                            wave.addData("Att", current.time, current.eSense.attention);
                        }
                    });

                    current = next();

                    if (current == null) { 
                        stopPlayback();
                    }
                }));

                playbackTimeline.setCycleCount(Timeline.INDEFINITE);
                playbackTimeline.play();
                stopped = false; 
            }
        } else {
            playbackTimeline.play(); 
        }
    }


    private EEGData next() {
        try {
            String line = file.readLine();
            if (line == null || line.trim().isEmpty()) {
                return null; 
            }

            String[] dataParts = line.trim().split(" ");
            if (dataParts.length != 2) {
                return null; 
            }

            EEGData data = new EEGData();
            for (String dataPart : dataParts) {
                String[] keyValue = dataPart.split(":");
                if (keyValue.length != 2) {
                    continue; 
                }

                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                switch (key) {
                    case "t":
                        data.time = Long.parseLong(value);
                        break;
                    case "att":
                        data.eSense.attention = Byte.parseByte(value);
                        break;
                    default:
                        break;
                }
            }
            return data.time > 0 ? data : null; 
        } catch (IOException | NumberFormatException ex) {
            System.err.println("Error al leer siguiente dato: " + ex.getMessage());
            return null; 
        }
    }




    private void stopPlayback() {
        if (playbackTimeline != null) {
            playbackTimeline.stop();
            playbackTimeline = null; 
        }
        stopped = true;
    }

    @Override
    public void pause() {
        if (playbackTimeline != null) {
            playbackTimeline.pause(); 
        }
    }

    @Override
    public void stop() {
        if (playbackTimeline != null) {
            playbackTimeline.stop(); 
            playbackTimeline = null;
        }

        if (wave != null) {
            wave.clear();
        }

        try {
            file.seek(0); 
            current = next(); 
            next = next();
        } catch (IOException ex) {
            System.err.println("Error al reiniciar el archivo: " + ex.getMessage());
        }

        stopped = true; 
    }


    @Override
    public void seek(long requestedMillis) {
        if (requestedMillis < start || requestedMillis > end) {
            return;
        }

        try {
            file.seek(0);
            current = next();
            while (current != null && current.time < requestedMillis) {
                current = next();
            }
        } catch (IOException ex) {
            System.err.println("Error al buscar en el archivo: " + ex.getMessage());
        }
    }

    @Override
    public long getStart() {
        return start;
    }

    @Override
    public long getEnd() {
        return end;
    }
    
    
    
    /*
    
    //Some of the old methods from the original version of MO:
    
     @Override
    public void seek(long requestedMillis) {
                if (requestedMillis < start
                || requestedMillis > end
                || requestedMillis == current.time
                || (requestedMillis > current.time &&
                    requestedMillis < next.time)) {
            return;
        }       
        
        EEGData data = current;

        if (requestedMillis < current.time) {
            try {
                file.seek(0);
                data = next();

            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }

        long marker;
        try {
            marker = file.getFilePointer();

            EEGData nextD = next();
            if (nextD == null) {
                return;
            }

            while (!(nextD.time > requestedMillis)) {
                data = nextD;

                marker = file.getFilePointer();
                nextD = next();
                
                if (nextD == null) { // no more events (end of file)
                    return;
                }
            }

            file.seek(marker);
            current = data;
            next = nextD;

        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void play(long millis) {
        if ( (millis >= start) && (millis <= end)) {
            seek(millis);
            if (current.time == millis) {
                SwingUtilities.invokeLater(() -> {
                    if (stopped) {
                        wave.clear();
                        stopped = false;
                    }
                    wave.addData("Att", current.time, current.eSense.attention);
                });
            }
        }
    }

   
    
    */
}
