/**
 * AtmosphericDragCalculator class handles atmospheric drag perturbations
 * Separated from Satellite class for better code organization and modularity
 */
public class AtmosphericDragCalculator {
    private static final double DRAG_COEFFICIENT = 2.2; // Given CD value
    private static final double SATELLITE_CROSS_SECTIONAL_AREA = 10.0; // Assumed 10 m² cross-sectional area
    private static final double SATELLITE_MASS = 1000.0; // Assumed 1000 kg satellite mass
    
    // Atmospheric model parameters (simplified exponential model)
    private static final double SEA_LEVEL_DENSITY = 1.225; // kg/m³ at sea level
    private static final double SCALE_HEIGHT = 8500.0; // Atmospheric scale height in meters
    private static final double MIN_ALTITUDE_FOR_DRAG = 80000.0; // 80 km - minimum altitude where drag is significant
    private static final double MAX_ALTITUDE_FOR_DRAG = 1000000.0; // 1000 km - maximum altitude where drag occurs
    
    /**
     * Applies atmospheric drag perturbations to satellite orbital elements
     * 
     * This method simulates atmospheric drag effects on the satellite's orbit.
     * Drag always opposes the satellite's velocity direction and causes orbital decay.
     * Most significant for low-altitude satellites (LEO).
     * 
     * @param satellite The satellite object to apply perturbations to
     * @param deltaTime Time step in seconds
     * @param simulation Reference to main simulation for Earth radius and other parameters
     */
    public void applyPerturbations(Satellite satellite, double deltaTime, OrbitalSimulation simulation) {
        // Get current satellite position and velocity
        double[] satPos3D = satellite.getPosition3D();
        double satX = satPos3D[0];
        double satY = satPos3D[1];
        double satZ = satPos3D[2];
        
        // Calculate altitude above Earth's surface
        double distanceFromCenter = Math.sqrt(satX*satX + satY*satY + satZ*satZ);
        double altitude = distanceFromCenter - simulation.getEarthRadius();
        
        // Only apply drag if satellite is within atmosphere
        if (altitude < MIN_ALTITUDE_FOR_DRAG || altitude > MAX_ALTITUDE_FOR_DRAG) {
            return; // No drag outside atmosphere or too low
        }
        
        // Calculate atmospheric density at current altitude using exponential model
        double density = calculateAtmosphericDensity(altitude);
        if (density <= 0) return;
        
        // Get satellite velocity
        double velocity = satellite.getVelocity();
        if (velocity <= 0) return;
        
        // Calculate drag force magnitude: F_drag = 0.5 * ρ * v² * CD * A
        double dragForceMagnitude = 0.5 * density * velocity * velocity * DRAG_COEFFICIENT * SATELLITE_CROSS_SECTIONAL_AREA;
        
        // Calculate drag acceleration magnitude: a_drag = F_drag / m
        double dragAcceleration = dragForceMagnitude / SATELLITE_MASS;
        
        // Scale perturbations based on time step to maintain stability
        double timeStepScale = Math.min(1.0, deltaTime / 1.0); // Normalize to 1-second steps
        double scaledAcceleration = dragAcceleration * timeStepScale;
        
        // Get current orbital elements
        double i = satellite.getInclination();
        double e = satellite.getEccentricity();
        double nu = satellite.getTrueAnomaly();
        
        // Calculate drag perturbation strength (relative to other forces)
        double perturbationStrength = calculateDragPerturbationStrength(dragAcceleration, distanceFromCenter, satellite.getEarthMass());
        
        // Apply drag perturbations to orbital elements
        // Atmospheric drag primarily causes:
        // 1. Orbital decay (semi-major axis decrease)
        // 2. Eccentricity damping (orbit becomes more circular)
        // 3. Slight inclination changes due to atmospheric rotation effects
        
        // 1. Semi-major axis decay (most significant effect)
        // Drag always reduces orbital energy, causing the orbit to decay
        double semiMajorAxisDecay = -scaledAcceleration * 1e-3 * deltaTime; // Negative for decay
        satellite.adjustSemiMajorAxis(semiMajorAxisDecay);
        
        // 2. Eccentricity damping (drag circularizes orbits)
        // Higher velocity at perigee means more drag there, reducing eccentricity
        double eccentricityDamping = -perturbationStrength * 1e-9 * e * Math.abs(Math.cos(nu)) * deltaTime;
        satellite.adjustEccentricity(eccentricityDamping);
        
        // 3. Argument of periapsis rotation (small effect)
        // Differential drag around the orbit can rotate the apsidal line
        double periapsisRotation = perturbationStrength * 5e-11 * Math.sin(2 * nu) * deltaTime;
        satellite.adjustArgumentOfPeriapsis(periapsisRotation);
        
        // 4. Inclination changes due to atmospheric rotation (very small effect)
        // Earth's rotating atmosphere can cause small inclination changes
        double inclinationChange = perturbationStrength * 1e-12 * Math.sin(nu) * deltaTime;
        satellite.adjustInclination(inclinationChange);
        
        // 5. Longitude of ascending node precession (small effect from atmospheric rotation)
        double nodePrecessRate = perturbationStrength * 1e-12 * Math.cos(i) * deltaTime;
        satellite.adjustLongitudeOfAscendingNode(nodePrecessRate);
    }
    
