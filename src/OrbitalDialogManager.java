import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import javax.swing.*;

/**
 * OrbitalDialogManager handles all dialog windows for the orbital simulation
 * Extracted from OrbitalSimulation for better code organization and maintainability
 */
public class OrbitalDialogManager {
    private OrbitalSimulation simulation;
    
    public OrbitalDialogManager(OrbitalSimulation simulation) {
        this.simulation = simulation;
    }
    
    /**
     * Shows the Non-Keplerian Effects configuration dialog
     */
    public void showNonKeplerianEffectsDialog() {
        JDialog dialog = new JDialog(simulation, "Non-Keplerian Effects", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(simulation);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Lunar effects toggle
        JCheckBox lunarEffectsBox = new JCheckBox("Enable Lunar Gravitational Effects", simulation.isLunarEffectsEnabled());
        
        // Solar effects toggle
        JCheckBox solarEffectsBox = new JCheckBox("Enable Solar Gravitational Effects", simulation.isSolarEffectsEnabled());
        
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(lunarEffectsBox, gbc);
        
        gbc.gridy = 1;
        panel.add(solarEffectsBox, gbc);
        
        // Information about effects
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JTextArea infoArea = new JTextArea(12, 40);
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // Update info based on current selections
        ActionListener updateInfo = e -> {
            StringBuilder info = new StringBuilder();
            
            if (lunarEffectsBox.isSelected()) {
                info.append("LUNAR GRAVITATIONAL EFFECTS:\n");
                info.append("• Moon orbits Earth every 29.530 days\n");
                info.append("• Started at 84.7° east on Jan 1, 1970\n");
                info.append("• Causes orbital plane precession\n");
                info.append("• Affects argument of periapsis\n");
                info.append("• Induces inclination oscillations\n");
                double moonAngle = (84.7 + (simulation.getCurrentSimulationTime() / (29.530 * 24 * 3600)) * 360.0) % 360.0;
                info.append(String.format("• Current Moon Position: %.1f°\n", moonAngle));
                info.append("\n");
            }
            
            if (solarEffectsBox.isSelected()) {
                info.append("SOLAR GRAVITATIONAL EFFECTS:\n");
                info.append("• Sun has apparent 1-year orbital period\n");
                info.append("• Started at 281° on Jan 1, 1970\n");
                info.append("• Causes long-term orbital evolution\n");
                info.append("• Affects eccentricity and inclination\n");
                info.append("• Stronger effects on high-altitude satellites\n");
                double sunAngle = (281.0 + (simulation.getCurrentSimulationTime() / (365.25 * 24 * 3600)) * 360.0) % 360.0;
                info.append(String.format("• Current Sun Position: %.1f°\n", sunAngle));
                info.append("\n");
            }
            
            if (!lunarEffectsBox.isSelected() && !solarEffectsBox.isSelected()) {
                info.append("NON-KEPLERIAN EFFECTS DISABLED\n\n");
                info.append("When enabled, these effects simulate:\n");
                info.append("• Third-body gravitational perturbations\n");
                info.append("• Orbital element evolution over time\n");
                info.append("• Realistic satellite orbital mechanics\n\n");
                info.append("Effects are most noticeable on:\n");
                info.append("• High-altitude satellites (GEO, HEO)\n");
                info.append("• Eccentric orbits\n");
                info.append("• Inclined orbital planes\n");
            }
            
            if (lunarEffectsBox.isSelected() && solarEffectsBox.isSelected()) {
                info.append("COMBINED EFFECTS:\n");
                info.append("• Complex multi-body dynamics\n");
                info.append("• Resonances and secular variations\n");
                info.append("• Enhanced orbital evolution\n");
                info.append("• Blue acceleration vector will be displayed\n");
                info.append("  showing combined gravitational forces\n");
            }
            
            infoArea.setText(info.toString());
        };
        
        lunarEffectsBox.addActionListener(updateInfo);
        solarEffectsBox.addActionListener(updateInfo);
        updateInfo.actionPerformed(null); // Initial update
        
        JScrollPane infoScroll = new JScrollPane(infoArea);
        panel.add(infoScroll, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(e -> {
            boolean oldLunarEffects = simulation.isLunarEffectsEnabled();
            boolean oldSolarEffects = simulation.isSolarEffectsEnabled();
            
            simulation.setLunarEffectsEnabled(lunarEffectsBox.isSelected());
            simulation.setSolarEffectsEnabled(solarEffectsBox.isSelected());
            
            // If any effects were toggled, update the satellite
            if (oldLunarEffects != simulation.isLunarEffectsEnabled() || 
                oldSolarEffects != simulation.isSolarEffectsEnabled()) {
                simulation.createSatellite();
                simulation.getSimulationPanel().repaint();
            }
            
            dialog.dispose();
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gbc);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    /**
     * Shows the orbital parameters configuration dialog
     */
    public void showOrbitalParametersDialog() {
        JDialog dialog = new JDialog(simulation, "Orbital Parameters", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(simulation);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Create input fields for orbital parameters
        JTextField smaField = new JTextField(String.valueOf(simulation.getSemiMajorAxis() / 1000), 10);
        JTextField eccField = new JTextField(String.valueOf(simulation.getEccentricity()), 10);
        JTextField incField = new JTextField(String.valueOf(simulation.getInclination()), 10);
        JTextField argPeriField = new JTextField(String.valueOf(simulation.getArgumentOfPeriapsis()), 10);
        JTextField lanField = new JTextField(String.valueOf(simulation.getLongitudeOfAscendingNode()), 10);
        JTextField anomalyField = new JTextField(String.valueOf(simulation.getTrueAnomaly()), 10);
        
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
                simulation.setSemiMajorAxis(Double.parseDouble(smaField.getText()) * 1000);
                simulation.setEccentricity(Double.parseDouble(eccField.getText()));
                simulation.setInclination(Double.parseDouble(incField.getText()));
                simulation.setArgumentOfPeriapsis(Double.parseDouble(argPeriField.getText()));
                simulation.setLongitudeOfAscendingNode(Double.parseDouble(lanField.getText()));
                simulation.setTrueAnomaly(Double.parseDouble(anomalyField.getText()));
                simulation.createSatellite();
                simulation.getSimulationPanel().repaint();
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
     * Shows the satellite type selection dialog
     */
    public void showSatelliteTypeDialog() {
        JDialog dialog = new JDialog(simulation, "Select Satellite Type", true);
        dialog.setSize(450, 300);
        dialog.setLocationRelativeTo(simulation);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create list of satellite types
        String[] satelliteTypes = simulation.getSatelliteTypes();
        JList<String> satelliteList = new JList<>(satelliteTypes);
        satelliteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        satelliteList.setSelectedValue(simulation.getCurrentSatelliteType(), true);
        
        JScrollPane scrollPane = new JScrollPane(satelliteList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Info panel
        JTextArea infoArea = new JTextArea(6, 35);
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane infoScroll = new JScrollPane(infoArea);
        panel.add(infoScroll, BorderLayout.SOUTH);
        
        // Update info when selection changes
        satelliteList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = satelliteList.getSelectedValue();
                if (selected != null) {
                    if (selected.equals("Default")) {
                        infoArea.setText("Selected: " + selected + "\n" +
                                       "Type: Colored dot (no image file)\n" +
                                       "Description: Simple colored circle\n" +
                                       "File: None required");
                    } else {
                        String imagePath = "src/resources/" + selected + ".png";
                        File imageFile = new File(imagePath);
                        boolean exists = imageFile.exists();
                        
                        infoArea.setText("Selected: " + selected + "\n" +
                                       "Image file: " + selected + ".png\n" +
                                       "Full path: " + imagePath + "\n" +
                                       "File exists: " + (exists ? "Yes" : "No") + "\n" +
                                       (exists ? "Ready to use!" : "File not found - will use colored dot"));
                    }
                }
            }
        });
        
        // Trigger initial info display
        if (satelliteList.getSelectedValue() != null) {
            satelliteList.getListSelectionListeners()[0].valueChanged(
                new javax.swing.event.ListSelectionEvent(satelliteList, 0, 0, false));
        }
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        JButton addCustomButton = new JButton("Add Custom...");
        
        okButton.addActionListener(e -> {
            String selected = satelliteList.getSelectedValue();
            if (selected != null) {
                simulation.setSatelliteType(selected);
            }
            dialog.dispose();
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        addCustomButton.addActionListener(e -> {
            String customName = JOptionPane.showInputDialog(dialog, 
                "Enter satellite name (without .png extension):", 
                "Add Custom Satellite", 
                JOptionPane.PLAIN_MESSAGE);
            if (customName != null && !customName.trim().isEmpty()) {
                // Add to the list temporarily (for this session only)
                String[] newTypes = new String[satelliteTypes.length + 1];
                System.arraycopy(satelliteTypes, 0, newTypes, 0, satelliteTypes.length);
                newTypes[satelliteTypes.length] = customName.trim();
                
                satelliteList.setListData(newTypes);
                satelliteList.setSelectedValue(customName.trim(), true);
            }
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(addCustomButton);
        panel.add(buttonPanel, BorderLayout.NORTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    /**
     * Shows the celestial body selection dialog
     */
    public void showCelestialBodyDialog() {
        JDialog dialog = new JDialog(simulation, "Select Celestial Body", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(simulation);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create list of celestial bodies
        String[] bodies = simulation.getCelestialBodies().keySet().toArray(new String[0]);
        Arrays.sort(bodies); // Sort alphabetically
        JList<String> bodyList = new JList<>(bodies);
        bodyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bodyList.setSelectedValue(simulation.getCurrentBody(), true);
        
        JScrollPane scrollPane = new JScrollPane(bodyList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Info panel to show body details
        JTextArea infoArea = new JTextArea(6, 30);
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane infoScroll = new JScrollPane(infoArea);
        panel.add(infoScroll, BorderLayout.SOUTH);
        
        // Update info when selection changes
        bodyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = bodyList.getSelectedValue();
                if (selected != null) {
                    double[] data = simulation.getCelestialBodies().get(selected);
                    infoArea.setText(String.format(
                        "Selected: %s\n" +
                        "Radius: %.0f km\n" +
                        "Mass: %.3e kg\n" +
                        "Surface Gravity: %.2f m/s²\n" +
                        "Escape Velocity: %.2f km/s",
                        selected,
                        data[0] / 1000,
                        data[1],
                        data[2] * data[1] / (data[0] * data[0]),
                        Math.sqrt(2 * data[2] * data[1] / data[0]) / 1000
                    ));
                }
            }
        });
        
        // Trigger initial info display
        if (bodyList.getSelectedValue() != null) {
            bodyList.getListSelectionListeners()[0].valueChanged(
                new javax.swing.event.ListSelectionEvent(bodyList, 0, 0, false));
        }
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(e -> {
            String selected = bodyList.getSelectedValue();
            if (selected != null) {
                simulation.setCelestialBody(selected);
            }
            dialog.dispose();
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, BorderLayout.NORTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    /**
     * Shows the trail settings configuration dialog
     */
    public void showTrailSettingsDialog() {
        JDialog dialog = new JDialog(simulation, "Trail Settings", true);
        dialog.setSize(350, 200);
        dialog.setLocationRelativeTo(simulation);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Auto-clear trail options
        JCheckBox autoClearZoomBox = new JCheckBox("Auto-clear trail when zooming", simulation.getAutoClearOnZoom());
        JCheckBox autoClearUpdateBox = new JCheckBox("Auto-clear trail when updating orbit", simulation.getAutoClearOnUpdate());
        
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
            simulation.setAutoClearOnZoom(autoClearZoomBox.isSelected());
            simulation.setAutoClearOnUpdate(autoClearUpdateBox.isSelected());
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
    public void showPhysicalConstantsDialog() {
        JDialog dialog = new JDialog(simulation, "Physical Constants", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(simulation);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Create input fields for physical constants
        JTextField earthRadiusField = new JTextField(String.valueOf(simulation.getEarthRadius() / 1000), 10);
        JTextField earthMassField = new JTextField(String.format("%.3e", simulation.getEarthMass()), 10);
        JTextField gravConstField = new JTextField(String.format("%.5e", simulation.getGravitationalConstant()), 10);
        
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
                simulation.setEarthRadius(Double.parseDouble(earthRadiusField.getText()) * 1000);
                simulation.setEarthMass(Double.parseDouble(earthMassField.getText()));
                simulation.setGravitationalConstant(Double.parseDouble(gravConstField.getText()));
                simulation.createSatellite();
                simulation.getSimulationPanel().repaint();
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
    public void showDisplaySettingsDialog() {
        JDialog dialog = new JDialog(simulation, "Display Settings", true);
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(simulation);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Trail Settings Tab
        JPanel trailPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JTextField trailLengthField = new JTextField(String.valueOf(simulation.getMaxTrailLength()), 10);
        JSlider trailOpacitySlider = new JSlider(0, 255, simulation.getTrailColor().getAlpha());
        JTextField trailWidthField = new JTextField(String.valueOf(simulation.getTrailWidth()), 10);
        
        gbc.gridy = 0;
        gbc.gridx = 0; trailPanel.add(new JLabel("Trail Length (points):"), gbc);
        gbc.gridx = 1; trailPanel.add(trailLengthField, gbc);
        
        gbc.gridy = 1;
        gbc.gridx = 0; trailPanel.add(new JLabel("Trail Opacity:"), gbc);
        gbc.gridx = 1; trailPanel.add(trailOpacitySlider, gbc);
        
        gbc.gridy = 2;
        gbc.gridx = 0; trailPanel.add(new JLabel("Trail Width (pixels):"), gbc);
        gbc.gridx = 1; trailPanel.add(trailWidthField, gbc);
        
        tabbedPane.add("Trail", trailPanel);
        
        // Colors Tab
        JPanel colorsPanel = createColorsPanel(dialog);
        tabbedPane.add("Colors", colorsPanel);
        
        // Scale & Size Tab
        JPanel scalePanel = createScalePanel();
        tabbedPane.add("Scale & Size", scalePanel);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        JButton applyButton = new JButton("Apply");
        
        okButton.addActionListener(e -> {
            applyDisplaySettings(trailLengthField, trailOpacitySlider, trailWidthField, scalePanel);
            dialog.dispose();
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        applyButton.addActionListener(e -> {
            applyDisplaySettings(trailLengthField, trailOpacitySlider, trailWidthField, scalePanel);
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
     * Creates the colors panel for the display settings dialog
     */
    private JPanel createColorsPanel(JDialog dialog) {
        JPanel colorsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JButton earthColorButton = new JButton("     ");
        earthColorButton.setBackground(simulation.getEarthColor());
        earthColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(dialog, "Choose Earth Color", simulation.getEarthColor());
            if (newColor != null) {
                simulation.setEarthColor(newColor);
                earthColorButton.setBackground(newColor);
            }
        });
        
        JButton satelliteColorButton = new JButton("     ");
        satelliteColorButton.setBackground(simulation.getSatelliteColor());
        satelliteColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(dialog, "Choose Satellite Color", simulation.getSatelliteColor());
            if (newColor != null) {
                simulation.setSatelliteColor(newColor);
                satelliteColorButton.setBackground(newColor);
            }
        });
        
        JButton trailColorButton = new JButton("     ");
        trailColorButton.setBackground(new Color(simulation.getTrailColor().getRed(), simulation.getTrailColor().getGreen(), simulation.getTrailColor().getBlue()));
        trailColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(dialog, "Choose Trail Color", 
                new Color(simulation.getTrailColor().getRed(), simulation.getTrailColor().getGreen(), simulation.getTrailColor().getBlue()));
            if (newColor != null) {
                simulation.setTrailColor(new Color(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), simulation.getTrailColor().getAlpha()));
                trailColorButton.setBackground(newColor);
            }
        });
        
        JButton moonColorButton = new JButton("     ");
        moonColorButton.setBackground(simulation.getMoonColor());
        moonColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(dialog, "Choose Moon Color", simulation.getMoonColor());
            if (newColor != null) {
                simulation.setMoonColor(newColor);
                moonColorButton.setBackground(newColor);
            }
        });
        
        JButton sunColorButton = new JButton("     ");
        sunColorButton.setBackground(simulation.getSunColor());
        sunColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(dialog, "Choose Sun Color", simulation.getSunColor());
            if (newColor != null) {
                simulation.setSunColor(newColor);
                sunColorButton.setBackground(newColor);
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
        
        gbc.gridy = 3;
        gbc.gridx = 0; colorsPanel.add(new JLabel("Moon Color:"), gbc);
        gbc.gridx = 1; colorsPanel.add(moonColorButton, gbc);
        
        gbc.gridy = 4;
        gbc.gridx = 0; colorsPanel.add(new JLabel("Sun Color:"), gbc);
        gbc.gridx = 1; colorsPanel.add(sunColorButton, gbc);
        
        return colorsPanel;
    }
    
    /**
     * Creates the scale panel for the display settings dialog
     */
    private JPanel createScalePanel() {
        JPanel scalePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JTextField baseScaleField = new JTextField(String.format("%.2e", simulation.getBaseScale()), 10);
        JTextField satSizeField = new JTextField(String.valueOf(simulation.getSatelliteSize()), 10);
        JTextField animDelayField = new JTextField(String.valueOf(simulation.getAnimationDelay()), 10);
        
        gbc.gridy = 0;
        gbc.gridx = 0; scalePanel.add(new JLabel("Base Scale Factor:"), gbc);
        gbc.gridx = 1; scalePanel.add(baseScaleField, gbc);
        
        gbc.gridy = 1;
        gbc.gridx = 0; scalePanel.add(new JLabel("Satellite Size (pixels):"), gbc);
        gbc.gridx = 1; scalePanel.add(satSizeField, gbc);
        
        gbc.gridy = 2;
        gbc.gridx = 0; scalePanel.add(new JLabel("Animation Delay (ms):"), gbc);
        gbc.gridx = 1; scalePanel.add(animDelayField, gbc);
        
        return scalePanel;
    }
    
    /**
     * Applies display settings from the dialog
     */
    private void applyDisplaySettings(JTextField trailLengthField, JSlider trailOpacitySlider, 
                                    JTextField trailWidthField, JPanel scalePanel) {
        try {
            // Apply trail settings
            simulation.setMaxTrailLength(Integer.parseInt(trailLengthField.getText()));
            Color currentTrail = simulation.getTrailColor();
            simulation.setTrailColor(new Color(currentTrail.getRed(), currentTrail.getGreen(), 
                                 currentTrail.getBlue(), trailOpacitySlider.getValue()));
            float newWidth = Float.parseFloat(trailWidthField.getText());
            
            // Validate trail width
            if (newWidth < 0.1f) newWidth = 0.1f;
            if (newWidth > 20.0f) newWidth = 20.0f;
            simulation.setTrailWidth(newWidth);
            
            // Apply scale settings from scalePanel
            Component[] components = scalePanel.getComponents();
            JTextField baseScaleField = null;
            JTextField satSizeField = null;
            JTextField animDelayField = null;
            
            for (Component comp : components) {
                if (comp instanceof JTextField) {
                    JTextField field = (JTextField) comp;
                    if (baseScaleField == null) baseScaleField = field;
                    else if (satSizeField == null) satSizeField = field;
                    else if (animDelayField == null) animDelayField = field;
                }
            }
            
            if (baseScaleField != null) {
                simulation.setBaseScale(Double.parseDouble(baseScaleField.getText()));
            }
            if (satSizeField != null) {
                simulation.setSatelliteSize(Integer.parseInt(satSizeField.getText()));
            }
            if (animDelayField != null) {
                int newDelay = Integer.parseInt(animDelayField.getText());
                if (newDelay != simulation.getAnimationDelay()) {
                    simulation.setAnimationDelay(newDelay);
                    simulation.restartAnimation();
                }
            }
            
            simulation.getSimulationPanel().updateSettings();
            simulation.getSimulationPanel().repaint();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Invalid input values!");
        }
    }
}