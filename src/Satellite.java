/**
 * Satellite class that handles orbital mechanics calculations
 * Implements Kepler's laws and coordinate transformations
 * Enhanced with lunar gravitational perturbations
 */
public class Satellite {
    private double a, e, i, omega, Omega; // Orbital elements
    private double nu; // True anomaly (current position)
    private double meanMotion; // Mean motion (radians per second)
    private double gravitationalConstant;
    private double earthMass;
    
    // === NEW: Lunar effects system ===
    private boolean lunarEffectsEnabled;
    private OrbitalSimulation simulation; // Reference to get Moon position and time
    private static final double MOON_MASS = 7.342e22; // Moon's mass in kg
    private static final double MOON_EARTH_DISTANCE = 384400000; // Average Moon-Earth distance in meters
    
    // Perturbation tracking for orbital element drift
    private double deltaOmega = 0; // Accumulated change in argument of periapsis
    private double deltaOmegaCapital = 0; // Accumulated change in longitude of ascending node
    private double deltaI = 0; // Accumulated change in inclination
    
    /**
     * Constructor: Initialize satellite with orbital elements
     */
    public Satellite(double semiMajorAxis, double eccentricity, double inclination,
                    double argumentOfPeriapsis, double longitudeOfAscendingNode, double trueAnomaly,
                    double gravitationalConstant, double earthMass, boolean lunarEffectsEnabled, 
                    OrbitalSimulation simulation) {
        this.a = semiMajorAxis;
        this.e = eccentricity;
        this.i = Math.toRadians(inclination);
        this.omega = Math.toRadians(argumentOfPeriapsis);
        this.Omega = Math.toRadians(longitudeOfAscendingNode);
        this.nu = Math.toRadians(trueAnomaly);
        this.gravitationalConstant = gravitationalConstant;
        this.earthMass = earthMass;
        this.lunarEffectsEnabled = lunarEffectsEnabled;
        this.simulation = simulation;
        
        // Calculate mean motion using Kepler's third law: n = √(μ/a³)
        double mu = gravitationalConstant * earthMass; // Standard gravitational parameter
        this.meanMotion = Math.sqrt(mu / (a * a * a));
    }
    
    /**
     * Updates satellite position by advancing time
     * Now includes lunar gravitational perturbations with enhanced adaptive integration
     * Optimized for extreme speeds up to 100,000x
     */
    public void updatePosition(double deltaTime) {
        // Use enhanced adaptive integration for numerical stability at extreme speeds
        // Dynamically adjust sub-step size based on the magnitude of the time step
        double maxSubStep;
        if (deltaTime > 3600) { // More than 1 hour
            maxSubStep = 5.0; // 5 seconds for very large time steps
        } else if (deltaTime > 600) { // More than 10 minutes  
            maxSubStep = 10.0; // 10 seconds for large time steps
        } else if (deltaTime > 60) { // More than 1 minute
            maxSubStep = 15.0; // 15 seconds for medium time steps
        } else {
            maxSubStep = deltaTime; // Use full time step for small steps
        }
        
        int numSubSteps = Math.max(1, (int)Math.ceil(deltaTime / maxSubStep));
        double subStepSize = deltaTime / numSubSteps;
        
        // Perform integration in multiple smaller steps
        for (int step = 0; step < numSubSteps; step++) {
            updateSingleStep(subStepSize);
        }
    }
    
