# Deploying the push-notification Cloud Function

This sends a push notification to the RestaurantPOS app whenever a new order
is created — including orders placed on the website — even if the app has
been completely closed.

## One-time setup

1. Your Firebase project must be on the **Blaze (pay-as-you-go) plan** —
   Cloud Functions require it. Go to
   https://console.firebase.google.com → your project → gear icon → Usage
   and billing → Upgrade. There's a generous free tier; a small restaurant's
   order volume will not incur real charges.

2. Install the Firebase CLI (one time, on your own computer):
   ```
   npm install -g firebase-tools
   firebase login
   ```

3. From the root of this project folder (the one with `firebase.json`
   and the `functions/` folder):
   ```
   firebase use --add
   ```
   and pick your existing Firebase project (the same one the app and
   website use).

## Deploy

```
cd functions
npm install
cd ..
firebase deploy --only functions
```

That's it. From then on, every new order (from the app or the website)
triggers `sendNewOrderPush`, which notifies every staff device subscribed
to `restaurant_<your-restaurant-id>_orders` — which happens automatically
the next time each device opens the (rebuilt) app.

## Re-deploying after edits

If you ever change `functions/index.js`, just run
`firebase deploy --only functions` again from the project root.
