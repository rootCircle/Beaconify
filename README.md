# Beaconify

A **Bluetooth Low Energy (BLE)** beacon management and location tracking application built using **Kotlin** and **Jetpack Compose**. The app allows users to register and transmit virtual beacons, track their location using nearby beacons, and visualize their position on an interactive map. The app uses a **two-step approach** that leverages **Affinity Propagation Clustering (APC)** for accurate location prediction.

## Features

- **Beacon Registration & Transmission**:
  - Register and transmit BLE beacons using UUID, Major, and Minor values.
  
- **Two-Step Location Prediction**:
  - **Offline Clustering**: Collect and cluster beacon data to establish baseline reference points using APC.
  - **Online Location Refinement**: Dynamically adjust user location predictions using real-time beacon data.
  - **Exponential Averaging**: Smooth location predictions over time for more consistent accuracy.

- **Interactive Map**:
  - View real-time user location and surrounding beacon positions on a dynamic map.

## Technical Workflow

### Location Prediction Process

1. **Offline Clustering**:
   - Static beacons provide reference points for the initial clustering phase.
   - Beacon signals are processed and grouped using **Affinity Propagation Clustering (APC)** to create initial location predictions.

2. **Online Location Refinement**:
   - The app continuously scans for nearby beacons and refines the user's location based on signal strength and proximity.
   - **Exponential Averaging** is applied to smooth out location predictions over time, helping avoid sudden jumps or instability in the predicted position.



## Help Setting Up (**Latitude and Longitude Calculation**):

To convert beacon proximity data into geographic coordinates, we use a **Modified Haversine Algorithm**. Here's an example Python implementation to demonstrate the concept:

```python
import math

def meters_to_latlon(x, y, lat_ref, lon_ref):
    # Earth's radius in meters
    R = 6371000
    
    # Convert latitude
    lat = lat_ref + (y / R) * (180 / math.pi)
    
    # Convert longitude
    lon = lon_ref + (x / (R * math.cos(math.radians(lat_ref)))) * (180 / math.pi)
    
    return lat, lon

# Example usage:
lat_ref = 0  # Example reference latitude
lon_ref = 0  # Example reference longitude
x = 5.7  # 5.7 meters East
y = 9.5  # 9.5 meters North

lat, lon = meters_to_latlon(x, y, lat_ref, lon_ref)
print(f"Latitude: {lat: .6f}")
print(f"Longitude: {lon: .6f}")
```

This function converts x and y (in meters) into latitude and longitude based on a reference location.



## Architecture and Technology Stack

- **Programming Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **BLE Processing**: ALT Beacon format
- **Location Prediction**: Affinity Propagation Clustering (APC)
- **Backend Store**: [Beaconify Backend](https://github.com/rootCircle/BeaconifyStore)



## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/rootCircle/Beaconify.git
   cd Beaconify
   ```

2. Open the project in **Android Studio**.

3. Sync the project with Gradle dependencies.

4. Ensure your device runs **Android 8.0 (API Level 26)** or higher, with BLE support.

5. **Run the app** on a physical device.

### Important Note for Backend Setup

To fully utilize the app, you need to set up the **BeaconifyStore** backend. The app relies on this backend to store beacon data and perform location tracking functions. 

#### Backend Setup

If you intend to run the app offline, make sure to have the backend set up and running. You can find the instructions for setting up **BeaconifyStore** in the **[BeaconifyStore README](https://github.com/rootCircle/BeaconifyStore)**. 

Please follow the instructions in that README to get the backend ready for use with the mobile app.

## Screenshots

| Register Virtual Beacon | Beacon Transmit | Locate Yourself | Interactive Map |  
|-------------------------|-----------------|-----------------|-----------------|  
| ![Register Beacon](https://github.com/user-attachments/assets/dcd8fa68-5776-40ce-9a77-3a58d1feed4c) | ![Register Beacon Notification](https://github.com/user-attachments/assets/2d4ed27a-8006-41d6-af92-15d74e2cb9bc) | ![Empty Locate Me](https://github.com/user-attachments/assets/cbe1eead-7920-4c88-a60f-463d4ccb5668) | ![LocateMe Map](https://github.com/user-attachments/assets/49e678e6-cc69-4010-a4cc-d1cb3daa0fd8) |  

| Location Prediction | Permission Prompts | Homepage |  
|---------------------|-------------|----------|  
| ![Active LocateMe](https://github.com/user-attachments/assets/1a441676-3fef-4a32-809b-4bbd65b4de20) | ![Prompt Permissions](https://github.com/user-attachments/assets/2a2a1a1a-23c5-441d-bf5c-d5756956ad90) | ![Homepage](https://github.com/user-attachments/assets/558111ef-5124-4be3-9861-f181755d83f5) |

## Usage

1. **Register a Beacon**:
   - Navigate to the "Register Beacon" screen.
   - Enter the beacon’s UUID, Major, and Minor values.
   - Save the beacon data to the backend.
   - Ensure you have at least **3 registered beacons** (or physical devices) in the network for accurate location prediction.

2. **Locate Yourself**:
   - Enable Bluetooth and location permissions on your device.
   - Open the "Locate Me" screen to view your location on the map.
   - See real-time updates of your latitude and longitude as well as nearby beacons and their locations.



## Research Insights

This app uses a novel approach for beacon-based location prediction:
- **Affinity Propagation Clustering (APC)** for beacon signal grouping and location prediction.
- **Two-Step Location Prediction** to refine user location with both offline and online adjustments.
- **Exponential Averaging** for smoother, more stable location predictions over time.

For a more in-depth explanation, refer to the [IEEE Paper](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=8667640).



## License

This project is licensed under the [Apache-2.0 License](LICENSE).



## Contributions

Contributions are welcome! Feel free to open issues or submit pull requests.



## Acknowledgements

- [AltBeacon Library](https://altbeacon.github.io/android-beacon-library/)
- [Beaconify Backend](https://github.com/rootCircle/Beaconify)
- Research inspired by [IEEE Paper](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=8667640).



### Contact

For questions or feedback, please reach out to <dev.frolics@gmail.com>.

