# API Keys Setup Guide

## ⚠️ Security Notice
This project uses API keys that must be kept secure. **Never commit API keys to version control!**

## Required API Keys

### 1. Google Maps API Key
**Purpose**: Map functionality and location-based features

**How to get it:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable "Maps SDK for Android" API
4. Go to "Credentials" and create an API key
5. Restrict the key to Android apps with your package name: `com.example.cstore`

### 2. OpenWeather API Key
**Purpose**: Weather-based product recommendations

**How to get it:**
1. Go to [OpenWeather](https://openweathermap.org/api)
2. Sign up for a free account
3. Navigate to "API keys" in your account
4. Copy your API key

## Setup Instructions

### Step 1: Copy the template
```bash
cp local.properties.example local.properties
```

### Step 2: Edit local.properties
Open `local.properties` and add your API keys:

```properties
sdk.dir=/path/to/your/Android/sdk

# API Keys
GOOGLE_MAPS_API_KEY=your_actual_google_maps_key_here
OPENWEATHER_API_KEY=your_actual_openweather_key_here
```

### Step 3: Sync and Build
Sync your Gradle files in Android Studio and rebuild the project.

## Verification

After setup, the app should:
- ✅ Display Google Maps on the Map screen
- ✅ Show weather information on the Home screen
- ✅ Build without errors

## Troubleshooting

**Problem**: Build fails with "API key is empty"
- **Solution**: Make sure you've added the keys to `local.properties` (not `local.properties.example`)

**Problem**: Map doesn't load
- **Solution**: Verify your Google Maps API key is enabled for "Maps SDK for Android" in Google Cloud Console

**Problem**: Weather doesn't load
- **Solution**: Check that your OpenWeather API key is active (new keys can take a few minutes to activate)

## Security Best Practices

1. ✅ **DO** keep API keys in `local.properties` (already in .gitignore)
2. ✅ **DO** use different keys for development and production
3. ✅ **DO** restrict API keys in their respective consoles
4. ❌ **DON'T** commit `local.properties` to version control
5. ❌ **DON'T** share API keys in screenshots or documentation
6. ❌ **DON'T** hardcode API keys in source files

## For Team Members

If you're a new team member:
1. Request access to the shared API key document (if available)
2. Or create your own free API keys following the instructions above
3. Never commit your `local.properties` file

## Questions?

Contact the project maintainers if you have issues with API key setup.

