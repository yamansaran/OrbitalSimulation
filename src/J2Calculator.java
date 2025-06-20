/**
 * J2Calculator class handles Earth's oblateness (J2) perturbations
 * J2 is the most significant gravitational perturbation for most satellites
 * Separated from Satellite class for better code organization and modularity
 */
public class J2Calculator {
    // Earth's J2 coefficient (dimensionless)
    private static final double J2_EARTH = 1.08263e-3; // Earth's second zonal harmonic coefficient
    
    // J2 can vary by celestial body - these are some examples
    private static final double J2_MARS = 1.956e-3;
    private static final double J2_JUPITER = 1.469e-2;
    private static final double J2_SATURN = 1.633e-2;
    
    /**
     * Applies J2 oblateness perturbations to satellite orbital elements
     * 
     * J2 perturbations are caused by Earth's oblate shape (flattened at poles).
     * These are typically the largest orbital perturbations for most satellites.
     * 
     * Main effects:
     * - Longitude of ascending node precession (nodal regression)
     * - Argument of periapsis rotation (apsidal precession)  
     * - No change to semi-major axis, eccentricity, or inclination (secular terms)
     * 
     * @param satellite The satellite object to apply perturbations to
     * @param deltaTime Time step in seconds
     * @param simulation Reference to main simulation for parameters
     */
    public void applyPerturbations(Satellite satellite, double deltaTime, OrbitalSimulation simulation) {
        // Get current orbital elements
        double a = satellite.getSemiMajorAxis(); // Semi-major axis
        double e = satellite.getEccentricity();   // Eccentricity
        double i = satellite.getInclination();   // Inclination (in radians)
        
        // Get celestial body parameters
        double earthRadius = simulation.getEarthRadius();
        double mu = simulation.getGravitationalConstant() * simulation.getEarthMass();
        
        // Select appropriate J2 coefficient based on celestial body
        double j2 = getJ2Coefficient(simulation.getCurrentBody());
        
        // Calculate mean motion (average angular velocity)
        double n = Math.sqrt(mu / (a * a * a)); // radians per second
        
        // Calculate J2 perturbation rates using analytical formulas
        // These are the standard formulas from orbital mechanics textbooks
        
        // 1. Longitude of Ascending Node precession rate (Ω̇)
        // Most significant J2 effect - causes orbital plane to precess
        double factor = -1.5 * j2 * (earthRadius * earthRadius) * n / (a * a * (1 - e*e) * (1 - e*e));
        double omegaCapitalDot = factor * Math.cos(i); // radians per second
        
        // 2. Argument of Periapsis rotation rate (ω̇)  
        // Causes the ellipse orientation within the orbital plane to rotate
        double omegaDot = factor * (2.5 * Math.sin(i) * Math.sin(i) - 2.0); // radians per second
        
        // Apply perturbations over the time step
        // Scale by time step to get actual angle changes
        double deltaOmegaCapital = omegaCapitalDot * deltaTime;
        double deltaOmega = omegaDot * deltaTime;
        
        // Apply stability limiting to prevent numerical instability
        // J2 effects are typically small but can accumulate
        double maxAngleChange = Math.toRadians(0.01); // Maximum 0.01 degree change per time step
        deltaOmegaCapital = Math.max(-maxAngleChange, Math.min(maxAngleChange, deltaOmegaCapital));
        deltaOmega = Math.max(-maxAngleChange, Math.min(maxAngleChange, deltaOmega));
        
        // Apply the perturbations to the satellite's orbital elements
        satellite.adjustLongitudeOfAscendingNode(deltaOmegaCapital);
        satellite.adjustArgumentOfPeriapsis(deltaOmega);
        
        // Note: J2 does not cause secular changes to a, e, or i in the averaged model
        // Any changes to these elements are periodic and average to zero over long periods
    }
    
    /**
     * Returns the appropriate J2 coefficient for the given celestial body
     * 
     * @param bodyName Name of the celestial body
     * @return J2 coefficient for the body
     */
    private double getJ2Coefficient(String bodyName) {
        switch (bodyName.toLowerCase()) {
            case "earth":
                return J2_EARTH;
            case "mars":
                return J2_MARS;
            case "jupiter":
                return J2_JUPITER;
            case "saturn":
                return J2_SATURN;
            case "moon":
                return 2.033e-4; // Moon's J2 (very small)
            case "venus":
                return 4.458e-6; // Venus is nearly spherical
            case "sun":
                return 2.0e-7;   // Sun is very nearly spherical
            default:
                // For fictional bodies or unknown bodies, use Earth's value as default
                return J2_EARTH;
        }
    }
    
    /**
     * Calculates the current J2 perturbation rates for display purposes
     * Returns the magnitude of nodal precession and apsidal precession rates
     * 
     * @param satellite The satellite object
     * @param simulation Reference to main simulation
     * @return Array containing [nodal_precession_rate, apsidal_precession_rate] in degrees per day
     */
    public double[] getCurrentJ2Rates(Satellite satellite, OrbitalSimulation simulation) {
        // Get current orbital elements
        double a = satellite.getSemiMajorAxis();
        double e = satellite.getEccentricity();
        double i = satellite.getInclination();
        
        // Get celestial body parameters
        double earthRadius = simulation.getEarthRadius();
        double mu = simulation.getGravitationalConstant() * simulation.getEarthMass();
        double j2 = getJ2Coefficient(simulation.getCurrentBody());
        
        // Calculate mean motion
        double n = Math.sqrt(mu / (a * a * a));
        
        // Calculate J2 perturbation rates (radians per second)
        double factor = -1.5 * j2 * (earthRadius * earthRadius) * n / (a * a * (1 - e*e) * (1 - e*e));
        double omegaCapitalDot = factor * Math.cos(i); // Nodal precession rate
        double omegaDot = factor * (2.5 * Math.sin(i) * Math.sin(i) - 2.0); // Apsidal precession rate
        
        // Convert from radians per second to degrees per day for display
        double secondsPerDay = 86400.0;
        double nodalPrecessionDegreesPerDay = Math.toDegrees(omegaCapitalDot) * secondsPerDay;
        double apsidaLPrecessionDegreesPerDay = Math.toDegrees(omegaDot) * secondsPerDay;
        
        return new double[]{nodalPrecessionDegreesPerDay, apsidaLPrecessionDegreesPerDay};
    }
    
