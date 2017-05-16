package mo.eeg.capture;

import mo.eeg.data.EEGData;

public interface EEGListener {
    void onData(EEGData data);
}
