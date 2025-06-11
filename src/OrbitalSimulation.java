import javax.swing.*;
import javax.swing.Timer;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.*;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Orbital Mechanics Simulation Program By Yaman Saran
 * Enhanced with Lunar Effects and Date/Time Control
 * 
 * Now includes ability to change simulation date/time while paused,
 * with celestial bodies updating to their correct positions.
 */
public class OrbitalSimulation extends JFrame {
    // Window dimensions and layout constants
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 800;
    private static final int CONTROL_PANEL_HEIGHT = 320;
    
    // Physical constants for Earth and orbital mechanics (now configurable)
    private double earthRadius = 6371000;
    private double gravitationalConstant = 6.67430e-11;
    private double earthMass = 5.972e24;
    
    // Display constants (now configurable)
    private double baseScale = 5e-6;
    private int maxTrailLength = 500;
    private float trailWidth = 1.0f;
    private Color earthColor = new Color(100, 149, 237);
    private Color earthOutlineColor = new Color(34, 139, 34);
    private Color satelliteColor = Color.RED;
    private Color trailColor = new Color(255, 255, 0, 100);
    private Color orbitColor = new Color(255, 255, 255, 80);
    private Color moonColor = new Color(192, 192, 192);
    private int satelliteSize = 4;
    private int animationDelay = 50;
    
    // GUI components for simulation display and animation control
    private SimulationPanel simulationPanel;
    private Timer animationTimer;
    private double timeMultiplier = 1.0;
    private boolean isPaused = false;
    
    // Dialog manager for handling all dialogs
    private OrbitalDialogManager dialogManager;
    
    // Classical orbital elements
    private double semiMajorAxis = 7000000;
    private double eccentricity = 0.1;
    private double inclination = 0;
    private double argumentOfPeriapsis = 0;
    private double longitudeOfAscendingNode = 0;
    private double trueAnomaly = 0;
    
    // Satellite object that handles orbital mechanics calculations
    private Satellite satellite;
    
    // Flag to switch between classical and equinoctial element input modes
    private boolean useEquinoctialElements = false;
    
    // Auto-clear trail settings
    private boolean autoClearOnZoom = false;
    private boolean autoClearOnUpdate = true;
    
    // Current celestial body settings
    private String currentBody = "Earth";
    private BufferedImage celestialBodyImage;
    
    // Current satellite settings
    private String currentSatelliteType = "Default";
    private BufferedImage satelliteImage;
    
    // === ENHANCED: Lunar and Solar Effects System with Date/Time Control ===
    private boolean lunarEffectsEnabled = false;
    private boolean solarEffectsEnabled = false;
    private long simulationStartTime;
    private double currentSimulationTime; // Current simulation time in seconds since Unix epoch
    private static final double LUNAR_ORBITAL_PERIOD = 29.530 * 24 * 3600;
    private static final double SOLAR_ORBITAL_PERIOD = 365.25 * 24 * 3600;
    private static final double INITIAL_MOON_ANGLE = 84.7;
    private static final double INITIAL_SUN_ANGLE = 281.0;
    private static final double MOON_EARTH_DISTANCE = 384400000;
    private static final double SUN_EARTH_DISTANCE = 149597870700.0;
    private static final double MOON_MASS = 7.342e22;
    private static final double SUN_MASS = 1.989e30;
    private Color sunColor = new Color(255, 255, 0);
    private JLabel dateTimeLabel;
    
    // Available satellite types
    private static final String[] SATELLITE_TYPES = {
        "Default", "Satellite1", "Satellite2", "Satellite3", "ISS", 
        "Hubble", "Voyager", "Sputnik", "Millennium Falcon"
    };
    
