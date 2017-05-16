package mo.eeg.data;

public class EEGData {
    
    public long time;

    public ESense eSense;

    public EEGPower eegPower;
    
    /**
     * 0-200. -1 when is not set.
     */
    public short poorSignalLevel = -1;
    
    /**
     * 1-255. -1 when is not set.
     */
    public short blinkStrength = -1;

    private double mentalEffort;
    public boolean mentalEffortIsSet;

    private double familiarity;
    public boolean familiarityIsSet;

    public String status;

    private short rawEeg;
    public boolean rawEegIsSet;
    
    public EEGData() {
        eSense = new ESense();
        eegPower = new EEGPower();
    }

    /**
     *
     * @param mentalEffort
     */
    public void setMentalEffort(double mentalEffort) {
        this.mentalEffort = mentalEffort;
        mentalEffortIsSet = true;
    }
    
    public double getMentalEffort() {
        return this.mentalEffort;
    }

    /**
     *
     * @param familiarity
     */
    public void setFamiliarity(double familiarity) {
        this.familiarity = familiarity;
        familiarityIsSet = true;
    }
    
    public double getFamiliarity() {
        return this.familiarity;
    }

    /**
     *
     * @param rawEeg
     */
    public void setRawEeg(short rawEeg) {
        this.rawEeg = rawEeg;
        rawEegIsSet = true;
    }
    
    public short getRawEeg() {
        return this.rawEeg;
    }

    @Override
    public String toString() {
        String s = "time:"+time+"\n"+
                "eSense:"+eSense +"\n" + 
                "eegPower:"+eegPower + "\n" +
                "ps:"+poorSignalLevel + "\n" + 
                "b:"+blinkStrength + "\n" +
                "effort:"+mentalEffort +" "+ mentalEffortIsSet+"\n" +
                "fam:"+familiarity + " " + familiarityIsSet +"\n"+
        "status: "+status+"\n"+
                "rawEeg:"+rawEeg+" "+rawEegIsSet;
        return s;
    }
}
