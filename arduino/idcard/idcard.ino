
#include <EEPROM.h>
#include "Wire.h"
#include "blake2s.h"

#define PERMISSION_MODE_STANDARD 1
#define PERMISSION_MODE_NOPISCINE 2
#define PERMISSION_MODE_STAFF 3
#define PERMISSION_MODE_MOVIEROOM 4

// EEPROM contents
typedef struct    s_config {
  byte door_id;
  byte permission_mode;

  byte blake2s_mac_key[BLAKE2S_KEY_SIZE];
} __attribute__((packed)) t_config;

t_config          g_config;
volatile byte     g_got_isr; // bit 0 for reader A, bit 1 for reader B

struct s_blake2s_state g_hasher;

static bool g_serial_debug = true;

void setup() {
  // put your setup code here, to run once:
  DDRB |= (1 << 5);
  pinMode(12, OUTPUT);

  attachInterrupt(digitalPinToInterrupt(2), &nfc_isr, RISING);

  // Read configuration from EEPROM
  {
    byte *cur = (byte*)&g_config;
    for (int i = 0; i < sizeof(t_config); i++) {
      *cur = EEPROM.read(i);
      cur++;
    }

    blake2s_init_key(&g_hasher, BLAKE2S_128_OUTPUT_SIZE, &g_config.blake2s_mac_key[0], BLAKE2S_KEY_SIZE);
  }

  if (g_serial_debug) {
    Serial.begin(9600);
    while (!Serial) {
      ; // wait for serial port to connect
    }
  }

  // test hashing
  if (g_serial_debug) {
    unsigned long StartTime = micros();
    byte buf[64];
    memset(buf, 1, 64);
    blake2s_reset(&g_hasher);
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
  }
}

void loop() {
  // put your main code here, to run repeatedly:
  static int count = 0;

  count++;
  if (count > 100) {
    count = 0;
    PORTB ^= (1 << 5);
  }

delay(10);
}

void nfc_isr() {
}

typedef struct    s_pin522 {
  // will probably be collapsed into a bool
  byte i2cID;
  byte ledPin;
  byte irqPin; // only 2 or 3
  //
  int state;
  byte serial[7];
  byte metadataContent[16];
  byte userInfoContent[32];
  byte doorContent[56];
}                 t_pin522;

// SINGLE READER STATE MACHINE
// soft powerdown
// card boot: repeat reads to address 0
// card detect: ask if a card is there
//   go to card found or soft powerdown
// card found:
//   select application
//   authenticate
//   read serial
//   read data file 1
//   read data file 2
//   read data file 4
//   read data file 7 part 1
//    

void cardFound(struct s_pin522 a) {
  ;
}

