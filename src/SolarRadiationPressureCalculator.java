/**
 * SolarRadiationPressureCalculator handles solar radiation pressure perturbations
 * Includes umbra, penumbra, and direct sunlight calculations
 * Separated from Satellite class for better code organization and modularity
 */
public class SolarRadiationPressureCalculator {
    // Solar radiation constants
    private static final double SOLAR_CONSTANT = 1361.0; // W/m² at 1 AU
    private static final double SPEED_OF_LIGHT = 299792458.0; // m/s
    private static final double AU_DISTANCE = 149597870700.0; // 1 AU in meters
    private static final double SUN_RADIUS = 695700000.0; // Sun radius in meters
    
    // Satellite physical properties (same as atmospheric drag for consistency)
    private static final double SATELLITE_AREA = 10.0; // m² cross-sectional area
    private static final double SATELLITE_MASS = 1000.0; // kg satellite mass
    private static final double REFLECTIVITY_COEFFICIENT = 0.6; // Surface reflectivity (0=absorb all, 1=reflect all)
    private static final double DIFFUSE_REFLECTION_FACTOR = 2.0/3.0; // Fraction of diffuse reflection
    
    // Shadow calculation constants
    private static final double UMBRA_FACTOR = 0.0; // No solar flux in umbra
    private static final double PENUMBRA_MIN_FACTOR = 0.0; // Minimum flux in penumbra
    private static final double PENUMBRA_MAX_FACTOR = 1.0; // Maximum flux in penumbra
    
    /**
     * Applies solar radiation pressure perturbations to satellite orbital elements
     * 
     * Solar radiation pressure is caused by photons from the Sun hitting the satellite.
     * The pressure depends on:
     * - Solar flux at satellite location
     * - Satellite's cross-sectional area and reflectivity
     * - Whether satellite is in Earth's shadow (umbra/penumbra)
     * 
     * @param satellite The satellite object to apply perturbations to
     * @param deltaTime Time step in seconds
     * @param simulation Reference to main simulation for Sun position and time
     */
    public void applyPerturbations(Satellite satellite, double deltaTime, OrbitalSimulation simulation) {
        // Get current satellite and sun positions
        double[] satPos3D = satellite.getPosition3D();
        double[] sunPos = simulation.getSunPosition();
        
        // Calculate shadow conditions (umbra, penumbra, or sunlight)
        ShadowCondition shadowCondition = calculateShadowCondition(satPos3D, sunPos, simulation);
        
        // No radiation pressure in complete shadow
        if (shadowCondition.lightingFactor <= 0) {
            return;
        }
        
        // Calculate solar radiation pressure acceleration
        double[] radiationAccel = calculateRadiationPressureAcceleration(satPos3D, sunPos, 
                                                                        shadowCondition, simulation);
        
        // Apply perturbations to orbital elements
        applyRadiationPerturbations(satellite, radiationAccel, deltaTime);
    }
    
