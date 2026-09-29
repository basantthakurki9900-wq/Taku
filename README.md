# Typing Rewards (Android)

Typing challenge + Daily reward + asli Unity Ads (Rewarded) se coins kamao.

## APK kaise banaye (GitHub par)
1. Is poore folder ko GitHub repo mein upload/push karo (branch `main`).
2. Repo ke **Actions** tab mein "Build APK" chalega (ya "Run workflow" dabao).
3. Run khatam hone par neeche **Artifacts → typing-rewards-debug-apk** download karo, zip kholo, `app-debug.apk` install karo.

## Unity Ads
- App: Basat | Game ID: `800360381` | Rewarded placement: `BP_Rewarded_Android`
- Debug APK mein **test ads** aate hain (`RewardedAdManager.TEST_MODE = BuildConfig.DEBUG`).
- Live ads sirf release build mein. Apne live ads par khud click mat karna.

## Baad mein: Firebase
`app/build.gradle.kts` mein firebase-bom + google-services plugin jodo aur `app/google-services.json` rakho.
