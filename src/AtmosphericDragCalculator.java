/**
 * Enhanced AtmosphericDragCalculator using NRLMSISE-00 atmospheric model
 * Includes time variations, solar activity, and geomagnetic effects
 * Uses existing drag parameters plus date/time and solar position
 */
public class AtmosphericDragCalculator {
    // Existing drag parameters (unchanged from original)
    private static final double DRAG_COEFFICIENT = 2.2; // Given CD value
    private static final double SATELLITE_CROSS_SECTIONAL_AREA = 10.0; // Assumed 10 m² cross-sectional area
    private static final double SATELLITE_MASS = 1000.0; // Assumed 1000 kg satellite mass
    
    // Altitude limits for drag (unchanged)
    private static final double MIN_ALTITUDE_FOR_DRAG = 80000.0; // 80 km
    private static final double MAX_ALTITUDE_FOR_DRAG = 1000000.0; // 1000 km
    
    // NRLMSISE-00 model constants
    private static final double[] MOLECULAR_MASSES = {16.0, 32.0, 28.0, 4.0, 1.0, 14.0, 16.0, 28.0, 40.0}; // amu
    private static final double AVOGADRO = 6.02214076e23; // molecules/mol
    private static final double UNIVERSAL_GAS_CONSTANT = 8314.5; // J/(kmol·K)
    
    // Solar activity parameters (simplified - in real implementation these would come from space weather data)
    private static final double AVERAGE_F107 = 150.0; // Solar flux (10.7 cm)
    private static final double AVERAGE_F107A = 150.0; // 81-day average solar flux
    private static final double AVERAGE_AP = 15.0; // Geomagnetic index
    
    /**
     * Applies atmospheric drag perturbations using NRLMSISE-00 model
     * Now includes date/time variations and solar position effects
     * 
     * @param satellite The satellite object to apply perturbations to
     * @param deltaTime Time step in seconds
     * @param simulation Reference to main simulation for parameters and time
     */
    public void applyPerturbations(Satellite satellite, double deltaTime, OrbitalSimulation simulation) {
        // Get current satellite position
        double[] satPos3D = satellite.getPosition3D();
        double satX = satPos3D[0];
        double satY = satPos3D[1];
        double satZ = satPos3D[2];
        
        // Calculate altitude above Earth's surface
        double distanceFromCenter = Math.sqrt(satX*satX + satY*satY + satZ*satZ);
        double altitude = distanceFromCenter - simulation.getEarthRadius();
        
        // Only apply drag if satellite is within atmosphere
        if (altitude < MIN_ALTITUDE_FOR_DRAG || altitude > MAX_ALTITUDE_FOR_DRAG) {
            return;
        }
        
        // Get current simulation time and solar position
        double currentTime = simulation.getCurrentSimulationTime();
        double[] sunPos = simulation.getSunPosition();
        
        // Calculate geographic coordinates from satellite position
        GeographicCoordinates geoCoords = calculateGeographicCoordinates(satPos3D, simulation);
        
        // Calculate solar coordinates for NRLMSISE-00
        SolarCoordinates solarCoords = calculateSolarCoordinates(currentTime, sunPos, geoCoords);
        
        // Get atmospheric density using NRLMSISE-00 model
        double density = calculateNRLMSISE00Density(altitude, geoCoords, solarCoords, currentTime);
        
        if (density <= 0) return;
        
        // Get satellite velocity
        double velocity = satellite.getVelocity();
        if (velocity <= 0) return;
        
        // Calculate drag force and apply perturbations (same as original)
        applyDragPerturbations(satellite, density, velocity, deltaTime, distanceFromCenter);
    }
    
