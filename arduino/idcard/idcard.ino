#include <errno.h>
#include <EEPROM.h>
#include <Wire.h>
#include <MFRC522.h> //https://playground.arduino.cc/Learning/MFRC522
#include "blake2s.h"
#include "reader_config.h" // Use flags go in here
#include "DESFireCard.h"

// Permission modes
#define PERMISSION_MODE_STANDARD 1
#define PERMISSION_MODE_NOPISCINE 2
#define PERMISSION_MODE_STAFF 3
#define PERMISSION_MODE_MOVIEROOM 4

// MFRC522 setup
#define PIN_SS(i) (49 - (i * 2))
#define PIN_RST(i) (48 - (i * 2))

#define STATE_IDLE 0
#define STATE_SELECT 1
#define STATE_READ 2
#define STATE_LOCK 3
#define STATE_WAIT 4
#define STATE_UNLOCK 5

#define APP_ID_CARD42 0xFB9852

#define NUM_READERS 1

MFRC522           *g_mfrc522[NUM_READERS] = { NULL };
int               g_states[NUM_READERS] = { STATE_IDLE };

#define UNLOCK_PERIOD 5000

unsigned long     g_unlock_period[NUM_READERS] = { 0 };

// EEPROM contents
struct    s_config {
  byte door_id;
  byte permission_mode;

  byte blake2s_mac_key[BLAKE2S_KEY_SIZE];
};

s_config          g_config;
//volatile byte     g_got_isr; // bit 0 for reader A, bit 1 for reader B

// blake2s setup
#define HASH_DEBUG false

struct s_blake2s_state g_hasher;

// Serial output
#define SERIAL_OUTPUT true

#if SERIAL_OUTPUT
# define SERIAL_PRINT(...) Serial.print(__VA_ARGS__);
# define SERIAL_PRINTLN(...) Serial.println(__VA_ARGS__);
#else
# define SERIAL_PRINT(...)
# define SERIAL_PRINTLN(...)
#endif

void setup() {
  //Wire.begin();

  // Reader A pin config
  //pinMode(PIN_RESET_A, OUTPUT);
  //pinMode(PIN_IRQ_A, INPUT);
  //attachInterrupt(digitalPinToInterrupt(PIN_IRQ_A), &nfc_isr, RISING);

  //Wire.setClock(I2C_FREQUENCY);

#if SERIAL_OUTPUT
  Serial.begin(9600);
  while (!Serial) {
    ; // wait for serial port to connect
  }
  //delay(1000);
#endif /* SERIAL_DEBUG */

  // Read configuration from EEPROM
  {
    for (int i = 0; i < sizeof(s_config); i++) {
      *((byte*)&g_config + i) = EEPROM.read(i);
    }

    memset(g_config.blake2s_mac_key, 42, BLAKE2S_KEY_SIZE);
    //SERIAL_PRINTLN("calling init_key");
    //SERIAL_PRINTLN();
    blake2s_init_key(&g_hasher, BLAKE2S_128_OUTPUT_SIZE, &g_config.blake2s_mac_key[0], BLAKE2S_KEY_SIZE);
    //delay(1000);
  }

#if HASH_DEBUG
  {
    SERIAL_PRINTLN("Starting test pattern");
    blake2s_reset(&g_hasher);

    unsigned long StartTime = micros();
    byte buf[64];
    memset(buf, 1, 64);
    blake2s_block(&g_hasher, buf, BLAKE2S_FLAG_NORMAL);
    memset(buf, 2, 56);
    blake2s_finish(&g_hasher, buf, 56);
    unsigned long EndTime = micros();

    blake2s_output_hash(&g_hasher, buf);
    SERIAL_PRINT("test pattern hash result:\t");
    for (int i = 0; i < BLAKE2S_128_OUTPUT_SIZE; i++) {
      if (buf[i] < 0x10)
        SERIAL_PRINT("0");
      SERIAL_PRINT(buf[i], HEX);
    }
    SERIAL_PRINTLN();

    SERIAL_PRINT(EndTime - StartTime, DEC);
    SERIAL_PRINTLN(" microseconds");
    SERIAL_PRINTLN();
  }
#endif /* HASH_DEBUG */

/*
  SERIAL_PRINTLN("setting up 522");
  
  if (!card_init()) {
    SERIAL_PRINTLN("failed to set up 522");
  }
*/

  //card init
  SPI.begin();

  for (int i = 0; i < NUM_READERS; i++) {
    SERIAL_PRINT("Initializing g_mfrc522[");
    SERIAL_PRINT(i);
    SERIAL_PRINTLN("]...");

    g_mfrc522[i] = new MFRC522(PIN_SS(i), PIN_RST(i));
    g_mfrc522[i]->PCD_Init();

    SERIAL_PRINT("g_mfrc522[");
    SERIAL_PRINT(i);
    SERIAL_PRINTLN("] initialized.");
  }
  SERIAL_PRINTLN();
}

