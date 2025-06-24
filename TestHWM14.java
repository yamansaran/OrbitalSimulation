public class TestHWM14 {
    public static void main(String[] args) {
        try {
            // Load the library
            System.loadLibrary("hwm14jni");
            
            // Create an instance
            HWM14Integration hwm14 = HWM14Integration.getInstance();
            
            if (hwm14.isAvailable()) {
                System.out.println("HWM14 loaded successfully!");
                System.out.println(hwm14.getModelInfo());
                
                // Test parameters (same as our Fortran test)
                int iyd = 15001;      // January 1, 2015
                double sec = 43200.0; // 12:00 UT
                double alt = 150.0;   // 150 km altitude
                double glat = 40.0;   // 40° North
                double glon = -75.0;  // 75° West
                double ap = 4.0;      // Quiet geomagnetic conditions
                
                // Get winds
                double[] winds = hwm14.getWinds(iyd, sec, alt, glat, glon, ap);
                
                System.out.println("\nTest Results:");
                System.out.println("Date: January 1, 2015, 12:00 UT");
                System.out.println("Location: 40°N, 75°W, 150 km altitude");
                System.out.println("Meridional wind: " + winds[0] + " m/s");
                System.out.println("Zonal wind: " + winds[1] + " m/s");
                
            } else {
                System.err.println("Failed to load HWM14: " + hwm14.getLoadError());
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}