#include <errno.h>
#include <EEPROM.h>
#include <Wire.h>
#include <MFRC522.h> //https://playground.arduino.cc/Learning/MFRC522
#include "blake2s.h"
#include "reader_config.h" // Use flags go in here
#include "eeprom_config.h"
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
    g_config.ReadFromEEPROM();
    for (int i = 0; i < sizeof(s_config); i++) {
      *((byte*)&g_config + i) = EEPROM.read(i);
    }
    SERIAL_PRINTLN("Loaded configuration from EEPROM");
    for (int i = 0; i < NUM_READERS; i++) {
      SERIAL_PRINT("DOOR ID: "); SERIAL_PRINTLN(g_config.door_confs[i].door_id);
      SERIAL_PRINT("PERMISSION MODE: "); SERIAL_PRINTLN(g_config.door_confs[i].permission_mode);
    }

    /* Do not expose MAC key in any case
    SERIAL_PRINT("ID MAC KEY: ");
    print_memory(g_config.id_mac_key[i], BLAKE2S_KEY_SIZE);
    print_memory(g_config.tk_mac_key[i], BLAKE2S_KEY_SIZE);
    */

    SERIAL_PRINTLN();
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

    g_readers[i].mfrc522 = new MFRC522(PIN_SS(i), PIN_RST(i));
    g_readers[i].mfrc522->PCD_Init();

    SERIAL_PRINT("g_mfrc522[");
    SERIAL_PRINT(i);
    SERIAL_PRINTLN("] initialized.");
  }
  SERIAL_PRINTLN();
}

void print_memory(byte *ptr, int len) {
  for (int i = 0; i < len; i++) {
    if (((i & 0xF) == 0) && i) SERIAL_PRINTLN();
    if (ptr[i] < 0x10) SERIAL_PRINT("0");
    SERIAL_PRINT(ptr[i], HEX);
  }
}

/*
 * =====================================================================
 * State Machine Functions
 * =====================================================================
 */

