/**
 * AccelerationCalculator provides static methods for calculating various accelerations
 * Extracted from SimulationPanel for better code organization
 */
public class AccelerationCalculator {
    
    /**
     * Calculates lunar gravitational acceleration components
     */
    public static double[] calculateLunarAcceleration(double satX, double satY, OrbitalSimulation simulation) {
        double[] moonPos = simulation.getMoonPosition();
        double moonX = moonPos[0];
        double moonY = moonPos[1];
        
        // Vector from satellite to Moon
        double satToMoonX = moonX - satX;
        double satToMoonY = moonY - satY;
        double satMoonDistance = Math.sqrt(satToMoonX * satToMoonX + satToMoonY * satToMoonY);
        
        if (satMoonDistance > 0) {
            double moonMass = 7.342e22;
            double G = 6.67430e-11;
            double moonAccelMagnitude = G * moonMass / (satMoonDistance * satMoonDistance);
            
            // Direction unit vector (toward Moon)
            double moonDirX = satToMoonX / satMoonDistance;
            double moonDirY = satToMoonY / satMoonDistance;
            
            return new double[]{moonAccelMagnitude * moonDirX, moonAccelMagnitude * moonDirY};
        }
        return new double[]{0, 0};
    }

    /**
     * Calculates solar gravitational acceleration components
     */
    public static double[] calculateSolarAcceleration(double satX, double satY, OrbitalSimulation simulation) {
        double[] sunPos = simulation.getSunPosition();
        double sunX = sunPos[0];
        double sunY = sunPos[1];
        
        // Vector from satellite to Sun
        double satToSunX = sunX - satX;
        double satToSunY = sunY - satY;
        double satSunDistance = Math.sqrt(satToSunX * satToSunX + satToSunY * satToSunY);
        
        if (satSunDistance > 0) {
            double sunMass = 1.989e30;
            double G = 6.67430e-11;
            double sunAccelMagnitude = G * sunMass / (satSunDistance * satSunDistance);
            
            // Direction unit vector (toward Sun)
            double sunDirX = satToSunX / satSunDistance;
            double sunDirY = satToSunY / satSunDistance;
            
            return new double[]{sunAccelMagnitude * sunDirX, sunAccelMagnitude * sunDirY};
        }
        return new double[]{0, 0};
    }
    
    /**
     * Calculates the magnitude of gravitational acceleration from the Moon
     */
    public static double calculateLunarAccelerationMagnitude(double[] satPos, OrbitalSimulation simulation) {
        double[] moonPos = simulation.getMoonPosition();
        double moonX = moonPos[0];
        double moonY = moonPos[1];
        double moonZ = 0; // Simplified to 2D
        
        // Vector from satellite to Moon
        double satToMoonX = moonX - satPos[0];
        double satToMoonY = moonY - satPos[1];
        double satToMoonZ = moonZ - satPos[2];
        double satMoonDistance = Math.sqrt(satToMoonX*satToMoonX + satToMoonY*satToMoonY + satToMoonZ*satToMoonZ);
        
        if (satMoonDistance > 0) {
            double moonMass = 7.342e22; // Moon mass in kg
            double G = 6.67430e-11; // Gravitational constant
            return G * moonMass / (satMoonDistance * satMoonDistance);
        }
        return 0;
    }
    
    /**
     * Calculates the magnitude of gravitational acceleration from the Sun
     */
    public static double calculateSolarAccelerationMagnitude(double[] satPos, OrbitalSimulation simulation) {
        double[] sunPos = simulation.getSunPosition();
        double sunX = sunPos[0];
        double sunY = sunPos[1];
        double sunZ = 0; // Simplified to 2D
        
        // Vector from satellite to Sun
        double satToSunX = sunX - satPos[0];
        double satToSunY = sunY - satPos[1];
        double satToSunZ = sunZ - satPos[2];
        double satSunDistance = Math.sqrt(satToSunX*satToSunX + satToSunY*satToSunY + satToSunZ*satToSunZ);
        
        if (satSunDistance > 0) {
            double sunMass = 1.989e30; // Sun mass in kg
            double G = 6.67430e-11; // Gravitational constant
            return G * sunMass / (satSunDistance * satSunDistance);
        }
        return 0;
    }
}