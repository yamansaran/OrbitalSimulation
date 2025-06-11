/**
 * OrbitTransforms class handles coordinate transformations for orbital mechanics
 * Extracted from Satellite class for better code organization
 */
public class OrbitTransforms {
    
    /**
     * Transforms position from orbital plane coordinates to 3D Earth-centered coordinates
     * This is the exact transformation logic from the original getPosition3D() method
     * 
     * @param x_orbital X coordinate in orbital plane
     * @param y_orbital Y coordinate in orbital plane
     * @param omega Argument of periapsis (radians)
     * @param i Inclination (radians)
     * @param Omega Longitude of ascending node (radians)
     * @return Array containing [x3, y3, z3] in Earth-centered coordinates
     */
    public static double[] orbitalToEarthCentered(double x_orbital, double y_orbital, 
                                                 double omega, double i, double Omega) {
        // Apply argument of periapsis rotation (same as original)
        double x1 = x_orbital * Math.cos(omega) - y_orbital * Math.sin(omega);
        double y1 = x_orbital * Math.sin(omega) + y_orbital * Math.cos(omega);
        double z1 = 0; // Still in orbital plane
        
        // Apply inclination rotation (same as original)
        double x2 = x1;
        double y2 = y1 * Math.cos(i) - z1 * Math.sin(i);
        double z2 = y1 * Math.sin(i) + z1 * Math.cos(i);
        
        // Apply longitude of ascending node rotation (same as original)
        double x3 = x2 * Math.cos(Omega) - y2 * Math.sin(Omega);
        double y3 = x2 * Math.sin(Omega) + y2 * Math.cos(Omega);
        double z3 = z2;
        
        return new double[]{x3, y3, z3};
    }
    
    /**
     * Calculates orbital radius for given true anomaly
     * Using the standard orbital mechanics formula: r = a(1-e²)/(1+e*cos(ν))
     * 
     * @param a Semi-major axis
     * @param e Eccentricity
     * @param nu True anomaly (radians)
     * @return Orbital radius
     */
    public static double getOrbitalRadius(double a, double e, double nu) {
        return a * (1 - e * e) / (1 + e * Math.cos(nu));
    }
    
    /**
     * Converts orbital radius and true anomaly to orbital plane coordinates
     * 
     * @param r Orbital radius
     * @param nu True anomaly (radians)
     * @return Array containing [x_orbital, y_orbital]
     */
    public static double[] polarToOrbitalCartesian(double r, double nu) {
        double x_orbital = r * Math.cos(nu);
        double y_orbital = r * Math.sin(nu);
        return new double[]{x_orbital, y_orbital};
    }
    
    /**
     * Complete transformation from orbital elements to 3D position
     * Combines all the steps for convenience
     * 
     * @param a Semi-major axis
     * @param e Eccentricity
     * @param nu True anomaly (radians)
     * @param omega Argument of periapsis (radians)
     * @param i Inclination (radians)
     * @param Omega Longitude of ascending node (radians)
     * @return Array containing [x, y, z] in Earth-centered coordinates
     */
    public static double[] orbitalElementsToPosition3D(double a, double e, double nu,
                                                      double omega, double i, double Omega) {
        // Calculate orbital radius
        double r = getOrbitalRadius(a, e, nu);
        
        // Get orbital plane coordinates
        double[] orbitalCoords = polarToOrbitalCartesian(r, nu);
        
        // Transform to Earth-centered coordinates
        return orbitalToEarthCentered(orbitalCoords[0], orbitalCoords[1], omega, i, Omega);
    }
    
    /**
     * Extracts 2D position from 3D position (for backward compatibility)
     * 
     * @param position3D Array containing [x, y, z]
     * @return Array containing [x, y]
     */
    public static double[] to2D(double[] position3D) {
        return new double[]{position3D[0], position3D[1]};
    }
}