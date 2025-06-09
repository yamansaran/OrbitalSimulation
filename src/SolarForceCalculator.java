/**
 * SolarForceCalculator class handles solar gravitational perturbations
 * Separated from Satellite class for better code organization and modularity
 */
public class SolarForceCalculator {
    private static final double SUN_MASS = 1.989e30; // Sun's mass in kg
    private static final double SUN_EARTH_DISTANCE = 149597870700.0; // Average Sun-Earth distance in meters (1 AU)
    
    /**
     * Applies solar gravitational perturbations to satellite orbital elements
     * 
     * This method simulates the Sun's gravitational influence on the satellite's orbit.
     * Solar effects are generally weaker than lunar effects but occur over longer periods.
     * Most significant for high-altitude satellites and highly eccentric orbits.
     * 
     * @param satellite The satellite object to apply perturbations to
     * @param deltaTime Time step in seconds
     * @param simulation Reference to main simulation for Sun position
     */
    public void applyPerturbations(Satellite satellite, double deltaTime, OrbitalSimulation simulation) {
        // Get current Sun position relative to Earth
        double[] sunPos = simulation.getSunPosition();
        double sunX = sunPos[0];
        double sunY = sunPos[1];
        double sunZ = 0; // Assume Sun stays in Earth's equatorial plane for simplification
        
        // Get current satellite position
        double[] satPos = satellite.getPosition3D(); // Get 3D position including Z component
        double satX = satPos[0];
        double satY = satPos[1];
        double satZ = satPos[2];
        
        // Calculate distance vectors
        double sunDistance = Math.sqrt(sunX*sunX + sunY*sunY + sunZ*sunZ);
        double satDistance = Math.sqrt(satX*satX + satY*satY + satZ*satZ);
        
        // Distance from satellite to Sun
        double satSunDx = satX - sunX;
        double satSunDy = satY - sunY;
        double satSunDz = satZ - sunZ;
        double satSunDistance = Math.sqrt(satSunDx*satSunDx + satSunDy*satSunDy + satSunDz*satSunDz);
        
        // Avoid division by zero and unrealistic scenarios
        if (satSunDistance < 10000000 || sunDistance < 100000000) return; // 10,000 km and 100,000 km minimums
        
        // Calculate solar perturbation strength
        double perturbationStrength = calculateSolarPerturbationStrength(satDistance, satSunDistance, satellite.getEarthMass());
        
        // Scale perturbations based on time step to maintain stability
        double timeStepScale = Math.min(1.0, deltaTime / 1.0); // Normalize to 1-second steps
        double scaledStrength = perturbationStrength * timeStepScale;
        
        // Get current orbital elements
        double i = satellite.getInclination();
        double e = satellite.getEccentricity();
        double nu = satellite.getTrueAnomaly();
        
        // Apply solar perturbations to orbital elements
        // Solar effects are generally weaker but more long-term than lunar effects
        
        // 1. Longitude of Ascending Node precession (very slow)
        // Solar perturbations cause gradual nodal precession
        double solarNodePrecessRate = scaledStrength * 2e-9 * Math.cos(i) * deltaTime; // Much smaller than lunar
        satellite.adjustLongitudeOfAscendingNode(solarNodePrecessRate);
        
        // 2. Argument of periapsis rotation (secular variations)
        // The apsidal line slowly rotates due to solar influence
        double solarPeriapsisRotationRate = scaledStrength * 1e-9 * (1 - e*e) * deltaTime;
        satellite.adjustArgumentOfPeriapsis(solarPeriapsisRotationRate);
        
        // 3. Eccentricity variations (most significant solar effect)
        // Solar gravity can pump or damp orbital eccentricity over long periods
        double eccentricityPumping = scaledStrength * 2e-11 * Math.cos(2 * nu) * deltaTime;
        satellite.adjustEccentricity(eccentricityPumping);
        
        // 4. Inclination variations (long-term secular changes)
        // Solar perturbations can cause slow inclination changes
        double inclinationVariation = scaledStrength * 5e-11 * Math.sin(nu) * deltaTime;
        satellite.adjustInclination(inclinationVariation);
        
        // 5. Semi-major axis variations (energy changes)
        // Solar gravity can cause very small orbital energy changes
        double solarSmaVariation = scaledStrength * 1e-5 * Math.sin(2 * nu) * deltaTime;
        satellite.adjustSemiMajorAxis(solarSmaVariation);
    }
    
    /**
     * Calculates the strength of solar perturbations
     * Solar effects are weaker than lunar but more significant for high-altitude satellites
     * 
     * @param satDistance Distance from Earth center to satellite
     * @param satSunDistance Distance from satellite to Sun
     * @param earthMass Mass of Earth for ratio calculations
     * @return Solar perturbation strength factor
     */
    private double calculateSolarPerturbationStrength(double satDistance, double satSunDistance, double earthMass) {
        // Solar perturbation strength calculation
        double earthSunMassRatio = SUN_MASS / earthMass; // ~333,000 (much larger than Moon ratio)
        double distanceRatio = satDistance / SUN_EARTH_DISTANCE; // Satellite distance relative to Sun orbit
        double proximityFactor = 1.0 / Math.max(0.5, satSunDistance / SUN_EARTH_DISTANCE); // How close satellite is to Sun
        
        // Apply smoothing to prevent sudden changes
        double minProximity = 0.5; // Minimum proximity factor
        double maxProximity = 2.0; // Maximum proximity factor (Sun is much farther than Moon)
        proximityFactor = Math.max(minProximity, Math.min(maxProximity, proximityFactor));
        
        // Solar perturbations are stronger for:
        // - Higher altitude satellites (larger distanceRatio)
        // - When satellite is closer to Sun (larger proximityFactor)
        // But the Sun is much farther away, so effects are generally weaker
        double strength = earthSunMassRatio * distanceRatio * proximityFactor;
        
        // Apply much smaller scaling factor than lunar effects
        // Solar effects are primarily long-term secular changes
        return strength * 0.001; // Much smaller scale factor than lunar (1000x smaller)
    }
}