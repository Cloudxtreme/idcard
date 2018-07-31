#ifndef READER_CONFIG_H
# define READER_CONFIG_H

// === Use Flags

#define USE_MFRC_522 1
#define USE_MFRC_630 0
#define USE_I2C_HS_MODE 0

// === End Use Flags

# include <stddef.h>

struct s_desfire_cmd {
  int command;
  int len;
  byte data[64];
};

bool card_init(void);
void card_startDetect(void);

/*
 * @returns true if a card is present
 */
bool card_checkDetect(void);
void card_transcieve(struct s_desfire_cmd *data);

// I2C Frequency
#ifdef USE_MFRC_522
# define I2C_FREQUENCY 400000 // Fast
# define I2C_SUPPORTS_HS_MODE 1
#elif USE_MFRC_630
# define I2C_FREQUENCY 1000000 // Fast+
# define I2C_SUPPORTS_HS_MODE 0
#endif
#define I2C_FREQUENCY_HS 3400000 // High Speed

#if USE_I2C_HS_MODE && !I2C_SUPPORTS_HS_MODE
# undef USE_I2C_HS_MODE
# define USE_I2C_HS_MODE 0
#endif


#ifdef USE_MFRC_522
# define PIN_RESET_A 2
# define PIN_IRQ_A 3
#endif

// Commands sent to the PICC.
enum PICC_Command : byte {
  // The commands used by the PCD to manage communication with several PICCs (ISO 14443-3, Type A, section 6.4)
  PICC_CMD_REQA     = 0x26,   // REQuest command, Type A. Invites PICCs in state IDLE to go to READY and prepare for anticollision or selection. 7 bit frame.
  PICC_CMD_WUPA     = 0x52,   // Wake-UP command, Type A. Invites PICCs in state IDLE and HALT to go to READY(*) and prepare for anticollision or selection. 7 bit frame.
  PICC_CMD_CT       = 0x88,   // Cascade Tag. Not really a command, but used during anti collision.
  PICC_CMD_SEL_CL1  = 0x93,   // Anti collision/Select, Cascade Level 1
  PICC_CMD_SEL_CL2  = 0x95,   // Anti collision/Select, Cascade Level 2
  PICC_CMD_SEL_CL3  = 0x97,   // Anti collision/Select, Cascade Level 3
  PICC_CMD_HLTA     = 0x50,   // HaLT command, Type A. Instructs an ACTIVE PICC to go to state HALT.
  PICC_CMD_RATS     = 0xE0, // Request command for Answer To Reset.
};

#endif // READER_CONFIG_H