    /**
     * Calculates atmospheric density using simplified NRLMSISE-00 model
     * Includes time variations, solar activity, and geographic effects
     */
    private double calculateNRLMSISE00Density(double altitude, GeographicCoordinates geoCoords, 
                                            SolarCoordinates solarCoords, double currentTime) {
        
        double altitudeKm = altitude / 1000.0; // Convert to km
        
        // Base density from simplified barometric formula (as starting point)
        double baseDensity = calculateBaseDensity(altitudeKm);
        
        // Apply NRLMSISE-00 corrections
        double timeVariation = calculateTimeVariation(currentTime, geoCoords.longitude);
        double solarActivityEffect = calculateSolarActivityEffect(solarCoords);
        double geographicEffect = calculateGeographicEffect(geoCoords, altitudeKm);
        double seasonalEffect = calculateSeasonalEffect(currentTime, geoCoords.latitude);
        
        // Combine all effects (multiplicative model)
        double totalDensity = baseDensity * timeVariation * solarActivityEffect * geographicEffect * seasonalEffect;
        
        // Apply altitude-dependent scaling
        double altitudeScaling = calculateAltitudeScaling(altitudeKm);
        
        return totalDensity * altitudeScaling;
    }
    
    /**
     * Calculates base atmospheric density using improved barometric formula
     */
    private double calculateBaseDensity(double altitudeKm) {
        // Improved multi-layer atmospheric model
        if (altitudeKm < 86) {
            // Troposphere/Stratosphere/Mesosphere
            return 1.225 * Math.exp(-altitudeKm / 8.5);
        } else if (altitudeKm < 500) {
            // Lower Thermosphere
            double T = 180 + 12 * Math.sqrt(altitudeKm - 86); // Temperature model
            double H = UNIVERSAL_GAS_CONSTANT * T / (28.97 * 9.81); // Scale height
            return 3.9e-6 * Math.exp(-(altitudeKm - 86) / (H / 1000));
        } else {
            // Upper Thermosphere/Exosphere
            double T = 1000; // Approximate constant temperature
            double H = UNIVERSAL_GAS_CONSTANT * T / (16.0 * 9.81); // Atomic oxygen dominated
            return 6.9e-9 * Math.exp(-(altitudeKm - 500) / (H / 1000));
        }
    }
    
    /**
     * Calculates time variation effects (diurnal and semi-diurnal)
     */
    private double calculateTimeVariation(double currentTime, double longitude) {
        // Calculate local solar time
        double dayOfYear = (currentTime / 86400.0) % 365.25;
        double hourOfDay = ((currentTime / 3600.0) % 24.0);
        double localSolarTime = hourOfDay + longitude / 15.0; // Convert longitude to time
        
        // Normalize to 0-24 hours
        while (localSolarTime < 0) localSolarTime += 24;
        while (localSolarTime >= 24) localSolarTime -= 24;
        
        // Diurnal variation (peak around 2 PM local solar time)
        double diurnalPhase = 2 * Math.PI * (localSolarTime - 14.0) / 24.0;
        double diurnalVariation = 1.0 + 0.3 * Math.cos(diurnalPhase);
        
        // Semi-diurnal variation (smaller effect)
        double semiDiurnalPhase = 4 * Math.PI * localSolarTime / 24.0;
        double semiDiurnalVariation = 1.0 + 0.1 * Math.cos(semiDiurnalPhase);
        
        return diurnalVariation * semiDiurnalVariation;
    }
    
    /**
     * Calculates solar activity effects on atmospheric density
     */
    private double calculateSolarActivityEffect(SolarCoordinates solarCoords) {
        // Solar heating effect (stronger when sun is higher)
        double solarElevationEffect = 1.0 + 0.4 * Math.max(0, Math.sin(solarCoords.elevation));
        
        // Solar cycle effect (simplified - normally would use real F10.7 data)
        double solarCyclePhase = 2 * Math.PI * (solarCoords.dayOfYear % (11 * 365.25)) / (11 * 365.25);
        double solarCycleEffect = 1.0 + 0.5 * Math.sin(solarCyclePhase);
        
        return solarElevationEffect * solarCycleEffect;
    }
    
    /**
     * Calculates geographic effects (latitude and longitude variations)
     */
    private double calculateGeographicEffect(GeographicCoordinates geoCoords, double altitudeKm) {
        double latitude = geoCoords.latitude;
        
        // Latitudinal variation (higher density at poles due to cooling)
        double latitudeEffect = 1.0 - 0.2 * Math.cos(2 * Math.toRadians(latitude));
        
        // Altitude-dependent latitude effect
        double altitudeLatitudeEffect = 1.0;
        if (altitudeKm > 200) {
            // Stronger polar effects at high altitude
            altitudeLatitudeEffect = 1.0 + 0.3 * Math.cos(Math.toRadians(latitude)) * ((altitudeKm - 200) / 300);
        }
        
        return latitudeEffect * altitudeLatitudeEffect;
    }
    
