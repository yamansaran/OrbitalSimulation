/*
 * 
 * LunarForceCalculator class handles lunar gravitational perturbations
 * Separated from Satellite class for better code organization and modularity
 */
public class LunarForceCalculator {
    private static final double MOON_MASS = 7.342e22; // Moon's mass in kg
    private static final double MOON_EARTH_DISTANCE = 384400000; // Average Moon-Earth distance in meters
    
    /*
     * Applies lunar gravitational perturbations to satellite orbital elements
     * 
     * This method simulates the Moon's gravitational influence on the satellite's orbit.
     * The perturbations cause gradual changes in the orbital elements over time.
     * Uses scaled perturbations for numerical stability at high speeds.
     * 
     * @param satellite The satellite object to apply perturbations to
     * @param deltaTime Time step in seconds
     * @param simulation Reference to main simulation for Moon position
     */
    public void applyPerturbations(Satellite satellite, double deltaTime, OrbitalSimulation simulation) {
        // Get current Moon position relative to Earth
        double[] moonPos = simulation.getMoonPosition();
        double moonX = moonPos[0];
        double moonY = moonPos[1];
        double moonZ = 0; // Assume Moon stays in Earth's equatorial plane for simplification
        
        // Get current satellite position
        double[] satPos = satellite.getPosition3D(); // Get 3D position including Z component
        double satX = satPos[0];
        double satY = satPos[1];
        double satZ = satPos[2];
        
        // Calculate distance vectors
        double moonDistance = Math.sqrt(moonX*moonX + moonY*moonY + moonZ*moonZ);
        double satDistance = Math.sqrt(satX*satX + satY*satY + satZ*satZ);
        
        // Distance from satellite to Moon
        double satMoonDx = satX - moonX;
        double satMoonDy = satY - moonY;
        double satMoonDz = satZ - moonZ;
        double satMoonDistance = Math.sqrt(satMoonDx*satMoonDx + satMoonDy*satMoonDy + satMoonDz*satMoonDz);
        
        // Avoid division by zero and unrealistic scenarios
        if (satMoonDistance < 1000 || moonDistance < 1000) return;
        
        // Calculate lunar perturbation strength based on satellite's orbital characteristics
        // Higher altitude satellites are more affected by lunar perturbations
        double perturbationStrength = calculatePerturbationStrength(satDistance, satMoonDistance, satellite.getEarthMass());
        
        // Scale perturbations based on time step to maintain stability
        double timeStepScale = Math.min(1.0, deltaTime / 1.0); // Normalize to 1-second steps
        double scaledStrength = perturbationStrength * timeStepScale;
        
        // Get current orbital elements
        double i = satellite.getInclination();
        double e = satellite.getEccentricity();
        double nu = satellite.getTrueAnomaly();
        
        // Apply perturbations to orbital elements with improved numerical stability
        // These are simplified models of how the Moon affects satellite orbits
        
        // 1. Longitude of Ascending Node precession (most significant effect)
        // The Moon causes the orbital plane to slowly rotate
        double nodePrecessRate = scaledStrength * 1e-8 * Math.cos(i) * deltaTime; // Reduced and scaled
        satellite.adjustLongitudeOfAscendingNode(nodePrecessRate);
        
        // 2. Argument of periapsis rotation
        // The orientation of the ellipse within the orbital plane changes
        double periapsisRotationRate = scaledStrength * 5e-9 * (1 - e*e) * deltaTime; // Reduced and scaled
        satellite.adjustArgumentOfPeriapsis(periapsisRotationRate);
        
        // 3. Inclination oscillation (smaller effect)
        // The orbital plane tilt oscillates slightly
        double inclinationOscillation = scaledStrength * 1e-10 * Math.sin(2 * nu) * deltaTime; // Much reduced
        satellite.adjustInclination(inclinationOscillation);
        
        // 4. Semi-major axis variation (very small effect)
        // The orbit size can change slightly due to energy transfer
        double semiMajorAxisVariation = scaledStrength * 1e-4 * Math.sin(nu) * deltaTime; // Reduced
        satellite.adjustSemiMajorAxis(semiMajorAxisVariation);
        
        // 5. Eccentricity variation (very small effect)
        // The orbit shape can change slightly
        double eccentricityVariation = scaledStrength * 1e-11 * Math.cos(nu) * deltaTime; // Much reduced
        satellite.adjustEccentricity(eccentricityVariation);
    }
    
    /**
     * Calculates the strength of lunar perturbations
     * Includes smoothing for numerical stability
     * 
     * @param satDistance Distance from Earth center to satellite
     * @param satMoonDistance Distance from satellite to Moon
     * @param earthMass Mass of Earth for ratio calculations
     * @return Perturbation strength factor
     */
    private double calculatePerturbationStrength(double satDistance, double satMoonDistance, double earthMass) {
        // Basic perturbation strength based on distances and masses
        // This is a simplified model - real perturbations are much more complex
        
        double earthMoonMassRatio = MOON_MASS / earthMass; // ~0.012
        double distanceRatio = satDistance / MOON_EARTH_DISTANCE; // Satellite distance relative to Moon orbit
        double proximityFactor = 1.0 / Math.max(1.0, satMoonDistance / MOON_EARTH_DISTANCE); // How close satellite is to Moon
        
        // Apply smoothing to prevent sudden changes in perturbation strength
        double minProximity = 0.1; // Minimum proximity factor to prevent extreme perturbations
        double maxProximity = 10.0; // Maximum proximity factor to prevent instability
        proximityFactor = Math.max(minProximity, Math.min(maxProximity, proximityFactor));
        
        // Perturbations are stronger for:
        // - Higher altitude satellites (larger distanceRatio)
        // - When satellite is closer to Moon (larger proximityFactor)
        double strength = earthMoonMassRatio * distanceRatio * proximityFactor;
        
        // Apply scaling factor to make effects visible in simulation timeframes
        // Real lunar perturbations occur over months/years, we compress this for visualization
        // Reduced scaling for better numerical stability
        return strength * 100; // Reduced scale factor for smoother operation
    }
}