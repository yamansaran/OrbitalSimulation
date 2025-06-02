// Import necessary Java libraries for GUI, graphics, events, and data structures
import javax.swing.*;
import javax.swing.Timer;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Orbital Mechanics Simulation Program By Yaman Saran
 * 
 * This program simulates satellite orbits around Earth using Kepler's laws and Newton's law of gravitation.
 * It supports both classical orbital elements (a, e, i, ω, Ω) and equinoctial elements (a, h, k, p, q).
 * 
 * Mathematical Foundation:
 * - Uses Kepler's equation to solve orbital motion: M = E - e⋅sin(E)
 * - Implements coordinate transformations from orbital plane to 3D space
 * - Converts between classical and equinoctial element representations
 */
public class OrbitalSimulation extends JFrame {
    // Window dimensions and layout constants
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 800;
    private static final int CONTROL_PANEL_HEIGHT = 280;
    
    // Physical constants for Earth and orbital mechanics (now configurable)
    private double earthRadius = 6371000; // Earth's radius in meters
    private double gravitationalConstant = 6.67430e-11; // Gravitational constant in m³/kg⋅s²
    private double earthMass = 5.972e24; // Earth's mass in kg
    
    // Display constants (now configurable)
    private double baseScale = 5e-6; // Base scale factor for converting meters to pixels
    private int maxTrailLength = 500; // Maximum trail points
    private Color earthColor = new Color(100, 149, 237); // Earth body color
    private Color earthOutlineColor = new Color(34, 139, 34); // Earth outline color
    private Color satelliteColor = Color.RED; // Satellite color
    private Color trailColor = new Color(255, 255, 0, 100); // Trail color (semi-transparent yellow)
    private Color orbitColor = new Color(255, 255, 255, 80); // Orbit path color
    private int satelliteSize = 4; // Base satellite size in pixels
    private int animationDelay = 50; // Animation timer delay in milliseconds
    
    // GUI components for simulation display and animation control
    private SimulationPanel simulationPanel; // Custom panel for drawing the orbital simulation
    private Timer animationTimer; // Swing timer for smooth animation updates
    private double timeMultiplier = 1.0; // Speed multiplier for time progression (1.0 = real time)
    private boolean isPaused = false; // Flag to control animation pause/resume
    
    // Classical orbital elements (Keplerian elements)
    // These define the shape, size, and orientation of an orbit
    private double semiMajorAxis = 7000000; // 'a' - half the major axis, defines orbit size (meters)
    private double eccentricity = 0.1; // 'e' - orbit shape (0=circle, 0-1=ellipse, 1=parabola)
    private double inclination = 0; // 'i' - orbit plane tilt relative to equator (degrees)
    private double argumentOfPeriapsis = 0; // 'ω' - orientation of ellipse in orbital plane (degrees)
    private double longitudeOfAscendingNode = 0; // 'Ω' - rotation of orbital plane (degrees)
    private double trueAnomaly = 0; // 'ν' - satellite's position along orbit (degrees)
    
    // Satellite object that handles orbital mechanics calculations
    private Satellite satellite;
    
    // Flag to switch between classical and equinoctial element input modes
    // Equinoctial elements avoid singularities for circular/equatorial orbits
    private boolean useEquinoctialElements = false;
    
    // Auto-clear trail settings
    private boolean autoClearOnZoom = true; // Auto-clear trail when zooming
    private boolean autoClearOnUpdate = true; // Auto-clear trail when updating orbit

    // Getter methods for SimulationPanel to access private fields
    public double getBaseScale() { return baseScale; }
    public double getEarthRadius() { return earthRadius; }
    public Color getEarthColor() { return earthColor; }
    public Color getEarthOutlineColor() { return earthOutlineColor; }
    public Color getSatelliteColor() { return satelliteColor; }
    public Color getTrailColor() { return trailColor; }
    public Color getOrbitColor() { return orbitColor; }
    public int getSatelliteSize() { return satelliteSize; }
    public int getMaxTrailLength() { return maxTrailLength; }
    public double getSemiMajorAxis() { return semiMajorAxis; }
    public double getEccentricity() { return eccentricity; }
    public Satellite getSatellite() { return satellite; }
    public boolean getAutoClearOnZoom() { return autoClearOnZoom; }

    /**
     * Constructor: Sets up the main window and initializes all components
     */
    public OrbitalSimulation() {
        // Configure the main window properties
        setTitle("Orbital Mechanics Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Exit program when window is closed
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null); // Center window on screen
        
        // Initialize GUI components and create initial satellite
        initializeComponents();
        createSatellite();
        startAnimation();
    }
    
    /**
     * Sets up the main window layout with simulation panel and control panel
     */
    private void initializeComponents() {
        setLayout(new BorderLayout()); // Use border layout for main window
        
        // Create menu bar with options
        createMenuBar();
        
        // Create and add the simulation display panel (center area)
        simulationPanel = new SimulationPanel(this);
        add(simulationPanel, BorderLayout.CENTER);
        
        // Create and add the control panel (bottom area)
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }
    
  
    
