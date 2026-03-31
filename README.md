# 🛡️ Sentinel

Sentinel is a privacy-focused Android application designed to provide transparency into how other apps on your device use their permissions and communicate with the network.

## 🚀 Key Features

- **VPN-Based Network Monitoring**: Uses a local VPN service to monitor DNS queries in real-time.
- **Privacy Nudges**: Sends intelligent notifications when apps contact potentially sensitive or tracking-related domains.
- **Permission Inspector**: Provides a detailed breakdown of granted vs. requested permissions for all user apps.
- **Risk Assessment**: Classifies apps based on their permission profile and network activity.
- **Usage Tracking**: Monitors app foreground time to provide context for privacy alerts.
- **Optimized Performance**: Enhanced search logic with debouncing for large app lists.

## 🛠️ How It Works

Sentinel operates by intercepting DNS traffic from monitored applications. When an app attempts to resolve a domain, Sentinel:
1. Matches the domain against a curated list of sensitive patterns (Analytics, Advertising, Social Tracking, etc.).
2. Cross-references the domain access with the app's currently granted permissions (e.g., Location, Contacts).
3. Evaluates the risk level using an internal engine (`NudgeEngine`).
4. Generates a "Nudge" — a user-friendly notification that explains the potential privacy implication.

## 🏗️ Build & Run Instructions

### Prerequisites
- Android Studio Ladybug or newer.
- Android SDK 36 (Android 15+) support.
- A physical device or emulator running Android 8.0 (API 26) or higher.

### Steps
1. **Clone the project** into your Android Studio workspace.
2. **Sync Gradle**: Allow Android Studio to download dependencies and sync the project.
3. **Run**: Click the "Run" button in Android Studio to deploy to your device.
4. **Permissions**: On first launch, the app will request notification permissions. You will also need to grant the "Usage Stats" permission when prompted to enable usage tracking.
5. **Start Monitoring**: Navigate to the "User Apps" tab, select an app, and start the Sentinel service.

## 🔒 Privacy Commitment

Sentinel is built with privacy at its core:
- **No Data Collection**: All monitoring and analysis happen strictly on-device.
- **Local VPN**: The VPN service is purely local; no traffic is routed to external servers.
- **Transparent Logic**: All privacy decisions are documented and visible in the code.

---
*Stay informed. Stay secure. Stay Sentinel.*
