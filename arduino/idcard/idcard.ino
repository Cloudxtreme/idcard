
#include <EEPROM.h>
#include "Wire.h"

#define PERMISSION_MODE_STANDARD 1
#define PERMISSION_MODE_NOPISCINE 2
#define PERMISSION_MODE_STAFF 3
#define PERMISSION_MODE_MOVIEROOM 4

// EEPROM contents
typedef struct    s_config {
  byte door_id;
  byte permission_mode;
  
} __attribute__((packed)) t_config;

t_config          g_config;
volatile byte     g_got_isr; // bit 0 for reader A, bit 1 for reader B

void setup() {
  // put your setup code here, to run once:
  DDRB |= (1 << 5);
  pinMode(12, OUTPUT);

  attachInterrupt(digitalPinToInterrupt(2), &nfc_isr, RISING);
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
  byte signatureUser[68];
  byte signatureDoor[68];
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

