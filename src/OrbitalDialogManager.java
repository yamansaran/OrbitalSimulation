import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import javax.swing.*;

/*
 * OrbitalDialogManager handles all dialog windows for the orbital simulation
 */
public class OrbitalDialogManager {
    private OrbitalSimulation simulation;
    
    public OrbitalDialogManager(OrbitalSimulation simulation) {
        this.simulation = simulation;
    }
    
    /**
     * Shows the Date/Time picker dialog for adjusting simulation time
     * Only allows changes when simulation is paused
     * RESTRUCTURED: Better layout with prominent text area
     */
    public void showDateTimeDialog() {
        // Check if simulation is paused
        if (!simulation.isPaused()) {
            JOptionPane.showMessageDialog(simulation, 
                "Simulation must be paused to change date/time.\nPress 'Pause' button first.", 
                "Simulation Running", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JDialog dialog = new JDialog(simulation, "Set Simulation Date/Time", true);
        dialog.setSize(700, 550);
        dialog.setLocationRelativeTo(simulation);
        
        // Use BorderLayout for better space distribution
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Convert current simulation time to date/time
        LocalDateTime currentDateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)
            .plusSeconds((long)simulation.getCurrentSimulationTime());
        
        // TOP PANEL: Date/Time inputs
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Set Date & Time"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Year input
        gbc.gridy = 0; gbc.gridx = 0;
        inputPanel.add(new JLabel("Year:"), gbc);
        gbc.gridx = 1;
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(currentDateTime.getYear(), 1970, 2100, 1));
        yearSpinner.setPreferredSize(new Dimension(80, 25));
        inputPanel.add(yearSpinner, gbc);
        
        // Month input
        gbc.gridx = 2;
        inputPanel.add(new JLabel("Month:"), gbc);
        gbc.gridx = 3;
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        JComboBox<String> monthCombo = new JComboBox<>(months);
        monthCombo.setSelectedIndex(currentDateTime.getMonthValue() - 1);
        monthCombo.setPreferredSize(new Dimension(120, 25));
        inputPanel.add(monthCombo, gbc);
        
        // Day input
        gbc.gridx = 4;
        inputPanel.add(new JLabel("Day:"), gbc);
        gbc.gridx = 5;
        JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(currentDateTime.getDayOfMonth(), 1, 31, 1));
        daySpinner.setPreferredSize(new Dimension(60, 25));
        inputPanel.add(daySpinner, gbc);
        
        // Second row for time
        gbc.gridy = 1; gbc.gridx = 0;
        inputPanel.add(new JLabel("Hour:"), gbc);
        gbc.gridx = 1;
        JSpinner hourSpinner = new JSpinner(new SpinnerNumberModel(currentDateTime.getHour(), 0, 23, 1));
        hourSpinner.setPreferredSize(new Dimension(60, 25));
        inputPanel.add(hourSpinner, gbc);
        
        gbc.gridx = 2;
        inputPanel.add(new JLabel("Minute:"), gbc);
        gbc.gridx = 3;
        JSpinner minuteSpinner = new JSpinner(new SpinnerNumberModel(currentDateTime.getMinute(), 0, 59, 1));
        minuteSpinner.setPreferredSize(new Dimension(60, 25));
        inputPanel.add(minuteSpinner, gbc);
        
        gbc.gridx = 4;
        inputPanel.add(new JLabel("Second:"), gbc);
        gbc.gridx = 5;
        JSpinner secondSpinner = new JSpinner(new SpinnerNumberModel(currentDateTime.getSecond(), 0, 59, 1));
        secondSpinner.setPreferredSize(new Dimension(60, 25));
        inputPanel.add(secondSpinner, gbc);
        
        // RIGHT PANEL: Preset buttons
        JPanel presetPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        presetPanel.setBorder(BorderFactory.createTitledBorder("Quick Presets"));
        presetPanel.setPreferredSize(new Dimension(140, 200));
        
