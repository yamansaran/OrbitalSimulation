public class HWM14Integration {
    private static HWM14Integration instance;
    private boolean isLoaded = false;
    private String loadError = null;
    
    static {
        try {
            System.loadLibrary("hwm14jni");
            instance = new HWM14Integration();
            instance.isLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            instance = new HWM14Integration();
            instance.loadError = e.getMessage();
        }
    }
    
    public static HWM14Integration getInstance() {
        return instance;
    }
    
    public boolean isAvailable() {
        return isLoaded;
    }
    
    public String getLoadError() {
        return loadError;
    }
    
    /**
     * Get winds from HWM14 model
     * @param iyd Year and day as YYDDD (e.g., 15001 for Jan 1, 2015)
     * @param sec Seconds of day (UT)
     * @param alt Altitude in km
     * @param glat Geographic latitude in degrees
     * @param glon Geographic longitude in degrees
     * @param ap Geomagnetic activity index
     * @return double[2] array: [meridional_wind, zonal_wind] in m/s
     */
    public native double[] getWinds(int iyd, double sec, double alt, 
                                   double glat, double glon, double ap);
    
    public String getModelInfo() {
        return "HWM14 - Horizontal Wind Model 2014";
    }
}