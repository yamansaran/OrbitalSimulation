import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * SatelliteTrail class manages the visual trail of satellite movement
 * Extracted from SimulationPanel for better code organization
 */
public class SatelliteTrail {
    private List<Point2D.Double> trail;
    private int maxTrailLength;
    private float trailWidth;
    private Color trailColor;
    
    public SatelliteTrail(int maxLength, float width, Color color) {
        this.trail = new ArrayList<>();
        this.maxTrailLength = maxLength;
        this.trailWidth = width;
        this.trailColor = color;
    }
    
    /**
     * Adds a point to the trail in world coordinates
     */
    public void addPoint(double worldX, double worldY) {
        trail.add(new Point2D.Double(worldX, worldY));
        if (trail.size() > maxTrailLength) {
            trail.remove(0);
        }
    }
    
    /**
     * Clears all trail points
     */
    public void clear() {
        trail.clear();
    }
    
    /**
     * Draws the trail using the existing drawing logic from SimulationPanel
     */
    public void draw(Graphics2D g2d, double currentScale, int centerX, int centerY, double zoomFactor) {
        g2d.setColor(trailColor);
        g2d.setStroke(new BasicStroke(trailWidth * (float)Math.max(1, zoomFactor)));
        
        for (int i = 1; i < trail.size(); i++) {
            Point2D.Double p1World = trail.get(i - 1);
            Point2D.Double p2World = trail.get(i);
            
            // Convert world coordinates to screen coordinates (same as original)
            int p1x = centerX + (int)(p1World.x * currentScale);
            int p1y = centerY - (int)(p1World.y * currentScale);
            int p2x = centerX + (int)(p2World.x * currentScale);
            int p2y = centerY - (int)(p2World.y * currentScale);
            
            g2d.drawLine(p1x, p1y, p2x, p2y);
        }
    }
    
    /**
     * Updates trail settings and trims if necessary
     */
    public void updateSettings(int maxLength, float width, Color color) {
        this.maxTrailLength = maxLength;
        this.trailWidth = width;
        this.trailColor = color;
        
        while (trail.size() > maxTrailLength) {
            trail.remove(0);
        }
    }
    
    public int size() {
        return trail.size();
    }
}