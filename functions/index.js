const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Fires whenever a new order document is created under any restaurant
 * (restaurants/{restaurantId}/orders/{orderId}) — including orders placed
 * from the public ordering website, which don't go through the app at all.
 *
 * Sends a push notification to the "restaurant_<restaurantId>_orders" topic.
 * Every staff device that has ever logged into that restaurant is subscribed
 * to this topic (see SessionManager.kt in the Android app), so this reaches
 * them even if the app has been fully closed — unlike the app's own
 * Firestore listener, which only fires while the app process is alive.
 */
exports.sendNewOrderPush = onDocumentCreated(
  "restaurants/{restaurantId}/orders/{orderId}",
  async (event) => {
    const order = event.data?.data();
    if (!order) return;

    // Only notify for genuinely new orders — avoids re-notifying on later
    // status updates, since this trigger only fires on document *creation*
    // anyway, but this guard also protects against any future write pattern.
    if (order.status !== "NEW") return;

    const { restaurantId } = event.params;
    const orderNumber = order.orderNumber || "";
    const customerName = order.customerName || "";

    const title = orderNumber ? `New order ${orderNumber}` : "New order received";
    const body = customerName ? `From ${customerName} — tap to view` : "Tap to view details";

    const message = {
      topic: `restaurant_${restaurantId}_orders`,
      notification: { title, body },
      data: {
        orderId: event.params.orderId,
        title,
        body,
      },
      android: {
        priority: "high",
        notification: { channelId: "new_orders", sound: "default" },
      },
    };

    try {
      await admin.messaging().send(message);
    } catch (err) {
      console.error("Failed to send new-order push:", err);
    }
  }
);