int connect_to_card(int i) {
  if (g_mfrc522[i]->PICC_IsNewCardPresent()) {
    if (!g_mfrc522[i]->PICC_ReadCardSerial()) {
      SERIAL_PRINTLN("Failed to retrieve card UID!");
      return (STATE_IDLE);
    }

    SERIAL_PRINT("Card UID:");
    for (int j = 0; j < g_mfrc522[i]->uid.size; j++) {
      SERIAL_PRINT(" ");
      if (g_mfrc522[i]->uid.uidByte[j] < 0x10)
        SERIAL_PRINT("0");
      SERIAL_PRINT(g_mfrc522[i]->uid.uidByte[j], HEX);
    }
    SERIAL_PRINTLN();

    //g_mfrc522[i]->PICC_DumpToSerial(&(g_mfrc522[i]->uid));
    //g_mfrc522[i]->PICC_HaltA();
    return (STATE_SELECT);
  }

  return (STATE_IDLE);
}

int select_app(int i) {
  MFRC522::StatusCode result = select_application(g_mfrc522[i], APP_ID_CARD42);

  if (result == MFRC522::STATUS_OK) {
    return (STATE_READ);
  }

  if (result == MFRC522::STATUS_INTERNAL_ERROR) {
    if (g_lerror == LERROR_ERRNO) {
      SERIAL_PRINT("SELECT_APPLICATION: errno: ");
      SERIAL_PRINTLN(errno);
    }
    else {
      SERIAL_PRINT("SELECT_APPLICATION: DESFire Status: ");
      SERIAL_PRINTLN(g_lerror);
    }
  }
  else {
    SERIAL_PRINT("SELECT_APPLICATION: MFRC522::StatusCode: ");
    SERIAL_PRINTLN(result);
  }

  return (STATE_IDLE);
}

int read_and_verify(int i) {
  if (true) { //TODO: read the card data and verify it
    return (STATE_UNLOCK); //At this point the card can be pulled away
  }

  return (STATE_IDLE);
}

int unlock_lock(int i) {
  //TODO: unlock the lock
  g_unlock_period[i] = millis() + UNLOCK_PERIOD;
  SERIAL_PRINTLN("Unlocked.");
  return (STATE_WAIT);
}

int wait_on_lock(int i) {
  if (millis() > g_unlock_period[i]) {
    return (STATE_LOCK);
  }

  return (STATE_WAIT);
}

int lock_lock(int i) {
  //TODO: lock the lock
  SERIAL_PRINTLN("Locked.");
  return (STATE_IDLE);
}

void loop() {
  for (int i = 0; i < NUM_READERS; i++) {
    switch (g_states[i]) {
      case STATE_IDLE:
        g_states[i] = connect_to_card(i);
        break;

      case STATE_SELECT:
        g_states[i] = select_app(i);
        break;

      case STATE_READ:
        g_states[i] = read_and_verify(i);
        break;

      case STATE_UNLOCK:
        g_states[i] = unlock_lock(i);
        break;

      case STATE_WAIT:
        g_states[i] = wait_on_lock(i);
        break;

      case STATE_LOCK:
        g_states[i] = lock_lock(i);
        break;

      default: // If something gets corrupted, just lock the door (potentially set the color of a light?)
        lock_lock(i);
        g_states[i] = STATE_IDLE;
    }
  }
  delay(1);
}

/*
void nfc_isr() {
  Serial.println("got IRQ from reader");
}
*/