    /**
     * Calculates whether satellite is in umbra, penumbra, or direct sunlight
     * Uses proper shadow cone geometry with Sun's finite size
     * 
     * @param satPos3D Satellite position in 3D coordinates
     * @param sunPos Sun position (2D, assuming z=0)
     * @param simulation Reference to simulation for Earth radius
     * @return ShadowCondition object with lighting factor and shadow type
     */
    private ShadowCondition calculateShadowCondition(double[] satPos3D, double[] sunPos, OrbitalSimulation simulation) {
        double satX = satPos3D[0];
        double satY = satPos3D[1];
        double satZ = satPos3D[2];
        
        // Extend sun position to 3D (assume Sun at z=0)
        double[] sunPos3D = {sunPos[0], sunPos[1], 0.0};
        
        // Calculate distances
        double satDistance = Math.sqrt(satX*satX + satY*satY + satZ*satZ);
        double sunDistance = Math.sqrt(sunPos3D[0]*sunPos3D[0] + sunPos3D[1]*sunPos3D[1]);
        
        // Vector from Earth center to satellite
        double[] earthToSat = {satX, satY, satZ};
        
        // Vector from Earth center to Sun (normalized)
        double[] earthToSun = {sunPos3D[0]/sunDistance, sunPos3D[1]/sunDistance, sunPos3D[2]/sunDistance};
        
        // Project satellite position onto Earth-Sun line
        double projectionLength = earthToSat[0]*earthToSun[0] + earthToSat[1]*earthToSun[1] + earthToSat[2]*earthToSun[2];
        
        // If satellite is on the day side, it's in direct sunlight
        if (projectionLength >= 0) {
            return new ShadowCondition(ShadowType.DIRECT_SUNLIGHT, 1.0);
        }
        
        // Satellite is on night side - check shadow geometry
        double earthRadius = simulation.getEarthRadius();
        
        // Calculate the point on Earth-Sun line closest to satellite
        double[] closestPoint = {
            projectionLength * earthToSun[0],
            projectionLength * earthToSun[1],
            projectionLength * earthToSun[2]
        };
        
        // Distance from satellite to Earth-Sun line
        double crossTrackDistance = Math.sqrt(
            Math.pow(satX - closestPoint[0], 2) +
            Math.pow(satY - closestPoint[1], 2) +
            Math.pow(satZ - closestPoint[2], 2)
        );
        
        // Calculate umbra and penumbra radii at satellite distance
        double distanceFromEarth = Math.abs(projectionLength);
        
        // Umbra cone (complete shadow)
        double umbralAngle = Math.atan((SUN_RADIUS - earthRadius) / sunDistance);
        double umbraRadius = earthRadius - distanceFromEarth * Math.tan(umbralAngle);
        
        // Penumbra cone (partial shadow)
        double penumbralAngle = Math.atan((SUN_RADIUS + earthRadius) / sunDistance);
        double penumbraRadius = earthRadius + distanceFromEarth * Math.tan(penumbralAngle);
        
        // Determine shadow condition
        if (umbraRadius > 0 && crossTrackDistance <= umbraRadius) {
            // Complete shadow (umbra)
            return new ShadowCondition(ShadowType.UMBRA, UMBRA_FACTOR);
        } else if (crossTrackDistance <= penumbraRadius) {
            // Partial shadow (penumbra)
            // Calculate lighting factor based on position within penumbra
            double penumbraWidth = penumbraRadius - Math.max(0, umbraRadius);
            double distanceFromUmbra = crossTrackDistance - Math.max(0, umbraRadius);
            double lightingFactor = distanceFromUmbra / penumbraWidth;
            
            // Ensure factor is between 0 and 1
            lightingFactor = Math.max(PENUMBRA_MIN_FACTOR, Math.min(PENUMBRA_MAX_FACTOR, lightingFactor));
            
            return new ShadowCondition(ShadowType.PENUMBRA, lightingFactor);
        } else {
            // Direct sunlight (outside shadow cones)
            return new ShadowCondition(ShadowType.DIRECT_SUNLIGHT, 1.0);
        }
    }
    
