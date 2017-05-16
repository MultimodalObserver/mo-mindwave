package mo.eeg.visualization.attention;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import mo.core.ui.dockables.DockableElement;
import mo.core.ui.dockables.DockablesRegistry;
import mo.eeg.data.EEGData;
import mo.visualization.Playable;
import org.apache.commons.io.input.ReversedLinesFileReader;

public class EEGPlayer implements Playable {

    private long start;
    private long end;

    private RandomAccessFile file;

    private EEGData current;
    private EEGData next;
    private LiveWave wave;

    private int count = 0;
    private static final Logger logger = Logger.getLogger(EEGPlayer.class.getName());
    private boolean stopped;

    public EEGPlayer(File file) {
        try {
            readLastTime(file);
            this.file = new RandomAccessFile(file, "r");
            while (current == null) {
                current = next();
            }
            start = current.time;
            
            next = next();

            wave = new LiveWave();
            wave.addVariable("Att", 0, 100, Color.blue);
            
            SwingUtilities.invokeLater(() -> {
                try {
                    DockableElement d = new DockableElement();
                    d.add(wave);
                    DockablesRegistry.getInstance().addDockableInProjectGroup("", d);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            });
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    private EEGData next() {
        try {
            String line = file.readLine();

            if (line == null || line.isEmpty()) {
                return null;
            }

            String[] dataParts = line.split(" ");

            if (dataParts.length == 2) {
                return null; //ignore blink data
            }

            if (dataParts.length == 12) {
                EEGData data = new EEGData();
                for (String dataPart : dataParts) {
                    String[] d = dataPart.split(":");
                    String name = d[0];
                    String value = d[1];
                    switch (name) {
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
                return data;
            }
        } catch (IOException | NumberFormatException ex) {
            pause();
            logger.log(Level.SEVERE, null, ex);
            return null;
        }
        return null;
    }

    private static long parseTimestamp(String str) {
        String[] parts = str.split(" ");

        if (parts.length == 2 || parts.length == 12) {

            String[] timeData = parts[0].split(":");

            if (timeData.length == 2 && timeData[0].equals("t")) {
                return Long.parseLong(timeData[1]);
            }
        }

        return -1;
    }

    private void readLastTime(File file) {
        String lastLine;
        try (ReversedLinesFileReader rev = new ReversedLinesFileReader(file, Charset.defaultCharset())) {
            lastLine = null;
            do {
                lastLine = rev.readLine();
            } while (lastLine == null || lastLine.trim().isEmpty());
            end = parseTimestamp(lastLine);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void pause() {
    }

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

    @Override
    public void stop() {
        stopped = true;
        pause();
    }

    @Override
    public long getStart() {
        return start;
    }

    @Override
    public long getEnd() {
        return end;
    }
}
