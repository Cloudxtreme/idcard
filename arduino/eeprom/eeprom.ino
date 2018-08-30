#include <EEPROM.h>

#define NUM_READERS 1
#define BLAKE2S_KEY_SIZE 32

#include "eeprom_config.h"

// Permission modes
#define PERMISSION_MODE_STANDARD 1
#define PERMISSION_MODE_OPEN 2
#define PERMISSION_MODE_STAFF 3
#define PERMISSION_MODE_MOVIEROOM 4
#define PERMISSION_MODE_NOPISCINE 5
#define PERMISSION_MODE_STANDARD_TIMEOPEN 6

#define DOOR_ID_A 5
#define PERMISSION_MODE_A PERMISSION_MODE_STANDARD
#define DOOR_ID_B 6
#define PERMISSION_MODE_B PERMISSION_MODE_STANDARD

#define DEBUG_MODE true

byte g_id_mac_key[0x20] = {
#if DEBUG_MODE
  0x2A, 0x2A, 0x2A, 0x2A, 0x2A, 0x2A, 0x2A, 0x2A,
  0x2A, 0x2A, 0x2A, 0x2A, 0x2A, 0x2A, 0x2A, 0x2A,
  0x2A, 0x2A, 0x2A, 0x2A, 0x2A, 0x2A, 0x2A, 0x2A,
  0x2A, 0x2A, 0x2A, 0x2A, 0x2A, 0x2A, 0x2A, 0x2A
#else
#include "id_mac_key.inc"
#endif
};

byte g_tk_mac_key[0x20] = {
#if DEBUG_MODE
  0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42,
  0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42,
  0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42,
  0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42
#else
#include "tk_mac_key.inc"
#endif
};

void setup() {
  Serial.begin(9600);
  while (!Serial) {
    ; // wait for serial port to connect
  }

  s_config my_conf;

  Serial.println("Preparing...");
  memcpy(my_conf.id_mac_key, g_id_mac_key, BLAKE2S_KEY_SIZE);
  memcpy(my_conf.tk_mac_key, g_tk_mac_key, BLAKE2S_KEY_SIZE);
  my_conf.door_confs[0].door_id = DOOR_ID_A;
  my_conf.door_confs[0].permission_mode = PERMISSION_MODE_A;
#if NUM_READERS > 1
  my_conf.door_confs[1].door_id = DOOR_ID_B;
  my_conf.door_confs[1].permission_mode = PERMISSION_MODE_B;
#endif

  Serial.println("Writing...");
  my_conf.WriteToEEPROM();
}

void loop() {
}

