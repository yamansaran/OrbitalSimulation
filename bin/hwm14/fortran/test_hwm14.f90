program test_hwm14
    use hwm14_module
    implicit none
    
    integer :: iyd
    real :: sec, alt, glat, glon, stl, f107a, f107
    real :: ap(2)
    real :: w(2)
    
    ! Test parameters for a specific date/time/location
    iyd = 15001      ! January 1, 2015 (year 15, day 001)
    sec = 43200.0    ! 12:00 UT (noon)
    alt = 150.0      ! 150 km altitude
    glat = 40.0      ! 40° North latitude
    glon = -75.0     ! 75° West longitude (East Coast US)
    stl = 0.0        ! Not used in HWM14
    f107a = 150.0    ! Not used in HWM14
    f107 = 150.0     ! Not used in HWM14
    ap(1) = 0.0      ! Not used
    ap(2) = 4.0      ! Geomagnetic activity index (quiet conditions)
    
    ! Call HWM14
    call hwm14(iyd, sec, alt, glat, glon, stl, f107a, f107, ap, w)
    
    ! Print results
    print *, 'HWM14 Test Results:'
    print *, 'Date: January 1, 2015, 12:00 UT'
    print *, 'Location: 40°N, 75°W, 150 km altitude'
    print *, 'Meridional wind (m/s):', w(1)
    print *, 'Zonal wind (m/s):', w(2)
    print *, 'Test completed successfully!'
    
end program test_hwm14