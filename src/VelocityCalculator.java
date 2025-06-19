/**
 * VelocityCalculator provides static methods for velocity calculations
 * Extracted from SimulationPanel for better code organization
 */
public class VelocityCalculator {
    
    /**
     * Calculates the 3D velocity vector of the satellite
     * Uses orbital mechanics formulas for accurate velocity calculation
     */
    public static double[] calculateVelocityVector3D(Satellite satellite, OrbitalSimulation simulation) {
        // Get current orbital elements
        double a = simulation.getSemiMajorAxis();
        double e = simulation.getEccentricity();
        double nu = satellite.getTrueAnomaly();
        double omega = Math.toRadians(simulation.getArgumentOfPeriapsis());
        double i = Math.toRadians(simulation.getInclination());
        double Omega = Math.toRadians(simulation.getLongitudeOfAscendingNode());
        
        // Calculate orbital velocity components using orbital mechanics formulas
        double mu = simulation.getGravitationalConstant() * simulation.getEarthMass();
        double r = OrbitTransforms.getOrbitalRadius(a, e, nu);
        
        // In orbital frame, velocity is perpendicular to radius vector
        // For elliptical orbit: v_r = (μ*e*sin(ν))/h, v_θ = (μ*(1+e*cos(ν)))/h
        double h = Math.sqrt(mu * a * (1 - e*e)); // Specific angular momentum
        double v_radial = (mu * e * Math.sin(nu)) / h;
        double v_tangential = (mu * (1 + e * Math.cos(nu))) / h;
        
        // Convert to orbital plane Cartesian coordinates
        double vx_orbital = v_radial * Math.cos(nu) - v_tangential * Math.sin(nu);
        double vy_orbital = v_radial * Math.sin(nu) + v_tangential * Math.cos(nu);
        double vz_orbital = 0;
        
        // Transform to Earth-centered coordinates using same rotations as position
        // Apply argument of periapsis rotation
        double vx1 = vx_orbital * Math.cos(omega) - vy_orbital * Math.sin(omega);
        double vy1 = vx_orbital * Math.sin(omega) + vy_orbital * Math.cos(omega);
        double vz1 = vz_orbital;
        
        // Apply inclination rotation
        double vx2 = vx1;
        double vy2 = vy1 * Math.cos(i) - vz1 * Math.sin(i);
        double vz2 = vy1 * Math.sin(i) + vz1 * Math.cos(i);
        
        // Apply longitude of ascending node rotation
        double vx3 = vx2 * Math.cos(Omega) - vy2 * Math.sin(Omega);
        double vy3 = vx2 * Math.sin(Omega) + vy2 * Math.cos(Omega);
        double vz3 = vz2;
        
        return new double[]{vx3, vy3, vz3};
    }
}