    /**
     * Calculates solar radiation pressure acceleration vector
     * 
     * @param satPos3D Satellite position
     * @param sunPos Sun position
     * @param shadowCondition Current shadow/lighting conditions
     * @param simulation Reference to simulation
     * @return Acceleration vector [x, y, z] in m/s² pointing AWAY from Sun
     */
    private double[] calculateRadiationPressureAcceleration(double[] satPos3D, double[] sunPos, 
                                                           ShadowCondition shadowCondition, OrbitalSimulation simulation) {
        // Extend sun position to 3D (assume Sun at z=0)
        double[] sunPos3D = {sunPos[0], sunPos[1], 0.0};
        
        // Vector from satellite to Sun
        double[] satToSun = {
            sunPos3D[0] - satPos3D[0],
            sunPos3D[1] - satPos3D[1],
            sunPos3D[2] - satPos3D[2]
        };
        
        double satSunDistance = Math.sqrt(satToSun[0]*satToSun[0] + satToSun[1]*satToSun[1] + satToSun[2]*satToSun[2]);
        
        // Normalize to unit vector pointing FROM satellite TO sun
        double[] toSunDirection = {
            satToSun[0] / satSunDistance,
            satToSun[1] / satSunDistance,
            satToSun[2] / satSunDistance
        };
        
        // Radiation pressure pushes AWAY from sun (opposite to sunlight direction)
        double[] awayFromSunDirection = {
            -toSunDirection[0],
            -toSunDirection[1],
            -toSunDirection[2]
        };
        
        // Calculate solar flux at satellite distance
        double nominalSolarFlux = SOLAR_CONSTANT * (AU_DISTANCE * AU_DISTANCE) / (satSunDistance * satSunDistance);
        
        // Apply shadow/lighting factor
        double effectiveSolarFlux = nominalSolarFlux * shadowCondition.lightingFactor;
        
        // Add solar cycle variations (optional enhancement)
        double currentTime = simulation.getCurrentSimulationTime();
        double solarCycleVariation = calculateSolarCycleVariation(currentTime);
        effectiveSolarFlux *= solarCycleVariation;
        
        // Calculate radiation pressure (momentum transfer)
        double radiationPressure = effectiveSolarFlux / SPEED_OF_LIGHT; // N/m²
        
        // Force calculation depends on surface properties
        // For a satellite with mixed absorption/reflection:
        // F = (flux/c) * Area * (1 + reflectivity_factor)
        double absorptionFactor = 1.0; // Momentum transfer from absorption
        double reflectionFactor = REFLECTIVITY_COEFFICIENT * (1.0 + DIFFUSE_REFLECTION_FACTOR);
        double totalMomentumFactor = absorptionFactor + reflectionFactor;
        
        double forcePerUnitArea = radiationPressure * totalMomentumFactor;
        double totalForce = forcePerUnitArea * SATELLITE_AREA; // Newtons
        double accelerationMagnitude = totalForce / SATELLITE_MASS; // m/s²
        
        // Return acceleration vector pointing AWAY from Sun
        return new double[]{
            accelerationMagnitude * awayFromSunDirection[0],
            accelerationMagnitude * awayFromSunDirection[1],
            accelerationMagnitude * awayFromSunDirection[2]
        };
    }
    
    /**
     * Calculates solar cycle variations in solar flux
     * 11-year solar cycle causes ±3.4% variation in solar output
     * 
     * @param currentTime Current simulation time in seconds since epoch
     * @return Solar flux multiplier (0.966 to 1.034)
     */
    private double calculateSolarCycleVariation(double currentTime) {
        // 11-year solar cycle (approximate)
        double solarCyclePeriod = 11.0 * 365.25 * 24 * 3600; // seconds
        double cyclePhase = 2 * Math.PI * currentTime / solarCyclePeriod;
        
        // ±3.4% variation around nominal
        double variationAmplitude = 0.034;
        double solarMultiplier = 1.0 + variationAmplitude * Math.sin(cyclePhase);
        
        return solarMultiplier;
    }
    
    /**
     * Applies radiation pressure perturbations to orbital elements
     * 
     * @param satellite Satellite object to perturb
     * @param radiationAccel Acceleration vector from radiation pressure
     * @param deltaTime Time step
     */
    private void applyRadiationPerturbations(Satellite satellite, double[] radiationAccel, double deltaTime) {
        double accelerationMagnitude = Math.sqrt(
            radiationAccel[0]*radiationAccel[0] + 
            radiationAccel[1]*radiationAccel[1] + 
            radiationAccel[2]*radiationAccel[2]
        );
        
        if (accelerationMagnitude <= 0) return;
        
        // Scale perturbations for orbital element changes
        // Solar radiation pressure effects are typically very small
        double perturbationStrength = accelerationMagnitude * 1e6; // Scale for visibility
        
        // Get current orbital elements for perturbation calculations
        double nu = satellite.getTrueAnomaly();
        double e = satellite.getEccentricity();
        
        // Apply perturbations (scaled for educational visibility)
        // In reality, these effects accumulate over months/years
        
        // 1. Semi-major axis variations (periodic, small secular change)
        double smaChange = perturbationStrength * 1e-6 * Math.sin(nu) * deltaTime;
        satellite.adjustSemiMajorAxis(smaChange);
        
        // 2. Eccentricity changes (can be significant over long periods)
        double eccChange = perturbationStrength * 1e-12 * Math.cos(nu) * deltaTime;
        satellite.adjustEccentricity(eccChange);
        
        // 3. Argument of periapsis rotation (small but measurable)
        double argPeriChange = perturbationStrength * 1e-11 * (1 + e * Math.cos(nu)) * deltaTime;
        satellite.adjustArgumentOfPeriapsis(argPeriChange);
        
        // 4. Small inclination effects (usually negligible)
        double inclinationChange = perturbationStrength * 1e-13 * Math.sin(nu) * deltaTime;
        satellite.adjustInclination(inclinationChange);
        
        // 5. Longitude of ascending node (very small effect)
        double nodeChange = perturbationStrength * 1e-13 * Math.cos(satellite.getInclination()) * deltaTime;
        satellite.adjustLongitudeOfAscendingNode(nodeChange);
    }
    