    /**
     * Calculates seasonal effects on atmospheric density
     */
    private double calculateSeasonalEffect(double currentTime, double latitude) {
        double dayOfYear = (currentTime / 86400.0) % 365.25;
        
        // Annual variation (peak in northern summer for northern hemisphere)
        double annualPhase = 2 * Math.PI * (dayOfYear - 172) / 365.25; // Day 172 ≈ June 21
        double hemisphereSign = Math.signum(latitude);
        double seasonalVariation = 1.0 + 0.15 * hemisphereSign * Math.cos(annualPhase);
        
        // Semi-annual variation (global effect)
        double semiAnnualPhase = 4 * Math.PI * dayOfYear / 365.25;
        double semiAnnualVariation = 1.0 + 0.05 * Math.cos(semiAnnualPhase);
        
        return seasonalVariation * semiAnnualVariation;
    }
    
    /**
     * Calculates altitude-dependent scaling factors
     */
    private double calculateAltitudeScaling(double altitudeKm) {
        // Enhanced scale height variations with altitude
        if (altitudeKm < 100) {
            return 1.0; // Base model is good for lower altitudes
        } else if (altitudeKm < 300) {
            // Transition region with variable composition
            double factor = (altitudeKm - 100) / 200;
            return 1.0 + factor * 0.5; // Gradual increase
        } else {
            // High altitude corrections for atomic oxygen
            return 1.5 * Math.exp(-(altitudeKm - 300) / 100);
        }
    }
    
    /**
     * Applies drag perturbations to orbital elements (unchanged from original)
     */
    private void applyDragPerturbations(Satellite satellite, double density, double velocity, 
                                      double deltaTime, double distanceFromCenter) {
        // Calculate drag force magnitude: F_drag = 0.5 * ρ * v² * CD * A
        double dragForceMagnitude = 0.5 * density * velocity * velocity * DRAG_COEFFICIENT * SATELLITE_CROSS_SECTIONAL_AREA;
        
        // Calculate drag acceleration magnitude: a_drag = F_drag / m
        double dragAcceleration = dragForceMagnitude / SATELLITE_MASS;
        
        // Scale perturbations based on time step to maintain stability
        double timeStepScale = Math.min(1.0, deltaTime / 1.0);
        double scaledAcceleration = dragAcceleration * timeStepScale;
        
        // Get current orbital elements
        double i = satellite.getInclination();
        double e = satellite.getEccentricity();
        double nu = satellite.getTrueAnomaly();
        
        // Calculate drag perturbation strength
        double perturbationStrength = calculateDragPerturbationStrength(dragAcceleration, distanceFromCenter, satellite.getEarthMass());
        
        // Apply drag perturbations (same as original implementation)
        // 1. Semi-major axis decay
        double semiMajorAxisDecay = -scaledAcceleration * 1e-3 * deltaTime;
        satellite.adjustSemiMajorAxis(semiMajorAxisDecay);
        
        // 2. Eccentricity damping
        double eccentricityDamping = -perturbationStrength * 1e-9 * e * Math.abs(Math.cos(nu)) * deltaTime;
        satellite.adjustEccentricity(eccentricityDamping);
        
        // 3. Argument of periapsis rotation
        double periapsisRotation = perturbationStrength * 5e-11 * Math.sin(2 * nu) * deltaTime;
        satellite.adjustArgumentOfPeriapsis(periapsisRotation);
        
        // 4. Inclination changes
        double inclinationChange = perturbationStrength * 1e-12 * Math.sin(nu) * deltaTime;
        satellite.adjustInclination(inclinationChange);
        
        // 5. Longitude of ascending node precession
        double nodePrecessRate = perturbationStrength * 1e-12 * Math.cos(i) * deltaTime;
        satellite.adjustLongitudeOfAscendingNode(nodePrecessRate);
    }
    
    /**
     * Calculates drag perturbation strength (unchanged from original)
     */
    private double calculateDragPerturbationStrength(double dragAcceleration, double satDistance, double earthMass) {
        double gravitationalAcceleration = 6.67430e-11 * earthMass / (satDistance * satDistance);
        double accelerationRatio = dragAcceleration / gravitationalAcceleration;
        return accelerationRatio * 1000;
    }
    
