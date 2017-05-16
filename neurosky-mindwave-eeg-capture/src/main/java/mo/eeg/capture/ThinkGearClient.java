package mo.eeg.capture;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import mo.eeg.data.EEGData;

public class ThinkGearClient {
    
    private final ArrayList<EEGListener> listeners;
    private String host = "127.0.0.1";
    private Integer port = 13854;
    private final boolean enableRawOutput;
    private Socket socket;
    
    private static final Logger logger = Logger.getLogger(ThinkGearClient.class.getName());
    
    public ThinkGearClient(String host, Integer port, boolean enableRawOutput) throws IOException {
        
        listeners = new ArrayList<>();
        
        if (host != null) {
            this.host = host;
        }
        
        if (port != null) {
            this.port = port;
        }
        
        this.enableRawOutput = enableRawOutput;
    }
    
    public void connect() throws IOException {
        socket = new Socket(host, port);
        
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        out.println("{enableRawOutput:" + enableRawOutput + ",format: \"Json\"}");
        
        out.println("{\"appName\":\"Multimodal Observer\",\"appKey\":\"9d3875a01fa7643b0618ae4618b7678a83124b4c\"}");
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        String line;
        while ( (line = in.readLine()) != null ) {
            long time = System.currentTimeMillis();
            
            try {
                EEGData data = mapper.readValue(line, EEGData.class);
                data.time = time;
                for (EEGListener listener : listeners) {
                    listener.onData(data);
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
    public void addEEGDataListener(EEGListener newListener) {
        if (!listeners.contains(newListener)) {
            listeners.add(newListener);
        }
    }
    
    public void removeEEGDataListener(EEGListener listenerToRemove) {
        listeners.remove(listenerToRemove);
    }

    public void disconnect() {
        if (socket != null && socket.isConnected()) {
            try {
                socket.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }
}
