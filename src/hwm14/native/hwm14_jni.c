#include <jni.h>
#include <stdio.h>
#include <stdlib.h>

// Forward declaration of Fortran HWM14 subroutine
// The actual function name might be different due to Fortran name mangling
extern void __hwm14_module_MOD_hwm14(int* iyd, float* sec, float* alt, float* glat, float* glon, 
                                     float* stl, float* f107a, float* f107, float* ap, float* w);

/*
 * Class:     HWM14Integration
 * Method:    getWinds
 * Signature: (IDDDDD)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_HWM14Integration_getWinds
  (JNIEnv *env, jobject obj, jint iyd, jdouble sec, jdouble alt, 
   jdouble glat, jdouble glon, jdouble ap) {
   
    // Convert Java parameters to Fortran types
    int f_iyd = (int)iyd;
    float f_sec = (float)sec;
    float f_alt = (float)alt;
    float f_glat = (float)glat;
    float f_glon = (float)glon;
    float f_stl = 0.0f;      // Not used in HWM14
    float f_f107a = 150.0f;  // Not used in HWM14
    float f_f107 = 150.0f;   // Not used in HWM14
    float f_ap[2] = {0.0f, (float)ap};  // ap array
    float f_w[2] = {0.0f, 0.0f};        // Output winds
    
    // Call the Fortran HWM14 subroutine
    __hwm14_module_MOD_hwm14(&f_iyd, &f_sec, &f_alt, &f_glat, &f_glon, 
                         &f_stl, &f_f107a, &f_f107, f_ap, f_w);
    
    // Create Java double array for return
    jdoubleArray result = (*env)->NewDoubleArray(env, 2);
    if (result == NULL) {
        return NULL; // Out of memory error
    }
    
    // Convert results to Java doubles
    jdouble winds[2];
    winds[0] = (jdouble)f_w[0];  // Meridional wind
    winds[1] = (jdouble)f_w[1];  // Zonal wind
    
    // Set the array elements
    (*env)->SetDoubleArrayRegion(env, result, 0, 2, winds);
    
    return result;
}