    // Celestial body data
    private static final Map<String, double[]> CELESTIAL_BODIES = new HashMap<String, double[]>() {{
        put("Sun", new double[]{696340000, 1.989e30, 6.67430e-11});
        put("Mercury", new double[]{2439700, 3.301e23, 6.67430e-11});
        put("Venus", new double[]{6051800, 4.867e24, 6.67430e-11});
        put("Earth", new double[]{6371000, 5.972e24, 6.67430e-11});
        put("Moon", new double[]{1737400, 7.342e22, 6.67430e-11});
        put("Mars", new double[]{3389500, 6.417e23, 6.67430e-11});
        put("Jupiter", new double[]{69911000, 1.898e27, 6.67430e-11});
        put("Saturn", new double[]{58232000, 5.683e26, 6.67430e-11});
        put("Uranus", new double[]{25362000, 8.681e25, 6.67430e-11});
        put("Neptune", new double[]{24622000, 1.024e26, 6.67430e-11});
        put("Pluto", new double[]{1188300, 1.309e22, 6.67430e-11});
        put("Tatooine", new double[]{5232500, 3e24, 6.67430e-11});
    }};

    /**
     * Constructor: Sets up the main window and initializes all components
     */
    public OrbitalSimulation() {
        setTitle("Orbital Mechanics Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        
        // Initialize simulation time (January 1, 1970 00:00:00 UTC)
        simulationStartTime = 0; // Unix timestamp 0
        currentSimulationTime = 0; // Start at 0 seconds
        
        // Initialize dialog manager
        dialogManager = new OrbitalDialogManager(this);
        
        // Initialize GUI components and create initial satellite
        initializeComponents();
        loadCelestialBodyImage();
        loadSatelliteImage();
        createSatellite();
        startAnimation();
    }
    
    /**
     * Sets up the main window layout with simulation panel and control panel
     */
    private void initializeComponents() {
        setLayout(new BorderLayout());
        
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
     * Creates the menu bar with options menu - now includes Date/Time option
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // Options menu
        JMenu optionsMenu = new JMenu("Options");
        
        // Date/Time menu item - NEW
        JMenuItem dateTimeItem = new JMenuItem("Set Date/Time...");
        dateTimeItem.addActionListener(e -> dialogManager.showDateTimeDialog());
        optionsMenu.add(dateTimeItem);
        
        optionsMenu.addSeparator();
        
        // All existing dialog menu items
        JMenuItem orbitalParamsItem = new JMenuItem("Orbital Parameters...");
        orbitalParamsItem.addActionListener(e -> dialogManager.showOrbitalParametersDialog());
        optionsMenu.add(orbitalParamsItem);
        
        JMenuItem physicalConstantsItem = new JMenuItem("Physical Constants...");
        physicalConstantsItem.addActionListener(e -> dialogManager.showPhysicalConstantsDialog());
        optionsMenu.add(physicalConstantsItem);
        
        JMenuItem displaySettingsItem = new JMenuItem("Display Settings...");
        displaySettingsItem.addActionListener(e -> dialogManager.showDisplaySettingsDialog());
        optionsMenu.add(displaySettingsItem);
        
        JMenuItem trailSettingsItem = new JMenuItem("Trail Settings...");
        trailSettingsItem.addActionListener(e -> dialogManager.showTrailSettingsDialog());
        optionsMenu.add(trailSettingsItem);
        
        JMenuItem nonKeplerianItem = new JMenuItem("Non-Keplerian Effects...");
        nonKeplerianItem.addActionListener(e -> dialogManager.showNonKeplerianEffectsDialog());
        optionsMenu.add(nonKeplerianItem);
        
        JMenuItem celestialBodyItem = new JMenuItem("Select Celestial Body...");
        celestialBodyItem.addActionListener(e -> dialogManager.showCelestialBodyDialog());
        optionsMenu.add(celestialBodyItem);
        
        JMenuItem satelliteTypeItem = new JMenuItem("Select Satellite Type...");
        satelliteTypeItem.addActionListener(e -> dialogManager.showSatelliteTypeDialog());
        optionsMenu.add(satelliteTypeItem);
        
        optionsMenu.addSeparator();
        
        JMenuItem resetDefaultsItem = new JMenuItem("Reset to Defaults");
        resetDefaultsItem.addActionListener(e -> resetToDefaults());
        optionsMenu.add(resetDefaultsItem);
        
        menuBar.add(optionsMenu);
        setJMenuBar(menuBar);
    }
    
    /**
     * === NEW: Updates the date/time label with current simulation time ===
     */
    public void updateDateTimeDisplay() {
        LocalDateTime dateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0).plusSeconds((long)currentSimulationTime);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss");
        dateTimeLabel.setText(formatter.format(dateTime) + " UTC");
    }
    
