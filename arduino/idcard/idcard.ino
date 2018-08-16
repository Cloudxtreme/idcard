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

#define APP_ID_CARD42 0xFB9852

// Speaker pins (TODO: A range of pins should be reserved for multiple speakers)
#define SPEAKER_PIN0(i) 27
#define SPEAKER_PIN1(i) 25
#define SPEAKER_PIN2(i) 23

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
#endif /* SERIAL_OUTPUT */

  // Read configuration from EEPROM
  {
    for (int i = 0; i < sizeof(s_config); i++) {
      *((byte*)&g_config + i) = EEPROM.read(i);
    }
    SERIAL_PRINTLN("Loaded configuration from EEPROM");
    SERIAL_PRINT("DOOR ID: "); SERIAL_PRINTLN(g_config.door_id);
    SERIAL_PRINT("PERMISSION MODE: "); SERIAL_PRINTLN(g_config.permission_mode);
    // Do not expose MAC key in any case
    SERIAL_PRINTLN();

    memset(g_config.blake2s_mac_key, 42, BLAKE2S_KEY_SIZE);
    blake2s_init_key(&g_hasher, BLAKE2S_128_OUTPUT_SIZE, g_config.blake2s_mac_key, BLAKE2S_KEY_SIZE);
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

  // Init speakers
  for (int i = 0; i < NUM_READERS; i++) {
    pinMode(SPEAKER_PIN0(i), OUTPUT);
    digitalWrite(SPEAKER_PIN0(i), LOW);
    pinMode(SPEAKER_PIN1(i), OUTPUT);
    digitalWrite(SPEAKER_PIN1(i), HIGH);
    pinMode(SPEAKER_PIN2(i), OUTPUT);
  }

  // Init readers
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

// STATE_IDLE
ReaderState connect_to_card(int i) {
  if (g_mfrc522[i]->PICC_IsNewCardPresent()) {
    if (!g_mfrc522[i]->PICC_ReadCardSerial()) {
      //SERIAL_PRINTLN("Failed to retrieve card UID!");
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

    auto status = g_mfrc522[i]->ISODEP_RATS();
    if (status != MFRC522::STATUS_OK) {
      return (handle_error(i, "RATS", status, true));
    }
    return (STATE_SELECT);
  }

  return (wait_then_do(i, SCAN_PERIOD_MS, STATE_IDLE));
}

// STATE_SELECT
ReaderState select_app(int i) {
  MFRC522::StatusCode result = select_application(g_mfrc522[i], APP_ID_CARD42);
  if (result != MFRC522::STATUS_OK) {
    return (handle_error(i, "SelectApplication", result, true));
  }

  return (STATE_READ);
}

void print_memory(byte *ptr, int len) {
  for (int i = 0; i < len; i++) {
    if (((i & 0xF) == 0) && i) SERIAL_PRINTLN();
    if (ptr[i] < 0x10) SERIAL_PRINT("0");
    SERIAL_PRINT(ptr[i], HEX);
  }
}

// STATE_READ
ReaderState read_and_verify(int i) {
  MFRC522::StatusCode status;
  byte verify_data[0x80];
  // 16 bytes serial, zeroes
  // 16 bytes file 0x1
  // 32 bytes file 0x2
  // 48 bytes file 0x4

  memset(verify_data + 0, 0, 16);
  for (int j = 0; j < g_mfrc522[i]->uid.size; j++) {
    verify_data[j] = g_mfrc522[i]->uid.uidByte[j];
  }
  status = read_file(g_mfrc522[i], 1, 0, &verify_data[0x10], 0x10);
  if (status != MFRC522::STATUS_OK) {
    return (handle_error(i, "ReadFile 1", status, true));
  }

  if (verify_data[0x1a] == 'I' || verify_data[0x1a] == 'T') {
    status = read_file(g_mfrc522[i], 2, 0, &verify_data[0x20], 0x20);
    if (status != MFRC522::STATUS_OK) {
      return (handle_error(i, "ReadFile 2", status, true));
    }

    status = read_file(g_mfrc522[i], 4, 0, &verify_data[0x40], 0x30);
    if (status != MFRC522::STATUS_OK) {
      return (handle_error(i, "ReadFile 4", status, true));
    }

    // Read the MAC
    status = read_file(g_mfrc522[i], 4, 0x30, &verify_data[0x70], 0x10);
    if (status != MFRC522::STATUS_OK) {
      return (handle_error(i, "ReadFile 4b", status, true));
    }

    // Compute the MAC
    blake2s_reset(&g_hasher);
    blake2s_block(&g_hasher, verify_data + 0x00, BLAKE2S_FLAG_NORMAL);
    blake2s_finish(&g_hasher, verify_data + 0x40, 0x30);
    byte compare_mac[16];
    blake2s_output_hash(&g_hasher, compare_mac);

    byte compare = 0;
    for (int i = 0; i < 16; i++) {
      compare |= verify_data[0x70 + i] ^ compare_mac[i];
    }

    SERIAL_PRINTLN("Card data:");
    print_memory(verify_data, 0x70); SERIAL_PRINTLN();
    SERIAL_PRINTLN("Calculated MAC:");
    print_memory(compare_mac, 0x10); SERIAL_PRINTLN();
    SERIAL_PRINTLN("Card MAC:");
    print_memory(verify_data + 0x70, 0x10); SERIAL_PRINTLN();

    if (!compare) {
      return (STATE_UNLOCK_START); //At this point the card can be pulled away
    }
    else {
      SERIAL_PRINTLN("MAC AUTH failure");
      return (STATE_UNLOCK_START);
      //return (handle_error(i, "MAC failure", MFRC522::STATUS_OK));
    }
  }
  else if (verify_data[0x1a] == 'U') {
    Serial.println("TODO read update data");
  }

  return (handle_error(i, "Unknown card type", MFRC522::STATUS_OK));
}

// STATE_UNLOCK_START
ReaderState unlock_start(int i) {
  //TODO: unlock the lock
  SERIAL_PRINTLN("Unlocked.");

  tone(SPEAKER_PIN2(i), GOODBEEP_HZ, GOODBEEP_ON_MS);
  SERIAL_PRINTLN("(speaker on)");
  return (wait_then_do(i, GOODBEEP_ON_MS, STATE_UNLOCK_NOBEEP));
}

// STATE_UNLOCK_NOBEEP
ReaderState unlock_endbeep(int i) {
  SERIAL_PRINTLN("(speaker off)");
  noTone(SPEAKER_PIN2(i));
  return (wait_then_do(i, UNLOCK_PERIOD_MS - GOODBEEP_ON_MS, STATE_UNLOCK_END));
}

// STATE_UNLOCK_END
ReaderState unlock_end(int i) {
  //TODO: lock the lock
  SERIAL_PRINTLN("Locked.");
  return (STATE_IDLE);
}

// STATE_ERRBEEP
ReaderState err_beeper(int i) {
  if (g_delayuntil[i] == 0) {
    SERIAL_PRINTLN("(speaker on)");
    g_delayuntil[i] = millis();
  }

  unsigned long now = millis();
  unsigned long period = (unsigned long)(now - g_delayuntil[i]) / (ERRBEEP_ON_MS + ERRBEEP_OFF_MS);
  if (period >= 3) {
    SERIAL_PRINTLN("(speaker off)");
    g_delayuntil[i] = 0;
    return (STATE_IDLE);
  }
  else if ((unsigned long)(now - g_delayuntil[i]) % (ERRBEEP_ON_MS + ERRBEEP_OFF_MS) < ERRBEEP_ON_MS) {
    //SERIAL_PRINTLN("(speaker on)");
    tone(SPEAKER_PIN2(i), ERRBEEP_HZ, ERRBEEP_ON_MS);
  }
  else {
    //SERIAL_PRINTLN("(speaker off)");
    noTone(SPEAKER_PIN2(i));
  }
  return (STATE_ERRBEEP);
}

// STATE_WAIT
ReaderState check_wait(int i) {
  unsigned long now = millis();
  if ((unsigned long)(now - g_delayuntil[i]) < EXPIRE_DETECT) {
    g_states[i] = g_nextstate[i];
    g_nextstate[i] = STATE_IDLE;
    g_delayuntil[i] = 0;
  }
  return (g_states[i]);
}

void loop() {
  for (int i = 0; i < NUM_READERS; i++) {
    switch (g_states[i]) {
      case STATE_WAIT:
        g_states[i] = check_wait(i);
        break;

      case STATE_IDLE:
        g_states[i] = connect_to_card(i);
        break;

      case STATE_SELECT:
        g_states[i] = select_app(i);
        break;

      case STATE_READ:
        g_states[i] = read_and_verify(i);
        break;

      case STATE_UNLOCK_START:
        g_states[i] = unlock_start(i);
        break;

      case STATE_UNLOCK_NOBEEP:
        g_states[i] = unlock_endbeep(i);
        break;

      case STATE_UNLOCK_END:
        g_states[i] = unlock_end(i);
        break;

      case STATE_ERRBEEP:
        g_states[i] = err_beeper(i);
        break;


      default: // If something gets corrupted, just lock the door (potentially set the color of a light?)
        unlock_end(i);
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

