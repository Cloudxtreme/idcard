
#include <EEPROM.h>
#include <Wire.h>
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

static bool g_serial_debug = true;

int i2c_master_code = 0x08;

extern unsigned int __data_start;
extern unsigned int __data_end;
extern unsigned int __bss_start;
extern unsigned int __bss_end;
extern unsigned int __heap_start;
extern void *__brkval;

void setup() {
  // put your setup code here, to run once:
  DDRB |= (1 << 5);
  pinMode(13, OUTPUT);

  // attachInterrupt(digitalPinToInterrupt(2), &nfc_isr, RISING);

  Wire.setClock(I2C_FREQUENCY);
  Wire.begin();

  if (true) {
    Serial.begin(9600);
    while (!Serial) {
      ; // wait for serial port to connect
    }
    Serial.print("hello");
    Serial.println();
    delay(1000);
  }

  // Read configuration from EEPROM
  {
    byte *cur = (byte*)&g_config;
    for (int i = 0; i < sizeof(s_config); i++) {
      *cur = EEPROM.read(i);
      cur++;
    }

    digitalWrite(13, true);
    ft_memset(&g_config.blake2s_mac_key[0], 43, BLAKE2S_KEY_SIZE);
    Serial.println("calling init_key");
    blake2s_init_key(&g_hasher, BLAKE2S_128_OUTPUT_SIZE, &g_config.blake2s_mac_key[0], BLAKE2S_KEY_SIZE);
    digitalWrite(13, false);
    delay(1000);
  }

  // test hashing
  if (g_serial_debug) {
    digitalWrite(13, true);
    Serial.println("Starting test pattern");
    blake2s_reset(&g_hasher);

    unsigned long StartTime = micros();
    byte buf[64];
    ft_memset(buf, 1, 64);
    blake2s_block(&g_hasher, buf, BLAKE2S_FLAG_NORMAL);
    ft_memset(buf, 2, 56);
    blake2s_finish(&g_hasher, buf, 56);
    unsigned long EndTime = micros();

    blake2s_output_hash(&g_hasher, buf);
    Serial.print("test pattern hash result:\t");
    for (int i = 0; i < BLAKE2S_128_OUTPUT_SIZE; i++) {
      Serial.print(buf[i], HEX);
    }
    Serial.println();
    Serial.print(EndTime - StartTime, DEC);
    Serial.print(" microseconds");
    Serial.println();
  }
    digitalWrite(13, false);
}

void loop() {
  // put your main code here, to run repeatedly:
  static int count = 0;

  count++;
  if (count > 100) {
    count = 0;
    PORTB ^= (1 << 5);
  }
  digitalWrite(13, true);
  delay(200);
  digitalWrite(13, false);
  delay(200);

delay(10);
}

void nfc_isr() {
}


