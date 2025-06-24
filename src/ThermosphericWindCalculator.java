/**
 * ThermosphericWindCalculator handles thermospheric wind effects using HWM14
 * Integrates with the existing atmospheric drag calculator to modify drag calculations
 */
public class ThermosphericWindCalculator {
    private HWM14Integration hwm14;
    
    public ThermosphericWindCalculator() {
        this.hwm14 = HWM14Integration.getInstance();
        if (!hwm14.isAvailable()) {
            System.err.println("HWM14 not available: " + hwm14.getLoadError());
        }
    }
    
    /**
     * Get thermospheric winds at satellite location
     * @param satellite Satellite object
     * @param simulation Simulation reference for time and position
     * @return double[3] array: [north_wind, east_wind, vertical_wind] in m/s (vertical=0 for HWM14)
     */
    public double[] getThermosphericWinds(Satellite satellite, OrbitalSimulation simulation) {
        if (!hwm14.isAvailable()) {
            return new double[]{0.0, 0.0, 0.0}; // No winds if HWM14 unavailable
        }
        
        // Get satellite position and convert to geographic coordinates
        double[] satPos3D = satellite.getPosition3D();
        double altitude = (Math.sqrt(satPos3D[0]*satPos3D[0] + satPos3D[1]*satPos3D[1] + satPos3D[2]*satPos3D[2]) 
                          - simulation.getEarthRadius()) / 1000.0; // Convert to km
        
        // Calculate geographic coordinates (simplified)
        double latitude = Math.toDegrees(Math.asin(satPos3D[2] / Math.sqrt(satPos3D[0]*satPos3D[0] + satPos3D[1]*satPos3D[1] + satPos3D[2]*satPos3D[2])));
        double longitude = Math.toDegrees(Math.atan2(satPos3D[1], satPos3D[0]));
        
        // Account for Earth rotation (simplified)
        double currentTime = simulation.getCurrentSimulationTime();
        double earthRotationAngle = (currentTime / 86400.0) * 360.0;
        longitude -= earthRotationAngle % 360.0;
        while (longitude > 180) longitude -= 360;
        while (longitude < -180) longitude += 360;
        
        // Convert simulation time to HWM14 format
        // For now, use a fixed year (2015) - you can enhance this later
        int iyd = 15001 + (int)(currentTime / 86400.0); // Days since Jan 1, 2015
        double sec = currentTime % 86400.0; // Seconds of day
        
        // Get geomagnetic activity (simplified - use quiet conditions for now)
        double ap = 4.0; 
        
        try {
            double[] winds = hwm14.getWindsWithTimeout(iyd, sec, altitude, latitude, longitude, ap);
            
            // Convert HWM14 winds to 3D vector
            // winds[0] = meridional (north-south), winds[1] = zonal (east-west)
            return new double[]{winds[0], winds[1], 0.0}; // No vertical component in HWM14
            
        } catch (Exception e) {
            System.err.println("Error getting HWM14 winds: " + e.getMessage());
            return new double[]{0.0, 0.0, 0.0};
        }
    }
    
    /**
     * Check if HWM14 is available
     */
    public boolean isAvailable() {
        return hwm14.isAvailable();
    }
    
    /**
     * Get status information
     */
    public String getStatusInfo() {
        if (hwm14.isAvailable()) {
            return "HWM14 Thermospheric Wind Model - Ready";
        } else {
            return "HWM14 Unavailable: " + hwm14.getLoadError();
        }
    }
}