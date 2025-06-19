import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * OrbitalRenderer handles all drawing operations for the orbital simulation
 * Separated from SimulationPanel for better organization and maintainability
 */
public class OrbitalRenderer {
    private OrbitalSimulation simulation;
    
    public OrbitalRenderer(OrbitalSimulation simulation) {
        this.simulation = simulation;
    }
    
    /**
     * Main rendering method - draws all visual elements
     */
    public void render(Graphics2D g2d, int centerX, int centerY, double currentScale, double zoomFactor) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw celestial body (Earth, etc.)
        drawCelestialBody(g2d, centerX, centerY, currentScale);
        
        // Draw celestial objects if effects are enabled
        if (simulation.isLunarEffectsEnabled()) {
            drawMoon(g2d, centerX, centerY, currentScale);
        }
        
        if (simulation.isSolarEffectsEnabled()) {
            drawSun(g2d, centerX, centerY, currentScale);
        }
        
        // Draw acceleration vectors
        if (simulation.isLunarEffectsEnabled() || simulation.isSolarEffectsEnabled()) {
            drawGravitationalAccelerationVector(g2d, centerX, centerY, currentScale);
        }
        
        if (simulation.isAtmosphericDragEnabled()) {
            drawAtmosphericDragVector(g2d, centerX, centerY, currentScale);
        }
        
