package mo.eeg.capture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import mo.eeg.data.EEGData;
import mo.eeg.data.EEGPower;
import mo.organization.FileDescription;

public class EEGRecorder implements EEGListener {
    
    private EEGConfiguration config;

    private ThinkGearClient client;
    
    private File output;
    
    private FileOutputStream outputStream;
    
    private FileDescription desc;
    
    private static final Logger logger = Logger.getLogger(EEGRecorder.class.getName());
    
    public EEGRecorder(File stageFolder, EEGConfiguration config) throws IOException {
        client = new ThinkGearClient(null, null, false);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client.connect();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        });
        t.start();
        //t.
        this.config = config;
        createFile(stageFolder);
    }

    private void createFile(File parent) {

        Date now = new Date();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss.SSS");

        String reportDate = df.format(now);

        output = new File(parent, reportDate + "_" + config.getId() + ".txt");
        try {
            output.createNewFile();
            outputStream = new FileOutputStream(output);
            desc = new FileDescription(output, EEGRecorder.class.getName());
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

    }
    
    private void deleteFile() {
        if (output.isFile()) {
            output.delete();
        }
        if (desc.getDescriptionFile().isFile()) {
            desc.deleteFileDescription();
        }
    }
    
    public void cancel() {
        stop();
        deleteFile();
    }

    public void start() {
        client.addEEGDataListener(this);
    }

    public void pause() {
        client.removeEEGDataListener(this);
    }
    
    public void resume() {
        client.addEEGDataListener(this);
    }

    public void stop() {
        client.removeEEGDataListener(this);
        client.disconnect();
        try {
            outputStream.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }  
    
    
    public static void main(String[] args) {
        
    }

    @Override
    public void onData(EEGData data) {
        try {
            outputStream.write(format(data).getBytes());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    private String format(EEGData data) {
        String line = "t:"+data.time;
        if (data.eSense != null) {
            line += " att:" + data.eSense.attention + " med:" + data.eSense.meditation;
        }
        if (data.eegPower != null) {
            EEGPower p = data.eegPower;
            line += " d:" + formatFloat(p.delta) + " th:" + formatFloat(p.theta) +
                    " la:" + formatFloat(p.lowAlpha) + " ha:" + formatFloat(p.highAlpha) + 
                    " lb:" + formatFloat(p.lowBeta)  + " hb:" + formatFloat(p.highBeta) + 
                    " lg:" + formatFloat(p.lowGamma) + " hg:" + formatFloat(p.highGamma);
        }
        if (data.poorSignalLevel > -1) {
            line += " psl:" + data.poorSignalLevel;
        }
        if (data.blinkStrength > -1) {
            line += " bs:" + data.blinkStrength;
        }
        if (data.mentalEffortIsSet) {
            line += " me:" + data.getMentalEffort();
        }
        if (data.familiarityIsSet) {
            line += " fam:" + data.getFamiliarity();
        }
        if (data.status != null) {
            line += " s:" + data.status;
        }
        if (data.rawEegIsSet) {
            line += " r:" + data.getRawEeg();
        }
        return line+"\n";
    }
    
    // get string without .0 if necessary
    private String formatFloat(float f) {
        if (f == (long) f) {
            return Long.toString((long) f);
        } else {
            return Float.toString(f);
        }
    }
}
