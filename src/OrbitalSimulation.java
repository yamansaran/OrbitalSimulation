// Import necessary Java libraries for GUI, graphics, events, and data structures
import javax.swing.*;
import javax.swing.Timer;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Orbital Mechanics Simulation Program
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
    
    // Physical constants for Earth and orbital mechanics
    private static final double EARTH_RADIUS = 6371000; // Earth's radius in meters
    private static final double G = 6.67430e-11; // Gravitational constant in m³/kg⋅s²
    private static final double EARTH_MASS = 5.972e24; // Earth's mass in kg
    
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
        
        // Create and add the simulation display panel (center area)
        simulationPanel = new SimulationPanel();
        add(simulationPanel, BorderLayout.CENTER);
        
        // Create and add the control panel (bottom area)
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
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
        
        // Display current time speed multiplier
        gbc.gridx = 7;
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
        
        // ZOOM CONTROL EVENT LISTENERS
        
        /**
         * Zoom In Button: Increases visual scale by factor of 1.5
         */
        zoomInButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                simulationPanel.zoomIn();
                zoomLabel.setText(String.format("%.1fx", simulationPanel.getZoomFactor()));
            }
        });
        
        /**
         * Zoom Out Button: Decreases visual scale by factor of 1.5
         */
        zoomOutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                simulationPanel.zoomOut();
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
                                argumentOfPeriapsis, longitudeOfAscendingNode, trueAnomaly);
    }
    
    /**
     * Starts the animation timer for smooth orbital motion display
     * Timer fires every 50ms (20 FPS) for smooth animation
     */
    private void startAnimation() {
        animationTimer = new Timer(50, new ActionListener() {
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
    
    /**
     * Custom JPanel for rendering the orbital simulation
     * Handles zooming, panning, and drawing all visual elements
     */
    private class SimulationPanel extends JPanel {
        // Base scale factor for converting meters to pixels
        private static final double BASE_SCALE = 5e-6; // 1 meter = 5×10⁻⁶ pixels
        
        // Zoom and pan state variables
        private double zoomFactor = 1.0; // Current zoom level (1.0 = default)
        private double offsetX = 0; // Horizontal pan offset
        private double offsetY = 0; // Vertical pan offset
        
        // Satellite trail for showing recent orbital path
        private List<Point> trail = new ArrayList<>();
        private static final int MAX_TRAIL_LENGTH = 500; // Maximum trail points
        
        /**
         * Constructor: Sets up mouse controls for zoom and pan
         */
        public SimulationPanel() {
            setBackground(Color.BLACK); // Space background
            
            // Mouse wheel listener for zooming functionality
            addMouseWheelListener(new MouseWheelListener() {
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (e.getWheelRotation() < 0) {
                        zoomIn(); // Scroll up = zoom in
                    } else {
                        zoomOut(); // Scroll down = zoom out
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
            double currentScale = BASE_SCALE * zoomFactor;
            
            // DRAW EARTH: Render Earth as a blue circle at the center
            int earthRadius = (int) (EARTH_RADIUS * currentScale); // Scale Earth radius to pixels
            
            // Earth body (blue color representing oceans)
            g2d.setColor(new Color(100, 149, 237)); // Cornflower blue
            g2d.fillOval(centerX - earthRadius, centerY - earthRadius, 
                        earthRadius * 2, earthRadius * 2);
            
            // Earth outline (green color suggesting continents)
            g2d.setColor(new Color(34, 139, 34)); // Forest green
            g2d.drawOval(centerX - earthRadius, centerY - earthRadius, 
                        earthRadius * 2, earthRadius * 2);
            
            if (satellite != null) {
                // DRAW ORBITAL PATH: Show the complete elliptical orbit
                drawOrbit(g2d, centerX, centerY, currentScale);
                
                // GET SATELLITE POSITION: Calculate current satellite coordinates
                double[] pos = satellite.getPosition(); // Returns [x, y] in meters
                int satX = centerX + (int) (pos[0] * currentScale); // Convert to screen X
                int satY = centerY - (int) (pos[1] * currentScale); // Convert to screen Y (flip Y axis)
                
                // UPDATE TRAIL: Add current position to satellite trail
                trail.add(new Point(satX, satY));
                if (trail.size() > MAX_TRAIL_LENGTH) {
                    trail.remove(0); // Remove oldest point to maintain trail length
                }
                
                // DRAW TRAIL: Show satellite's recent orbital path
                g2d.setColor(new Color(255, 255, 0, 100)); // Semi-transparent yellow
                g2d.setStroke(new BasicStroke((float)Math.max(1, zoomFactor))); // Scale line thickness with zoom
                for (int i = 1; i < trail.size(); i++) {
                    Point p1 = trail.get(i - 1);
                    Point p2 = trail.get(i);
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y); // Connect consecutive trail points
                }
                
                // DRAW SATELLITE: Render satellite as a red dot
                g2d.setColor(Color.RED);
                int satSize = (int)Math.max(4, 4 * zoomFactor); // Scale satellite size with zoom
                g2d.fillOval(satX - satSize/2, satY - satSize/2, satSize, satSize);
                
                // DRAW INFORMATION: Display orbital parameters and status
                drawInfo(g2d);
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
            g2d.setColor(new Color(255, 255, 255, 80)); // Semi-transparent white
            g2d.setStroke(new BasicStroke((float)Math.max(1, zoomFactor/2), BasicStroke.CAP_BUTT, 
                         BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f}, 0.0f));
            
            // Calculate ellipse parameters from orbital elements
            double a = semiMajorAxis * currentScale; // Semi-major axis in pixels
            double b = a * Math.sqrt(1 - eccentricity * eccentricity); // Semi-minor axis using b = a√(1-e²)
            double c = a * eccentricity; // Distance from center to focus using c = ae
            
            // Create ellipse positioned with Earth at one focus (not center)
            // Earth is located at distance 'c' from the geometric center of the ellipse
            Ellipse2D.Double ellipse = new Ellipse2D.Double(
                centerX - a + c, centerY - b, 2 * a, 2 * b);
            g2d.draw(ellipse);
        }
        
        /**
         * Draws real-time orbital information as text overlay
         * 
         * @param g2d Graphics context for text rendering
         */
        private void drawInfo(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            
            // Calculate current orbital parameters from satellite state
            double[] pos = satellite.getPosition();
            
            // Calculate altitude: distance from Earth surface
            // √(x² + y²) gives distance from Earth center, subtract Earth radius
            double altitude = (Math.sqrt(pos[0] * pos[0] + pos[1] * pos[1]) - EARTH_RADIUS) / 1000;
            
            // Get current orbital velocity and period from satellite
            double velocity = satellite.getVelocity(); // m/s
            double period = satellite.getOrbitalPeriod() / 3600; // Convert seconds to hours
            
            // Format information strings for display
            String[] info = {
                String.format("Altitude: %.1f km", altitude),
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
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new OrbitalSimulation().setVisible(true);
            }
        });
    }
    
}