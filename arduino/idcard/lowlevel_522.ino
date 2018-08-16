#include "reader_config.h"

#if 1 //USE_MFRC_522

#define MAX_FIFO_SIZE 64
#define WIRE_MAX_READ 32

#define RC522REG_Reserved00 0
#define RC522REG_Command 1
#define RC522REG_ComIEn 2
#define RC522REG_DivIEn 3
#define RC522REG_ComIRQ 4
#define RC522REG_DivIRQ 5
#define RC522REG_Error 6
#define RC522REG_Status1 7
#define RC522REG_Status2 8
#define RC522REG_FIFOData 9
#define RC522REG_FIFOLevel 0xa
#define RC522REG_WaterLevel 0xb
#define RC522REG_Control 0xc
#define RC522REG_BitFraming 0xd
#define RC522REG_Collision 0xe
#define RC522REG_Reserved0F 0xf

#define RC522REG_Reserved10 0x10
#define RC522REG_Mode 0x11
#define RC522REG_TxMode 0x12
#define RC522REG_RxMode 0x13
#define RC522REG_TxControl 0x14
#define RC522REG_TxASK 0x15
#define RC522REG_TxSel 0x16
#define RC522REG_RxSel 0x17
#define RC522REG_RxThreshold 0x18
#define RC522REG_Demod 0x19
#define RC522REG_Reserved1A 0x1a
#define RC522REG_Reserved1B 0x1b
#define RC522REG_MifareTx 0x1c
#define RC522REG_MifareRx 0x1d
#define RC522REG_Reserved1E 0x1e
#define RC522REG_SerialSpeed 0x1f

#define RC522REG_Reserved20 0x20
#define RC522REG_CRCResultHi 0x21
#define RC522REG_CRCResultLo 0x22
#define RC522REG_Reserved23 0x23
#define RC522REG_ModWidth 0x24
#define RC522REG_Reserved25 0x25
#define RC522REG_RFCfg 0x26
#define RC522REG_GsN 0x27
#define RC522REG_CWGsP 0x28
#define RC522REG_ModGsP 0x29
#define RC522REG_TimerMode 0x2a
#define RC522REG_TimerPrescaler 0x2b
#define RC522REG_TimerReloadLo 0x2c
#define RC522REG_TimerReloadHi 0x2d
#define RC522REG_TimerCounterLo 0x2e
#define RC522REG_TimerCounterHi 0x2f

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

int cur_i2c_id = 0b0101000;

byte mfrc522_writeRegister(byte reg, byte value) {
  Wire.beginTransmission(cur_i2c_id);
  Wire.write(reg);
  Wire.write(value);
  return Wire.endTransmission(true);
}

byte mfrc522_writeFIFO(byte *data, int len) {
  Wire.beginTransmission(cur_i2c_id);
  Wire.write(RC522REG_FIFOData);
  Wire.write(data, len);
  return Wire.endTransmission(true);
}

byte mfrc522_readRegister(byte reg) {
  Serial.print("beginTransmission("); Serial.print(cur_i2c_id); Serial.print(")\n");
  Wire.beginTransmission(cur_i2c_id);
  Wire.write(reg);
  byte st = Wire.endTransmission(true);
  if (st != 0) {
    Serial.print("error "); Serial.print(st); Serial.print("\n");
    return 0xFF;
  }
  Serial.print("requestFrom("); Serial.print(cur_i2c_id); Serial.print(", 1)\n");
  Wire.requestFrom(cur_i2c_id, 1);
  if (Wire.available() == 0) {
    Serial.print("readRegister failed\n");
  }
  return Wire.read();
}

// up to 64 bytes maximum
int mfrc522_readFIFO(byte *output) {
  Wire.beginTransmission(cur_i2c_id);
  Wire.write(RC522REG_FIFOData);
  Wire.endTransmission(true);

#if I2C_SUPPORTS_HS_MODE
  Wire.setClock(I2C_FREQUENCY_HS);
#endif

  // WIRE_MAX_READ < FIFO_SIZE, so read twice
  Wire.requestFrom(cur_i2c_id, WIRE_MAX_READ, false);
  int count = Wire.available();
  for (int i = 0; i < count; i++) {
    output[i] = Wire.read();
  }
  if (count == WIRE_MAX_READ) {
    Wire.requestFrom(cur_i2c_id, WIRE_MAX_READ, true);
    count += Wire.available();
    for (int i = WIRE_MAX_READ; i < count; i++) {
      output[i] = Wire.read();
    }
  } else {
    Wire.requestFrom(cur_i2c_id, 0, true);
  }

#if I2C_SUPPORTS_HS_MODE
  Wire.setClock(I2C_FREQUENCY);
#endif
  return count;
}

#endif
