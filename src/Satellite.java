/**
 * Satellite class that handles orbital mechanics calculations
 * Implements Kepler's laws and coordinate transformations
 * Enhanced with lunar and solar gravitational perturbations using separate force calculators
 * Modified to use OrbitTransforms class for coordinate transformations
 */
public class Satellite {
    private double a, e, i, omega, Omega; // Orbital elements
    private double nu; // True anomaly (current position)
    private double meanMotion; // Mean motion (radians per second)
    private double gravitationalConstant;
    private double earthMass;
    
    // === NEW: Lunar and Solar effects system with separate calculators ===
    private boolean lunarEffectsEnabled;
    private boolean solarEffectsEnabled;
    private OrbitalSimulation simulation; // Reference to get Moon/Sun position and time
    private LunarForceCalculator lunarCalculator; // Separate lunar force calculator
    private SolarForceCalculator solarCalculator; // Separate solar force calculator
    
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
                    boolean solarEffectsEnabled, OrbitalSimulation simulation) {
        this.a = semiMajorAxis;
        this.e = eccentricity;
        this.i = Math.toRadians(inclination);
        this.omega = Math.toRadians(argumentOfPeriapsis);
        this.Omega = Math.toRadians(longitudeOfAscendingNode);
        this.nu = Math.toRadians(trueAnomaly);
        this.gravitationalConstant = gravitationalConstant;
        this.earthMass = earthMass;
        this.lunarEffectsEnabled = lunarEffectsEnabled;
        this.solarEffectsEnabled = solarEffectsEnabled;
        this.simulation = simulation;
        
        // Initialize force calculators
        this.lunarCalculator = new LunarForceCalculator();
        this.solarCalculator = new SolarForceCalculator();
        
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
        
        // === Apply lunar and solar perturbations if enabled (before updating position) ===
        if ((lunarEffectsEnabled || solarEffectsEnabled) && simulation != null) {
            if (lunarEffectsEnabled) {
                lunarCalculator.applyPerturbations(this, deltaTime, simulation);
            }
            if (solarEffectsEnabled) {
                solarCalculator.applyPerturbations(this, deltaTime, simulation);
            }
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
     * === NEW: Getter methods for orbital elements (needed by force calculators) ===
     */
    public double getInclination() { return i; }
    public double getEccentricity() { return e; }
    public double getEarthMass() { return earthMass; }
    
    /**
     * === NEW: Adjustment methods for orbital elements (used by force calculators) ===
     */
    public void adjustLongitudeOfAscendingNode(double delta) {
        deltaOmegaCapital += delta;
        Omega += delta;
    }
    
    public void adjustArgumentOfPeriapsis(double delta) {
        deltaOmega += delta;
        omega += delta;
    }
    
    public void adjustInclination(double delta) {
        deltaI += delta;
        i += delta;
    }
    
    public void adjustSemiMajorAxis(double delta) {
        a += delta;
    }
    
    public void adjustEccentricity(double delta) {
        e += delta;
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
     * Gets current satellite position in 2D coordinates (now using OrbitTransforms)
     */
    public double[] getPosition() {
        return OrbitTransforms.to2D(getPosition3D());
    }
    
    /**
     * === NEW: Gets current satellite position in 3D coordinates (now using OrbitTransforms) ===
     * This is needed for lunar perturbation calculations
     */
    public double[] getPosition3D() {
        // Calculate orbital radius using OrbitTransforms
        double r = OrbitTransforms.getOrbitalRadius(a, e, nu);
        
        // Get orbital plane coordinates using OrbitTransforms
        double[] orbitalCoords = OrbitTransforms.polarToOrbitalCartesian(r, nu);
        
        // Transform to Earth-centered coordinates using OrbitTransforms
        return OrbitTransforms.orbitalToEarthCentered(orbitalCoords[0], orbitalCoords[1], omega, i, Omega);
    }
    
    /**
     * Gets current orbital velocity
     */
    public double getVelocity() {
        double r = OrbitTransforms.getOrbitalRadius(a, e, nu);
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