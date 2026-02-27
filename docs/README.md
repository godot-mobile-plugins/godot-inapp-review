<p align="center">
	<img width="128" height="128" src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/demo/assets/inappreview-android.png">
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	<img width="128" height="128" src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/demo/assets/inappreview-ios.png">
</p>

---

<div align="center">
	<a href="https://github.com/godot-mobile-plugins/godot-inapp-review">
	<img src="https://img.shields.io/github/stars/godot-mobile-plugins/godot-inapp-review?style=social" />
	</a>
	<img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-inapp-review/total" />
	<img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-inapp-review/latest/total" />
	<img src="https://img.shields.io/github/v/release/godot-mobile-plugins/godot-inapp-review" />
</div>

---

# <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="24"> Godot In-app Review Plugin

The **Godot In-app Review Plugin** lets you trigger native in‑app review prompts on **Android (Google Play Store)** and **iOS (Apple App Store)** using a single, unified **GDScript API**. This allows players to leave ratings and reviews without ever leaving your game, following each platform’s official guidelines.

**Features**

- Native in‑app review flow for **Google Play** (Android)
- Native in‑app review flow for **Apple App Store** (iOS)
- Unified, platform‑agnostic GDScript interface

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="20"> Table of Contents

- [Demo](#demo)
- [Installation](#installation)
- [Usage](#usage)
- [Demo](#demo)
- [Signals](#signals)
- [Methods](#methods)
- [Platform-Specific Notes](#platform-specific-notes)
- [Links](#links)
- [All Plugins](#all-plugins)
- [Credits](#credits)
- [Contributing](#contributing)

---

<a name="demo"></a>

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="20"> Demo

Try the **demo app** located in the `demo` directory.

<p align="center">
	<img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/docs/assets/demo_screenshot_android_521.png" width="243">
	<img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/docs/assets/demo_screenshot_ios_521.png" width="252">
</p>

---

<a name="installation"></a>

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="20"> Installation

> **Important:** Uninstall any previous versions of the plugin before installing a new one.
> If you are targeting **both Android and iOS**, make sure the **addon interface version matches** for both platforms.

### Installation Options

#### 1. AssetLib (Recommended)

- Open the Godot **AssetLib** and search for `In-app Review`
- Click **Download** → **Install**
- Install to the **project root** with **Ignore asset root** enabled
- Enable the plugin via **Project → Project Settings → Plugins**
- For **iOS**, also enable the plugin in the **export settings**
- If installing both Android and iOS versions, you may safely ignore file conflict warnings for shared GDScript interface files

#### 2. Manual Installation

- Download the latest release from GitHub
- Extract the archive into your project root
- Enable the plugin via **Project → Project Settings → Plugins**
- For **iOS**, also enable the plugin in the **export settings**

---

<a name="usage"></a>

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="20"> Usage

1. Add an **`InappReview`** node to your scene.
2. Connect to the relevant [signals](#signals) emitted by the `InappReview` node.
3. Call `generate_review_info()` to prepare the review flow.
4. When the `review_info_generated` signal is emitted, call `launch_review_flow()`.

   * Depending on the platform, either the **Google Play** or **App Store** review dialog will be shown.
   * The dialog **may not appear** if the review flow was triggered recently (this is controlled by the platform, not the plugin).
5. Resume normal app behavior once the `review_flow_launched` signal is emitted.

---

<a name="demo"></a>

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="20"> Demo

The demo project exists solely to demonstrate usage and provide sample code. Because the demo app is **not registered** on the Google Play Store or Apple App Store, the actual in‑app review dialog will **not** be displayed when running the demo.

---

<a name="signals"></a>

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="20"> Signals

- `review_info_generated` — Emitted when review information is successfully generated.
- `review_info_generation_failed` — Emitted when review information generation fails.
- `review_flow_launched` — Emitted when the review flow is successfully launched.
- `review_flow_launch_failed` — Emitted when the review flow fails to launch.
- `app_review_url_ready(url: String)` — Emitted when a store review URL has been successfully generated.
- `get_app_review_url_failed` — Emitted when generating the store review URL fails.

---

<a name="methods"></a>

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="20"> Methods

- `generate_review_info()` — Prepares and fetches the data required to start the in‑app review flow.
- `launch_review_flow()` — Launches the native review dialog using the previously generated review info.
- `get_app_review_url()` — Asynchronously generates a platform‑specific store review URL.

---

<a name="platform-specific-notes"></a>

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="20"> Platform-Specific Notes

### Android
- **Build:** [Create custom Android gradle build](https://docs.godotengine.org/en/stable/tutorials/export/android_gradle_build.html).
- **Registration:** App must be registered with the Google Play Store.
- **Troubleshooting:**
  - Logs: `adb logcat | grep 'godot'` (Linux), `adb.exe logcat | select-string "godot"` (Windows)
  - _No review dialog shown_: Check [Google Play quotas](https://developer.android.com/guide/playcore/in-app-review#quotas)

### iOS
- **Registration:** App must be registered with the App Store.
- **Troubleshooting:**
	- View XCode logs while running the game for troubleshooting.
	- See [Godot iOS Export Troubleshooting](https://docs.godotengine.org/en/stable/tutorials/export/exporting_for_ios.html#troubleshooting).
	- **Export settings:** Plugin must be enabled also in the export settings.

---

<a name="links"></a>

# <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="20"> Links

- [AssetLib Entry Android](https://godotengine.org/asset-library/asset/2549)
- [AssetLib Entry iOS](https://godotengine.org/asset-library/asset/2906)

---

<a name="all-plugins"></a>

# <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="24"> All Plugins

|  | Plugin | Android | iOS | Free | Open Source | License |
| :---  | :--- | :---: | :---: | :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/godot-sdk-integrations/godot-admob/main/addon/src/icon.png" width="20"> | [Admob](https://github.com/godot-sdk-integrations/godot-admob) | ✅ | ✅ | ✅ | ✅ | MIT |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-notification-scheduler/main/addon/src/icon.png" width="20"> | [Notification Scheduler](https://github.com/godot-mobile-plugins/godot-notification-scheduler) | ✅ | ✅ | ✅ | ✅ | MIT |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-deeplink/main/addon/src/icon.png" width="20"> | [Deeplink](https://github.com/godot-mobile-plugins/godot-deeplink) | ✅ | ✅ | ✅ | ✅ | MIT |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-share/main/addon/src/icon.png" width="20"> | [Share](https://github.com/godot-mobile-plugins/godot-share) | ✅ | ✅ | ✅ | ✅ | MIT |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="20"> | [In-App Review](https://github.com/godot-mobile-plugins/godot-inapp-review) | ✅ | ✅ | ✅ | ✅ | MIT |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-native-camera/main/addon/src/icon.png" width="20"> | [Native Camera](https://github.com/godot-mobile-plugins/godot-native-camera) | ✅ | ✅ | ✅ | ✅ | MIT |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-connection-state/main/addon/src/icon.png" width="20"> | [Connection State](https://github.com/godot-mobile-plugins/godot-connection-state) | ✅ | ✅ | ✅ | ✅ | MIT |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-oauth2/main/addon/src/icon.png" width="20"> | [OAuth 2.0](https://github.com/godot-mobile-plugins/godot-oauth2) | ✅ | ✅ | ✅ | ✅ | MIT |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-qr/main/addon/src/icon.png" width="20"> | [QR](https://github.com/godot-mobile-plugins/godot-qr) | ✅ | ✅ | ✅ | ✅ | MIT |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-firebase/main/addon/src/icon.png" width="20"> | [Firebase](https://github.com/godot-mobile-plugins/godot-firebase) | ✅ | ✅ | ✅ | ✅ | MIT |

---

<a name="video-tutorials"></a>

# <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="24"> Video Tutorials

## **Google Play Reviews with the In-app Review Plugin** -- _by [Code Artist](https://www.youtube.com/@codeartist1687)_
[![In-app Review Plugin on Android](https://img.youtube.com/vi/3iSd7_rE1m8/0.jpg)](https://www.youtube.com/watch?v=3iSd7_rE1m8)

---

<a name="credits"></a>

# <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="24"> Credits

Developed by [Cengiz](https://github.com/cengiz-pz)

Based on [Godot Mobile Plugin Template](https://github.com/godot-mobile-plugins/godot-plugin-template)

Original repository: [Godot In-app Review Plugin](https://github.com/godot-mobile-plugins/godot-inapp-review)

---

<a name="contributing"></a>

# <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="24"> Contributing

See [our guide](https://github.com/godot-mobile-plugins/godot-inapp-review?tab=contributing-ov-file) if you would like to contribute to this project.

---

# 💖 Support the Project

If this plugin has helped you, consider supporting its development! Every bit of support helps keep the plugin updated and bug-free.

| | Ways to Help | How to do it |
| :--- | :--- | :--- |
|✨⭐| **Spread the Word** | [Star this repo](https://github.com/godot-mobile-plugins/godot-inapp-review/stargazers) to help others find it. |
|💡✨| **Give Feedback** | [Open an issue](https://github.com/godot-mobile-plugins/godot-inapp-review/issues) or [suggest a feature](https://github.com/godot-mobile-plugins/godot-inapp-review/issues/new). |
|🧩| **Contribute** | [Submit a PR](https://github.com/godot-mobile-plugins/godot-inapp-review?tab=contributing-ov-file) to help improve the codebase. |
|❤️| **Buy a Coffee** | Support the maintainers on GitHub Sponsors or other platforms. |

## ⭐ Star History
[![Star History Chart](https://api.star-history.com/svg?repos=godot-mobile-plugins/godot-inapp-review&type=Date)](https://star-history.com/#godot-mobile-plugins/godot-inapp-review&Date)
