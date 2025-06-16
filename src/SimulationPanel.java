import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.io.File;
import javax.swing.*;
import javax.imageio.ImageIO;

/*
 * Custom JPanel for rendering the orbital simulation
 * Handles zooming, panning, and drawing all visual elements
 */
public class SimulationPanel extends JPanel {
    // Zoom and pan state variables
    private double zoomFactor = 1.0; // Current zoom level (1.0 = default)
    private double offsetX = 0; // Horizontal pan offset
    private double offsetY = 0; // Vertical pan offset
    
    // Satellite trail for showing recent orbital path - now using extracted class
    private SatelliteTrail satelliteTrail;
    
    // Reference to the main simulation for accessing parameters
    private OrbitalSimulation simulation;
    
    /**
     * Constructor: Sets up mouse controls for zoom and pan
     */
    public SimulationPanel(OrbitalSimulation simulation) {
        this.simulation = simulation;
        
        // Initialize satellite trail with simulation settings
        this.satelliteTrail = new SatelliteTrail(
            simulation.getMaxTrailLength(),
            simulation.getTrailWidth(),
            simulation.getTrailColor()
        );
        
        setBackground(Color.BLACK); // Space background
        
        // Mouse wheel listener for zooming functionality
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getWheelRotation() < 0) {
                    zoomIn(); // Scroll up = zoom in
                } else {
                    zoomOut(); // Scroll down = zoom out
                }
                if (simulation.getAutoClearOnZoom()) {
                    clearTrail();
                }
                repaint(); // Update display immediately
            }
        });
        
        // Mouse listeners for click-and-drag panning
        MouseAdapter mouseHandler = new MouseAdapter() {
            private Point lastPanPoint; // Last mouse position for drag calculation
        
            // Record initial mouse position when dragging starts
            public void mousePressed(MouseEvent e) {
                lastPanPoint = e.getPoint();
            }
            
            // Update pan offset based on mouse movement
            public void mouseDragged(MouseEvent e) {
                if (lastPanPoint != null) {
                    Point currentPoint = e.getPoint();
                    
                    // Calculate the change in mouse position
                    int deltaX = currentPoint.x - lastPanPoint.x;
                    int deltaY = currentPoint.y - lastPanPoint.y;
                    
                    // Update pan offsets (scale by zoom factor for consistent feel)
                    offsetX += deltaX / zoomFactor;
                    offsetY += deltaY / zoomFactor;
                    
                    // Update last pan point for next movement
                    lastPanPoint = currentPoint;
                    
                    repaint(); // Update display during drag
                }
            }
            
            // Clear the last pan point when mouse is released
            public void mouseReleased(MouseEvent e) {
                lastPanPoint = null;
            }
        };
        
        // Attach mouse handlers to panel
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    
    /**
     * Updates display settings when changed in options
     */
    public void updateSettings() {
        // Update trail settings using the new trail class
        satelliteTrail.updateSettings(
            simulation.getMaxTrailLength(),
            simulation.getTrailWidth(),
            simulation.getTrailColor()
        );
    }
    
    /**
     * Increases zoom level by factor of 1.5, with maximum limit of 50x
     */
    public void zoomIn() {
        zoomFactor *= 1.5;
        if (zoomFactor > 100.0) zoomFactor = 50.0; // Prevent excessive zoom
    }
    
    /**
     * Decreases zoom level by factor of 1.5, with minimum limit of 0.1x
     */
    public void zoomOut() {
        zoomFactor /= 1.5;
        if (zoomFactor < 0.01) zoomFactor = 0.1; // Prevent negative/zero zoom
    }
    
    /**
     * Resets zoom to default level and centers the view
     */
    public void resetZoom() {
        zoomFactor = 1.0;
        offsetX = 0;
        offsetY = 0;
        repaint();
    }
    
    /**
     * Returns current zoom factor for display purposes
     */
    public double getZoomFactor() {
        return zoomFactor;
    }
    
    /**
     * Clears the satellite trail (now using extracted trail class)
     */
    public void clearTrail() {
        satelliteTrail.clear();
    }
    
    /**
     * Main rendering method - draws all visual elements of the simulation
     * Called automatically by Swing whenever the panel needs to be redrawn
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Clear previous frame
        Graphics2D g2d = (Graphics2D) g; // Use Graphics2D for advanced rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Calculate screen center with pan offset applied
        int centerX = getWidth() / 2 + (int)(offsetX * zoomFactor);
        int centerY = getHeight() / 2 + (int)(offsetY * zoomFactor);
        
        // Calculate current scale factor (base scale × zoom level)
        double currentScale = simulation.getBaseScale() * zoomFactor;
        
        // DRAW CELESTIAL BODY: Render the celestial body (Earth, etc.) at the center
        int bodyRadiusPixels = (int) (simulation.getEarthRadius() * currentScale); // Scale radius to pixels
        
        BufferedImage bodyImage = simulation.getCelestialBodyImage();
        if (bodyImage != null) {
            // Draw the celestial body image
            int imageSize = bodyRadiusPixels * 2;
            g2d.drawImage(bodyImage, centerX - bodyRadiusPixels, centerY - bodyRadiusPixels, 
                         imageSize, imageSize, null);
            
            // Optional: Add a subtle outline
            g2d.setColor(simulation.getEarthOutlineColor());
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawOval(centerX - bodyRadiusPixels, centerY - bodyRadiusPixels, 
                        imageSize, imageSize);
        } else {
            // Fallback: Draw colored circle if no image is available
            // Body fill (configurable color)
            g2d.setColor(simulation.getEarthColor());
            g2d.fillOval(centerX - bodyRadiusPixels, centerY - bodyRadiusPixels, 
                        bodyRadiusPixels * 2, bodyRadiusPixels * 2);
            
            // Body outline (configurable color)
            g2d.setColor(simulation.getEarthOutlineColor());
            g2d.drawOval(centerX - bodyRadiusPixels, centerY - bodyRadiusPixels, 
                        bodyRadiusPixels * 2, bodyRadiusPixels * 2);
        }
        
        // === NEW: DRAW MOON if lunar effects are enabled ===
        if (simulation.isLunarEffectsEnabled()) {
            drawMoon(g2d, centerX, centerY, currentScale);
        }
        
        // === NEW: DRAW SUN if solar effects are enabled ===
        if (simulation.isSolarEffectsEnabled()) {
            drawSun(g2d, centerX, centerY, currentScale);
        }
        
        // === FIXED: DRAW COMBINED GRAVITATIONAL ACCELERATION VECTOR ===
        // Only show when BOTH lunar and solar effects are enabled
        if (simulation.isLunarEffectsEnabled() && simulation.isSolarEffectsEnabled()) {
            drawCombinedAccelerationVector(g2d, centerX, centerY, currentScale);
        }
        
        // === NEW: DRAW ATMOSPHERIC DRAG ACCELERATION VECTOR ===
        if (simulation.isAtmosphericDragEnabled()) {
            drawAtmosphericDragVector(g2d, centerX, centerY, currentScale);
        }
        
        Satellite satellite = simulation.getSatellite();
        if (satellite != null) {
            // DRAW ORBITAL PATH: Show the complete elliptical orbit
            drawOrbit(g2d, centerX, centerY, currentScale);
            
            // GET SATELLITE POSITION: Calculate current satellite coordinates
            double[] pos = satellite.getPosition(); // Returns [x, y] in meters
            int satX = centerX + (int) (pos[0] * currentScale); // Convert to screen X
            int satY = centerY - (int) (pos[1] * currentScale); // Convert to screen Y (flip Y axis)
            
            // UPDATE TRAIL: Add current position to satellite trail (using extracted trail class)
            satelliteTrail.addPoint(pos[0], pos[1]);
            
            // DRAW TRAIL: Show satellite's recent orbital path (using extracted trail class)
            satelliteTrail.draw(g2d, currentScale, centerX, centerY, zoomFactor);
            
            // DRAW SATELLITE: Render satellite as an image or colored dot
            BufferedImage satImage = simulation.getSatelliteImage();
            if (satImage != null) {
                // Draw satellite image
                int satSize = (int)Math.max(simulation.getSatelliteSize() * 2, simulation.getSatelliteSize() * 2 * zoomFactor);
                satSize = Math.max(satSize, 8); // Minimum size for visibility
                g2d.drawImage(satImage, satX - satSize/2, satY - satSize/2, satSize, satSize, null);
            } else {
                // Fallback: Draw satellite as a colored dot
                g2d.setColor(simulation.getSatelliteColor());
                int satSize = (int)Math.max(simulation.getSatelliteSize(), simulation.getSatelliteSize() * zoomFactor);
                g2d.fillOval(satX - satSize/2, satY - satSize/2, satSize, satSize);
            }
            
            // DRAW INFORMATION: Display orbital parameters and status
            drawInfo(g2d, satellite);
        }
        
        // DRAW UI OVERLAYS: Show zoom level and controls information
        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString(String.format("Zoom: %.1fx", zoomFactor), getWidth() - 100, 20);
        g2d.drawString("Mouse wheel: zoom, drag: pan", getWidth() - 200, getHeight() - 10);
        
        // === UPDATED: Show effects status ===
        if (simulation.isLunarEffectsEnabled() || simulation.isSolarEffectsEnabled() || simulation.isAtmosphericDragEnabled()) {
            g2d.setColor(Color.YELLOW);
            String effectsStatus = "";
            if (simulation.isLunarEffectsEnabled() && simulation.isSolarEffectsEnabled() && simulation.isAtmosphericDragEnabled()) {
                effectsStatus = "Lunar + Solar + Drag Effects: ON";
                // Add info about the acceleration vectors
                g2d.setColor(Color.CYAN);
                g2d.drawString("Blue line = Combined gravity, Gray line = Atmospheric drag", getWidth() - 380, 60);
            } else if (simulation.isLunarEffectsEnabled() && simulation.isSolarEffectsEnabled()) {
                effectsStatus = "Lunar + Solar Effects: ON";
                g2d.setColor(Color.CYAN);
                g2d.drawString("Blue line = Combined acceleration vector", getWidth() - 280, 60);
            } else if (simulation.isAtmosphericDragEnabled()) {
                if (simulation.isLunarEffectsEnabled()) {
                    effectsStatus = "Lunar + Drag Effects: ON";
                } else if (simulation.isSolarEffectsEnabled()) {
                    effectsStatus = "Solar + Drag Effects: ON";
                } else {
                    effectsStatus = "Atmospheric Drag: ON";
                }
                g2d.setColor(Color.CYAN);
                g2d.drawString("Gray line = Atmospheric drag vector", getWidth() - 250, 60);
            } else if (simulation.isLunarEffectsEnabled()) {
                effectsStatus = "Lunar Effects: ON";
            } else if (simulation.isSolarEffectsEnabled()) {
                effectsStatus = "Solar Effects: ON";
            }
            g2d.setColor(Color.YELLOW);
            g2d.drawString(effectsStatus, getWidth() - 180, 40);
        }
    }
    
    /**
     * === NEW: Draws the Moon at its current orbital position ===
     * Now supports Moon.png image
     * 
     * @param g2d Graphics context for drawing
     * @param centerX Screen X coordinate of Earth's center
     * @param centerY Screen Y coordinate of Earth's center  
     * @param currentScale Current meter-to-pixel conversion factor
     */
    private void drawMoon(Graphics2D g2d, int centerX, int centerY, double currentScale) {
        double[] moonPos = simulation.getMoonPosition();
        double moonX = moonPos[0]; // Moon X position in meters
        double moonY = moonPos[1]; // Moon Y position in meters
        
        // Convert Moon position to screen coordinates
        int moonScreenX = centerX + (int)(moonX * currentScale);
        int moonScreenY = centerY - (int)(moonY * currentScale); // Flip Y axis
        
        // Calculate Moon size on screen (Moon radius = 1,737,400 meters)
        double moonRadius = 1737400; // Moon's radius in meters
        int moonRadiusPixels = Math.max(4, (int)(moonRadius * currentScale)); // Minimum 4 pixels for visibility
        
        // Try to load and draw Moon image
        try {
            BufferedImage moonImage = null;
            try {
                File moonFile = new File("src/resources/Moon.png"); // UPDATED PATH
                if (moonFile.exists()) {
                    moonImage = javax.imageio.ImageIO.read(moonFile);
                }
            } catch (Exception e) {
                // If image loading fails, moonImage stays null
            }
            
            if (moonImage != null) {
                // Draw Moon image
                int imageSize = moonRadiusPixels * 2;
                g2d.drawImage(moonImage, moonScreenX - moonRadiusPixels, moonScreenY - moonRadiusPixels, 
                             imageSize, imageSize, null);
                
                // Optional: Add subtle outline
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawOval(moonScreenX - moonRadiusPixels, moonScreenY - moonRadiusPixels, 
                            imageSize, imageSize);
            } else {
                // Fallback: Draw Moon as a colored circle
                g2d.setColor(simulation.getMoonColor());
                g2d.fillOval(moonScreenX - moonRadiusPixels, moonScreenY - moonRadiusPixels, 
                            moonRadiusPixels * 2, moonRadiusPixels * 2);
                
                // Draw Moon outline
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawOval(moonScreenX - moonRadiusPixels, moonScreenY - moonRadiusPixels, 
                            moonRadiusPixels * 2, moonRadiusPixels * 2);
            }
        } catch (Exception e) {
            // Final fallback if anything goes wrong
            g2d.setColor(simulation.getMoonColor());
            g2d.fillOval(moonScreenX - moonRadiusPixels, moonScreenY - moonRadiusPixels, 
                        moonRadiusPixels * 2, moonRadiusPixels * 2);
        }
        
        // Draw Moon orbit path (optional - only when zoomed out enough)
        if (currentScale < 1e-8) { // Only show orbit when very zoomed out
            g2d.setColor(new Color(128, 128, 128, 50)); // Semi-transparent gray
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, 
                         BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f}, 0.0f));
            
            double moonOrbitRadius = 384400000 * currentScale; // Moon-Earth distance in pixels
            int orbitDiameter = (int)(moonOrbitRadius * 2);
            g2d.drawOval(centerX - (int)moonOrbitRadius, centerY - (int)moonOrbitRadius, 
                        orbitDiameter, orbitDiameter);
        }
        
        // Draw line from Earth to Moon (when zoomed in enough to see detail)
        if (currentScale > 1e-9) { // Adjusted to match Sun line range better
            g2d.setColor(new Color(255, 255, 255, 40)); // Slightly more visible white line
            g2d.setStroke(new BasicStroke(1.5f)); // Slightly thicker line
            g2d.drawLine(centerX, centerY, moonScreenX, moonScreenY);
        }
    }
    
    /**
     * === NEW: Draws the Sun as a directional line extending to screen edge ===
     * 
     * @param g2d Graphics context for drawing
     * @param centerX Screen X coordinate of Earth's center
     * @param centerY Screen Y coordinate of Earth's center  
     * @param currentScale Current meter-to-pixel conversion factor
     */
    private void drawSun(Graphics2D g2d, int centerX, int centerY, double currentScale) {
        double[] sunPos = simulation.getSunPosition();
        double sunX = sunPos[0]; // Sun X position in meters
        double sunY = sunPos[1]; // Sun Y position in meters
        
        // Calculate Sun direction (normalize to unit vector)
        double sunDistance = Math.sqrt(sunX * sunX + sunY * sunY);
        if (sunDistance == 0) return; // Avoid division by zero
        
        double sunDirX = sunX / sunDistance;
        double sunDirY = sunY / sunDistance;
        
        // Calculate line that extends to screen edge
        // Get screen dimensions
        int screenWidth = getWidth();
        int screenHeight = getHeight();
        
        // Calculate how far we need to extend the line to reach screen edge
        // Check intersections with all four screen edges and use the farthest one
        double maxDistance = Math.max(screenWidth, screenHeight) * 2; // Ensure it reaches edge
        
        // Calculate end point that definitely extends beyond screen
        int lineEndX = centerX + (int)(sunDirX * maxDistance);
        int lineEndY = centerY - (int)(sunDirY * maxDistance); // Flip Y axis
        
        // Draw the Sun direction line extending to infinity (screen edge)
        g2d.setColor(simulation.getSunColor());
        g2d.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(centerX, centerY, lineEndX, lineEndY);
        
        // Optional: Draw a small arrowhead near Earth to show direction
        double arrowDistance = Math.max(50, simulation.getEarthRadius() * currentScale * 2);
        int arrowX = centerX + (int)(sunDirX * arrowDistance);
        int arrowY = centerY - (int)(sunDirY * arrowDistance);
        drawArrowHead(g2d, centerX, centerY, arrowX, arrowY, simulation.getSunColor());
    }
    
    /**
     * === FIXED: Draws the combined gravitational acceleration vector from Sun and Moon ===
     * Only shows when BOTH lunar and solar effects are enabled
     * Calculates actual acceleration imparted on the satellite, not just force
     * 
     * @param g2d Graphics context for drawing
     * @param centerX Screen X coordinate of Earth's center
     * @param centerY Screen Y coordinate of Earth's center  
     * @param currentScale Current meter-to-pixel conversion factor
     */
    private void drawCombinedAccelerationVector(Graphics2D g2d, int centerX, int centerY, double currentScale) {
        // Get satellite position for accurate acceleration calculation
        Satellite satellite = simulation.getSatellite();
        if (satellite == null) return;
        
        double[] satPos = satellite.getPosition(); // Get 2D satellite position
        double satX = satPos[0];
        double satY = satPos[1];
        
        double combinedAccelX = 0;
        double combinedAccelY = 0;
        
        // Calculate lunar gravitational acceleration
        double[] moonPos = simulation.getMoonPosition();
        double moonX = moonPos[0];
        double moonY = moonPos[1];
        
        // Vector from satellite to Moon
        double satToMoonX = moonX - satX;
        double satToMoonY = moonY - satY;
        double satMoonDistance = Math.sqrt(satToMoonX * satToMoonX + satToMoonY * satToMoonY);
        
        if (satMoonDistance > 0) {
            // Gravitational acceleration: a = GM/r²
            double moonMass = 7.342e22; // Moon mass in kg
            double G = 6.67430e-11; // Gravitational constant
            double moonAccelMagnitude = G * moonMass / (satMoonDistance * satMoonDistance);
            
            // Direction unit vector (toward Moon)
            double moonDirX = satToMoonX / satMoonDistance;
            double moonDirY = satToMoonY / satMoonDistance;
            
            // Acceleration vector components
            combinedAccelX += moonAccelMagnitude * moonDirX;
            combinedAccelY += moonAccelMagnitude * moonDirY;
        }
        
        // Calculate solar gravitational acceleration
        double[] sunPos = simulation.getSunPosition();
        double sunX = sunPos[0];
        double sunY = sunPos[1];
        
        // Vector from satellite to Sun
        double satToSunX = sunX - satX;
        double satToSunY = sunY - satY;
        double satSunDistance = Math.sqrt(satToSunX * satToSunX + satToSunY * satToSunY);
        
        if (satSunDistance > 0) {
            // Gravitational acceleration: a = GM/r²
            double sunMass = 1.989e30; // Sun mass in kg
            double G = 6.67430e-11; // Gravitational constant
            double sunAccelMagnitude = G * sunMass / (satSunDistance * satSunDistance);
            
            // Direction unit vector (toward Sun)
            double sunDirX = satToSunX / satSunDistance;
            double sunDirY = satToSunY / satSunDistance;
            
            // Acceleration vector components
            combinedAccelX += sunAccelMagnitude * sunDirX;
            combinedAccelY += sunAccelMagnitude * sunDirY;
        }
        
        // Calculate combined acceleration magnitude and direction
        double combinedAccelMagnitude = Math.sqrt(combinedAccelX * combinedAccelX + combinedAccelY * combinedAccelY);
        if (combinedAccelMagnitude > 0) {
            // Normalize the acceleration vector for display
            double accelDirectionX = combinedAccelX / combinedAccelMagnitude;
            double accelDirectionY = combinedAccelY / combinedAccelMagnitude;
            
            // Scale the vector length for visibility
            // Use logarithmic scaling to handle the wide range of acceleration values
            double baseLength = 100; // Base length in pixels
            double scaleFactor = Math.log10(combinedAccelMagnitude * 1e6 + 1) * 15; // Scale factor
            double vectorLength = Math.min(baseLength + scaleFactor, 300); // Cap at 300 pixels
            
            // Get satellite screen position
            int satScreenX = centerX + (int)(satX * currentScale);
            int satScreenY = centerY - (int)(satY * currentScale);
            
            // Calculate end point of acceleration vector
            int vectorEndX = satScreenX + (int)(accelDirectionX * vectorLength);
            int vectorEndY = satScreenY - (int)(accelDirectionY * vectorLength); // Flip Y axis
            
            // Draw the combined acceleration vector as a thick blue line
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(satScreenX, satScreenY, vectorEndX, vectorEndY);
            
            // Draw arrowhead to show direction
            drawArrowHead(g2d, satScreenX, satScreenY, vectorEndX, vectorEndY, Color.BLUE);
            
            // Add a small blue circle at the satellite position to mark the origin
            g2d.setColor(Color.BLUE);
            g2d.fillOval(satScreenX - 3, satScreenY - 3, 6, 6);
            
            // Optional: Draw magnitude indicator text
            if (vectorLength > 80) { // Only show text if vector is long enough
                g2d.setColor(Color.BLUE);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                String magnitudeText = String.format("a: %.2e m/s²", combinedAccelMagnitude);
                
                // Position text near the end of vector
                int textX = vectorEndX + 10;
                int textY = vectorEndY - 5;
                
                // Keep text on screen
                if (textX > getWidth() - 80) textX = vectorEndX - 80;
                if (textY < 15) textY = vectorEndY + 15;
                
                g2d.drawString(magnitudeText, textX, textY);
            }
        }
    }
    
    /**
     * === NEW: Draws the atmospheric drag acceleration vector ===
     * Shows drag force direction (opposite to velocity) when satellite is in atmosphere
     * 
     * @param g2d Graphics context for drawing
     * @param centerX Screen X coordinate of Earth's center
     * @param centerY Screen Y coordinate of Earth's center  
     * @param currentScale Current meter-to-pixel conversion factor
     */
    private void drawAtmosphericDragVector(Graphics2D g2d, int centerX, int centerY, double currentScale) {
        Satellite satellite = simulation.getSatellite();
        if (satellite == null) return;
        
        // Get current drag acceleration
        double dragAcceleration = satellite.getDragAcceleration();
        if (dragAcceleration <= 0) return; // No drag or satellite outside atmosphere
        
        // Get satellite position and velocity for drag direction calculation
        double[] satPos = satellite.getPosition();
        double satX = satPos[0];
        double satY = satPos[1];
        
        // Calculate velocity vector direction using orbital mechanics
        double[] vel3D = calculateVelocityVector3D(satellite);
        double velX = vel3D[0];
        double velY = vel3D[1];
        double velMagnitude = Math.sqrt(velX * velX + velY * velY);
        
        if (velMagnitude <= 0) return; // No velocity
        
        // Drag direction is opposite to velocity direction
        double dragDirX = -velX / velMagnitude;
        double dragDirY = -velY / velMagnitude;
        
        // Scale the vector length for visibility
        // Use logarithmic scaling to handle wide range of acceleration values
        double baseLength = 80; // Base length in pixels
        double scaleFactor = Math.log10(dragAcceleration * 1e4 + 1) * 20; // Scale factor
        double vectorLength = Math.min(baseLength + scaleFactor, 200); // Cap at 200 pixels
        
        // Get satellite screen position
        int satScreenX = centerX + (int)(satX * currentScale);
        int satScreenY = centerY - (int)(satY * currentScale);
        
        // Calculate end point of drag vector
        int vectorEndX = satScreenX + (int)(dragDirX * vectorLength);
        int vectorEndY = satScreenY - (int)(dragDirY * vectorLength); // Flip Y axis
        
        // Draw the atmospheric drag vector as a thick gray line
        g2d.setColor(simulation.getDragColor());
        g2d.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(satScreenX, satScreenY, vectorEndX, vectorEndY);
        
        // Draw arrowhead to show direction
        drawArrowHead(g2d, satScreenX, satScreenY, vectorEndX, vectorEndY, simulation.getDragColor());
        
        // Add a small gray circle at the satellite position to mark the origin
        g2d.setColor(simulation.getDragColor());
        g2d.fillOval(satScreenX - 3, satScreenY - 3, 6, 6);
        
        // Optional: Draw magnitude indicator text
        if (vectorLength > 60) { // Only show text if vector is long enough
            g2d.setColor(simulation.getDragColor());
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            String magnitudeText = String.format("Drag: %.2e m/s²", dragAcceleration);
            
            // Position text near the end of vector
            int textX = vectorEndX + 10;
            int textY = vectorEndY - 5;
            
            // Keep text on screen
            if (textX > getWidth() - 100) textX = vectorEndX - 100;
            if (textY < 15) textY = vectorEndY + 15;
            
            g2d.drawString(magnitudeText, textX, textY);
        }
    }
     
    private void drawArrowHead(Graphics2D g2d, int startX, int startY, int endX, int endY, Color color) {
        // Calculate arrow direction
        double dx = endX - startX;
        double dy = endY - startY;
        double length = Math.sqrt(dx * dx + dy * dy);
        
        if (length == 0) return;
        
        // Normalize direction vector
        dx /= length;
        dy /= length;
        
        // Calculate arrowhead points
        double arrowLength = 15;
        double arrowAngle = Math.PI / 6; // 30 degrees
        
        // Left side of arrowhead
        double leftX = endX - arrowLength * (dx * Math.cos(arrowAngle) - dy * Math.sin(arrowAngle));
        double leftY = endY - arrowLength * (dy * Math.cos(arrowAngle) + dx * Math.sin(arrowAngle));
        
        // Right side of arrowhead
        double rightX = endX - arrowLength * (dx * Math.cos(-arrowAngle) - dy * Math.sin(-arrowAngle));
        double rightY = endY - arrowLength * (dy * Math.cos(-arrowAngle) + dx * Math.sin(-arrowAngle));
        
        // Draw arrowhead
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(endX, endY, (int)leftX, (int)leftY);
        g2d.drawLine(endX, endY, (int)rightX, (int)rightY);
    }
    
    /**
     * Draws the complete elliptical orbital path
     * 
     * @param g2d Graphics context for drawing
     * @param centerX Screen X coordinate of Earth's center
     * @param centerY Screen Y coordinate of Earth's center  
     * @param currentScale Current meter-to-pixel conversion factor
     */
    private void drawOrbit(Graphics2D g2d, int centerX, int centerY, double currentScale) {
        // Set up dashed line style for orbital path
        g2d.setColor(simulation.getOrbitColor());
        g2d.setStroke(new BasicStroke((float)Math.max(1, zoomFactor/2), BasicStroke.CAP_BUTT, 
                     BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f}, 0.0f));
        
        // Calculate ellipse parameters from orbital elements
        double a = simulation.getSemiMajorAxis() * currentScale; // Semi-major axis in pixels
        double e = simulation.getEccentricity();
        double b = a * Math.sqrt(1 - e * e); // Semi-minor axis using b = a√(1-e²)
        double c = a * e; // Distance from center to focus using c = ae
        
        // Create ellipse positioned with the celestial body at one focus (not center)
        // The celestial body is located at distance 'c' from the geometric center of the ellipse
        Ellipse2D.Double ellipse = new Ellipse2D.Double(
            centerX - a + c, centerY - b, 2 * a, 2 * b);
        g2d.draw(ellipse);
    }
    
    /**
     * Draws real-time orbital information as text overlay
     * ENHANCED: Now includes specific mechanical energy, specific angular momentum,
     * acceleration from Sun/Moon/Drag, and flight path angle
     * 
     * @param g2d Graphics context for text rendering
     * @param satellite The satellite object containing orbital data
     */
    private void drawInfo(Graphics2D g2d, Satellite satellite) {
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
        
        // === NEW CALCULATIONS ===
        
        // 1. Specific Mechanical Energy (ε = v²/2 - μ/r)
        double mu = simulation.getGravitationalConstant() * simulation.getEarthMass();
        double specificEnergy = (velocity * velocity) / 2.0 - mu / distanceFromCenter; // J/kg
        
        // 2. Specific Angular Momentum (h = r × v)
        // For simplified 2D case: |h| = |r| * |v| * sin(γ) where γ is flight path angle
        // We'll calculate the full cross product for accuracy
        double[] vel3D = calculateVelocityVector3D(satellite);
        double angMomX = pos3D[1] * vel3D[2] - pos3D[2] * vel3D[1];
        double angMomY = pos3D[2] * vel3D[0] - pos3D[0] * vel3D[2];
        double angMomZ = pos3D[0] * vel3D[1] - pos3D[1] * vel3D[0];
        double specificAngularMomentum = Math.sqrt(angMomX*angMomX + angMomY*angMomY + angMomZ*angMomZ);
        
        // 3. Flight Path Angle (γ = angle between velocity vector and local horizontal)
        // Local horizontal is perpendicular to position vector
        double velRadial = (satX * vel3D[0] + satY * vel3D[1] + satZ * vel3D[2]) / distanceFromCenter;
        double flightPathAngle = Math.toDegrees(Math.asin(velRadial / velocity));
        
        // 4. Calculate individual accelerations from Sun and Moon
        double moonAccelMagnitude = 0;
        double sunAccelMagnitude = 0;
        
        if (simulation.isLunarEffectsEnabled()) {
            moonAccelMagnitude = calculateLunarAcceleration(pos3D);
        }
        
        if (simulation.isSolarEffectsEnabled()) {
            sunAccelMagnitude = calculateSolarAcceleration(pos3D);
        }
        
        // Format information strings for display
        java.util.List<String> infoList = new java.util.ArrayList<>();
        
        // Basic orbital parameters
        infoList.add(String.format("%s Altitude: %.1f km", simulation.getCurrentBody(), altitude));
        infoList.add(String.format("Velocity: %.2f km/s", velocity / 1000));
        infoList.add(String.format("Period: %.2f hours", period));
        infoList.add(String.format("True Anomaly: %.1f°", Math.toDegrees(satellite.getTrueAnomaly())));
        
        // New advanced parameters
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
        
        // NEW: Atmospheric drag acceleration
        if (simulation.isAtmosphericDragEnabled()) {
            double dragAccel = satellite.getDragAcceleration();
            if (dragAccel > 0) {
                infoList.add(String.format("Drag Acceleration: %.3e m/s²", dragAccel));
                
                // Calculate and show atmospheric density using existing altitude variable
                double density = 1.225 * Math.exp(-(altitude * 1000) / 8500.0); // Convert km to m, same model as in calculator
                infoList.add(String.format("Atmospheric Density: %.3e kg/m³", density));
            } else {
                infoList.add("Drag: N/A (outside atmosphere)");
            }
        }
        
        // === Effects information if enabled ===
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
        
        // Convert list to array for drawing
        String[] info = infoList.toArray(new String[0]);
        
        // Draw information text in top-left corner
        for (int i = 0; i < info.length; i++) {
            g2d.drawString(info[i], 10, 20 + i * 15);
        }
    }
    
    /**
     * Calculates the 3D velocity vector of the satellite
     * Uses orbital mechanics formulas for accurate velocity calculation
     */
    private double[] calculateVelocityVector3D(Satellite satellite) {
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
    
    /**
     * Calculates the magnitude of gravitational acceleration from the Moon
     */
    private double calculateLunarAcceleration(double[] satPos) {
        double[] moonPos = simulation.getMoonPosition();
        double moonX = moonPos[0];
        double moonY = moonPos[1];
        double moonZ = 0; // Simplified to 2D
        
        // Vector from satellite to Moon
        double satToMoonX = moonX - satPos[0];
        double satToMoonY = moonY - satPos[1];
        double satToMoonZ = moonZ - satPos[2];
        double satMoonDistance = Math.sqrt(satToMoonX*satToMoonX + satToMoonY*satToMoonY + satToMoonZ*satToMoonZ);
        
        if (satMoonDistance > 0) {
            double moonMass = 7.342e22; // Moon mass in kg
            double G = 6.67430e-11; // Gravitational constant
            return G * moonMass / (satMoonDistance * satMoonDistance);
        }
        return 0;
    }
    
    /**
     * Calculates the magnitude of gravitational acceleration from the Sun
     */
    private double calculateSolarAcceleration(double[] satPos) {
        double[] sunPos = simulation.getSunPosition();
        double sunX = sunPos[0];
        double sunY = sunPos[1];
        double sunZ = 0; // Simplified to 2D
        
        // Vector from satellite to Sun
        double satToSunX = sunX - satPos[0];
        double satToSunY = sunY - satPos[1];
        double satToSunZ = sunZ - satPos[2];
        double satSunDistance = Math.sqrt(satToSunX*satToSunX + satToSunY*satToSunY + satToSunZ*satToSunZ);
        
        if (satSunDistance > 0) {
            double sunMass = 1.989e30; // Sun mass in kg
            double G = 6.67430e-11; // Gravitational constant
            return G * sunMass / (satSunDistance * satSunDistance);
        }
        return 0;
    }
}