    /**
     * === NEW: Performs a single integration step ===
     * Separated for cleaner adaptive time stepping
     */
    private void updateSingleStep(double deltaTime) {
        // Store original orbital elements for perturbation calculations
        double originalOmega = omega;
        double originalOmegaCapital = Omega;
        double originalI = i;
        double originalA = a;
        double originalE = e;
        
        // Convert true anomaly to mean anomaly
        double E = trueToEccentricAnomaly(nu, e); // Eccentric anomaly
        double M = E - e * Math.sin(E); // Mean anomaly from Kepler's equation
        
        // Advance mean anomaly by time step
        M += meanMotion * deltaTime;
        
        // === Apply lunar perturbations if enabled (before updating position) ===
        if (lunarEffectsEnabled && simulation != null) {
            applyLunarPerturbations(deltaTime);
        }
        
        // Convert back to true anomaly with updated orbital elements
        E = solveKeplersEquation(M, e); // Solve Kepler's equation iteratively
        nu = eccentricToTrueAnomaly(E, e); // Convert to true anomaly
        
        // Ensure numerical stability - check for unrealistic changes
        validateOrbitalElements(originalA, originalE, originalI, originalOmega, originalOmegaCapital);
    }
    
    /**
     * === NEW: Validates orbital elements to prevent numerical instability ===
     */
    private void validateOrbitalElements(double origA, double origE, double origI, double origOmega, double origOmegaCapital) {
        // Prevent extreme changes that could cause numerical instability
        double maxChangePercent = 0.01; // Maximum 1% change per step
        
        // Validate semi-major axis
        double maxAChange = origA * maxChangePercent;
        if (Math.abs(a - origA) > maxAChange) {
            a = origA + Math.signum(a - origA) * maxAChange;
        }
        
        // Validate eccentricity
        double maxEChange = 0.001; // Maximum eccentricity change per step
        if (Math.abs(e - origE) > maxEChange) {
            e = origE + Math.signum(e - origE) * maxEChange;
        }
        
        // Validate inclination
        double maxIChange = Math.toRadians(0.1); // Maximum 0.1 degree change per step
        if (Math.abs(i - origI) > maxIChange) {
            i = origI + Math.signum(i - origI) * maxIChange;
        }
        
        // Validate angles (allow larger changes as these are more naturally varying)
        double maxAngleChange = Math.toRadians(1.0); // Maximum 1 degree change per step
        if (Math.abs(omega - origOmega) > maxAngleChange) {
            // Handle angle wrapping
            double angleDiff = normalizeAngleDifference(omega - origOmega);
            if (Math.abs(angleDiff) > maxAngleChange) {
                omega = origOmega + Math.signum(angleDiff) * maxAngleChange;
            }
        }
        
        if (Math.abs(Omega - origOmegaCapital) > maxAngleChange) {
            // Handle angle wrapping
            double angleDiff = normalizeAngleDifference(Omega - origOmegaCapital);
            if (Math.abs(angleDiff) > maxAngleChange) {
                Omega = origOmegaCapital + Math.signum(angleDiff) * maxAngleChange;
            }
        }
        
        // Ensure orbital elements stay within physical bounds
        e = Math.max(0, Math.min(0.99, e)); // Eccentricity between 0 and 0.99
        i = Math.max(0, Math.min(Math.PI, i)); // Inclination between 0 and 180 degrees
        a = Math.max(simulation.getEarthRadius() * 1.01, a); // Semi-major axis above Earth's surface
        
        // Normalize angles to [0, 2π] range
        omega = normalizeAngle(omega);
        Omega = normalizeAngle(Omega);
        
        // Recalculate mean motion with validated semi-major axis
        double mu = gravitationalConstant * earthMass;
        meanMotion = Math.sqrt(mu / (a * a * a));
    }
    
    /**
     * === NEW: Normalizes angle difference to [-π, π] range ===
     * Handles angle wrapping correctly
     */
    private double normalizeAngleDifference(double angleDiff) {
        while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
        while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;
        return angleDiff;
    }
    
