/**
 * SATELLITE CLASS: Handles all orbital mechanics calculations
 * 
 * This class implements Kepler's laws and Newton's law of gravitation to
 * accurately simulate satellite motion around Earth. It solves Kepler's equation
 * and performs coordinate transformations from orbital plane to 3D space.
 */
public class Satellite {
    // Physical constants for orbital mechanics
    private static final double G = 6.67430e-11; // Gravitational constant in m³/kg⋅s²
    private static final double EARTH_MASS = 5.972e24; // Earth's mass in kg
    
    // ORBITAL ELEMENTS: Define the satellite's orbit
    private double a, e, i, omega, Omega, nu; // Classical orbital elements in SI units/radians
    private double meanMotion; // n = √(GM/a³) - average angular velocity
    private double time = 0; // Elapsed time since epoch (seconds)
    
    /**
     * Constructor: Initialize satellite with orbital parameters
     * 
     * @param semiMajorAxis Semi-major axis 'a' (meters) - defines orbit size
     * @param eccentricity Eccentricity 'e' (dimensionless) - defines orbit shape  
     * @param inclination Inclination 'i' (degrees) - orbital plane tilt
     * @param argPeriapsis Argument of periapsis 'ω' (degrees) - ellipse orientation
     * @param longAscNode Longitude of ascending node 'Ω' (degrees) - plane rotation
     * @param trueAnomaly True anomaly 'ν' (degrees) - position in orbit
     */
    public Satellite(double semiMajorAxis, double eccentricity, double inclination,
                    double argPeriapsis, double longAscNode, double trueAnomaly) {
        // Store orbital elements (convert angles to radians for calculations)
        this.a = semiMajorAxis;
        this.e = eccentricity;
        this.i = Math.toRadians(inclination);
        this.omega = Math.toRadians(argPeriapsis);
        this.Omega = Math.toRadians(longAscNode);
        this.nu = Math.toRadians(trueAnomaly);
        
        // Calculate mean motion using Kepler's third law: n = √(GM/a³)
        // This gives the average angular velocity of the satellite
        this.meanMotion = Math.sqrt(G * EARTH_MASS / (a * a * a));
    }
    
    /**
     * Updates satellite position by advancing time and solving orbital motion
     * 
     * Uses Kepler's equation to find the satellite's position at the new time.
     * This is the core of the orbital mechanics simulation.
     * 
     * @param deltaTime Time step to advance (seconds)
     */
    public void updatePosition(double deltaTime) {
        time += deltaTime; // Advance simulation time
        
        // Calculate mean anomaly: M = n⋅t (average angular position)
        // Mean anomaly increases linearly with time
        double M = meanMotion * time;
        
        // Solve Kepler's equation: M = E - e⋅sin(E) for eccentric anomaly E
        // This transcendental equation requires iterative solution
        double E = solveKeplersEquation(M, e);
        
        // Calculate true anomaly from eccentric anomaly
        // True anomaly is the actual angular position in the orbit
        // Formula: ν = 2⋅arctan(√((1+e)/(1-e)) ⋅ tan(E/2))
        nu = 2 * Math.atan2(Math.sqrt(1 + e) * Math.sin(E / 2),
                           Math.sqrt(1 - e) * Math.cos(E / 2));
    }
    
    /**
     * Solves Kepler's equation using Newton's method iteration
     * 
     * Kepler's equation M = E - e⋅sin(E) cannot be solved analytically,
     * so we use iterative numerical methods to find E given M and e.
     * 
     * @param M Mean anomaly (radians)
     * @param e Eccentricity (dimensionless)
     * @return Eccentric anomaly E (radians)
     */
    private double solveKeplersEquation(double M, double e) {
        double E = M; // Initial guess: E₀ = M
        
        // Newton's method iteration: E(n+1) = E(n) - f(E(n))/f'(E(n))
        // For Kepler's equation: E(n+1) = E(n) - (E(n) - e⋅sin(E(n)) - M)/(1 - e⋅cos(E(n)))
        // Simplified form: E(n+1) = M + e⋅sin(E(n))
        for (int i = 0; i < 10; i++) {
            E = M + e * Math.sin(E);
        }
        return E;
    }
    
    /**
     * Calculates satellite's current position in 3D Cartesian coordinates
     * 
     * Transforms from orbital plane coordinates to Earth-centered coordinates
     * using rotation matrices for inclination, argument of periapsis, and
     * longitude of ascending node.
     * 
     * @return Array containing [x, y] coordinates in meters
     */
    public double[] getPosition() {
        // STEP 1: Calculate position in orbital plane using polar coordinates
        // r = a(1-e²)/(1+e⋅cos(ν)) - orbital radius equation
        double r = a * (1 - e * e) / (1 + e * Math.cos(nu));
        
        // Convert to Cartesian coordinates in orbital plane
        double x_orb = r * Math.cos(nu); // X in orbital plane
        double y_orb = r * Math.sin(nu); // Y in orbital plane
        
        // STEP 2: Transform from orbital plane to 3D space using rotation matrices
        // Apply three rotations: Ω (ascending node), i (inclination), ω (periapsis)
        
        // Pre-calculate trigonometric values for efficiency
        double cosOmega = Math.cos(omega);    // cos(ω)
        double sinOmega = Math.sin(omega);    // sin(ω)  
        double cosOmega_cap = Math.cos(Omega); // cos(Ω)
        double sinOmega_cap = Math.sin(Omega); // sin(Ω)
        double cosi = Math.cos(i);            // cos(i)
        double sini = Math.sin(i);            // sin(i)
        
        // Apply rotation matrix transformation
        // This combines all three rotations into final 3D coordinates
        double x = x_orb * (cosOmega * cosOmega_cap - sinOmega * sinOmega_cap * cosi) -
                  y_orb * (sinOmega * cosOmega_cap + cosOmega * sinOmega_cap * cosi);
        
        double y = x_orb * (cosOmega * sinOmega_cap + sinOmega * cosOmega_cap * cosi) -
                  y_orb * (sinOmega * sinOmega_cap - cosOmega * cosOmega_cap * cosi);
        
        return new double[]{x, y}; // Return 2D projection for display
    }
    
    /**
     * Calculates current orbital velocity using vis-viva equation
     * 
     * The vis-viva equation relates orbital velocity to position and orbit size:
     * v = √(GM(2/r - 1/a))
     * 
     * @return Current velocity magnitude (m/s)
     */
    public double getVelocity() {
        // Calculate current distance from Earth center
        double r = a * (1 - e * e) / (1 + e * Math.cos(nu));
        
        // Apply vis-viva equation: v² = GM(2/r - 1/a)
        return Math.sqrt(G * EARTH_MASS * (2 / r - 1 / a));
    }
    
    /**
     * Calculates orbital period using Kepler's third law
     * 
     * Kepler's third law: T² = (4π²/GM)⋅a³
     * Therefore: T = 2π⋅√(a³/GM)
     * 
     * @return Orbital period (seconds)
     */
    public double getOrbitalPeriod() {
        return 2 * Math.PI * Math.sqrt(a * a * a / (G * EARTH_MASS));
    }
    
    /**
     * Returns current true anomaly for display purposes
     * 
     * @return True anomaly (radians)
     */
    public double getTrueAnomaly() {
        return nu;
    }
}