        // Draw satellite and orbit
        Satellite satellite = simulation.getSatellite();
        if (satellite != null) {
            drawOrbit(g2d, centerX, centerY, currentScale, zoomFactor);
            drawSatellite(g2d, centerX, centerY, currentScale, zoomFactor, satellite);
        }
    }
    
    /**
     * Draws the celestial body at the center
     */
    private void drawCelestialBody(Graphics2D g2d, int centerX, int centerY, double currentScale) {
        int bodyRadiusPixels = (int) (simulation.getEarthRadius() * currentScale);
        
        BufferedImage bodyImage = simulation.getCelestialBodyImage();
        if (bodyImage != null) {
            // Draw the celestial body image
            int imageSize = bodyRadiusPixels * 2;
            g2d.drawImage(bodyImage, centerX - bodyRadiusPixels, centerY - bodyRadiusPixels, 
                         imageSize, imageSize, null);
            
            // Add a subtle outline
            g2d.setColor(simulation.getEarthOutlineColor());
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawOval(centerX - bodyRadiusPixels, centerY - bodyRadiusPixels, 
                        imageSize, imageSize);
        } else {
            // Fallback: Draw colored circle
            g2d.setColor(simulation.getEarthColor());
            g2d.fillOval(centerX - bodyRadiusPixels, centerY - bodyRadiusPixels, 
                        bodyRadiusPixels * 2, bodyRadiusPixels * 2);
            
            g2d.setColor(simulation.getEarthOutlineColor());
            g2d.drawOval(centerX - bodyRadiusPixels, centerY - bodyRadiusPixels, 
                        bodyRadiusPixels * 2, bodyRadiusPixels * 2);
        }
    }
    
    /**
     * Draws the Moon at its current orbital position
     */
    private void drawMoon(Graphics2D g2d, int centerX, int centerY, double currentScale) {
        double[] moonPos = simulation.getMoonPosition();
        double moonX = moonPos[0];
        double moonY = moonPos[1];
        
        int moonScreenX = centerX + (int)(moonX * currentScale);
        int moonScreenY = centerY - (int)(moonY * currentScale);
        
        double moonRadius = 1737400; // Moon's radius in meters
        int moonRadiusPixels = Math.max(4, (int)(moonRadius * currentScale));
        
        // Try to load and draw Moon image
        try {
            BufferedImage moonImage = null;
            try {
                File moonFile = new File("src/resources/Moon.png");
                if (moonFile.exists()) {
                    moonImage = ImageIO.read(moonFile);
                }
            } catch (Exception e) {
                // Image loading failed, use fallback
            }
            
            if (moonImage != null) {
                int imageSize = moonRadiusPixels * 2;
                g2d.drawImage(moonImage, moonScreenX - moonRadiusPixels, moonScreenY - moonRadiusPixels, 
                             imageSize, imageSize, null);
                
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawOval(moonScreenX - moonRadiusPixels, moonScreenY - moonRadiusPixels, 
                            imageSize, imageSize);
            } else {
                // Fallback: Draw Moon as colored circle
                g2d.setColor(simulation.getMoonColor());
                g2d.fillOval(moonScreenX - moonRadiusPixels, moonScreenY - moonRadiusPixels, 
                            moonRadiusPixels * 2, moonRadiusPixels * 2);
                
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawOval(moonScreenX - moonRadiusPixels, moonScreenY - moonRadiusPixels, 
                            moonRadiusPixels * 2, moonRadiusPixels * 2);
            }
        } catch (Exception e) {
            // Final fallback
            g2d.setColor(simulation.getMoonColor());
            g2d.fillOval(moonScreenX - moonRadiusPixels, moonScreenY - moonRadiusPixels, 
                        moonRadiusPixels * 2, moonRadiusPixels * 2);
        }
        
        // Draw Moon orbit path when zoomed out
        if (currentScale < 1e-8) {
            g2d.setColor(new Color(128, 128, 128, 50));
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, 
                         BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f}, 0.0f));
            
            double moonOrbitRadius = 384400000 * currentScale;
            int orbitDiameter = (int)(moonOrbitRadius * 2);
            g2d.drawOval(centerX - (int)moonOrbitRadius, centerY - (int)moonOrbitRadius, 
                        orbitDiameter, orbitDiameter);
        }
        
        // Draw line from Earth to Moon
        if (currentScale > 1e-9) {
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawLine(centerX, centerY, moonScreenX, moonScreenY);
        }
    }
    
    /**
     * Draws the Sun as a directional line extending to screen edge
     */
    private void drawSun(Graphics2D g2d, int centerX, int centerY, double currentScale) {
        double[] sunPos = simulation.getSunPosition();
        double sunX = sunPos[0];
        double sunY = sunPos[1];
        
        double sunDistance = Math.sqrt(sunX * sunX + sunY * sunY);
        if (sunDistance == 0) return;
        
        double sunDirX = sunX / sunDistance;
        double sunDirY = sunY / sunDistance;
        
        // Calculate line extending to screen edge
        int screenWidth = 2000; // Use large value to ensure it reaches edge
        int screenHeight = 2000;
        double maxDistance = Math.max(screenWidth, screenHeight) * 2;
        
        int lineEndX = centerX + (int)(sunDirX * maxDistance);
        int lineEndY = centerY - (int)(sunDirY * maxDistance);
        
        // Draw the Sun direction line
        g2d.setColor(simulation.getSunColor());
        g2d.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(centerX, centerY, lineEndX, lineEndY);
        
        // Draw arrowhead near Earth
        double arrowDistance = Math.max(50, simulation.getEarthRadius() * currentScale * 2);
        int arrowX = centerX + (int)(sunDirX * arrowDistance);
        int arrowY = centerY - (int)(sunDirY * arrowDistance);
        drawArrowHead(g2d, centerX, centerY, arrowX, arrowY, simulation.getSunColor());
    }
    
    /**
     * Draws the satellite at its current position
     */
    private void drawSatellite(Graphics2D g2d, int centerX, int centerY, double currentScale, double zoomFactor, Satellite satellite) {
        double[] pos = satellite.getPosition();
        int satX = centerX + (int) (pos[0] * currentScale);
        int satY = centerY - (int) (pos[1] * currentScale);
        
        BufferedImage satImage = simulation.getSatelliteImage();
        if (satImage != null) {
            int satSize = (int)Math.max(simulation.getSatelliteSize() * 2, simulation.getSatelliteSize() * 2 * zoomFactor);
            satSize = Math.max(satSize, 8);
            g2d.drawImage(satImage, satX - satSize/2, satY - satSize/2, satSize, satSize, null);
        } else {
            g2d.setColor(simulation.getSatelliteColor());
            int satSize = (int)Math.max(simulation.getSatelliteSize(), simulation.getSatelliteSize() * zoomFactor);
            g2d.fillOval(satX - satSize/2, satY - satSize/2, satSize, satSize);
        }
    }
    
    /**
     * Draws the complete elliptical orbital path
     */
    private void drawOrbit(Graphics2D g2d, int centerX, int centerY, double currentScale, double zoomFactor) {
        g2d.setColor(simulation.getOrbitColor());
        g2d.setStroke(new BasicStroke((float)Math.max(1, zoomFactor/2), BasicStroke.CAP_BUTT, 
                     BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f}, 0.0f));
        
        double a = simulation.getSemiMajorAxis() * currentScale;
        double e = simulation.getEccentricity();
        double b = a * Math.sqrt(1 - e * e);
        double c = a * e;
        
        Ellipse2D.Double ellipse = new Ellipse2D.Double(
            centerX - a + c, centerY - b, 2 * a, 2 * b);
        g2d.draw(ellipse);
    }
    
    /**
     * Draws the gravitational acceleration vector from Sun and/or Moon
     */
    private void drawGravitationalAccelerationVector(Graphics2D g2d, int centerX, int centerY, double currentScale) {
        Satellite satellite = simulation.getSatellite();
        if (satellite == null) return;
        
        double[] satPos = satellite.getPosition();
        double satX = satPos[0];
        double satY = satPos[1];
        
        double combinedAccelX = 0;
        double combinedAccelY = 0;
        
        if (simulation.isLunarEffectsEnabled()) {
            double[] lunarAccel = AccelerationCalculator.calculateLunarAcceleration(satX, satY, simulation);
            combinedAccelX += lunarAccel[0];
            combinedAccelY += lunarAccel[1];
        }
        
        if (simulation.isSolarEffectsEnabled()) {
            double[] solarAccel = AccelerationCalculator.calculateSolarAcceleration(satX, satY, simulation);
            combinedAccelX += solarAccel[0];
            combinedAccelY += solarAccel[1];
        }
        
        double combinedAccelMagnitude = Math.sqrt(combinedAccelX * combinedAccelX + combinedAccelY * combinedAccelY);
        if (combinedAccelMagnitude > 0) {
            double accelDirectionX = combinedAccelX / combinedAccelMagnitude;
            double accelDirectionY = combinedAccelY / combinedAccelMagnitude;
            
            double baseLength = 100;
            double scaleFactor = Math.log10(combinedAccelMagnitude * 1e6 + 1) * 15;
            double vectorLength = Math.min(baseLength + scaleFactor, 300);
            
            int satScreenX = centerX + (int)(satX * currentScale);
            int satScreenY = centerY - (int)(satY * currentScale);
            
            int vectorEndX = satScreenX + (int)(accelDirectionX * vectorLength);
            int vectorEndY = satScreenY - (int)(accelDirectionY * vectorLength);
            
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(satScreenX, satScreenY, vectorEndX, vectorEndY);
            
            drawArrowHead(g2d, satScreenX, satScreenY, vectorEndX, vectorEndY, Color.BLUE);
            
            g2d.setColor(Color.BLUE);
            g2d.fillOval(satScreenX - 3, satScreenY - 3, 6, 6);
            
            if (vectorLength > 80) {
                g2d.setColor(Color.BLUE);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                String sourceText = "";
                if (simulation.isLunarEffectsEnabled() && simulation.isSolarEffectsEnabled()) {
                    sourceText = "Combined";
                } else if (simulation.isLunarEffectsEnabled()) {
                    sourceText = "Lunar";
                } else if (simulation.isSolarEffectsEnabled()) {
                    sourceText = "Solar";
                }
                String magnitudeText = String.format("%s a: %.2e m/s²", sourceText, combinedAccelMagnitude);
                
                int textX = vectorEndX + 10;
                int textY = vectorEndY - 5;
                
                if (textX > 1200) textX = vectorEndX - 120; // Approximate screen width
                if (textY < 15) textY = vectorEndY + 15;
                
                g2d.drawString(magnitudeText, textX, textY);
            }
        }
    }
    
    /**
     * Draws the atmospheric drag acceleration vector
     */
    private void drawAtmosphericDragVector(Graphics2D g2d, int centerX, int centerY, double currentScale) {
        Satellite satellite = simulation.getSatellite();
        if (satellite == null) return;
        
        double dragAcceleration = satellite.getDragAcceleration();
        if (dragAcceleration <= 0) return;
        
        double[] satPos = satellite.getPosition();
        double satX = satPos[0];
        double satY = satPos[1];
        
        double[] vel3D = VelocityCalculator.calculateVelocityVector3D(satellite, simulation);
        double velX = vel3D[0];
        double velY = vel3D[1];
        double velMagnitude = Math.sqrt(velX * velX + velY * velY);
        
        if (velMagnitude <= 0) return;
        
        double dragDirX = -velX / velMagnitude;
        double dragDirY = -velY / velMagnitude;
        
        double baseLength = 80;
        double scaleFactor = Math.log10(dragAcceleration * 1e4 + 1) * 20;
        double vectorLength = Math.min(baseLength + scaleFactor, 200);
        
        int satScreenX = centerX + (int)(satX * currentScale);
        int satScreenY = centerY - (int)(satY * currentScale);
        
        int vectorEndX = satScreenX + (int)(dragDirX * vectorLength);
        int vectorEndY = satScreenY - (int)(dragDirY * vectorLength);
        
        g2d.setColor(simulation.getDragColor());
        g2d.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(satScreenX, satScreenY, vectorEndX, vectorEndY);
        
        drawArrowHead(g2d, satScreenX, satScreenY, vectorEndX, vectorEndY, simulation.getDragColor());
        
        g2d.setColor(simulation.getDragColor());
        g2d.fillOval(satScreenX - 3, satScreenY - 3, 6, 6);
        
        if (vectorLength > 60) {
            g2d.setColor(simulation.getDragColor());
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            String magnitudeText = String.format("Drag: %.2e m/s²", dragAcceleration);
            
            int textX = vectorEndX + 10;
            int textY = vectorEndY - 5;
            
            if (textX > 1100) textX = vectorEndX - 100;
            if (textY < 15) textY = vectorEndY + 15;
            
            g2d.drawString(magnitudeText, textX, textY);
        }
    }
    
    /**
     * Draws an arrowhead at the end of a vector
     */
    private void drawArrowHead(Graphics2D g2d, int startX, int startY, int endX, int endY, Color color) {
        double dx = endX - startX;
        double dy = endY - startY;
        double length = Math.sqrt(dx * dx + dy * dy);
        
        if (length == 0) return;
        
        dx /= length;
        dy /= length;
        
        double arrowLength = 15;
        double arrowAngle = Math.PI / 6;
        
        double leftX = endX - arrowLength * (dx * Math.cos(arrowAngle) - dy * Math.sin(arrowAngle));
        double leftY = endY - arrowLength * (dy * Math.cos(arrowAngle) + dx * Math.sin(arrowAngle));
        
        double rightX = endX - arrowLength * (dx * Math.cos(-arrowAngle) - dy * Math.sin(-arrowAngle));
        double rightY = endY - arrowLength * (dy * Math.cos(-arrowAngle) + dx * Math.sin(-arrowAngle));
        
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(endX, endY, (int)leftX, (int)leftY);
        g2d.drawLine(endX, endY, (int)rightX, (int)rightY);
    }
}