import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * OrbitalInfoDisplay handles the text information display for the orbital simulation
 * Separated from SimulationPanel for better organization and maintainability
 */
public class OrbitalInfoDisplay {
    private OrbitalSimulation simulation;
    
    public OrbitalInfoDisplay(OrbitalSimulation simulation) {
        this.simulation = simulation;
    }
    
    /**
     * Draws real-time orbital information as text overlay
     */
    public void drawInfo(Graphics2D g2d, Satellite satellite) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        
        // Calculate current orbital parameters from satellite state
        double[] pos = satellite.getPosition();
        double[] pos3D = satellite.getPosition3D();
        double satX = pos[0];
        double satY = pos[1];
        double satZ = pos3D[2];
        
        // Calculate distance from celestial body center
        double distanceFromCenter = Math.sqrt(satX * satX + satY * satY + satZ * satZ);
        
        // Calculate altitude: distance from celestial body surface
        double altitude = (distanceFromCenter - simulation.getEarthRadius()) / 1000;
        
        // Get current orbital velocity and period from satellite
        double velocity = satellite.getVelocity(); // m/s
        double period = satellite.getOrbitalPeriod() / 3600; // Convert seconds to hours
        
        // === ADVANCED CALCULATIONS ===
        
        // 1. Specific Mechanical Energy (ε = v²/2 - μ/r)
        double mu = simulation.getGravitationalConstant() * simulation.getEarthMass();
        double specificEnergy = (velocity * velocity) / 2.0 - mu / distanceFromCenter; // J/kg
        
        // 2. Specific Angular Momentum (h = r × v)
        double[] vel3D = VelocityCalculator.calculateVelocityVector3D(satellite, simulation);
        double angMomX = pos3D[1] * vel3D[2] - pos3D[2] * vel3D[1];
        double angMomY = pos3D[2] * vel3D[0] - pos3D[0] * vel3D[2];
        double angMomZ = pos3D[0] * vel3D[1] - pos3D[1] * vel3D[0];
        double specificAngularMomentum = Math.sqrt(angMomX*angMomX + angMomY*angMomY + angMomZ*angMomZ);
        
        // 3. Flight Path Angle (γ = angle between velocity vector and local horizontal)
        double velRadial = (satX * vel3D[0] + satY * vel3D[1] + satZ * vel3D[2]) / distanceFromCenter;
        double flightPathAngle = Math.toDegrees(Math.asin(velRadial / velocity));
        
        // 4. Calculate individual accelerations from Sun and Moon
        double moonAccelMagnitude = 0;
        double sunAccelMagnitude = 0;
        
        if (simulation.isLunarEffectsEnabled()) {
            moonAccelMagnitude = AccelerationCalculator.calculateLunarAccelerationMagnitude(pos3D, simulation);
        }
        
        if (simulation.isSolarEffectsEnabled()) {
            sunAccelMagnitude = AccelerationCalculator.calculateSolarAccelerationMagnitude(pos3D, simulation);
        }
        
        // Build information list
        List<String> infoList = buildInfoList(altitude, velocity, period, satellite, specificEnergy, 
                                            specificAngularMomentum, flightPathAngle, moonAccelMagnitude, sunAccelMagnitude);
        