    /**
     * === NEW: Applies lunar gravitational perturbations to orbital elements ===
     * 
     * This method simulates the Moon's gravitational influence on the satellite's orbit.
     * The perturbations cause gradual changes in the orbital elements over time.
     * Now uses scaled perturbations for numerical stability at high speeds.
     * 
     * @param deltaTime Time step in seconds
     */
    private void applyLunarPerturbations(double deltaTime) {
        // Get current Moon position relative to Earth
        double[] moonPos = simulation.getMoonPosition();
        double moonX = moonPos[0];
        double moonY = moonPos[1];
        double moonZ = 0; // Assume Moon stays in Earth's equatorial plane for simplification
        
        // Get current satellite position
        double[] satPos = getPosition3D(); // Get 3D position including Z component
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
        double perturbationStrength = calculatePerturbationStrength(satDistance, satMoonDistance);
        
        // === NEW: Scale perturbations based on time step to maintain stability ===
        double timeStepScale = Math.min(1.0, deltaTime / 1.0); // Normalize to 1-second steps
        double scaledStrength = perturbationStrength * timeStepScale;
        
        // Apply perturbations to orbital elements with improved numerical stability
        // These are simplified models of how the Moon affects satellite orbits
        
        // 1. Longitude of Ascending Node precession (most significant effect)
        // The Moon causes the orbital plane to slowly rotate
        double nodePrecessRate = scaledStrength * 1e-8 * Math.cos(i) * deltaTime; // Reduced and scaled
        deltaOmegaCapital += nodePrecessRate;
        Omega += nodePrecessRate;
        
        // 2. Argument of periapsis rotation
        // The orientation of the ellipse within the orbital plane changes
        double periapsisRotationRate = scaledStrength * 5e-9 * (1 - e*e) * deltaTime; // Reduced and scaled
        deltaOmega += periapsisRotationRate;
        omega += periapsisRotationRate;
        
        // 3. Inclination oscillation (smaller effect)
        // The orbital plane tilt oscillates slightly
        double inclinationOscillation = scaledStrength * 1e-10 * Math.sin(2 * nu) * deltaTime; // Much reduced
        deltaI += inclinationOscillation;
        i += inclinationOscillation;
        
        // 4. Semi-major axis variation (very small effect)
        // The orbit size can change slightly due to energy transfer
        double semiMajorAxisVariation = scaledStrength * 1e-4 * Math.sin(nu) * deltaTime; // Reduced
        a += semiMajorAxisVariation;
        
        // 5. Eccentricity variation (very small effect)
        // The orbit shape can change slightly
        double eccentricityVariation = scaledStrength * 1e-11 * Math.cos(nu) * deltaTime; // Much reduced
        e += eccentricityVariation;
        
        // Note: Bounds checking and normalization is now handled in validateOrbitalElements()
    }
    
    /**
     * === NEW: Calculates the strength of lunar perturbations ===
     * Now includes smoothing for numerical stability
     * 
     * @param satDistance Distance from Earth center to satellite
     * @param satMoonDistance Distance from satellite to Moon
     * @return Perturbation strength factor
     */
    private double calculatePerturbationStrength(double satDistance, double satMoonDistance) {
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
    
    /**
     * === NEW: Normalizes angle to [0, 2π] range ===
     */
    private double normalizeAngle(double angle) {
        while (angle < 0) angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI) angle -= 2 * Math.PI;
        return angle;
    }
    
    /**
     * Gets current satellite position in 2D coordinates
     */
    public double[] getPosition() {
        double[] pos3D = getPosition3D();
        return new double[]{pos3D[0], pos3D[1]}; // Return only X and Y
    }
    
    /**
     * === NEW: Gets current satellite position in 3D coordinates ===
     * This is needed for lunar perturbation calculations
     */
    public double[] getPosition3D() {
        // Calculate position in orbital plane
        double r = a * (1 - e * e) / (1 + e * Math.cos(nu)); // Orbital radius
        double x_orbital = r * Math.cos(nu); // X in orbital plane
        double y_orbital = r * Math.sin(nu); // Y in orbital plane
        
        // Transform to Earth-centered coordinates using rotation matrices
        // Apply argument of periapsis rotation
        double x1 = x_orbital * Math.cos(omega) - y_orbital * Math.sin(omega);
        double y1 = x_orbital * Math.sin(omega) + y_orbital * Math.cos(omega);
        double z1 = 0; // Still in orbital plane
        
        // Apply inclination rotation
        double x2 = x1;
        double y2 = y1 * Math.cos(i) - z1 * Math.sin(i);
        double z2 = y1 * Math.sin(i) + z1 * Math.cos(i);
        
        // Apply longitude of ascending node rotation
        double x3 = x2 * Math.cos(Omega) - y2 * Math.sin(Omega);
        double y3 = x2 * Math.sin(Omega) + y2 * Math.cos(Omega);
        double z3 = z2;
        
        return new double[]{x3, y3, z3}; // Return 3D position
    }
    
