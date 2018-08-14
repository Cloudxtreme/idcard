#include <EEPROM.h>
#include <Wire.h>
#include <MFRC522.h>
#include "blake2s.h"
#include "reader_config.h" // Use flags go in here

#define PERMISSION_MODE_STANDARD 1
#define PERMISSION_MODE_NOPISCINE 2
#define PERMISSION_MODE_STAFF 3
#define PERMISSION_MODE_MOVIEROOM 4

// EEPROM contents
struct    s_config {
  byte door_id;
  byte permission_mode;

  byte blake2s_mac_key[BLAKE2S_KEY_SIZE];
};

s_config          g_config;
volatile byte     g_got_isr; // bit 0 for reader A, bit 1 for reader B

struct s_blake2s_state g_hasher;

#define NUM_READERS 1

MFRC522           *g_mfrc522[NUM_READERS]= { NULL };

#define HASH_DEBUG true

void setup() {
  Wire.begin();

  // Reader A pin config
  pinMode(PIN_RESET_A, OUTPUT);
  pinMode(PIN_IRQ_A, INPUT);
  //attachInterrupt(digitalPinToInterrupt(PIN_IRQ_A), &nfc_isr, RISING);

  // Wire.setClock(I2C_FREQUENCY);

  if (true) {
    Serial.begin(19200);
    while (!Serial) {
      ; // wait for serial port to connect
    }
    //delay(1000);
  }

  // Read configuration from EEPROM
  {
    byte *cur = (byte*)&g_config;
    for (int i = 0; i < sizeof(s_config); i++) {
      *cur = EEPROM.read(i);
      cur++;
    }

    memset(&g_config.blake2s_mac_key[0], 42, BLAKE2S_KEY_SIZE);
    //Serial.println("calling init_key");
    //Serial.println();
    blake2s_init_key(&g_hasher, BLAKE2S_128_OUTPUT_SIZE, &g_config.blake2s_mac_key[0], BLAKE2S_KEY_SIZE);
    //delay(1000);
  }

  // test hashing
#if HASH_DEBUG
  {
    Serial.println("Starting test pattern");
    blake2s_reset(&g_hasher);

    unsigned long StartTime = micros();
    byte buf[64];
    memset(buf, 1, 64);
    blake2s_block(&g_hasher, buf, BLAKE2S_FLAG_NORMAL);
    memset(buf, 2, 56);
    blake2s_finish(&g_hasher, buf, 56);
    unsigned long EndTime = micros();

    blake2s_output_hash(&g_hasher, buf);
    Serial.print("test pattern hash result:\t");
    for (int i = 0; i < BLAKE2S_128_OUTPUT_SIZE; i++) {
      Serial.print(buf[i], HEX);
    }
    Serial.println();
    Serial.print(EndTime - StartTime, DEC);
    Serial.println(" microseconds");
    Serial.println();
  }
#endif /* HASH_DEBUG */

/*
  Serial.println("setting up 522");
  
  if (!card_init()) {
    Serial.println("failed to set up 522");
  }
*/

  //card init
  SPI.begin();
  for (int i = 0; i < NUM_READERS; i++) {
    Serial.print("Initializing g_mfrc522[");
    Serial.print(i);
    Serial.println("]...");
    
    g_mfrc522[i] = new MFRC522(49 - (i * 2), 48 - (i * 2));
    g_mfrc522[i]->PCD_Init();
    
    Serial.print("g_mfrc522[");
    Serial.print(i);
    Serial.println("] initialized.");
    Serial.println();
  }
  
  digitalWrite(13, true);
}

void loop() {
  for (int i = 0; i < NUM_READERS; i++) {
    if (g_mfrc522[i]->PICC_IsNewCardPresent()) {
      Serial.println("Card found!");
      
      if (!g_mfrc522[i]->PICC_ReadCardSerial()) {
        Serial.println("Failed to retrieve serial!");
        return;
      }
      
      g_mfrc522[i]->PICC_DumpToSerial(&(g_mfrc522[i]->uid)); //REMOVE LATER
      //state machine time m9
      Serial.println();
    }
  }
}
/*
void nfc_isr() {
  Serial.println("got IRQ from reader");
}
*/

