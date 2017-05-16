package mo.eeg.data;



public class EEGPower {
    public float delta;
    public float theta;
    public float lowAlpha;
    public float highAlpha;
    public float lowBeta;
    public float highBeta;
    public float lowGamma;
    public float highGamma;

    @Override
    public String toString() {
        return "d:"+delta+" t:"+theta+" la:"+lowAlpha+" ha:"+highAlpha+" lb:"+lowBeta+" hb:"+highBeta+" lg:"+lowGamma+" hg:"+highGamma;
    }
    
}
