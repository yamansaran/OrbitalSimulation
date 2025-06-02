/**
 * Satellite class that handles orbital mechanics calculations
 * Implements Kepler's laws and coordinate transformations
 */
public class Satellite {
    private double a, e, i, omega, Omega; // Orbital elements
    private double nu; // True anomaly (current position)
    private double meanMotion; // Mean motion (radians per second)
    private double gravitationalConstant;
    private double earthMass;
    
    /**
     * Constructor: Initialize satellite with orbital elements
     */
    public Satellite(double semiMajorAxis, double eccentricity, double inclination,
                    double argumentOfPeriapsis, double longitudeOfAscendingNode, double trueAnomaly,
                    double gravitationalConstant, double earthMass) {
        this.a = semiMajorAxis;
        this.e = eccentricity;
        this.i = Math.toRadians(inclination);
        this.omega = Math.toRadians(argumentOfPeriapsis);
        this.Omega = Math.toRadians(longitudeOfAscendingNode);
        this.nu = Math.toRadians(trueAnomaly);
        this.gravitationalConstant = gravitationalConstant;
        this.earthMass = earthMass;
        
        // Calculate mean motion using Kepler's third law: n = √(μ/a³)
        double mu = gravitationalConstant * earthMass; // Standard gravitational parameter
        this.meanMotion = Math.sqrt(mu / (a * a * a));
    }
    
    /**
     * Updates satellite position by advancing time
     */
    public void updatePosition(double deltaTime) {
        // Convert true anomaly to mean anomaly
        double E = trueToEccentricAnomaly(nu, e); // Eccentric anomaly
        double M = E - e * Math.sin(E); // Mean anomaly from Kepler's equation
        
        // Advance mean anomaly by time step
        M += meanMotion * deltaTime;
        
        // Convert back to true anomaly
        E = solveKeplersEquation(M, e); // Solve Kepler's equation iteratively
        nu = eccentricToTrueAnomaly(E, e); // Convert to true anomaly
    }
    
    /**
     * Gets current satellite position in 2D coordinates
     */
    public double[] getPosition() {
        // Calculate position in orbital plane
        double r = a * (1 - e * e) / (1 + e * Math.cos(nu)); // Orbital radius
        double x_orbital = r * Math.cos(nu); // X in orbital plane
        double y_orbital = r * Math.sin(nu); // Y in orbital plane
        
        // Transform to Earth-centered coordinates using rotation matrices
        // Apply argument of periapsis rotation
        double x1 = x_orbital * Math.cos(omega) - y_orbital * Math.sin(omega);
        double y1 = x_orbital * Math.sin(omega) + y_orbital * Math.cos(omega);
        
        // Apply inclination rotation
        double x2 = x1;
        double y2 = y1 * Math.cos(i);
        double z2 = y1 * Math.sin(i);
        
        // Apply longitude of ascending node rotation
        double x3 = x2 * Math.cos(Omega) - y2 * Math.sin(Omega);
        double y3 = x2 * Math.sin(Omega) + y2 * Math.cos(Omega);
        
        return new double[]{x3, y3}; // Return 2D projection
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