namespace sm {

// STATE_IDLE
ReturnSentinel Rdr::connect_to_card() {
  if (mfrc522->PICC_IsNewCardPresent()) {
    if (mfrc522->PICC_ReadCardSerial()) {
      //SERIAL_PRINTLN("Failed to retrieve card UID!");
      return (sm_return(STATE_IDLE));
    }

    SERIAL_PRINT("Card UID:");
    for (int j = 0; j < mfrc522->uid.size; j++) {
      SERIAL_PRINT(" ");
      if (mfrc522->uid.uidByte[j] < 0x10)
        SERIAL_PRINT("0");
      SERIAL_PRINT(mfrc522->uid.uidByte[j], HEX);
    }
    SERIAL_PRINTLN();

    auto status = mfrc522->ISODEP_RATS();
    if (status != MFRC522::STATUS_OK) {
      return (handle_error("RATS", status, true));
    }
    return (sm_return(STATE_SELECT));
  }

  return (wait_then_do(SCAN_PERIOD_MS, STATE_IDLE));
}

// STATE_SELECT
ReturnSentinel Rdr::select_app() {
  MFRC522::StatusCode result;
  result = select_7816_app(mfrc522);
  if (result != MFRC522::STATUS_OK) {
    return (handle_error("SelectISOApplication", result, true));
  }
  
  result = select_application(mfrc522, APP_ID_CARD42);
  if (result != MFRC522::STATUS_OK) {
    return (handle_error("SelectApplication", result, true));
  }

  return (sm_return(STATE_READ_START));
}

// STATE_READ_START, STATE_READ_POSTPHONEWAIT
ReturnSentinel Rdr::read_and_verify(void) {
  MFRC522::StatusCode status;
  byte verify_data[0x80];
  // 16 bytes serial, zeroes
  // 16 bytes file 0x1
  // 32 bytes file 0x2
  // 48 bytes file 0x4

  status = read_file(mfrc522, 1, 0, &verify_data[0x10], 0x10);
  if (status != MFRC522::STATUS_OK) {
    return (handle_error("ReadFile 1", status, true));
  }

  if ((verify_data[0x1a] == 'U') && (verify_data[0x1b] == 'P')) {
    return (sm_return(STATE_READ_UPDATE));
  } else if ((verify_data[0x1a] == 'T') && (verify_data[0x1b] == 'K')) {
    if (m_curstate == STATE_READ_START) {
      u.m_extra = 0; // attempts counter
      return (sm_return(STATE_PHONE_WAIT));
    } else {
      // continue, is POSTPHONEWAIT
    }
  } else if ((verify_data[0x1a] == 'I') && (verify_data[0x1b] == 'D')) {
    // continue
  } else {
    return (handle_error("ReadFile1: Unrecognized type", status, true));
  }

  if (((verify_data[0x1a] == 'I') && (verify_data[0x1b] == 'D')) ||
      ((verify_data[0x1a] == 'T') && (verify_data[0x1b] == 'K'))) {
    status = read_file(mfrc522, 2, 0, &verify_data[0x20], 0x20);
    if (status != MFRC522::STATUS_OK) {
      return (handle_error("ReadFile 2", status, true));
    }

    status = read_file(mfrc522, 4, 0, &verify_data[0x40], 0x30);
    if (status != MFRC522::STATUS_OK) {
      return (handle_error("ReadFile 4", status, true));
    }

    // Read the MAC
    byte card_mac[0x10];
    status = read_file(mfrc522, 4, 0x30, card_mac, 0x10);
    if (status != MFRC522::STATUS_OK) {
      return (handle_error("ReadFile 4b", status, true));
    }

    // Clear out the card serial
    memset(verify_data, 0, 16);

    // ID cards and Tickets have separate MAC keys and serial methods (?)
    if (verify_data[0x1a] == 'I') {
      for (int j = 0; j < mfrc522->uid.size; j++) {
        verify_data[j] = mfrc522->uid.uidByte[j];
      }

      blake2s_init_key(&g_hasher, BLAKE2S_128_OUTPUT_SIZE, g_config.id_mac_key, BLAKE2S_KEY_SIZE);
    }
    else {
      memset(verify_data, 0xFF, 11); // kyork: card serials cannot possibly be 11 bytes long
      blake2s_init_key(&g_hasher, BLAKE2S_128_OUTPUT_SIZE, g_config.tk_mac_key, BLAKE2S_KEY_SIZE);
    }

    // Compute the MAC
    blake2s_block(&g_hasher, verify_data + 0x00, BLAKE2S_FLAG_NORMAL);
    blake2s_finish(&g_hasher, verify_data + 0x40, 0x30);
    byte compare_mac[16];
    blake2s_output_hash(&g_hasher, compare_mac);

    byte compare = 0;
    for (int i = 0; i < 16; i++) {
      compare |= card_mac[i] ^ compare_mac[i];
    }

    SERIAL_PRINTLN("Card data:");
    print_memory(verify_data, 0x70); SERIAL_PRINTLN();

    SERIAL_PRINTLN("Card MAC:");
    print_memory(card_mac, 0x10); SERIAL_PRINTLN();

    SERIAL_PRINTLN("Calculated MAC:");
    print_memory(compare_mac, 0x10); SERIAL_PRINTLN();

    if (!compare) {
      //TODO: Verify card information when MAC is valid
      return (sm_return(STATE_UNLOCK_START)); //At this point the card can be pulled away
    }
    else {
      return (handle_error("MAC failure", MFRC522::STATUS_OK));
    }
  }

  return (handle_error("Unknown card type", MFRC522::STATUS_OK));
}

// STATE_PHONE_WAIT
ReturnSentinel Rdr::check_phone_ready(void) {
  u.m_extra++;
  if (u.m_extra > 40) { // ~1 seconds
    return (handle_error("phone_wait_exceeded", MFRC522::STATUS_TIMEOUT, true));
  }

  byte reply[1];
  byte reply_len = 1;
  byte cmd[2];

  cmd[0] = CMD_CUSTOM_IS_READY;
  cmd[1] = 0; // TODO - door ID
  MFRC522::StatusCode status = send_wrapped_request(mfrc522, cmd, sizeof(cmd), reply, &reply_len);
  if (status != MFRC522::STATUS_OK) {
    return (handle_error("phone_wait", status, true));
  }
  // SERIAL_PRINT("PhoneWait result: "); SERIAL_PRINTLN(reply[0]);
  if (reply[0] == 1) {
    return (sm_return(STATE_READ_POSTPHONEWAIT));
  } else if (reply[0] == 2) {
    return (wait_then_do(60, STATE_PHONE_WAIT));
  } else {
    return (handle_error("phone_wait", MFRC522::STATUS_ERROR, true));
  }
}

// STATE_UNLOCK_START
ReturnSentinel Rdr::unlock_start(void) {
  //TODO: unlock the lock
  SERIAL_PRINTLN("Unlocked.");

  tone(SPEAKER_PIN2(i), GOODBEEP_HZ, GOODBEEP_ON_MS);
  SERIAL_PRINTLN("(speaker on)");
  return (wait_then_do(GOODBEEP_ON_MS, STATE_UNLOCK_NOBEEP));
}

// STATE_UNLOCK_NOBEEP
ReturnSentinel Rdr::unlock_endbeep(void) {
  SERIAL_PRINTLN("(speaker off)");
  noTone(SPEAKER_PIN2(i));
  return (wait_then_do(UNLOCK_PERIOD_MS - GOODBEEP_ON_MS, STATE_UNLOCK_END));
}

// STATE_UNLOCK_END
ReturnSentinel Rdr::unlock_end(void) {
  //TODO: lock the lock
  SERIAL_PRINTLN("Locked.");
  return (sm_return(STATE_IDLE));
}

// STATE_ERRBEEP
ReturnSentinel Rdr::err_beeper(void) {
  if (m_delayuntil == 0) {
    SERIAL_PRINTLN("(speaker on)");
    m_delayuntil = millis();
  }

  unsigned long now = millis();
  unsigned long period = (unsigned long)(now - m_delayuntil) / (ERRBEEP_ON_MS + ERRBEEP_OFF_MS);
  if (period >= 3) {
    SERIAL_PRINTLN("(speaker off)");
    m_delayuntil = 0;
    return (sm_return(STATE_IDLE));
  }
  else if ((unsigned long)(now - m_delayuntil) % (ERRBEEP_ON_MS + ERRBEEP_OFF_MS) < ERRBEEP_ON_MS) {
    tone(SPEAKER_PIN2(i), ERRBEEP_HZ, ERRBEEP_ON_MS);
  }
  else {
    noTone(SPEAKER_PIN2(i));
  }
  return (sm_return(STATE_ERRBEEP));
}

// STATE_WAIT
ReturnSentinel Rdr::check_wait(void) {
  unsigned long now = millis();
  if ((unsigned long)(now - m_delayuntil) < EXPIRE_DETECT) {
    m_curstate = u.m_nextstate;
    u.m_nextstate = STATE_IDLE;
    m_delayuntil = 0;
  }
  return (ReturnSentinel{});
}

}

void loop() {
  for (int i = 0; i < NUM_READERS; i++) {
    g_readers[i].loop();
  }

  delay(1);
}

/*
  void nfc_isr() {
  Serial.println("got IRQ from reader");
  }
*/

