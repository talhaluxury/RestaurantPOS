package com.talha.restaurantpos.data.remote

/**
 * Fill these two values in from your Cloudinary dashboard (cloudinary.com -> Console):
 *  - CLOUD_NAME: shown right on the dashboard home page.
 *  - UPLOAD_PRESET: Settings -> Upload -> Upload presets -> "Add upload preset".
 *    Set its "Signing Mode" to UNSIGNED. This lets the app upload directly to Cloudinary
 *    without ever shipping your API secret inside the APK.
 */
object CloudinaryConfig {
    const val CLOUD_NAME = "pwyofud8"
    const val UPLOAD_PRESET = "restaurantpos_unsigned"
}