        // Draw information text in top-left corner
        for (int i = 0; i < infoList.size(); i++) {
            g2d.drawString(infoList.get(i), 10, 20 + i * 15);
        }
    }
    
    /**
     * Builds the list of information strings to display
     */
    private List<String> buildInfoList(double altitude, double velocity, double period, Satellite satellite,
                                     double specificEnergy, double specificAngularMomentum, double flightPathAngle,
                                     double moonAccelMagnitude, double sunAccelMagnitude) {
        List<String> infoList = new ArrayList<>();
        
        // Basic orbital parameters
        infoList.add(String.format("%s Altitude: %.1f km", simulation.getCurrentBody(), altitude));
        infoList.add(String.format("Velocity: %.2f km/s", velocity / 1000));
        infoList.add(String.format("Period: %.2f hours", period));
        infoList.add(String.format("True Anomaly: %.1f°", Math.toDegrees(satellite.getTrueAnomaly())));
        
        // Advanced parameters
        infoList.add(String.format("Specific Energy: %.2f MJ/kg", specificEnergy / 1e6));
        infoList.add(String.format("Angular Momentum: %.2e m²/s", specificAngularMomentum));
        infoList.add(String.format("Flight Path Angle: %.2f°", flightPathAngle));
        
        // Perturbation accelerations
        if (simulation.isLunarEffectsEnabled()) {
            infoList.add(String.format("Lunar Acceleration: %.3e m/s²", moonAccelMagnitude));
        }
        
        if (simulation.isSolarEffectsEnabled()) {
            infoList.add(String.format("Solar Acceleration: %.3e m/s²", sunAccelMagnitude));
        }
        
        // Atmospheric drag acceleration
        if (simulation.isAtmosphericDragEnabled()) {
            addDragInfo(infoList, satellite, altitude);
        }
        if (simulation.isJ2EffectsEnabled()) {
            double j2Accel = satellite.getJ2Acceleration();
            double[] j2Rates = satellite.getJ2Rates();
            infoList.add(String.format("J2 Acceleration: %.3e m/s²", j2Accel));
            infoList.add(String.format("Nodal Precession: %.3f°/day", j2Rates[0]));
            infoList.add(String.format("Apsidal Precession: %.3f°/day", j2Rates[1]));
        }
        if (simulation.isSolarRadiationPressureEnabled()) {
        double radiationAccel = satellite.getRadiationPressureAcceleration();
        SolarRadiationPressureCalculator.ShadowCondition shadowCondition = satellite.getShadowCondition();
        
        infoList.add(String.format("Solar Radiation Pressure: %.3e m/s²", radiationAccel));
        infoList.add(String.format("Shadow Condition: %s", shadowCondition.shadowType));
        
        if (shadowCondition.shadowType == SolarRadiationPressureCalculator.ShadowType.PENUMBRA) {
            infoList.add(String.format("Lighting Factor: %.3f", shadowCondition.lightingFactor));
        }
        
        if (radiationAccel <= 0) {
            infoList.add("SRP: N/A (in shadow)");
        }
}

        
        // Celestial body positions
        addCelestialBodyInfo(infoList);
        
        return infoList;
    }
    
    /**
     * Adds atmospheric drag information to the info list
     */
    private void addDragInfo(List<String> infoList, Satellite satellite, double altitude) {
        double dragAccel = satellite.getDragAcceleration();
        if (dragAccel > 0) {
            infoList.add(String.format("Drag Acceleration: %.3e m/s²", dragAccel));
            
            // Calculate and show atmospheric density
            double density = 1.225 * Math.exp(-(altitude * 1000) / 8500.0); // Convert km to m
            infoList.add(String.format("Atmospheric Density: %.3e kg/m³", density));
        } else {
            infoList.add("Drag: N/A (outside atmosphere)");
        }
    }
    
    /**
     * Adds celestial body position information to the info list
     */
    private void addCelestialBodyInfo(List<String> infoList) {
        if (simulation.isLunarEffectsEnabled() || simulation.isSolarEffectsEnabled()) {
            if (simulation.isLunarEffectsEnabled()) {
                // Calculate Moon's current angle
                double[] moonPos = simulation.getMoonPosition();
                double moonAngle = Math.toDegrees(Math.atan2(moonPos[1], moonPos[0]));
                if (moonAngle < 0) moonAngle += 360; // Normalize to 0-360 degrees
                
                infoList.add(String.format("Moon Position: %.1f°", moonAngle));
            }
            
            if (simulation.isSolarEffectsEnabled()) {
                // Calculate Sun's current angle
                double[] sunPos = simulation.getSunPosition();
                double sunAngle = Math.toDegrees(Math.atan2(sunPos[1], sunPos[0]));
                if (sunAngle < 0) sunAngle += 360; // Normalize to 0-360 degrees
                
                infoList.add(String.format("Sun Position: %.1f°", sunAngle));
            }
        }
    }
    
    /**
     * Draws UI overlays and effects status
     */
    public void drawUIOverlays(Graphics2D g2d, int width, int height, double zoomFactor) {
        // Draw zoom level and controls information
        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString(String.format("Zoom: %.1fx", zoomFactor), width - 100, 20);
        g2d.drawString("Mouse wheel: zoom, drag: pan", width - 200, height - 10);
        
        // Draw effects status
        drawEffectsStatus(g2d, width);
    }
    
    /**
     * Draws the effects status display
     */
    private void drawEffectsStatus(Graphics2D g2d, int width) {
        // Build effects status string dynamically
        List<String> activeEffects = new ArrayList<>();
        if (simulation.isLunarEffectsEnabled()) activeEffects.add("Lunar");
        if (simulation.isSolarEffectsEnabled()) activeEffects.add("Solar");
        if (simulation.isAtmosphericDragEnabled()) activeEffects.add("Drag");
        if (simulation.isSolarRadiationPressureEnabled()) activeEffects.add("Radiation");

        if (!activeEffects.isEmpty()) {
            g2d.setColor(Color.YELLOW);
            String effectsStatus = String.join(" + ", activeEffects) + " Effects: ON";
            g2d.drawString(effectsStatus, width - 200, 40);
            
            // Show vector legend
            g2d.setColor(Color.CYAN);
            List<String> vectorInfo = new ArrayList<>();
            
            if (simulation.isLunarEffectsEnabled() || simulation.isSolarEffectsEnabled()) {
                vectorInfo.add("Blue line = Gravitational forces");
            }
            if (simulation.isAtmosphericDragEnabled()) {
                vectorInfo.add("Gray line = Atmospheric drag");
            }
            if (simulation.isJ2EffectsEnabled()) {
            vectorInfo.add("Green line = J2 oblateness");
            }
            if (!vectorInfo.isEmpty()) {
                String vectorText = String.join(", ", vectorInfo);
                g2d.drawString(vectorText, width - Math.max(280, vectorText.length() * 7), 60);
            }
            if (simulation.isSolarRadiationPressureEnabled()) {
            vectorInfo.add("Orange line = Solar radiation pressure");
            }
        }
    }
}