

# <p align="center">
  <img src="app/src/main/res/drawable/sentinel_logo.png" width="100" alt="Sentinel Logo">
</p>

## Sentinel

Sentinel is a privacy-focused Android application designed to provide transparency and **granular control** into how other apps on your device use their permissions and communicate with the network.

## 🚀 Key Features 

- **Advanced Application Firewall**: Take full control of your device's connectivity. Block or allow internet access for any application with a single toggle.
- **VPN-Based Network Monitoring**: Uses a local VPN service to monitor DNS queries and network traffic in real-time.
- **Live Traffic Analytics**: View real-time packet statistics (Allowed vs. Blocked) for each application to understand background connectivity patterns.
- **Blocked Connection Logs**: Monitor detailed logs of blocked network attempts, including destination IPs and ports, to see exactly what was stopped.
- **Smart Privacy Nudges**: Receive intelligent notifications when apps contact sensitive domains (Analytics, Advertising, Tracking).
- **Predictive Recommendations**: Get smart firewall suggestions based on an app's **Privacy Risk Score (PRS)** and sensitive data access patterns.
- **Permission Inspector**: Provides a granular breakdown of granted vs. requested permissions for all user apps.
- **Risk Assessment**: Classifies apps based on their permission profile and real-world network activity.

## 🏗️ How It Works

Sentinel operates by intercepting traffic through a specialized local VPN service. When an app attempts to communicate over the network, Sentinel:
1. **Firewall Enforcement**: Checks the application's specific firewall rule to instantly drop or allow traffic.
2. **Domain Analysis**: Matches DNS queries against a curated list of sensitive patterns (Analytics, Advertising, Social Tracking, etc.).
3. **Contextual Risk Evaluation**: Cross-references network activity with the app's currently granted permissions using the `NudgeEngine`.
4. **Real-Time Feedback**: Generates "Nudges" and updates live statistics to keep you informed of potential privacy implications.

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
- **No Data Collection**: All monitoring, filtering, and analysis happen strictly on-device.
- **Local VPN Only**: The VPN service is 100% local; no traffic is ever routed to external servers.
- **Transparent Logic**: All firewall decisions and privacy logic are documented and visible in the code.

---
*Stay informed. Stay secure. Stay Sentinel.*
