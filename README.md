# VisionGuard: Smart Obstacle & Puddle Detection for the Visually Impaired

VisionGuard is a real-time assistive technology system designed to provide spatial awareness for visually impaired individuals. It combines computer vision for ground-level hazards (puddles) and ultrasonic sensing for side-level hazards (walls), delivering haptic and auditory feedback through wearable bracelets.

## System Architecture

The project utilizes a **Distributed Computing Model** to minimize latency and maximize detection accuracy:

1.  **Remote Control (Samsung S25):** Acts as the system dashboard and trigger via a custom Jetpack Compose Android app.
2.  **Vision Hub (Laptop):** Receives the high-definition feed from a **Logitech Brio 105**. It runs **OpenCV-based image processing** to detect puddles and slippery surfaces.
3.  **Haptic Core (ESP32):** A microcontroller connected via USB to the laptop. It manages:
    * **Side-channel sensing:** HC-SR04 Ultrasound sensors for wall detection.
    * **Feedback Output:** Micro-speakers/Piezo buzzers on wearable bracelets.

## Hardware Stack

* **Smartphone:** Samsung S25 (System Trigger)
* **Camera:** Logitech Brio 105 (1080p, Wide-Angle)
* **Processing:** Laptop (Intel/Apple Silicon)
* **Microcontroller:** ESP32
* **Sensors:** HC-SR04 Ultrasonic Sensors
* **Output:** Piezo Speakers / Vibration Motors

## Software & Logic

### Android App (Samsung S25)
* Built with **Kotlin** and **Jetpack Compose**.
* Uses **CameraX** for frame analysis and stream management.
* Communicates with the Laptop Hub via **TCP Sockets**.

### Computer Vision (Python/OpenCV)
* Real-time frame processing.
* **Puddle Detection:** Uses HSV color masking and edge detection to identify liquid hazards on varied floor surfaces.
* **Serial Communication:** Sends triggers to the ESP32 over a high-speed serial bridge (115200 baud).

### Firmware (C++/Arduino)
* Asynchronous monitoring of Serial commands and Ultrasound triggers.
* **Distinct Alert Patterns:** * *Puddle:* Intermittent rapid beeping.
    * *Wall:* Solid tone/vibration based on proximity.

## 🔧 Installation & Setup

1.  **Clone the Repo:**
    ```bash
    git clone https://github.com/yourusername/visionguard.git
    ```
2.  **Android App:**
    * Open the `/android` folder in Android Studio.
    * Update `LAPTOP_IP` in `MainActivity.kt`.
    * Deploy to Samsung S25 (Ensure Developer Mode & USB Debugging are ON).
3.  **Python Hub:**
    ```bash
    pip install opencv-python pyserial numpy
    python main_hub.py
    ```
4.  **ESP32:**
    * Flash the `.ino` file from the `/firmware` folder using Arduino IDE.

## Hackathon Pitch
VisionGuard solves the "latency gap" in assistive tech. By offloading heavy AI processing to a local laptop hub while keeping the interface on a flagship smartphone, we provide sub-100ms response times—ensuring the user is alerted to hazards before they become accidents.
