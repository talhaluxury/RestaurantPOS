package com.talha.restaurantpos.data.remote

/**
 * Centralized Firestore path builder.
 *
 * Structure:
 *  restaurants/{restaurantId}
 *  restaurants/{restaurantId}/users/{userId}
 *  restaurants/{restaurantId}/categories/{categoryId}
 *  restaurants/{restaurantId}/products/{productId}
 *  restaurants/{restaurantId}/orders/{orderId}
 *  restaurants/{restaurantId}/tables/{tableId}
 *  restaurants/{restaurantId}/inventory/{itemId}
 *  restaurants/{restaurantId}/staff/{staffId}
 *  restaurants/{restaurantId}/settings/config
 *  userRestaurants/{userId} -> { restaurantId }   (fast lookup: which restaurant does this user belong to)
 */
object FirestorePaths {
    const val RESTAURANTS = "restaurants"
    const val USER_RESTAURANTS = "userRestaurants"

    fun restaurant(restaurantId: String) = "$RESTAURANTS/$restaurantId"
    fun categories(restaurantId: String) = "${restaurant(restaurantId)}/categories"
    fun products(restaurantId: String) = "${restaurant(restaurantId)}/products"
    fun orders(restaurantId: String) = "${restaurant(restaurantId)}/orders"
    fun tables(restaurantId: String) = "${restaurant(restaurantId)}/tables"
    fun inventory(restaurantId: String) = "${restaurant(restaurantId)}/inventory"
    fun staff(restaurantId: String) = "${restaurant(restaurantId)}/staff"
    fun settings(restaurantId: String) = "${restaurant(restaurantId)}/settings/config"
}

/**
 * Suggested Firestore Security Rules (deploy via firebase console / firestore.rules):
 *
 * rules_version = '2';
 * service cloud.firestore {
 *   match /databases/{database}/documents {
 *     function isSignedIn() { return request.auth != null; }
 *     function belongsToRestaurant(restaurantId) {
 *       return isSignedIn() &&
 *         get(/databases/$(database)/documents/userRestaurants/$(request.auth.uid)).data.restaurantId == restaurantId;
 *     }
 *     match /userRestaurants/{userId} {
 *       allow read: if isSignedIn() && request.auth.uid == userId;
 *       allow write: if isSignedIn() && request.auth.uid == userId;
 *     }
 *     match /restaurants/{restaurantId} {
 *       allow read, write: if belongsToRestaurant(restaurantId);
 *       match /{document=**} {
 *         allow read, write: if belongsToRestaurant(restaurantId);
 *       }
 *     }
 *   }
 * }
 */