    /**
     * Gets current solar radiation pressure acceleration for display purposes
     * 
     * @param satellite Satellite object
     * @param simulation Simulation reference
     * @return Radiation pressure acceleration magnitude in m/s²
     */
    public double getCurrentRadiationAcceleration(Satellite satellite, OrbitalSimulation simulation) {
        double[] satPos3D = satellite.getPosition3D();
        double[] sunPos = simulation.getSunPosition();
        
        ShadowCondition shadowCondition = calculateShadowCondition(satPos3D, sunPos, simulation);
        
        if (shadowCondition.lightingFactor <= 0) {
            return 0; // No radiation pressure in complete shadow
        }
        
        double[] radiationAccel = calculateRadiationPressureAcceleration(satPos3D, sunPos, shadowCondition, simulation);
        
        return Math.sqrt(radiationAccel[0]*radiationAccel[0] + 
                        radiationAccel[1]*radiationAccel[1] + 
                        radiationAccel[2]*radiationAccel[2]);
    }
    
    /**
     * Gets current shadow condition for display purposes
     * 
     * @param satellite Satellite object
     * @param simulation Simulation reference
     * @return Current shadow condition
     */
    public ShadowCondition getCurrentShadowCondition(Satellite satellite, OrbitalSimulation simulation) {
        double[] satPos3D = satellite.getPosition3D();
        double[] sunPos = simulation.getSunPosition();
        
        return calculateShadowCondition(satPos3D, sunPos, simulation);
    }
    
    /**
     * Gets detailed radiation pressure information for display
     * 
     * @param satellite Satellite object
     * @param simulation Simulation reference
     * @return Information string about current radiation pressure state
     */
    public String getRadiationPressureInfo(Satellite satellite, OrbitalSimulation simulation) {
        ShadowCondition condition = getCurrentShadowCondition(satellite, simulation);
        double acceleration = getCurrentRadiationAcceleration(satellite, simulation);
        
        StringBuilder info = new StringBuilder();
        info.append("Solar Radiation Pressure:\n");
        info.append(String.format("• Shadow condition: %s\n", condition.shadowType));
        info.append(String.format("• Lighting factor: %.3f\n", condition.lightingFactor));
        info.append(String.format("• Acceleration: %.3e m/s²\n", acceleration));
        
        if (condition.shadowType == ShadowType.PENUMBRA) {
            info.append(String.format("• Partial eclipse: %.1f%% sunlight\n", condition.lightingFactor * 100));
        }
        
        return info.toString();
    }
    
    /**
     * Inner class to represent shadow conditions
     */
    public static class ShadowCondition {
        public final ShadowType shadowType;
        public final double lightingFactor; // 0.0 = complete shadow, 1.0 = direct sunlight
        
        public ShadowCondition(ShadowType shadowType, double lightingFactor) {
            this.shadowType = shadowType;
            this.lightingFactor = lightingFactor;
        }
    }
    
    /**
     * Enumeration of shadow types
     */
    public enum ShadowType {
        DIRECT_SUNLIGHT("Direct Sunlight"),
        PENUMBRA("Penumbra (Partial Shadow)"),
        UMBRA("Umbra (Complete Shadow)");
        
        private final String description;
        
        ShadowType(String description) {
            this.description = description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
}