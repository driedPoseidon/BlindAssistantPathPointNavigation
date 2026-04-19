#define TRIG_L      12
#define ECHO_L      14
#define TRIG_R      40
#define ECHO_R      39
#define SPEAKER_L   17
#define SPEAKER_R   21

#define OBSTACLE_FAR   80.0
#define OBSTACLE_CLOSE 30.0
#define COOLDOWN_MS    3000

enum State { CLEAR, FAR, CLOSE };

State lastStateL = CLEAR;
State lastStateR = CLEAR;
unsigned long lastAlertL = 0;
unsigned long lastAlertR = 0;

float readDistance(int trig, int echo) {
  digitalWrite(trig, LOW);
  delayMicroseconds(2);
  digitalWrite(trig, HIGH);
  delayMicroseconds(10);
  digitalWrite(trig, LOW);
  long dur = pulseIn(echo, HIGH, 30000);
  if (dur == 0) return 999.0;
  return (dur * 0.034) / 2.0;
}

void beep(int pin, int freq, int dur_ms, int volume) {
  ledcAttach(pin, freq, 8);
  ledcWrite(pin, volume);
  delay(dur_ms);
  ledcWrite(pin, 0);
  ledcDetach(pin);
}

State getState(float dist) {
  if (dist < OBSTACLE_CLOSE) return CLOSE;
  if (dist < OBSTACLE_FAR)   return FAR;
  return CLEAR;
}

void handleSensor(float dist, int pin,
                  State &lastState,
                  unsigned long &lastAlert,
                  String side) {

  unsigned long now = millis();
  State newState    = getState(dist);
  bool changed      = (newState != lastState);
  bool cooldown     = (now - lastAlert > COOLDOWN_MS);

  if (newState == CLOSE) {
    lastState  = newState;
    lastAlert  = now;
    int pause  = map((int)dist, 0, 30, 80, 300);
    Serial.printf("%s CLOSE %.1fcm\n", side.c_str(), dist);
    beep(pin, 2500, 100, 40);
    delay(pause);
    return;
  }

  if (newState == FAR) {
    if (!changed && !cooldown) return;
    lastState = newState;
    lastAlert = now;
    Serial.printf("%s FAR %.1fcm\n", side.c_str(), dist);
    beep(pin, 1500, 150, 30);
    return;
  }

  if (newState == CLEAR && changed) {
    lastState = CLEAR;
    Serial.printf("%s clear\n", side.c_str());
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(TRIG_L, OUTPUT); pinMode(ECHO_L, INPUT);
  pinMode(TRIG_R, OUTPUT); pinMode(ECHO_R, INPUT);
  pinMode(SPEAKER_L, OUTPUT);
  pinMode(SPEAKER_R, OUTPUT);
  Serial.println("Ready");
}

void loop() {
  float distL = readDistance(TRIG_L, ECHO_L);
  float distR = readDistance(TRIG_R, ECHO_R);

  Serial.printf("L: %.1fcm  R: %.1fcm\n", distL, distR);

  handleSensor(distL, SPEAKER_L,
               lastStateL, lastAlertL, "LEFT");

  handleSensor(distR, SPEAKER_R,
               lastStateR, lastAlertR, "RIGHT");

  delay(50);
}