    /**
     * === NEW: Sets the current simulation time (used by Date/Time dialog) ===
     */
    public void setCurrentSimulationTime(double time) {
        this.currentSimulationTime = time;
    }
    
    /**
     * === NEW: Returns whether simulation is paused (needed by Date/Time dialog) ===
     */
    public boolean isPaused() {
        return isPaused;
    }
    
    /**
     * Sets the current satellite type and loads the corresponding image
     */
    public void setSatelliteType(String satelliteType) {
        currentSatelliteType = satelliteType;
        loadSatelliteImage();
        simulationPanel.repaint();
    }
    
    /**
     * Loads the image for the current satellite type
     */
    private void loadSatelliteImage() {
        if (currentSatelliteType.equals("Default")) {
            satelliteImage = null;
            return;
        }
        
        try {
            String imagePath = "src/resources/" + currentSatelliteType + ".png";
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                satelliteImage = ImageIO.read(imageFile);
                System.out.println("Loaded satellite image: " + imagePath);
            } else {
                satelliteImage = null;
                System.out.println("Satellite image not found: " + imagePath + " - using colored dot instead");
            }
        } catch (IOException e) {
            satelliteImage = null;
            System.out.println("Error loading satellite image: " + e.getMessage());
        }
    }
    
    /**
     * Sets the current celestial body and updates all related parameters
     */
    public void setCelestialBody(String bodyName) {
        if (CELESTIAL_BODIES.containsKey(bodyName)) {
            currentBody = bodyName;
            double[] data = CELESTIAL_BODIES.get(bodyName);
            
            earthRadius = data[0];
            earthMass = data[1];
            gravitationalConstant = data[2];
            
            semiMajorAxis = earthRadius * 1.2;
            
            loadCelestialBodyImage();
            createSatellite();
            
            if (autoClearOnUpdate) {
                simulationPanel.clearTrail();
            }
            
            setTitle("Orbital Mechanics Simulation - " + bodyName);
            simulationPanel.repaint();
        }
    }
    
    /**
     * Loads the image for the current celestial body
     */
    private void loadCelestialBodyImage() {
        try {
            String imagePath = "src/resources/" + currentBody + ".png";
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                celestialBodyImage = ImageIO.read(imageFile);
            } else {
                celestialBodyImage = null;
                System.out.println("Image not found: " + imagePath + " - using colored circle instead");
            }
        } catch (IOException e) {
            celestialBodyImage = null;
            System.out.println("Error loading celestial body image: " + e.getMessage());
        }
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
            trailWidth = 1.0f;
            earthColor = new Color(100, 149, 237);
            earthOutlineColor = new Color(34, 139, 34);
            satelliteColor = Color.RED;
            trailColor = new Color(255, 255, 0, 100);
            orbitColor = new Color(255, 255, 255, 80);
            moonColor = new Color(192, 192, 192);
            satelliteSize = 4;
            animationDelay = 50;
            
            timeMultiplier = 1.0;
            useEquinoctialElements = false;
            autoClearOnZoom = true;
            autoClearOnUpdate = true;
            
            lunarEffectsEnabled = false;
            solarEffectsEnabled = false;
            currentSimulationTime = 0;
            
            setCelestialBody("Earth");
            setSatelliteType("Default");
            
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
    public void restartAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        startAnimation();
    }
    
    // GETTER AND SETTER METHODS FOR DIALOG MANAGER ACCESS
    
    // Getter methods for SimulationPanel to access private fields
    public double getBaseScale() { return baseScale; }
    public double getEarthRadius() { return earthRadius; }
    public double getEarthMass() { return earthMass; }
    public double getGravitationalConstant() { return gravitationalConstant; }
    public Color getEarthColor() { return earthColor; }
    public Color getEarthOutlineColor() { return earthOutlineColor; }
    public Color getSatelliteColor() { return satelliteColor; }
    public Color getTrailColor() { return trailColor; }
    public Color getOrbitColor() { return orbitColor; }
    public Color getMoonColor() { return moonColor; }
    public Color getSunColor() { return sunColor; }
    public int getSatelliteSize() { return satelliteSize; }
    public int getMaxTrailLength() { return maxTrailLength; }
    public float getTrailWidth() { return trailWidth; }
    public int getAnimationDelay() { return animationDelay; }
    public double getSemiMajorAxis() { return semiMajorAxis; }
    public double getEccentricity() { return eccentricity; }
    public double getInclination() { return inclination; }
    public double getArgumentOfPeriapsis() { return argumentOfPeriapsis; }
    public double getLongitudeOfAscendingNode() { return longitudeOfAscendingNode; }
    public double getTrueAnomaly() { return trueAnomaly; }
    public Satellite getSatellite() { return satellite; }
    public boolean getAutoClearOnZoom() { return autoClearOnZoom; }
    public boolean getAutoClearOnUpdate() { return autoClearOnUpdate; }
    public BufferedImage getCelestialBodyImage() { return celestialBodyImage; }
    public String getCurrentBody() { return currentBody; }
    public BufferedImage getSatelliteImage() { return satelliteImage; }
    public String getCurrentSatelliteType() { return currentSatelliteType; }
    public String[] getSatelliteTypes() { return SATELLITE_TYPES; }
    public Map<String, double[]> getCelestialBodies() { return CELESTIAL_BODIES; }
    public SimulationPanel getSimulationPanel() { return simulationPanel; }
    public double getCurrentSimulationTime() { return currentSimulationTime; }
    
    // Lunar and Solar Effects Getters - UPDATED to use currentSimulationTime
    public boolean isLunarEffectsEnabled() { return lunarEffectsEnabled; }
    public boolean isSolarEffectsEnabled() { return solarEffectsEnabled; }
    
    public double[] getMoonPosition() {
        if (!lunarEffectsEnabled) return new double[]{0, 0};
        
        // Calculate Moon angle based on current simulation time
        double moonAngle = Math.toRadians(INITIAL_MOON_ANGLE + (currentSimulationTime / LUNAR_ORBITAL_PERIOD) * 360.0);
        double moonX = MOON_EARTH_DISTANCE * Math.cos(moonAngle);
        double moonY = MOON_EARTH_DISTANCE * Math.sin(moonAngle);
        return new double[]{moonX, moonY};
    }
    
    public double[] getSunPosition() {
        if (!solarEffectsEnabled) return new double[]{0, 0};
        
        // Calculate Sun angle based on current simulation time
        double sunAngle = Math.toRadians(INITIAL_SUN_ANGLE + (currentSimulationTime / SOLAR_ORBITAL_PERIOD) * 360.0);
        double sunX = SUN_EARTH_DISTANCE * Math.cos(sunAngle);
        double sunY = SUN_EARTH_DISTANCE * Math.sin(sunAngle);
        return new double[]{sunX, sunY};
    }
    
    // Setter methods for DialogManager to modify state
    public void setEarthRadius(double earthRadius) { this.earthRadius = earthRadius; }
    public void setEarthMass(double earthMass) { this.earthMass = earthMass; }
    public void setGravitationalConstant(double gravitationalConstant) { this.gravitationalConstant = gravitationalConstant; }
    public void setEarthColor(Color earthColor) { this.earthColor = earthColor; }
    public void setSatelliteColor(Color satelliteColor) { this.satelliteColor = satelliteColor; }
    public void setTrailColor(Color trailColor) { this.trailColor = trailColor; }
    public void setMoonColor(Color moonColor) { this.moonColor = moonColor; }
    public void setSunColor(Color sunColor) { this.sunColor = sunColor; }
    public void setBaseScale(double baseScale) { this.baseScale = baseScale; }
    public void setSatelliteSize(int satelliteSize) { this.satelliteSize = satelliteSize; }
    public void setMaxTrailLength(int maxTrailLength) { this.maxTrailLength = maxTrailLength; }
    public void setTrailWidth(float trailWidth) { this.trailWidth = trailWidth; }
    public void setAnimationDelay(int animationDelay) { this.animationDelay = animationDelay; }
    public void setSemiMajorAxis(double semiMajorAxis) { this.semiMajorAxis = semiMajorAxis; }
    public void setEccentricity(double eccentricity) { this.eccentricity = eccentricity; }
    public void setInclination(double inclination) { this.inclination = inclination; }
    public void setArgumentOfPeriapsis(double argumentOfPeriapsis) { this.argumentOfPeriapsis = argumentOfPeriapsis; }
    public void setLongitudeOfAscendingNode(double longitudeOfAscendingNode) { this.longitudeOfAscendingNode = longitudeOfAscendingNode; }
    public void setTrueAnomaly(double trueAnomaly) { this.trueAnomaly = trueAnomaly; }
    public void setAutoClearOnZoom(boolean autoClearOnZoom) { this.autoClearOnZoom = autoClearOnZoom; }
    public void setAutoClearOnUpdate(boolean autoClearOnUpdate) { this.autoClearOnUpdate = autoClearOnUpdate; }
    public void setLunarEffectsEnabled(boolean lunarEffectsEnabled) { this.lunarEffectsEnabled = lunarEffectsEnabled; }
    public void setSolarEffectsEnabled(boolean solarEffectsEnabled) { this.solarEffectsEnabled = solarEffectsEnabled; }
    
    /**
     * Creates the control panel with orbital parameters, time controls, and zoom controls
     * Enhanced with date/time button
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(WINDOW_WIDTH, CONTROL_PANEL_HEIGHT));
        panel.setBorder(BorderFactory.createTitledBorder("Orbital Parameters & Controls"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // ROW 1: Primary orbital parameters
        gbc.gridy = 0;
        
        gbc.gridx = 0;
        panel.add(new JLabel("Semi-major axis (km):"), gbc);
        gbc.gridx = 1;
        JTextField smaField = new JTextField(String.valueOf(semiMajorAxis / 1000), 10);
        smaField.setPreferredSize(new Dimension(100, 25));
        panel.add(smaField, gbc);
        
        gbc.gridx = 2;
        panel.add(new JLabel("Eccentricity:"), gbc);
        gbc.gridx = 3;
        JTextField eccField = new JTextField(String.valueOf(eccentricity), 10);
        eccField.setPreferredSize(new Dimension(100, 25));
        panel.add(eccField, gbc);
        
        gbc.gridx = 4;
        panel.add(new JLabel("Inclination (°):"), gbc);
        gbc.gridx = 5;
        JTextField incField = new JTextField(String.valueOf(inclination), 10);
        incField.setPreferredSize(new Dimension(100, 25));
        panel.add(incField, gbc);
        
        // ROW 2: Secondary orbital parameters and control buttons
        gbc.gridy = 1;
        
        gbc.gridx = 0;
        panel.add(new JLabel("Arg. Periapsis (°):"), gbc);
        gbc.gridx = 1;
        JTextField argPeriField = new JTextField(String.valueOf(argumentOfPeriapsis), 10);
        argPeriField.setPreferredSize(new Dimension(100, 25));
        panel.add(argPeriField, gbc);
        
        gbc.gridx = 2;
        panel.add(new JLabel("Long. Asc. Node (°):"), gbc);
        gbc.gridx = 3;
        JTextField lanField = new JTextField(String.valueOf(longitudeOfAscendingNode), 10);
        lanField.setPreferredSize(new Dimension(100, 25));
        panel.add(lanField, gbc);
        
        gbc.gridx = 4;
        JButton updateButton = new JButton("Update Orbit");
        updateButton.setPreferredSize(new Dimension(120, 30));
        panel.add(updateButton, gbc);
        
        gbc.gridx = 5;
        JButton toggleElementsButton = new JButton("Switch to Equinoctial");
        toggleElementsButton.setPreferredSize(new Dimension(150, 30));
        panel.add(toggleElementsButton, gbc);
        
        // ROW 2.5: Date/Time Display and Button
        gbc.gridy = 2;
        gbc.gridx = 0;
        panel.add(new JLabel("Simulation Date/Time:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        dateTimeLabel = new JLabel("Jan 1, 1970 00:00:00 UTC");
        dateTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        dateTimeLabel.setForeground(new Color(0, 150, 0));
        panel.add(dateTimeLabel, gbc);
        gbc.gridwidth = 1;
        
        // NEW: Date/Time button
        gbc.gridx = 3;
        JButton setTimeButton = new JButton("Set Time...");
        setTimeButton.setPreferredSize(new Dimension(100, 30));
        setTimeButton.addActionListener(e -> dialogManager.showDateTimeDialog());
        panel.add(setTimeButton, gbc);
        
        // ROW 3: Time control system
        gbc.gridy = 3;
        
        gbc.gridx = 0;
        panel.add(new JLabel("Time Speed:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JSlider speedSlider = new JSlider(-3, 5, 0);
        speedSlider.setMajorTickSpacing(1);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setPreferredSize(new Dimension(200, 40));
        panel.add(speedSlider, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridx = 3;
        JTextField customSpeedField = new JTextField("1", 8);
        customSpeedField.setPreferredSize(new Dimension(80, 25));
        panel.add(customSpeedField, gbc);
        
        gbc.gridx = 4;
        JButton setSpeedButton = new JButton("Set");
        setSpeedButton.setPreferredSize(new Dimension(50, 25));
        panel.add(setSpeedButton, gbc);
        
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

        gbc.gridx = 8;
        JLabel speedLabel = new JLabel("1x");
        speedLabel.setPreferredSize(new Dimension(40, 25));
        panel.add(speedLabel, gbc);
        
        // ROW 4: Zoom control system
        gbc.gridy = 4;
        
        gbc.gridx = 0;
        panel.add(new JLabel("Zoom:"), gbc);
        
        gbc.gridx = 1;
        JButton zoomInButton = new JButton("Zoom In (+)");
        zoomInButton.setPreferredSize(new Dimension(100, 25));
        panel.add(zoomInButton, gbc);
        
        gbc.gridx = 2;
        JButton zoomOutButton = new JButton("Zoom Out (-)");
        zoomOutButton.setPreferredSize(new Dimension(100, 25));
        panel.add(zoomOutButton, gbc);
        
        gbc.gridx = 3;
        JButton resetZoomButton = new JButton("Reset Zoom");
        resetZoomButton.setPreferredSize(new Dimension(100, 25));
        panel.add(resetZoomButton, gbc);
        
        gbc.gridx = 4;
        JLabel zoomLabel = new JLabel("1.0x");
        zoomLabel.setPreferredSize(new Dimension(50, 25));
        panel.add(zoomLabel, gbc);
        
        // EVENT LISTENERS
        
        // Update Orbit Button
        updateButton.addActionListener(e -> {
            try {
                if (useEquinoctialElements) {
                    double a_eq = Double.parseDouble(smaField.getText()) * 1000;
                    double h = Double.parseDouble(eccField.getText());
                    double k = Double.parseDouble(incField.getText());
                    double p = Double.parseDouble(argPeriField.getText());
                    double q = Double.parseDouble(lanField.getText());
                    
                    double[] classical = equinoctialToClassical(a_eq, h, k, p, q);
                    semiMajorAxis = classical[0];
                    eccentricity = classical[1];
                    inclination = classical[2];
                    argumentOfPeriapsis = classical[3];
                    longitudeOfAscendingNode = classical[4];
                } else {
                    semiMajorAxis = Double.parseDouble(smaField.getText()) * 1000;
                    eccentricity = Double.parseDouble(eccField.getText());
                    inclination = Double.parseDouble(incField.getText());
                    argumentOfPeriapsis = Double.parseDouble(argPeriField.getText());
                    longitudeOfAscendingNode = Double.parseDouble(lanField.getText());
                }
                createSatellite();
                if (autoClearOnUpdate) {
                    simulationPanel.clearTrail();
                }
                simulationPanel.repaint();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid input values!");
            }
        });
        
        // Toggle Elements Button
        toggleElementsButton.addActionListener(e -> {
            useEquinoctialElements = !useEquinoctialElements;
            updateFieldLabels(panel, gbc, toggleElementsButton, smaField, eccField, incField, argPeriField, lanField);
        });
        
        // Time Speed Controls
        speedSlider.addChangeListener(e -> {
            int value = speedSlider.getValue();
            if (value == 5) {
                timeMultiplier = 100000;
                speedLabel.setText("100000x");
            } else if (value == 4) {
                timeMultiplier = 10000;
                speedLabel.setText("10000x");
            } else {
                timeMultiplier = Math.pow(10, value);
                speedLabel.setText(String.format("%.0fx", timeMultiplier));
            }
            customSpeedField.setText(String.valueOf((int)timeMultiplier));
        });
        
        setSpeedButton.addActionListener(e -> {
            try {
                double customSpeed = Double.parseDouble(customSpeedField.getText());
                if (customSpeed > 0 && customSpeed <= 100000) {
                    timeMultiplier = customSpeed;
                    speedLabel.setText(String.format("%.0fx", timeMultiplier));
                    if (customSpeed >= 100000) {
                        speedSlider.setValue(5);
                    } else if (customSpeed >= 10000) {
                        speedSlider.setValue(4);
                    } else {
                        int sliderValue = (int)Math.round(Math.log10(customSpeed));
                        speedSlider.setValue(Math.max(-3, Math.min(3, sliderValue)));
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Speed must be between 0.001 and 100000!");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid speed value!");
            }
        });
        
        // Animation Controls
        pauseButton.addActionListener(e -> {
            isPaused = !isPaused;
            pauseButton.setText(isPaused ? "Resume" : "Pause");
        });
        
        resetButton.addActionListener(e -> {
            trueAnomaly = 0;
            currentSimulationTime = 0;
            createSatellite();
            simulationPanel.clearTrail();
            updateDateTimeDisplay();
            simulationPanel.repaint();
        });
        
        clearTrailButton.addActionListener(e -> {
            simulationPanel.clearTrail();
            simulationPanel.repaint();
        });
        
        // Zoom Controls
        zoomInButton.addActionListener(e -> {
            simulationPanel.zoomIn();
            if (autoClearOnZoom) {
                simulationPanel.clearTrail();
            }
            zoomLabel.setText(String.format("%.1fx", simulationPanel.getZoomFactor()));
        });
        
        zoomOutButton.addActionListener(e -> {
            simulationPanel.zoomOut();
            if (autoClearOnZoom) {
                simulationPanel.clearTrail();
            }
            zoomLabel.setText(String.format("%.1fx", simulationPanel.getZoomFactor()));
        });
        
        resetZoomButton.addActionListener(e -> {
            simulationPanel.resetZoom();
            zoomLabel.setText(String.format("%.1fx", simulationPanel.getZoomFactor()));
        });
        
        return panel;
    }
    
    /**
     * Updates field labels and values when switching between classical and equinoctial elements
     */
    private void updateFieldLabels(JPanel panel, GridBagConstraints gbc, JButton toggleButton, 
                                  JTextField smaField, JTextField eccField, JTextField incField, 
                                  JTextField argPeriField, JTextField lanField) {
        // Remove old labels
        Component[] components = panel.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JLabel) {
                JLabel label = (JLabel) components[i];
                String text = label.getText();
                if (text.contains("Semi-major") || text.contains("Eccentricity") || text.contains("Inclination") ||
                    text.contains("Arg.") || text.contains("Long.") || text.contains("a (km)") || 
                    text.contains("h") || text.contains("k") || text.contains("p") || text.contains("q")) {
                    panel.remove(label);
                }
            }
        }
        
        gbc.gridy = 0;
        if (useEquinoctialElements) {
            toggleButton.setText("Switch to Classical");
            
            gbc.gridx = 0;
            panel.add(new JLabel("a (km):"), gbc, 0);
            gbc.gridx = 2;
            panel.add(new JLabel("h:"), gbc, 2);
            gbc.gridx = 4;
            panel.add(new JLabel("k:"), gbc, 4);
            
            gbc.gridy = 1;
            gbc.gridx = 0;
            panel.add(new JLabel("p:"), gbc, 6);
            gbc.gridx = 2;
            panel.add(new JLabel("q:"), gbc, 8);
            
            double[] equinoctial = classicalToEquinoctial(semiMajorAxis, eccentricity, inclination, 
                                                         argumentOfPeriapsis, longitudeOfAscendingNode);
            smaField.setText(String.format("%.1f", equinoctial[0] / 1000));
            eccField.setText(String.format("%.6f", equinoctial[1]));
            incField.setText(String.format("%.6f", equinoctial[2]));
            argPeriField.setText(String.format("%.6f", equinoctial[3]));
            lanField.setText(String.format("%.6f", equinoctial[4]));
        } else {
            toggleButton.setText("Switch to Equinoctial");
            
            gbc.gridx = 0;
            panel.add(new JLabel("Semi-major axis (km):"), gbc, 0);
            gbc.gridx = 2;
            panel.add(new JLabel("Eccentricity:"), gbc, 2);
            gbc.gridx = 4;
            panel.add(new JLabel("Inclination (°):"), gbc, 4);
            
            gbc.gridy = 1;
            gbc.gridx = 0;
            panel.add(new JLabel("Arg. Periapsis (°):"), gbc, 6);
            gbc.gridx = 2;
            panel.add(new JLabel("Long. Asc. Node (°):"), gbc, 8);
            
            smaField.setText(String.format("%.1f", semiMajorAxis / 1000));
            eccField.setText(String.format("%.3f", eccentricity));
            incField.setText(String.format("%.1f", inclination));
            argPeriField.setText(String.format("%.1f", argumentOfPeriapsis));
            lanField.setText(String.format("%.1f", longitudeOfAscendingNode));
        }
        
        panel.revalidate();
        panel.repaint();
    }
    
    /**
     * Mathematical conversion from classical to equinoctial orbital elements
     */
    private double[] classicalToEquinoctial(double a, double e, double i_deg, double omega_deg, double Omega_deg) {
        double i = Math.toRadians(i_deg);
        double omega = Math.toRadians(omega_deg);
        double Omega = Math.toRadians(Omega_deg);
        
        double h = e * Math.sin(omega + Omega);
        double k = e * Math.cos(omega + Omega);
        double p = Math.tan(i / 2) * Math.sin(Omega);
        double q = Math.tan(i / 2) * Math.cos(Omega);
        
        return new double[]{a, h, k, p, q};
    }
    
    /**
     * Mathematical conversion from equinoctial to classical orbital elements
     */
    private double[] equinoctialToClassical(double a, double h, double k, double p, double q) {
        double e = Math.sqrt(h * h + k * k);
        double i = 2 * Math.atan(Math.sqrt(p * p + q * q));
        double Omega = Math.atan2(p, q);
        double omega = Math.atan2(h, k) - Omega;
        
        double i_deg = Math.toDegrees(i);
        double omega_deg = Math.toDegrees(omega);
        double Omega_deg = Math.toDegrees(Omega);
        
        omega_deg = ((omega_deg % 360) + 360) % 360;
        Omega_deg = ((Omega_deg % 360) + 360) % 360;
        
        return new double[]{a, e, i_deg, omega_deg, Omega_deg};
    }
    
    /**
     * Creates a new satellite object with current orbital parameters
     */
    public void createSatellite() {
        satellite = new Satellite(semiMajorAxis, eccentricity, inclination, 
                                argumentOfPeriapsis, longitudeOfAscendingNode, trueAnomaly,
                                gravitationalConstant, earthMass, lunarEffectsEnabled, solarEffectsEnabled, this);
    }
    
    /**
     * Starts the animation timer for smooth orbital motion display
     * UPDATED: Now updates currentSimulationTime continuously
     */
    private void startAnimation() {
        animationTimer = new Timer(animationDelay, e -> {
            if (!isPaused) {
                double baseTimeStep = 0.05;
                double effectiveTimeStep = baseTimeStep * timeMultiplier;
                
                double maxSubStep;
                if (timeMultiplier > 50000) {
                    maxSubStep = 30.0;
                } else if (timeMultiplier > 10000) {
                    maxSubStep = 45.0;
                } else if (timeMultiplier > 1000) {
                    maxSubStep = 60.0;
                } else {
                    maxSubStep = 120.0;
                }
                
                int numSubSteps = Math.max(1, (int)Math.ceil(effectiveTimeStep / maxSubStep));
                double subStepSize = effectiveTimeStep / numSubSteps;
                
                for (int i = 0; i < numSubSteps; i++) {
                    currentSimulationTime += subStepSize;
                    satellite.updatePosition(subStepSize);
                }
                
                updateDateTimeDisplay();
                simulationPanel.repaint();
            }
        });
        animationTimer.start();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OrbitalSimulation().setVisible(true));
    }
}