    /**
     * Creates the menu bar with options menu
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // Options menu
        JMenu optionsMenu = new JMenu("Options");
        
        // Orbital Parameters submenu
        JMenuItem orbitalParamsItem = new JMenuItem("Orbital Parameters...");
        orbitalParamsItem.addActionListener(e -> showOrbitalParametersDialog());
        optionsMenu.add(orbitalParamsItem);
        
        // Physical Constants submenu
        JMenuItem physicalConstantsItem = new JMenuItem("Physical Constants...");
        physicalConstantsItem.addActionListener(e -> showPhysicalConstantsDialog());
        optionsMenu.add(physicalConstantsItem);
        
        // Display Settings submenu
        JMenuItem displaySettingsItem = new JMenuItem("Display Settings...");
        displaySettingsItem.addActionListener(e -> showDisplaySettingsDialog());
        optionsMenu.add(displaySettingsItem);
        
        // Trail Settings submenu
        JMenuItem trailSettingsItem = new JMenuItem("Trail Settings...");
        trailSettingsItem.addActionListener(e -> showTrailSettingsDialog());
        optionsMenu.add(trailSettingsItem);
        
        optionsMenu.addSeparator();
        
        // Reset to Defaults option
        JMenuItem resetDefaultsItem = new JMenuItem("Reset to Defaults");
        resetDefaultsItem.addActionListener(e -> resetToDefaults());
        optionsMenu.add(resetDefaultsItem);
        
        menuBar.add(optionsMenu);
        setJMenuBar(menuBar);
    }
    
    /**
     * Shows the orbital parameters configuration dialog
     */
    private void showOrbitalParametersDialog() {
        JDialog dialog = new JDialog(this, "Orbital Parameters", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Create input fields for orbital parameters
        JTextField smaField = new JTextField(String.valueOf(semiMajorAxis / 1000), 10);
        JTextField eccField = new JTextField(String.valueOf(eccentricity), 10);
        JTextField incField = new JTextField(String.valueOf(inclination), 10);
        JTextField argPeriField = new JTextField(String.valueOf(argumentOfPeriapsis), 10);
        JTextField lanField = new JTextField(String.valueOf(longitudeOfAscendingNode), 10);
        JTextField anomalyField = new JTextField(String.valueOf(trueAnomaly), 10);
        
        // Add components to dialog
        gbc.gridy = 0;
        gbc.gridx = 0; panel.add(new JLabel("Semi-major axis (km):"), gbc);
        gbc.gridx = 1; panel.add(smaField, gbc);
        
        gbc.gridy = 1;
        gbc.gridx = 0; panel.add(new JLabel("Eccentricity:"), gbc);
        gbc.gridx = 1; panel.add(eccField, gbc);
        
        gbc.gridy = 2;
        gbc.gridx = 0; panel.add(new JLabel("Inclination (°):"), gbc);
        gbc.gridx = 1; panel.add(incField, gbc);
        
        gbc.gridy = 3;
        gbc.gridx = 0; panel.add(new JLabel("Argument of Periapsis (°):"), gbc);
        gbc.gridx = 1; panel.add(argPeriField, gbc);
        
        gbc.gridy = 4;
        gbc.gridx = 0; panel.add(new JLabel("Longitude of Asc. Node (°):"), gbc);
        gbc.gridx = 1; panel.add(lanField, gbc);
        
        gbc.gridy = 5;
        gbc.gridx = 0; panel.add(new JLabel("True Anomaly (°):"), gbc);
        gbc.gridx = 1; panel.add(anomalyField, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(e -> {
            try {
                semiMajorAxis = Double.parseDouble(smaField.getText()) * 1000;
                eccentricity = Double.parseDouble(eccField.getText());
                inclination = Double.parseDouble(incField.getText());
                argumentOfPeriapsis = Double.parseDouble(argPeriField.getText());
                longitudeOfAscendingNode = Double.parseDouble(lanField.getText());
                trueAnomaly = Double.parseDouble(anomalyField.getText());
                createSatellite();
                simulationPanel.repaint();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid input values!");
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridy = 6;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gbc);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    /**
     * Shows the trail settings configuration dialog
     */
    private void showTrailSettingsDialog() {
        JDialog dialog = new JDialog(this, "Trail Settings", true);
        dialog.setSize(350, 200);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Auto-clear trail options
        JCheckBox autoClearZoomBox = new JCheckBox("Auto-clear trail when zooming", autoClearOnZoom);
        JCheckBox autoClearUpdateBox = new JCheckBox("Auto-clear trail when updating orbit", autoClearOnUpdate);
        
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(autoClearZoomBox, gbc);
        
        gbc.gridy = 1;
        panel.add(autoClearUpdateBox, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(e -> {
            autoClearOnZoom = autoClearZoomBox.isSelected();
            autoClearOnUpdate = autoClearUpdateBox.isSelected();
            dialog.dispose();
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gbc);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    /**
     * Shows the physical constants configuration dialog
     */
    private void showPhysicalConstantsDialog() {
        JDialog dialog = new JDialog(this, "Physical Constants", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Create input fields for physical constants
        JTextField earthRadiusField = new JTextField(String.valueOf(earthRadius / 1000), 10);
        JTextField earthMassField = new JTextField(String.format("%.3e", earthMass), 10);
        JTextField gravConstField = new JTextField(String.format("%.5e", gravitationalConstant), 10);
        
        // Add components to dialog
        gbc.gridy = 0;
        gbc.gridx = 0; panel.add(new JLabel("Earth Radius (km):"), gbc);
        gbc.gridx = 1; panel.add(earthRadiusField, gbc);
        
        gbc.gridy = 1;
        gbc.gridx = 0; panel.add(new JLabel("Earth Mass (kg):"), gbc);
        gbc.gridx = 1; panel.add(earthMassField, gbc);
        
        gbc.gridy = 2;
        gbc.gridx = 0; panel.add(new JLabel("Gravitational Constant:"), gbc);
        gbc.gridx = 1; panel.add(gravConstField, gbc);
        
        // Add explanation labels
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Note: Changing these values affects orbital calculations"), gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        JButton resetButton = new JButton("Reset to Earth");
        
        okButton.addActionListener(e -> {
            try {
                earthRadius = Double.parseDouble(earthRadiusField.getText()) * 1000;
                earthMass = Double.parseDouble(earthMassField.getText());
                gravitationalConstant = Double.parseDouble(gravConstField.getText());
                createSatellite();
                simulationPanel.repaint();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid input values!");
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        resetButton.addActionListener(e -> {
            earthRadiusField.setText("6371");
            earthMassField.setText("5.972e24");
            gravConstField.setText("6.67430e-11");
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(resetButton);
        
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gbc);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    /**
     * Shows the display settings configuration dialog
     */
    private void showDisplaySettingsDialog() {
        JDialog dialog = new JDialog(this, "Display Settings", true);
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(this);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Trail Settings Tab
        JPanel trailPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JTextField trailLengthField = new JTextField(String.valueOf(maxTrailLength), 10);
        JSlider trailOpacitySlider = new JSlider(0, 255, trailColor.getAlpha());
        
        gbc.gridy = 0;
        gbc.gridx = 0; trailPanel.add(new JLabel("Trail Length (points):"), gbc);
        gbc.gridx = 1; trailPanel.add(trailLengthField, gbc);
        
        gbc.gridy = 1;
        gbc.gridx = 0; trailPanel.add(new JLabel("Trail Opacity:"), gbc);
        gbc.gridx = 1; trailPanel.add(trailOpacitySlider, gbc);
        
        tabbedPane.add("Trail", trailPanel);
        
        // Colors Tab
        JPanel colorsPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JButton earthColorButton = new JButton("     ");
        earthColorButton.setBackground(earthColor);
        earthColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(dialog, "Choose Earth Color", earthColor);
            if (newColor != null) {
                earthColor = newColor;
                earthColorButton.setBackground(newColor);
            }
        });
        
        JButton satelliteColorButton = new JButton("     ");
        satelliteColorButton.setBackground(satelliteColor);
        satelliteColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(dialog, "Choose Satellite Color", satelliteColor);
            if (newColor != null) {
                satelliteColor = newColor;
                satelliteColorButton.setBackground(newColor);
            }
        });
        
        JButton trailColorButton = new JButton("     ");
        trailColorButton.setBackground(new Color(trailColor.getRed(), trailColor.getGreen(), trailColor.getBlue()));
        trailColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(dialog, "Choose Trail Color", 
                new Color(trailColor.getRed(), trailColor.getGreen(), trailColor.getBlue()));
            if (newColor != null) {
                trailColor = new Color(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), trailColor.getAlpha());
                trailColorButton.setBackground(newColor);
            }
        });
        
        gbc.gridy = 0;
        gbc.gridx = 0; colorsPanel.add(new JLabel("Earth Color:"), gbc);
        gbc.gridx = 1; colorsPanel.add(earthColorButton, gbc);
        
        gbc.gridy = 1;
        gbc.gridx = 0; colorsPanel.add(new JLabel("Satellite Color:"), gbc);
        gbc.gridx = 1; colorsPanel.add(satelliteColorButton, gbc);
        
        gbc.gridy = 2;
        gbc.gridx = 0; colorsPanel.add(new JLabel("Trail Color:"), gbc);
        gbc.gridx = 1; colorsPanel.add(trailColorButton, gbc);
        
        tabbedPane.add("Colors", colorsPanel);
        
        // Scale & Size Tab
        JPanel scalePanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JTextField baseScaleField = new JTextField(String.format("%.2e", baseScale), 10);
        JTextField satSizeField = new JTextField(String.valueOf(satelliteSize), 10);
        JTextField animDelayField = new JTextField(String.valueOf(animationDelay), 10);
        
        gbc.gridy = 0;
        gbc.gridx = 0; scalePanel.add(new JLabel("Base Scale Factor:"), gbc);
        gbc.gridx = 1; scalePanel.add(baseScaleField, gbc);
        
        gbc.gridy = 1;
        gbc.gridx = 0; scalePanel.add(new JLabel("Satellite Size (pixels):"), gbc);
        gbc.gridx = 1; scalePanel.add(satSizeField, gbc);
        
        gbc.gridy = 2;
        gbc.gridx = 0; scalePanel.add(new JLabel("Animation Delay (ms):"), gbc);
        gbc.gridx = 1; scalePanel.add(animDelayField, gbc);
        
        tabbedPane.add("Scale & Size", scalePanel);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        JButton applyButton = new JButton("Apply");
        
        okButton.addActionListener(e -> {
            try {
                // Apply trail settings
                maxTrailLength = Integer.parseInt(trailLengthField.getText());
                trailColor = new Color(trailColor.getRed(), trailColor.getGreen(), 
                                     trailColor.getBlue(), trailOpacitySlider.getValue());
                
                // Apply scale settings
                baseScale = Double.parseDouble(baseScaleField.getText());
                satelliteSize = Integer.parseInt(satSizeField.getText());
                int newDelay = Integer.parseInt(animDelayField.getText());
                if (newDelay != animationDelay) {
                    animationDelay = newDelay;
                    restartAnimation();
                }
                
                simulationPanel.updateSettings();
                simulationPanel.repaint();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid input values!");
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        applyButton.addActionListener(e -> {
            try {
                // Apply settings without closing dialog
                maxTrailLength = Integer.parseInt(trailLengthField.getText());
                trailColor = new Color(trailColor.getRed(), trailColor.getGreen(), 
                                     trailColor.getBlue(), trailOpacitySlider.getValue());
                baseScale = Double.parseDouble(baseScaleField.getText());
                satelliteSize = Integer.parseInt(satSizeField.getText());
                int newDelay = Integer.parseInt(animDelayField.getText());
                if (newDelay != animationDelay) {
                    animationDelay = newDelay;
                    restartAnimation();
                }
                
                simulationPanel.updateSettings();
                simulationPanel.repaint();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid input values!");
            }
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(applyButton);
        
        dialog.setLayout(new BorderLayout());
        dialog.add(tabbedPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    /**
     * Resets all settings to their default values
     */
    private void resetToDefaults() {
        int result = JOptionPane.showConfirmDialog(this, 
            "Reset all settings to default values?", 
            "Confirm Reset", 
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            // Reset physical constants
            earthRadius = 6371000;
            gravitationalConstant = 6.67430e-11;
            earthMass = 5.972e24;
            
            // Reset orbital parameters
            semiMajorAxis = 7000000;
            eccentricity = 0.1;
            inclination = 0;
            argumentOfPeriapsis = 0;
            longitudeOfAscendingNode = 0;
            trueAnomaly = 0;
            
            // Reset display settings
            baseScale = 5e-6;
            maxTrailLength = 500;
            earthColor = new Color(100, 149, 237);
            earthOutlineColor = new Color(34, 139, 34);
            satelliteColor = Color.RED;
            trailColor = new Color(255, 255, 0, 100);
            orbitColor = new Color(255, 255, 255, 80);
            satelliteSize = 4;
            animationDelay = 50;
            
            // Reset animation
            timeMultiplier = 1.0;
            useEquinoctialElements = false;
            autoClearOnZoom = true;
            autoClearOnUpdate = true;
            
            // Update simulation
            createSatellite();
            simulationPanel.updateSettings();
            simulationPanel.clearTrail();
            simulationPanel.resetZoom();
            restartAnimation();
            
            JOptionPane.showMessageDialog(this, "Settings reset to defaults.");
        }
    }
    
    /**
     * Restarts the animation timer with current delay setting
     */
    private void restartAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        startAnimation();
    }
    
    /**
     * Creates the control panel with orbital parameters, time controls, and zoom controls
     * Uses GridBagLayout for precise component positioning
     */
    private JPanel createControlPanel() {
        // Initialize panel with grid bag layout for flexible component arrangement
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(WINDOW_WIDTH, CONTROL_PANEL_HEIGHT));
        panel.setBorder(BorderFactory.createTitledBorder("Orbital Parameters & Controls"));
        
        // Grid bag constraints for component positioning and sizing
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8); // Padding around components
        gbc.fill = GridBagConstraints.HORIZONTAL; // Allow horizontal stretching
        
        // ROW 1: Primary orbital parameters (semi-major axis, eccentricity, inclination)
        gbc.gridy = 0; // First row
        
        // Semi-major axis input (defines orbit size)
        gbc.gridx = 0;
        panel.add(new JLabel("Semi-major axis (km):"), gbc);
        gbc.gridx = 1;
        JTextField smaField = new JTextField(String.valueOf(semiMajorAxis / 1000), 10);
        smaField.setPreferredSize(new Dimension(100, 25));
        panel.add(smaField, gbc);
        
        // Eccentricity input (defines orbit shape: 0=circle, approaching 1=elongated ellipse)
        gbc.gridx = 2;
        panel.add(new JLabel("Eccentricity:"), gbc);
        gbc.gridx = 3;
        JTextField eccField = new JTextField(String.valueOf(eccentricity), 10);
        eccField.setPreferredSize(new Dimension(100, 25));
        panel.add(eccField, gbc);
        
        // Inclination input (orbit plane angle relative to Earth's equator)
        gbc.gridx = 4;
        panel.add(new JLabel("Inclination (°):"), gbc);
        gbc.gridx = 5;
        JTextField incField = new JTextField(String.valueOf(inclination), 10);
        incField.setPreferredSize(new Dimension(100, 25));
        panel.add(incField, gbc);
        
        // ROW 2: Secondary orbital parameters and control buttons
        gbc.gridy = 1; // Second row
        
        // Argument of periapsis (orientation of ellipse within the orbital plane)
        gbc.gridx = 0;
        panel.add(new JLabel("Arg. Periapsis (°):"), gbc);
        gbc.gridx = 1;
        JTextField argPeriField = new JTextField(String.valueOf(argumentOfPeriapsis), 10);
        argPeriField.setPreferredSize(new Dimension(100, 25));
        panel.add(argPeriField, gbc);
        
        // Longitude of ascending node (rotation of the orbital plane in space)
        gbc.gridx = 2;
        panel.add(new JLabel("Long. Asc. Node (°):"), gbc);
        gbc.gridx = 3;
        JTextField lanField = new JTextField(String.valueOf(longitudeOfAscendingNode), 10);
        lanField.setPreferredSize(new Dimension(100, 25));
        panel.add(lanField, gbc);
        
        // Button to apply orbital parameter changes
        gbc.gridx = 4;
        JButton updateButton = new JButton("Update Orbit");
        updateButton.setPreferredSize(new Dimension(120, 30));
        panel.add(updateButton, gbc);
        
        // Button to switch between classical and equinoctial element representations
        gbc.gridx = 5;
        JButton toggleElementsButton = new JButton("Switch to Equinoctial");
        toggleElementsButton.setPreferredSize(new Dimension(150, 30));
        panel.add(toggleElementsButton, gbc);
        
        // ROW 3: Time control system for animation speed
        gbc.gridy = 2; // Third row
        
        // Time speed control label
        gbc.gridx = 0;
        panel.add(new JLabel("Time Speed:"), gbc);
        
        // Logarithmic slider for time speed (powers of 10 from 0.001x to 10000x)
        gbc.gridx = 1;
        gbc.gridwidth = 2; // Span two columns
        JSlider speedSlider = new JSlider(-3, 4, 0); // -3 = 0.001x, 4 = 10000x
        speedSlider.setMajorTickSpacing(1);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setPreferredSize(new Dimension(200, 40));
        panel.add(speedSlider, gbc);
        
        // Custom time speed input field for precise control
        gbc.gridwidth = 1; // Reset to single column
        gbc.gridx = 3;
        JTextField customSpeedField = new JTextField("1", 8);
        customSpeedField.setPreferredSize(new Dimension(80, 25));
        panel.add(customSpeedField, gbc);
        
        // Button to apply custom time speed
        gbc.gridx = 4;
        JButton setSpeedButton = new JButton("Set");
        setSpeedButton.setPreferredSize(new Dimension(50, 25));
        panel.add(setSpeedButton, gbc);
        
        // Animation control buttons (pause/resume, reset position)
        gbc.gridx = 5;
        JButton pauseButton = new JButton("Pause");
        pauseButton.setPreferredSize(new Dimension(80, 30));
        panel.add(pauseButton, gbc);
        
        gbc.gridx = 6;
        JButton resetButton = new JButton("Reset");
        resetButton.setPreferredSize(new Dimension(80, 30));
        panel.add(resetButton, gbc);
        
        gbc.gridx = 7;
        JButton clearTrailButton = new JButton("Clear Trail");
        clearTrailButton.setPreferredSize(new Dimension(90, 30));
        panel.add(clearTrailButton, gbc);

        // Display current time speed multiplier
        gbc.gridx = 8;
        JLabel speedLabel = new JLabel("1x");
        speedLabel.setPreferredSize(new Dimension(40, 25));
        panel.add(speedLabel, gbc);
        
        // ROW 4: Zoom control system for visual scaling
        gbc.gridy = 3; // Fourth row
        
        // Zoom control label
        gbc.gridx = 0;
        panel.add(new JLabel("Zoom:"), gbc);
        
        // Zoom in button (increases visual scale by factor of 1.5)
        gbc.gridx = 1;
        JButton zoomInButton = new JButton("Zoom In (+)");
        zoomInButton.setPreferredSize(new Dimension(100, 25));
        panel.add(zoomInButton, gbc);
        
        // Zoom out button (decreases visual scale by factor of 1.5)
        gbc.gridx = 2;
        JButton zoomOutButton = new JButton("Zoom Out (-)");
        zoomOutButton.setPreferredSize(new Dimension(100, 25));
        panel.add(zoomOutButton, gbc);
        
        // Reset zoom to default 1.0x scale
        gbc.gridx = 3;
        JButton resetZoomButton = new JButton("Reset Zoom");
        resetZoomButton.setPreferredSize(new Dimension(100, 25));
        panel.add(resetZoomButton, gbc);
        
        // Display current zoom level
        gbc.gridx = 4;
        JLabel zoomLabel = new JLabel("1.0x");
        zoomLabel.setPreferredSize(new Dimension(50, 25));
        panel.add(zoomLabel, gbc);
        
        // EVENT LISTENERS: Define behavior for user interactions
        
        /**
         * Update Orbit Button: Applies new orbital parameters
         * Handles both classical and equinoctial element inputs
         */
        updateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (useEquinoctialElements) {
                        // Parse equinoctial elements (a, h, k, p, q)
                        // These are non-singular alternatives to classical elements
                        double a_eq = Double.parseDouble(smaField.getText()) * 1000; // Convert km to m
                        double h = Double.parseDouble(eccField.getText()); // h = e⋅sin(ω + Ω)
                        double k = Double.parseDouble(incField.getText()); // k = e⋅cos(ω + Ω)
                        double p = Double.parseDouble(argPeriField.getText()); // p = tan(i/2)⋅sin(Ω)
                        double q = Double.parseDouble(lanField.getText()); // q = tan(i/2)⋅cos(Ω)
                        
                        // Convert equinoctial elements to classical orbital elements
                        // This allows the simulation to use familiar Keplerian elements internally
                        double[] classical = equinoctialToClassical(a_eq, h, k, p, q);
                        semiMajorAxis = classical[0];
                        eccentricity = classical[1];
                        inclination = classical[2];
                        argumentOfPeriapsis = classical[3];
                        longitudeOfAscendingNode = classical[4];
                    } else {
                        // Parse classical orbital elements directly
                        semiMajorAxis = Double.parseDouble(smaField.getText()) * 1000; // Convert km to m
                        eccentricity = Double.parseDouble(eccField.getText());
                        inclination = Double.parseDouble(incField.getText());
                        argumentOfPeriapsis = Double.parseDouble(argPeriField.getText());
                        longitudeOfAscendingNode = Double.parseDouble(lanField.getText());
                    }
                    // Create new satellite with updated parameters and refresh display
                    createSatellite();
                    if (autoClearOnUpdate) {
                        simulationPanel.clearTrail();
                    }
                    simulationPanel.repaint();
                } catch (NumberFormatException ex) {
                    // Handle invalid input with user-friendly error message
                    JOptionPane.showMessageDialog(OrbitalSimulation.this, "Invalid input values!");
                }
            }
        });
        
        /**
         * Toggle Elements Button: Switches between classical and equinoctial input modes
         * Updates field labels and converts values between representations
         */
        toggleElementsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                useEquinoctialElements = !useEquinoctialElements; // Toggle mode
                // Update all field labels and values to match new element type
                updateFieldLabels(panel, gbc, toggleElementsButton, smaField, eccField, incField, argPeriField, lanField);
            }
        });
        
        /**
         * Time Speed Slider: Logarithmic control for animation speed
         * Uses powers of 10 for intuitive speed scaling
         */
        speedSlider.addChangeListener(e -> {
            int value = speedSlider.getValue();
            if (value == 4) {
                // Special case: maximum slider position = 10000x speed
                timeMultiplier = 10000;
                speedLabel.setText("10000x");
            } else {
                // Calculate speed as power of 10: 10^(-3) to 10^3
                timeMultiplier = Math.pow(10, value);
                speedLabel.setText(String.format("%.0fx", timeMultiplier));
            }
            // Sync custom speed field with slider value
            customSpeedField.setText(String.valueOf((int)timeMultiplier));
        });
        
        /**
         * Custom Speed Set Button: Applies user-specified time speed
         * Validates input range and updates both slider and display
         */
        setSpeedButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    double customSpeed = Double.parseDouble(customSpeedField.getText());
                    if (customSpeed > 0 && customSpeed <= 10000) {
                        timeMultiplier = customSpeed;
                        speedLabel.setText(String.format("%.0fx", timeMultiplier));
                        // Update slider position to approximate logarithmic value
                        if (customSpeed >= 10000) {
                            speedSlider.setValue(4); // Maximum position
                        } else {
                            // Calculate logarithmic slider position
                            int sliderValue = (int)Math.round(Math.log10(customSpeed));
                            speedSlider.setValue(Math.max(-3, Math.min(3, sliderValue)));
                        }
                    } else {
                        JOptionPane.showMessageDialog(OrbitalSimulation.this, "Speed must be between 0.001 and 10000!");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(OrbitalSimulation.this, "Invalid speed value!");
                }
            }
        });
        
        /**
         * Pause/Resume Button: Controls animation state
         */
        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isPaused = !isPaused; // Toggle pause state
                pauseButton.setText(isPaused ? "Resume" : "Pause"); // Update button text
            }
        });
        
        /**
         * Reset Button: Returns satellite to initial position and clears trail
         */
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                trueAnomaly = 0; // Reset to starting position in orbit
                createSatellite(); // Recreate satellite with reset position
                simulationPanel.clearTrail(); // Clear the orbital trail display
                simulationPanel.repaint(); // Refresh the display
            }
        });
        
        /**
         * Clear Trail Button: Manually clears the satellite trail
         */
        clearTrailButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                simulationPanel.clearTrail(); // Clear the orbital trail display
                simulationPanel.repaint(); // Refresh the display
            }
        });
        
        // ZOOM CONTROL EVENT LISTENERS
        
        /**
         * Zoom In Button: Increases visual scale by factor of 1.5
         */
        zoomInButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                simulationPanel.zoomIn();
                if (autoClearOnZoom) {
                    simulationPanel.clearTrail();
                }
                zoomLabel.setText(String.format("%.1fx", simulationPanel.getZoomFactor()));
            }
        });
        
        /**
         * Zoom Out Button: Decreases visual scale by factor of 1.5
         */
        zoomOutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                simulationPanel.zoomOut();
                if (autoClearOnZoom) {
                    simulationPanel.clearTrail();
                }
                zoomLabel.setText(String.format("%.1fx", simulationPanel.getZoomFactor()));
            }
        });
        
        /**
         * Reset Zoom Button: Returns to default 1.0x zoom and centers view
         */
        resetZoomButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                simulationPanel.resetZoom();
                zoomLabel.setText(String.format("%.1fx", simulationPanel.getZoomFactor()));
            }
        });
        
        return panel;
    }
    
    /**
     * Updates field labels and values when switching between classical and equinoctial elements
     * Performs automatic conversion between element representations
     */
    private void updateFieldLabels(JPanel panel, GridBagConstraints gbc, JButton toggleButton, 
                                  JTextField smaField, JTextField eccField, JTextField incField, 
                                  JTextField argPeriField, JTextField lanField) {
        // Remove old labels by scanning all panel components
        Component[] components = panel.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JLabel) {
                JLabel label = (JLabel) components[i];
                String text = label.getText();
                // Remove labels related to orbital elements
                if (text.contains("Semi-major") || text.contains("Eccentricity") || text.contains("Inclination") ||
                    text.contains("Arg.") || text.contains("Long.") || text.contains("a (km)") || 
                    text.contains("h") || text.contains("k") || text.contains("p") || text.contains("q")) {
                    panel.remove(label);
                }
            }
        }
        
        // Add new labels based on current element representation mode
        gbc.gridy = 0; // Position in first row
        if (useEquinoctialElements) {
            // EQUINOCTIAL ELEMENTS MODE
            // These elements avoid singularities present in classical elements
            toggleButton.setText("Switch to Classical");
            
            // a: Semi-major axis (same as classical)
            gbc.gridx = 0;
            panel.add(new JLabel("a (km):"), gbc, 0);
            
            // h: e⋅sin(ω + Ω) - combines eccentricity and longitude information
            gbc.gridx = 2;
            panel.add(new JLabel("h:"), gbc, 2);
            
            // k: e⋅cos(ω + Ω) - combines eccentricity and longitude information
            gbc.gridx = 4;
            panel.add(new JLabel("k:"), gbc, 4);
            
            gbc.gridy = 1; // Second row
            
            // p: tan(i/2)⋅sin(Ω) - combines inclination and node information
            gbc.gridx = 0;
            panel.add(new JLabel("p:"), gbc, 6);
            
            // q: tan(i/2)⋅cos(Ω) - combines inclination and node information
            gbc.gridx = 2;
            panel.add(new JLabel("q:"), gbc, 8);
            
            // Convert current classical elements to equinoctial for display
            // This ensures continuity when switching representations
            double[] equinoctial = classicalToEquinoctial(semiMajorAxis, eccentricity, inclination, 
                                                         argumentOfPeriapsis, longitudeOfAscendingNode);
            smaField.setText(String.format("%.1f", equinoctial[0] / 1000)); // Convert m to km
            eccField.setText(String.format("%.6f", equinoctial[1])); // h component
            incField.setText(String.format("%.6f", equinoctial[2])); // k component
            argPeriField.setText(String.format("%.6f", equinoctial[3])); // p component
            lanField.setText(String.format("%.6f", equinoctial[4])); // q component
        } else {
            // CLASSICAL ELEMENTS MODE
            // Traditional Keplerian orbital elements
            toggleButton.setText("Switch to Equinoctial");
            
            // Classical element labels with physical interpretations
            gbc.gridx = 0;
            panel.add(new JLabel("Semi-major axis (km):"), gbc, 0); // Orbit size
            gbc.gridx = 2;
            panel.add(new JLabel("Eccentricity:"), gbc, 2); // Orbit shape
            gbc.gridx = 4;
            panel.add(new JLabel("Inclination (°):"), gbc, 4); // Orbit tilt
            
            gbc.gridy = 1;
            gbc.gridx = 0;
            panel.add(new JLabel("Arg. Periapsis (°):"), gbc, 6); // Ellipse orientation
            gbc.gridx = 2;
            panel.add(new JLabel("Long. Asc. Node (°):"), gbc, 8); // Orbital plane rotation
            
            // Display current classical elements
            smaField.setText(String.format("%.1f", semiMajorAxis / 1000)); // Convert m to km
            eccField.setText(String.format("%.3f", eccentricity));
            incField.setText(String.format("%.1f", inclination));
            argPeriField.setText(String.format("%.1f", argumentOfPeriapsis));
            lanField.setText(String.format("%.1f", longitudeOfAscendingNode));
        }
        
        // Refresh panel layout and display
        panel.revalidate();
        panel.repaint();
    }
    
    /**
     * Mathematical conversion from classical to equinoctial orbital elements
     * 
     * Equinoctial elements avoid singularities that occur in classical elements
     * for circular orbits (e=0) and equatorial orbits (i=0)
     * 
     * @param a Semi-major axis (meters)
     * @param e Eccentricity (dimensionless)
     * @param i_deg Inclination (degrees)
     * @param omega_deg Argument of periapsis (degrees)
     * @param Omega_deg Longitude of ascending node (degrees)
     * @return Array containing [a, h, k, p, q]
     */
    private double[] classicalToEquinoctial(double a, double e, double i_deg, double omega_deg, double Omega_deg) {
        // Convert angles from degrees to radians for mathematical calculations
        double i = Math.toRadians(i_deg);
        double omega = Math.toRadians(omega_deg);
        double Omega = Math.toRadians(Omega_deg);
        
        // Calculate equinoctial elements using trigonometric transformations
        // h and k encode both eccentricity and periapsis orientation
        double h = e * Math.sin(omega + Omega); // Eccentricity vector Y-component
        double k = e * Math.cos(omega + Omega); // Eccentricity vector X-component
        
        // p and q encode both inclination and node orientation
        double p = Math.tan(i / 2) * Math.sin(Omega); // Inclination vector Y-component
        double q = Math.tan(i / 2) * Math.cos(Omega); // Inclination vector X-component
        
        return new double[]{a, h, k, p, q};
    }
    
    /**
     * Mathematical conversion from equinoctial to classical orbital elements
     * 
     * Reconstructs traditional Keplerian elements from non-singular equinoctial representation
     * 
     * @param a Semi-major axis (meters)
     * @param h Eccentricity vector Y-component
     * @param k Eccentricity vector X-component  
     * @param p Inclination vector Y-component
     * @param q Inclination vector X-component
     * @return Array containing [a, e, i_deg, omega_deg, Omega_deg]
     */
    private double[] equinoctialToClassical(double a, double h, double k, double p, double q) {
        // Reconstruct eccentricity magnitude from vector components
        // e = √(h² + k²) - magnitude of eccentricity vector
        double e = Math.sqrt(h * h + k * k);
        
        // Reconstruct inclination from vector components
        // i = 2⋅arctan(√(p² + q²)) - orbital plane tilt
        double i = 2 * Math.atan(Math.sqrt(p * p + q * q));
        
        // Reconstruct longitude of ascending node
        // Ω = arctan(p/q) - rotation of orbital plane
        double Omega = Math.atan2(p, q);
        
        // Reconstruct argument of periapsis
        // ω = arctan(h/k) - Ω - orientation of ellipse in orbital plane
        double omega = Math.atan2(h, k) - Omega;
        
        // Convert angles from radians back to degrees for user interface
        double i_deg = Math.toDegrees(i);
        double omega_deg = Math.toDegrees(omega);
        double Omega_deg = Math.toDegrees(Omega);
        
        // Normalize angles to [0, 360) degree range for consistency
        omega_deg = ((omega_deg % 360) + 360) % 360;
        Omega_deg = ((Omega_deg % 360) + 360) % 360;
        
        return new double[]{a, e, i_deg, omega_deg, Omega_deg};
    }
    
    /**
     * Creates a new satellite object with current orbital parameters
     */
    private void createSatellite() {
        satellite = new Satellite(semiMajorAxis, eccentricity, inclination, 
                                argumentOfPeriapsis, longitudeOfAscendingNode, trueAnomaly,
                                gravitationalConstant, earthMass);
    }
    
    /**
     * Starts the animation timer for smooth orbital motion display
     * Timer fires every animationDelay ms for smooth animation
     */
    private void startAnimation() {
        animationTimer = new Timer(animationDelay, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isPaused) {
                    // Update satellite position with time step scaled by speed multiplier
                    // 0.05 represents 0.05 seconds of simulated time per frame
                    satellite.updatePosition(0.05 * timeMultiplier);
                    simulationPanel.repaint(); // Trigger redraw of simulation display
                }
            }
        });
        animationTimer.start(); // Begin animation loop
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new OrbitalSimulation().setVisible(true);
            }
        });
    }
}