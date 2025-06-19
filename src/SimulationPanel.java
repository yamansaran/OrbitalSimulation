import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Custom JPanel for rendering the orbital simulation
 * NOW FOCUSED ONLY ON: zooming, panning, and coordinating rendering
 * All drawing operations have been extracted to helper classes
 */
public class SimulationPanel extends JPanel {
    // Zoom and pan state variables
    private double zoomFactor = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    
    // Helper classes for different responsibilities
    private SatelliteTrail satelliteTrail;
    private OrbitalRenderer renderer;
    private OrbitalInfoDisplay infoDisplay;
    
    // Reference to the main simulation
    private OrbitalSimulation simulation;
    
    /**
     * Constructor: Sets up mouse controls and initializes helper classes
     */
    public SimulationPanel(OrbitalSimulation simulation) {
        this.simulation = simulation;
        
        // Initialize helper classes
        this.satelliteTrail = new SatelliteTrail(
            simulation.getMaxTrailLength(),
            simulation.getTrailWidth(),
            simulation.getTrailColor()
        );
        this.renderer = new OrbitalRenderer(simulation);
        this.infoDisplay = new OrbitalInfoDisplay(simulation);
        
        setBackground(Color.BLACK);
        setupMouseControls();
    }
    
    /**
     * Sets up mouse controls for zoom and pan
     */
    private void setupMouseControls() {
        // Mouse wheel listener for zooming
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getWheelRotation() < 0) {
                    zoomIn();
                } else {
                    zoomOut();
                }
                if (simulation.getAutoClearOnZoom()) {
                    clearTrail();
                }
                repaint();
            }
        });
        
        // Mouse listeners for click-and-drag panning
        MouseAdapter mouseHandler = new MouseAdapter() {
            private Point lastPanPoint;
        
            public void mousePressed(MouseEvent e) {
                lastPanPoint = e.getPoint();
            }
            
            public void mouseDragged(MouseEvent e) {
                if (lastPanPoint != null) {
                    Point currentPoint = e.getPoint();
                    
                    int deltaX = currentPoint.x - lastPanPoint.x;
                    int deltaY = currentPoint.y - lastPanPoint.y;
                    
                    offsetX += deltaX / zoomFactor;
                    offsetY += deltaY / zoomFactor;
                    
                    lastPanPoint = currentPoint;
                    repaint();
                }
            }
            
            public void mouseReleased(MouseEvent e) {
                lastPanPoint = null;
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    
    /**
     * Updates display settings when changed in options
     */
    public void updateSettings() {
        satelliteTrail.updateSettings(
            simulation.getMaxTrailLength(),
            simulation.getTrailWidth(),
            simulation.getTrailColor()
        );
    }
    
    /**
     * Main rendering method - now much cleaner and focused
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Calculate rendering parameters
        int centerX = getWidth() / 2 + (int)(offsetX * zoomFactor);
        int centerY = getHeight() / 2 + (int)(offsetY * zoomFactor);
        double currentScale = simulation.getBaseScale() * zoomFactor;
        
        // Use the renderer to draw all visual elements
        renderer.render(g2d, centerX, centerY, currentScale, zoomFactor);
        
        // Handle satellite trail
        Satellite satellite = simulation.getSatellite();
        if (satellite != null) {
            // Update trail with current position
            double[] pos = satellite.getPosition();
            satelliteTrail.addPoint(pos[0], pos[1]);
            
            // Draw trail
            satelliteTrail.draw(g2d, currentScale, centerX, centerY, zoomFactor);
            
            // Draw information overlay
            infoDisplay.drawInfo(g2d, satellite);
        }
        
        // Draw UI overlays
        infoDisplay.drawUIOverlays(g2d, getWidth(), getHeight(), zoomFactor);
    }
    
    // === ZOOM AND PAN CONTROLS ===
    
    public void zoomIn() {
        zoomFactor *= 1.5;
        if (zoomFactor > 100.0) zoomFactor = 50.0;
    }
    
    public void zoomOut() {
        zoomFactor /= 1.5;
        if (zoomFactor < 0.01) zoomFactor = 0.1;
    }
    
    public void resetZoom() {
        zoomFactor = 1.0;
        offsetX = 0;
        offsetY = 0;
        repaint();
    }
    
    public double getZoomFactor() {
        return zoomFactor;
    }
    
    public void clearTrail() {
        satelliteTrail.clear();
    }
}