        JButton epochButton = new JButton("Unix Epoch");
        JButton nowButton = new JButton("Current Time");
        JButton y2kButton = new JButton("Y2K (2000)");
        JButton apolloButton = new JButton("Apollo 11");
        JButton futureButton = new JButton("Future (2030)");
        JButton spacerButton = new JButton(""); // Empty spacer
        spacerButton.setEnabled(false);
        spacerButton.setVisible(false);
        
        presetPanel.add(epochButton);
        presetPanel.add(nowButton);
        presetPanel.add(y2kButton);
        presetPanel.add(apolloButton);
        presetPanel.add(futureButton);
        presetPanel.add(spacerButton);
        
        // CENTER PANEL: Information area (gets most space)
        JTextArea infoArea = new JTextArea(15, 55);
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        infoArea.setBackground(new Color(248, 248, 248));
        infoArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JScrollPane infoScroll = new JScrollPane(infoArea);
        infoScroll.setBorder(BorderFactory.createTitledBorder("Date/Time Information & Celestial Positions"));
        infoScroll.setPreferredSize(new Dimension(500, 250));
        infoScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        infoScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        // Update info when date/time changes
        ActionListener updateInfo = e -> {
            try {
                int year = (Integer) yearSpinner.getValue();
                int month = monthCombo.getSelectedIndex() + 1;
                int day = (Integer) daySpinner.getValue();
                int hour = (Integer) hourSpinner.getValue();
                int minute = (Integer) minuteSpinner.getValue();
                int second = (Integer) secondSpinner.getValue();
                
                LocalDateTime newDateTime = LocalDateTime.of(year, month, day, hour, minute, second);
                long newSimulationTime = newDateTime.toEpochSecond(ZoneOffset.UTC);
                
                // Calculate Moon and Sun positions for this time
                double moonAngle = (84.7 + (newSimulationTime / (29.530 * 24 * 3600)) * 360.0) % 360.0;
                if (moonAngle < 0) moonAngle += 360.0;
                
                double sunAngle = (281.0 + (newSimulationTime / (365.25 * 24 * 3600)) * 360.0) % 360.0;
                if (sunAngle < 0) sunAngle += 360.0;
                
                StringBuilder info = new StringBuilder();
                info.append("SELECTED DATE & TIME:\n");
                info.append("=====================================\n");
                info.append(newDateTime.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")) + "\n");
                info.append(newDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " UTC\n");
                info.append("Unix Timestamp: ").append(newSimulationTime).append(" seconds\n\n");
                
                info.append("CELESTIAL BODY POSITIONS:\n");
                info.append("=====================================\n");
                info.append(String.format("Moon Position: %.1f° east of reference\n", moonAngle));
                info.append(String.format("Sun Position:  %.1f° from reference\n", sunAngle));
                info.append("\n");
                
                // Add some context about the date
                if (year == 1969 && month == 7 && day == 20) {
                    info.append("HISTORICAL SIGNIFICANCE:\n");
                    info.append("=====================================\n");
                    info.append("Apollo 11 Moon Landing!\n");
                    info.append("Neil Armstrong and Buzz Aldrin landed on\n");
                    info.append("the Moon at 20:17:40 UTC on this date.\n\n");
                } else if (year == 2000 && month == 1 && day == 1) {
                    info.append("HISTORICAL SIGNIFICANCE:\n");
                    info.append("=====================================\n");
                    info.append("Y2K - Millennium celebration!\n");
                    info.append("The start of the 21st century.\n\n");
                } else if (year == 1970 && month == 1 && day == 1) {
                    info.append("HISTORICAL SIGNIFICANCE:\n");
                    info.append("=====================================\n");
                    info.append("Unix Epoch - Beginning of computer time!\n");
                    info.append("Reference point for all Unix timestamps.\n\n");
                }
                
                info.append("SIMULATION EFFECTS:\n");
                info.append("=====================================\n");
                if (simulation.isLunarEffectsEnabled() || simulation.isSolarEffectsEnabled()) {
                    info.append("• Celestial bodies will move to new positions\n");
                    info.append("• Gravitational effects will be recalculated\n");
                    info.append("• Satellite orbital elements remain unchanged\n");
                    info.append("• Only time reference point changes\n");
                } else {
                    info.append("• Lunar/Solar effects are currently disabled\n");
                    info.append("• Enable them in Non-Keplerian Effects menu\n");
                    info.append("• Only simulation time display will change\n");
                }
                
                infoArea.setText(info.toString());
                infoArea.setCaretPosition(0); // Scroll to top
            } catch (Exception ex) {
                infoArea.setText("ERROR: Invalid date/time selection.\n\nPlease check your inputs:\n" +
                               "• Year: 1970-2100\n" +
                               "• Month: Valid month\n" +
                               "• Day: Valid for selected month\n" +
                               "• Time: 24-hour format\n\n" +
                               "Error details: " + ex.getMessage());
            }
        };
        
        // Add listeners to all components
        yearSpinner.addChangeListener(e -> updateInfo.actionPerformed(null));
        monthCombo.addActionListener(updateInfo);
        daySpinner.addChangeListener(e -> updateInfo.actionPerformed(null));
        hourSpinner.addChangeListener(e -> updateInfo.actionPerformed(null));
        minuteSpinner.addChangeListener(e -> updateInfo.actionPerformed(null));
        secondSpinner.addChangeListener(e -> updateInfo.actionPerformed(null));
        
        // Preset button actions
        epochButton.addActionListener(e -> {
            yearSpinner.setValue(1970);
            monthCombo.setSelectedIndex(0);
            daySpinner.setValue(1);
            hourSpinner.setValue(0);
            minuteSpinner.setValue(0);
            secondSpinner.setValue(0);
            updateInfo.actionPerformed(null);
        });
        
        nowButton.addActionListener(e -> {
            LocalDateTime now = LocalDateTime.now();
            yearSpinner.setValue(now.getYear());
            monthCombo.setSelectedIndex(now.getMonthValue() - 1);
            daySpinner.setValue(now.getDayOfMonth());
            hourSpinner.setValue(now.getHour());
            minuteSpinner.setValue(now.getMinute());
            secondSpinner.setValue(now.getSecond());
            updateInfo.actionPerformed(null);
        });
        
        y2kButton.addActionListener(e -> {
            yearSpinner.setValue(2000);
            monthCombo.setSelectedIndex(0);
            daySpinner.setValue(1);
            hourSpinner.setValue(0);
            minuteSpinner.setValue(0);
            secondSpinner.setValue(0);
            updateInfo.actionPerformed(null);
        });
        
        apolloButton.addActionListener(e -> {
            yearSpinner.setValue(1969);
            monthCombo.setSelectedIndex(6); // July
            daySpinner.setValue(20);
            hourSpinner.setValue(20);
            minuteSpinner.setValue(17);
            secondSpinner.setValue(40);
            updateInfo.actionPerformed(null);
        });
        
        futureButton.addActionListener(e -> {
            yearSpinner.setValue(2030);
            monthCombo.setSelectedIndex(0);
            daySpinner.setValue(1);
            hourSpinner.setValue(12);
            minuteSpinner.setValue(0);
            secondSpinner.setValue(0);
            updateInfo.actionPerformed(null);
        });
        
        // Initial info update
        updateInfo.actionPerformed(null);
        
        // BOTTOM PANEL: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("Set Time");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.setPreferredSize(new Dimension(100, 30));
        
        okButton.addActionListener(e -> {
            try {
                int year = (Integer) yearSpinner.getValue();
                int month = monthCombo.getSelectedIndex() + 1;
                int day = (Integer) daySpinner.getValue();
                int hour = (Integer) hourSpinner.getValue();
                int minute = (Integer) minuteSpinner.getValue();
                int second = (Integer) secondSpinner.getValue();
                
                LocalDateTime newDateTime = LocalDateTime.of(year, month, day, hour, minute, second);
                long newSimulationTime = newDateTime.toEpochSecond(ZoneOffset.UTC);
                
                simulation.setCurrentSimulationTime(newSimulationTime);
                simulation.updateDateTimeDisplay();
                simulation.getSimulationPanel().repaint();
                
                dialog.dispose();
                
                JOptionPane.showMessageDialog(simulation, 
                    "Simulation time updated to:\n" + 
                    newDateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss")) + " UTC\n\n" +
                    "Celestial bodies have moved to their positions for this date/time.", 
                    "Time Updated", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, 
                    "Invalid date/time values!\n\n" + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        // Assemble the main layout
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(presetPanel, BorderLayout.EAST);
        mainPanel.add(infoScroll, BorderLayout.CENTER);  // Gets most space
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
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

        // NEW: Atmospheric drag toggle
        JCheckBox atmosphericDragBox = new JCheckBox("Enable Atmospheric Drag Effects", simulation.isAtmosphericDragEnabled());
    
        
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(lunarEffectsBox, gbc);
        
        gbc.gridy = 1;
        panel.add(solarEffectsBox, gbc);

        gbc.gridy = 2;
        panel.add(atmosphericDragBox, gbc); // NEW: Add atmospheric drag checkbox
    
        
        // Information about effects
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JTextArea infoArea = new JTextArea(15, 40);
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

            if (atmosphericDragBox.isSelected()) {
            info.append("ATMOSPHERIC DRAG EFFECTS:\n");
            info.append("• Active between 80-1000 km altitude\n");
            info.append("• Opposes satellite velocity direction\n");
            info.append("• Causes orbital decay (energy loss)\n");
            info.append("• Circularizes orbits (reduces eccentricity)\n");
            info.append("• Most significant for LEO satellites\n");
            info.append("• Uses CD = 2.2, Area = 10 m², Mass = 1000 kg\n");
            info.append("• Gray acceleration vector will be displayed\n");
            info.append("  showing drag force direction\n");
            info.append("\n");
            }
            
            if (!lunarEffectsBox.isSelected() && !solarEffectsBox.isSelected() && !atmosphericDragBox.isSelected()) {
            info.append("NON-KEPLERIAN EFFECTS DISABLED\n\n");
            info.append("When enabled, these effects simulate:\n");
            info.append("• Third-body gravitational perturbations\n");
            info.append("• Atmospheric drag forces\n");
            info.append("• Orbital element evolution over time\n");
            info.append("• Realistic satellite orbital mechanics\n\n");
            info.append("Effects are most noticeable on:\n");
            info.append("• High-altitude satellites (GEO, HEO)\n");
            info.append("• Low-altitude satellites (LEO for drag)\n");
            info.append("• Eccentric orbits\n");
            info.append("• Inclined orbital planes\n");
        }
        
        if ((lunarEffectsBox.isSelected() || solarEffectsBox.isSelected()) && atmosphericDragBox.isSelected()) {
            info.append("COMBINED EFFECTS:\n");
            info.append("• Complex multi-body dynamics with drag\n");
            info.append("• Gravitational + dissipative forces\n");
            info.append("• Enhanced orbital evolution\n");
            info.append("• Multiple acceleration vectors displayed:\n");
            info.append("  - Blue: Combined lunar/solar gravity\n");
            info.append("  - Gray: Atmospheric drag\n");
        } else if (lunarEffectsBox.isSelected() && solarEffectsBox.isSelected()) {
            info.append("COMBINED GRAVITATIONAL EFFECTS:\n");
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
        atmosphericDragBox.addActionListener(updateInfo); // NEW: Add listener for atmospheric drag
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
        boolean oldAtmosphericDrag = simulation.isAtmosphericDragEnabled(); // NEW
        
        simulation.setLunarEffectsEnabled(lunarEffectsBox.isSelected());
        simulation.setSolarEffectsEnabled(solarEffectsBox.isSelected());
        simulation.setAtmosphericDragEnabled(atmosphericDragBox.isSelected()); // NEW
        
        // If any effects were toggled, update the satellite
        if (oldLunarEffects != simulation.isLunarEffectsEnabled() || 
            oldSolarEffects != simulation.isSolarEffectsEnabled() ||
            oldAtmosphericDrag != simulation.isAtmosphericDragEnabled()) { // NEW: Check atmospheric drag too
            simulation.createSatellite();
            simulation.getSimulationPanel().repaint();
        }
        
        dialog.dispose();
    });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridy = 4;
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
        
        String[] bodies = simulation.getCelestialBodies().keySet().toArray(new String[0]);
        Arrays.sort(bodies);
        JList<String> bodyList = new JList<>(bodies);
        bodyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bodyList.setSelectedValue(simulation.getCurrentBody(), true);
        
        JScrollPane scrollPane = new JScrollPane(bodyList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JTextArea infoArea = new JTextArea(6, 30);
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane infoScroll = new JScrollPane(infoArea);
        panel.add(infoScroll, BorderLayout.SOUTH);
        
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
        
        if (bodyList.getSelectedValue() != null) {
            bodyList.getListSelectionListeners()[0].valueChanged(
                new javax.swing.event.ListSelectionEvent(bodyList, 0, 0, false));
        }
        
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
        
        JCheckBox autoClearZoomBox = new JCheckBox("Auto-clear trail when zooming", simulation.getAutoClearOnZoom());
        JCheckBox autoClearUpdateBox = new JCheckBox("Auto-clear trail when updating orbit", simulation.getAutoClearOnUpdate());
        
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(autoClearZoomBox, gbc);
        
        gbc.gridy = 1;
        panel.add(autoClearUpdateBox, gbc);
        
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
        
        JTextField earthRadiusField = new JTextField(String.valueOf(simulation.getEarthRadius() / 1000), 10);
        JTextField earthMassField = new JTextField(String.format("%.3e", simulation.getEarthMass()), 10);
        JTextField gravConstField = new JTextField(String.format("%.5e", simulation.getGravitationalConstant()), 10);
        
        gbc.gridy = 0;
        gbc.gridx = 0; panel.add(new JLabel("Earth Radius (km):"), gbc);
        gbc.gridx = 1; panel.add(earthRadiusField, gbc);
        
        gbc.gridy = 1;
        gbc.gridx = 0; panel.add(new JLabel("Earth Mass (kg):"), gbc);
        gbc.gridx = 1; panel.add(earthMassField, gbc);
        
        gbc.gridy = 2;
        gbc.gridx = 0; panel.add(new JLabel("Gravitational Constant:"), gbc);
        gbc.gridx = 1; panel.add(gravConstField, gbc);
        
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Note: Changing these values affects orbital calculations"), gbc);
        
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
        
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(e -> {
            try {
                simulation.setMaxTrailLength(Integer.parseInt(trailLengthField.getText()));
                Color currentTrail = simulation.getTrailColor();
                simulation.setTrailColor(new Color(currentTrail.getRed(), currentTrail.getGreen(), 
                                     currentTrail.getBlue(), trailOpacitySlider.getValue()));
                float newWidth = Float.parseFloat(trailWidthField.getText());
                if (newWidth < 0.1f) newWidth = 0.1f;
                if (newWidth > 20.0f) newWidth = 20.0f;
                simulation.setTrailWidth(newWidth);
                
                simulation.getSimulationPanel().updateSettings();
                simulation.getSimulationPanel().repaint();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid input values!");
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        dialog.setLayout(new BorderLayout());
        dialog.add(tabbedPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
}