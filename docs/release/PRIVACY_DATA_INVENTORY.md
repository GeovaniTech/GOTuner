# Privacy and data inventory

This describes the inspected code and release manifest, not a completed legal privacy policy. Developer approval and console review are required before submission.

| Data / capability | Processing, storage and destination | Release disclosure work |
|---|---|---|
| Microphone PCM | Mono AudioRecord buffers; local YIN processing in RAM; no recording files, audio logs or upload path | Explain foreground tuning purpose; permission grant required. Audio remaining entirely on-device is not off-device collection |
| Frequencies/notes/cents | Local shared calculation and transient UI state; no custom Analytics events for these values | Do not claim these are sent to Firebase |
| Permission-request flag | One local SharedPreferences boolean for Settings recovery | No cloud/device-transfer backup; no user identity attached |
| Analytics | Standard SDK app/session/device and interaction measurements, app-instance identifiers, and possible coarse location derived by Google's service | Review actual Firebase/Analytics settings and classify app interactions, identifiers and applicable approximate location accurately |
| Crashlytics | Crash/ANR diagnostics, stack traces, app/device state and installation UUID; Firebase Installations/Sessions dependencies also apply | Declare crash logs/diagnostics and applicable identifiers; purpose: stability/analytics |
| Advertising identifiers | Advertising/ad-services permissions removed; Analytics ad-ID collection and personalization disabled | No advertising feature. Verify merged manifest and console links/settings before final declaration |
| SDK local cache | Firebase SDKs may persist event/crash reports and identifiers for delivery | Include retention/deletion/contact practices based on the actual account settings; these were not inspected |

Debug manifest disables Analytics and Crashlytics collection. Release enables them. There is no in-app consent selector or user account. Automatic Firebase SDK initialization is retained. No custom user ID, custom audio metadata, geolocation permission, contacts, camera, purchases or ad SDK was found. Handled microphone errors are shown locally rather than sent as custom nonfatal reports.

Firebase traffic is HTTPS; Android cleartext traffic is disabled. The app has no backend/audio upload service. SDK transport/storage must be included in disclosures even though the tuner itself is local.

## Developer decisions still required

- Publish a public HTTPS privacy policy identifying GOTuner/developer, data categories, purposes, Firebase service-provider relationship, retention/deletion practices and contact method. Supply `GOTUNER_PRIVACY_POLICY_URL` when building and verify the in-app link.
- Review consent/collection requirements for intended countries and target age groups. Do not launch automatic release telemetry in a market requiring an unimplemented consent flow. Either configure collection appropriately or implement the required consent before releasing there.
- Complete Data Safety using actual Firebase product/account settings. Do not select “no data collected” for the current release configuration. Collection versus sharing exemptions depend on your processing relationships and Google Play definitions.
- No account exists, so account-deletion UI is not applicable; this does not eliminate other privacy/data-rights obligations.
- Firebase console retention, Google Analytics linkage, deletion procedures and actual report delivery were not verified.

Sources checked 2026-10-31: [Firebase Android disclosure guidance](https://firebase.google.com/docs/android/play-data-disclosure), [Analytics disclosure guidance](https://support.google.com/analytics/answer/11582702), [Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311). SDK documentation is guidance; the developer remains responsible for the form and policy.