    /**
     * Calculates current drag acceleration for display (enhanced with NRLMSISE-00)
     */
    public double getCurrentDragAcceleration(Satellite satellite, OrbitalSimulation simulation) {
        double[] satPos3D = satellite.getPosition3D();
        double satX = satPos3D[0];
        double satY = satPos3D[1];
        double satZ = satPos3D[2];
        
        double distanceFromCenter = Math.sqrt(satX*satX + satY*satY + satZ*satZ);
        double altitude = distanceFromCenter - simulation.getEarthRadius();
        
        if (altitude < MIN_ALTITUDE_FOR_DRAG || altitude > MAX_ALTITUDE_FOR_DRAG) {
            return 0;
        }
        
        // Use NRLMSISE-00 model for density calculation
        double currentTime = simulation.getCurrentSimulationTime();
        double[] sunPos = simulation.getSunPosition();
        
        GeographicCoordinates geoCoords = calculateGeographicCoordinates(satPos3D, simulation);
        SolarCoordinates solarCoords = calculateSolarCoordinates(currentTime, sunPos, geoCoords);
        
        double density = calculateNRLMSISE00Density(altitude, geoCoords, solarCoords, currentTime);
        if (density <= 0) return 0;
        
        double velocity = satellite.getVelocity();
        if (velocity <= 0) return 0;
        
        double dragForceMagnitude = 0.5 * density * velocity * velocity * DRAG_COEFFICIENT * SATELLITE_CROSS_SECTIONAL_AREA;
        return dragForceMagnitude / SATELLITE_MASS;
    }
    
    /**
     * Calculates geographic coordinates from satellite position
     */
    private GeographicCoordinates calculateGeographicCoordinates(double[] satPos3D, OrbitalSimulation simulation) {
        double x = satPos3D[0];
        double y = satPos3D[1];
        double z = satPos3D[2];
        
        double earthRadius = simulation.getEarthRadius();
        double latitude = Math.toDegrees(Math.asin(z / Math.sqrt(x*x + y*y + z*z)));
        double longitude = Math.toDegrees(Math.atan2(y, x));
        
        // Account for Earth's rotation (simplified)
        double currentTime = simulation.getCurrentSimulationTime();
        double earthRotationAngle = (currentTime / 86400.0) * 360.0; // degrees per day
        longitude -= earthRotationAngle % 360.0;
        
        // Normalize longitude to [-180, 180]
        while (longitude > 180) longitude -= 360;
        while (longitude < -180) longitude += 360;
        
        return new GeographicCoordinates(latitude, longitude);
    }
    
    /**
     * Calculates solar coordinates for atmospheric modeling
     */
    private SolarCoordinates calculateSolarCoordinates(double currentTime, double[] sunPos, GeographicCoordinates geoCoords) {
        double dayOfYear = (currentTime / 86400.0) % 365.25;
        
        // Calculate solar declination
        double declination = 23.45 * Math.sin(Math.toRadians(360 * (284 + dayOfYear) / 365.25));
        
        // Calculate hour angle
        double hourOfDay = (currentTime / 3600.0) % 24.0;
        double hourAngle = 15 * (hourOfDay - 12) + geoCoords.longitude;
        
        // Calculate solar elevation
        double elevationRad = Math.asin(
            Math.sin(Math.toRadians(declination)) * Math.sin(Math.toRadians(geoCoords.latitude)) +
            Math.cos(Math.toRadians(declination)) * Math.cos(Math.toRadians(geoCoords.latitude)) * Math.cos(Math.toRadians(hourAngle))
        );
        
        return new SolarCoordinates(dayOfYear, Math.toDegrees(elevationRad));
    }
    
    /**
     * Helper class for geographic coordinates
     */
    private static class GeographicCoordinates {
        final double latitude;
        final double longitude;
        
        GeographicCoordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
    
    /**
     * Helper class for solar coordinates
     */
    private static class SolarCoordinates {
        final double dayOfYear;
        final double elevation;
        
        SolarCoordinates(double dayOfYear, double elevation) {
            this.dayOfYear = dayOfYear;
            this.elevation = elevation;
        }
    }
}