    /**
     * Calculates the magnitude of J2 acceleration for visualization purposes
     * This gives an idea of how strong the J2 perturbation is compared to other forces
     * 
     * @param satellite The satellite object
     * @param simulation Reference to main simulation
     * @return J2 acceleration magnitude in m/s²
     */
    public double getCurrentJ2Acceleration(Satellite satellite, OrbitalSimulation simulation) {
        // Get satellite position
        double[] pos3D = satellite.getPosition3D();
        double x = pos3D[0];
        double y = pos3D[1];
        double z = pos3D[2];
        
        double r = Math.sqrt(x*x + y*y + z*z); // Distance from Earth center
        
        // Get celestial body parameters
        double earthRadius = simulation.getEarthRadius();
        double mu = simulation.getGravitationalConstant() * simulation.getEarthMass();
        double j2 = getJ2Coefficient(simulation.getCurrentBody());
        
        // Calculate J2 acceleration magnitude using the J2 potential
        // The J2 acceleration is proportional to the gradient of the J2 potential
        
        // J2 potential: U_J2 = -(μ*J2*Re²)/(2*r³) * (3*sin²φ - 1)
        // where φ is the latitude (angle from equatorial plane)
        
        double latitude = Math.asin(z / r); // Satellite latitude
        double latitudeTerm = 3 * Math.sin(latitude) * Math.sin(latitude) - 1;
        
        // Approximate J2 acceleration magnitude
        double j2AccelMagnitude = (mu * j2 * earthRadius * earthRadius) / (r * r * r * r) * Math.abs(latitudeTerm);
        
        return j2AccelMagnitude;
    }
    
    /**
     * Determines if J2 effects are significant for the current orbit
     * Helps users understand when J2 effects are important vs negligible
     * 
     * @param satellite The satellite object
     * @param simulation Reference to main simulation
     * @return true if J2 effects are significant (> 0.1 deg/year precession)
     */
    public boolean areJ2EffectsSignificant(Satellite satellite, OrbitalSimulation simulation) {
        double[] rates = getCurrentJ2Rates(satellite, simulation);
        double nodalPrecessionPerYear = Math.abs(rates[0]) * 365.25; // degrees per year
        double apsidaLPrecessionPerYear = Math.abs(rates[1]) * 365.25; // degrees per year
        
        // Consider significant if either precession rate exceeds 0.1 degrees per year
        return (nodalPrecessionPerYear > 0.1) || (apsidaLPrecessionPerYear > 0.1);
    }
    
    /**
     * Provides a description of the J2 effects for the current orbit
     * Useful for educational purposes and user understanding
     * 
     * @param satellite The satellite object
     * @param simulation Reference to main simulation
     * @return Description string explaining the J2 effects
     */
    public String getJ2EffectsDescription(Satellite satellite, OrbitalSimulation simulation) {
        double[] rates = getCurrentJ2Rates(satellite, simulation);
        double i = Math.toDegrees(satellite.getInclination());
        double j2 = getJ2Coefficient(simulation.getCurrentBody()); // Get J2 coefficient for calculations
        
        StringBuilder description = new StringBuilder();
        description.append("J2 Oblateness Effects:\n");
        description.append(String.format("• Nodal precession: %.3f°/day\n", rates[0]));
        description.append(String.format("• Apsidal precession: %.3f°/day\n", rates[1]));
        
        // Add interpretation based on orbit characteristics
        if (Math.abs(i - 90) < 1) {
            description.append("• Polar orbit: Maximum nodal precession\n");
        } else if (Math.abs(i) < 1) {
            description.append("• Equatorial orbit: No nodal precession\n");
        } else if (i > 90) {
            description.append("• Retrograde orbit: Eastward nodal drift\n");
        } else {
            description.append("• Prograde orbit: Westward nodal drift\n");
        }
        
        // Special case: Sun-synchronous orbits (only calculate if J2 is significant)
        if (j2 > 1e-6) { // Only for bodies with significant oblateness
            try {
                double earthRadius = simulation.getEarthRadius();
                double semiMajorAxis = satellite.getSemiMajorAxis();
                double radiusRatio = earthRadius / semiMajorAxis;
                double sunSyncFactor = -2.0/3.0 * Math.pow(radiusRatio, -3.5) / j2;
                
                if (Math.abs(sunSyncFactor) <= 1.0) { // Check if acos argument is valid
                    double sunSyncInclination = Math.toDegrees(Math.acos(sunSyncFactor));
                    if (Math.abs(i - sunSyncInclination) < 0.5) {
                        description.append("• Near sun-synchronous inclination!\n");
                    }
                }
            } catch (Exception e) {
                // If calculation fails, skip sun-sync check
            }
        }
        
        return description.toString();
    }
}