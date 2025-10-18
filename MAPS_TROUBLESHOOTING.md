# Google Maps Troubleshooting - Blank Yellow Screen

## Issue: Map shows blank yellow/beige screen

This happens when the Google Maps API key is not properly configured.

## Solution Steps:

### Step 1: Check Google Cloud Console

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project
3. Navigate to **APIs & Services** → **Enabled APIs & Services**
4. Verify these APIs are **ENABLED**:
   - ✅ **Maps SDK for Android** (This is the critical one!)
   - ✅ Places API (if using place search)
   - ✅ Geolocation API (optional)

### Step 2: Check/Update API Key

1. Go to **APIs & Services** → **Credentials**
2. Find your API key: `AIzaSyCYbe7acMtdAqIzsdoziM3q2j8diwKp3ZI`
3. Click on it to edit

### Step 3: Update API Key Restrictions

**Option A: Remove all restrictions (for testing only):**
- Set **Application restrictions** to "None"
- Set **API restrictions** to "Don't restrict key"
- Click **Save**

**Option B: Proper restrictions (recommended for production):**
- **Application restrictions**: Select "Android apps"
  - Click "Add an item"
  - Package name: `com.example.cstore`
  - SHA-1 certificate fingerprint: Get this from your debug keystore (see below)
  
- **API restrictions**: Select "Restrict key"
  - Check: Maps SDK for Android
  - Check: Places API (if needed)

### Step 4: Get SHA-1 Certificate Fingerprint

Run this command in terminal:

**For Debug Build:**
```bash
cd /Users/hirunweththewa/Library/Android/sdk
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**For Release Build:**
```bash
keytool -list -v -keystore /path/to/your/release.keystore -alias your_alias_name
```

Copy the SHA-1 fingerprint and add it to your API key restrictions in Google Cloud Console.

### Step 5: Clean and Rebuild

After updating the API key settings:

```bash
cd /Users/hirunweththewa/Desktop/cstore2
./gradlew clean
./gradlew assembleDebug
```

Then reinstall the app on your device/emulator.

### Step 6: Check Logcat for Errors

In Android Studio, open **Logcat** and filter by "Maps" or "Google". Look for error messages like:
- `Authorization failure`
- `API key not authorized`
- `Maps SDK for Android has not been enabled`

## Quick Fix for Testing

If you need the map to work immediately for testing:

1. Go to Google Cloud Console → Credentials
2. Click on your API key
3. Under **API restrictions**, select "Don't restrict key"
4. Click **Save**
5. Wait 5 minutes for changes to propagate
6. Clean rebuild: `./gradlew clean assembleDebug`
7. Reinstall the app

⚠️ **Remember to add proper restrictions later for security!**

## Create a New API Key (Alternative)

If the old key doesn't work, create a fresh one:

1. Go to **APIs & Services** → **Credentials**
2. Click **+ CREATE CREDENTIALS** → **API Key**
3. Copy the new key
4. Update `local.properties`:
   ```
   GOOGLE_MAPS_API_KEY=your_new_key_here
   ```
5. Enable **Maps SDK for Android** API
6. Clean and rebuild

## Still Not Working?

Check these:
- ✅ Internet permission in AndroidManifest.xml
- ✅ Location permissions granted on device
- ✅ Google Play Services is installed on emulator/device
- ✅ Waited 5+ minutes after changing API key settings
- ✅ Completely uninstalled and reinstalled the app

## Debug Checklist

Run through this checklist:

- [ ] Maps SDK for Android is enabled in Google Cloud Console
- [ ] API key has no restrictions OR has correct package name + SHA-1
- [ ] Ran `./gradlew clean`
- [ ] Rebuilt and reinstalled the app
- [ ] Waited 5 minutes after API key changes
- [ ] Checked Logcat for specific error messages
- [ ] Internet connection is working
- [ ] Google Play Services is up to date

## Need More Help?

1. Check Logcat output for specific error messages
2. Verify the API key in the compiled APK's manifest
3. Test with a completely unrestricted API key temporarily
4. Make sure billing is enabled in Google Cloud (required for Maps API)

