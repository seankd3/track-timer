# TrackTimer

An Android application that allows users to select a start and end point on a road and track their travel time using GPS.

## Features

- Select start and end points on a map using OpenStreetMap
- Real-time GPS tracking of travel time, distance, and speed
- Background tracking using a foreground service (continues even when app is minimized)
- History of previous routes with detailed statistics
- Clean, modern Material Design interface with dark mode support
- Works offline with cached map tiles
- No API key required - completely free mapping solution

## Technical Details

TrackTimer is built with:
- Kotlin
- MVVM Architecture
- OSMDroid for map visualization (OpenStreetMap)
- FusedLocationProvider for accurate GPS tracking
- Room Database for persistent storage of route records
- LiveData and ViewModel for reactive UI updates
- Foreground service with notifications for background tracking
- Offline map functionality with tile caching

## Setup Requirements

1. **Internet Connection for First Use**: 
   - The app needs internet connection for the initial download of map tiles
   - Once downloaded, tiles are cached and can be used offline

2. **Location Permissions**:
   - The app requires precise location permissions to function correctly
   - These are requested at runtime following Android best practices
   
3. **Storage Permission**:
   - Used for caching map tiles for offline use
   - This helps reduce data usage and enables offline functionality

## Usage Instructions

1. Open the app and wait for the map to load
2. Tap "Select Start Point" and then tap the desired location on the map
3. Tap "Select End Point" and then tap the destination location on the map
4. Press "Start Tracking" to begin tracking your journey
5. The app will track your location and update time, distance, and speed statistics in real-time
6. When you finish, tap "Stop Tracking" to end the session
7. You'll be asked if you want to save the record
8. View your history by tapping the "History" button

## Battery Optimization

For the most accurate tracking, consider:
- Disabling battery optimization for this app
- Ensuring location services are set to high accuracy mode
- Keeping your device charged during long tracking sessions

## Offline Usage

TrackTimer works seamlessly in offline mode:
- Map tiles are cached locally for offline use
- The app will automatically detect when you're offline and adjust accordingly
- You can still track your location and record routes without internet
- Some map detail may be limited when offline depending on previously cached data

## Privacy

TrackTimer respects your privacy:
- All data is stored locally on your device
- No location data is transmitted to external servers
- Map data is downloaded directly from OpenStreetMap servers without tracking
- You can delete your route history and map cache at any time

## Open Source Components

TrackTimer uses the following open source components:
- [OSMDroid](https://github.com/osmdroid/osmdroid) - An OpenStreetMap tools library for Android
- [OpenStreetMap](https://www.openstreetmap.org) - A collaborative project to create a free editable map of the world

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request or open an issue for any bugs or feature requests.