    /**
     * Gets current orbital velocity
     */
    public double getVelocity() {
        double r = a * (1 - e * e) / (1 + e * Math.cos(nu));
        double mu = gravitationalConstant * earthMass;
        return Math.sqrt(mu * (2.0 / r - 1.0 / a)); // Vis-viva equation
    }
    
    /**
     * Gets orbital period in seconds
     */
    public double getOrbitalPeriod() {
        return 2 * Math.PI / meanMotion;
    }
    
    /**
     * Gets current true anomaly
     */
    public double getTrueAnomaly() {
        return nu;
    }
    
    /**
     * === NEW: Gets current orbital elements in degrees (for display) ===
     */
    public double[] getOrbitalElementsDegrees() {
        return new double[]{
            a / 1000, // Semi-major axis in km
            e, // Eccentricity
            Math.toDegrees(i), // Inclination in degrees
            Math.toDegrees(omega), // Argument of periapsis in degrees
            Math.toDegrees(Omega), // Longitude of ascending node in degrees
            Math.toDegrees(nu) // True anomaly in degrees
        };
    }
    
    /**
     * === NEW: Gets accumulated perturbations (for analysis) ===
     */
    public double[] getPerturbations() {
        return new double[]{
            Math.toDegrees(deltaOmega), // Change in argument of periapsis (degrees)
            Math.toDegrees(deltaOmegaCapital), // Change in longitude of ascending node (degrees)
            Math.toDegrees(deltaI) // Change in inclination (degrees)
        };
    }
    
    /**
     * Converts true anomaly to eccentric anomaly
     */
    private double trueToEccentricAnomaly(double trueAnomaly, double eccentricity) {
        double cosE = (eccentricity + Math.cos(trueAnomaly)) / (1 + eccentricity * Math.cos(trueAnomaly));
        double sinE = Math.sqrt(1 - eccentricity * eccentricity) * Math.sin(trueAnomaly) / 
                     (1 + eccentricity * Math.cos(trueAnomaly));
        return Math.atan2(sinE, cosE);
    }
    
    /**
     * Converts eccentric anomaly to true anomaly
     */
    private double eccentricToTrueAnomaly(double eccentricAnomaly, double eccentricity) {
        double cosNu = (Math.cos(eccentricAnomaly) - eccentricity) / (1 - eccentricity * Math.cos(eccentricAnomaly));
        double sinNu = Math.sqrt(1 - eccentricity * eccentricity) * Math.sin(eccentricAnomaly) / 
                      (1 - eccentricity * Math.cos(eccentricAnomaly));
        return Math.atan2(sinNu, cosNu);
    }
    
    /**
     * Solves Kepler's equation M = E - e*sin(E) for eccentric anomaly E
     * Uses Newton-Raphson iteration for numerical solution
     */
    private double solveKeplersEquation(double meanAnomaly, double eccentricity) {
        double E = meanAnomaly; // Initial guess
        double tolerance = 1e-10;
        int maxIterations = 100;
        
        for (int i = 0; i < maxIterations; i++) {
            double f = E - eccentricity * Math.sin(E) - meanAnomaly; // Function
            double fp = 1 - eccentricity * Math.cos(E); // Derivative
            double deltaE = f / fp; // Newton-Raphson step
            E -= deltaE;
            
            if (Math.abs(deltaE) < tolerance) {
                break; // Converged
            }
        }
        
        return E;
    }
}