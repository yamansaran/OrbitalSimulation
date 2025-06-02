import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Custom JPanel for rendering the orbital simulation
 * Handles zooming, panning, and drawing all visual elements
 */
public class SimulationPanel extends JPanel {
    // Zoom and pan state variables
    private double zoomFactor = 1.0; // Current zoom level (1.0 = default)
    private double offsetX = 0; // Horizontal pan offset
    private double offsetY = 0; // Vertical pan offset
    
    // Satellite trail for showing recent orbital path
    private List<Point> trail = new ArrayList<>();
    
    // Reference to the main simulation for accessing parameters
    private OrbitalSimulation simulation;
    
    /**
     * Constructor: Sets up mouse controls for zoom and pan
     */
    public SimulationPanel(OrbitalSimulation simulation) {
        this.simulation = simulation;
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
                    lastPanPoint = currentPoint;
                    repaint(); // Update display during drag
                }
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
        // Trim trail if it's longer than new max length
        while (trail.size() > simulation.getMaxTrailLength()) {
            trail.remove(0);
        }
    }
    
    /**
     * Increases zoom level by factor of 1.5, with maximum limit of 50x
     */
    public void zoomIn() {
        zoomFactor *= 1.5;
        if (zoomFactor > 50.0) zoomFactor = 50.0; // Prevent excessive zoom
    }
    
    /**
     * Decreases zoom level by factor of 1.5, with minimum limit of 0.1x
     */
    public void zoomOut() {
        zoomFactor /= 1.5;
        if (zoomFactor < 0.1) zoomFactor = 0.1; // Prevent negative/zero zoom
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
     * Clears the satellite trail (used when resetting simulation)
     */
    public void clearTrail() {
        trail.clear();
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
        
        Satellite satellite = simulation.getSatellite();
        if (satellite != null) {
            // DRAW ORBITAL PATH: Show the complete elliptical orbit
            drawOrbit(g2d, centerX, centerY, currentScale);
            
            // GET SATELLITE POSITION: Calculate current satellite coordinates
            double[] pos = satellite.getPosition(); // Returns [x, y] in meters
            int satX = centerX + (int) (pos[0] * currentScale); // Convert to screen X
            int satY = centerY - (int) (pos[1] * currentScale); // Convert to screen Y (flip Y axis)
            
            // UPDATE TRAIL: Add current position to satellite trail
            trail.add(new Point(satX, satY));
            if (trail.size() > simulation.getMaxTrailLength()) {
                trail.remove(0); // Remove oldest point to maintain trail length
            }
            
            // DRAW TRAIL: Show satellite's recent orbital path
            g2d.setColor(simulation.getTrailColor());
            g2d.setStroke(new BasicStroke((float)Math.max(1, zoomFactor))); // Scale line thickness with zoom
            for (int i = 1; i < trail.size(); i++) {
                Point p1 = trail.get(i - 1);
                Point p2 = trail.get(i);
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y); // Connect consecutive trail points
            }
            
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
    }
    
    /**
     * Draws the complete orbital ellipse as a dashed white line
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
     * 
     * @param g2d Graphics context for text rendering
     * @param satellite The satellite object containing orbital data
     */
    private void drawInfo(Graphics2D g2d, Satellite satellite) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        
        // Calculate current orbital parameters from satellite state
        double[] pos = satellite.getPosition();
        
        // Calculate altitude: distance from celestial body surface
        // √(x² + y²) gives distance from body center, subtract body radius
        double altitude = (Math.sqrt(pos[0] * pos[0] + pos[1] * pos[1]) - simulation.getEarthRadius()) / 1000;
        
        // Get current orbital velocity and period from satellite
        double velocity = satellite.getVelocity(); // m/s
        double period = satellite.getOrbitalPeriod() / 3600; // Convert seconds to hours
        
        // Format information strings for display
        String[] info = {
            String.format("%s Altitude: %.1f km", simulation.getCurrentBody(), altitude),
            String.format("Velocity: %.2f km/s", velocity / 1000), // Convert m/s to km/s
            String.format("Period: %.2f hours", period),
            String.format("True Anomaly: %.1f°", Math.toDegrees(satellite.getTrueAnomaly()))
        };
        
        // Draw information text in top-left corner
        for (int i = 0; i < info.length; i++) {
            g2d.drawString(info[i], 10, 20 + i * 15);
        }
    }
}