    /**
     * Calculates atmospheric density at a given altitude using simplified exponential model
     * 
     * @param altitude Altitude above Earth's surface in meters
     * @return Atmospheric density in kg/m³
     */
    private double calculateAtmosphericDensity(double altitude) {
        if (altitude < 0) return SEA_LEVEL_DENSITY; // Below sea level
        if (altitude > MAX_ALTITUDE_FOR_DRAG) return 0; // Above atmosphere
        
        // Exponential atmosphere model: ρ(h) = ρ₀ * exp(-h/H)
        // where ρ₀ = sea level density, h = altitude, H = scale height
        return SEA_LEVEL_DENSITY * Math.exp(-altitude / SCALE_HEIGHT);
    }
    
    /**
     * Calculates the strength of atmospheric drag perturbations
     * 
     * @param dragAcceleration Magnitude of drag acceleration
     * @param satDistance Distance from Earth center to satellite
     * @param earthMass Mass of Earth for ratio calculations
     * @return Drag perturbation strength factor
     */
    private double calculateDragPerturbationStrength(double dragAcceleration, double satDistance, double earthMass) {
        // Drag perturbation strength based on acceleration magnitude and orbital characteristics
        double gravitationalAcceleration = 6.67430e-11 * earthMass / (satDistance * satDistance);
        
        // Ratio of drag acceleration to gravitational acceleration
        double accelerationRatio = dragAcceleration / gravitationalAcceleration;
        
        // Apply scaling factor to make effects visible in simulation timeframes
        return accelerationRatio * 1000; // Scale factor for visualization
    }
    
    /**
     * Calculates current drag acceleration for display purposes
     * 
     * @param satellite The satellite object
     * @param simulation Reference to main simulation
     * @return Drag acceleration magnitude in m/s²
     */
    public double getCurrentDragAcceleration(Satellite satellite, OrbitalSimulation simulation) {
        // Get current satellite position
        double[] satPos3D = satellite.getPosition3D();
        double satX = satPos3D[0];
        double satY = satPos3D[1];
        double satZ = satPos3D[2];
        
        // Calculate altitude above Earth's surface
        double distanceFromCenter = Math.sqrt(satX*satX + satY*satY + satZ*satZ);
        double altitude = distanceFromCenter - simulation.getEarthRadius();
        
        // Only calculate if satellite is within atmosphere
        if (altitude < MIN_ALTITUDE_FOR_DRAG || altitude > MAX_ALTITUDE_FOR_DRAG) {
            return 0; // No drag outside atmosphere
        }
        
        // Calculate atmospheric density at current altitude
        double density = calculateAtmosphericDensity(altitude);
        if (density <= 0) return 0;
        
        // Get satellite velocity
        double velocity = satellite.getVelocity();
        if (velocity <= 0) return 0;
        
        // Calculate drag force magnitude: F_drag = 0.5 * ρ * v² * CD * A
        double dragForceMagnitude = 0.5 * density * velocity * velocity * DRAG_COEFFICIENT * SATELLITE_CROSS_SECTIONAL_AREA;
        
        // Calculate drag acceleration magnitude: a_drag = F_drag / m
        return dragForceMagnitude / SATELLITE_MASS;
    }
}