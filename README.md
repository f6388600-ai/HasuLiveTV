# Hasu Live TV — Clean Shared Catalog Build

## Editions
- `mobile`: user-facing mobile app
- `tv`: Android TV app
- `admin`: catalog management app (no login)

## Shared channel catalog
All three editions connect to the same Firebase Cloud Firestore `channels` collection.

- Admin add/edit/delete -> Firestore
- Mobile/TV listen in real time -> changes appear without reinstalling the app
- Local SharedPreferences cache keeps the last catalog available offline
- Channel supports name, logo URL, banner URL, stream URL, category, country, language, channel number, description, enabled, featured, popular and sort order.

## Firebase setup
1. Use the existing flavor-specific `google-services.json` files.
2. In Firebase Console, enable **Cloud Firestore**.
3. Publish the included `firestore.rules` if you want this auth-free catalog to work immediately.

### Security note
This build intentionally has no user/admin authentication. Therefore the included Firestore rule permits read/write access to the catalog. This is suitable for a controlled prototype/test deployment, but it does not securely enforce "only Admin can write". For a public production release, add an authenticated backend or another trusted write API.
