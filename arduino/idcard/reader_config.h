#ifndef READER_CONFIG_H
# define READER_CONFIG_H

// === Use Flags

#define USE_MFRC_522 1
#define USE_MFRC_630 0
#define USE_I2C_HS_MODE 0

#define NUM_READERS 1

// === End Use Flags

enum ReaderState : int {
  STATE_IDLE = 0,
  STATE_WAIT = 1,
  STATE_SELECT,
  STATE_READ,
  STATE_LOCK,
  STATE_UNLOCK_START,
  STATE_UNLOCK_NOBEEP,
  STATE_UNLOCK_END,
  STATE_ERRBEEP,
  STATE_READ_UPDATE,
};

/*
struct  s_reader {
  MFRC522       *mfrc522;
  ReaderState   state;
  ReaderState   nextstate;
  unsigned long delayuntil;
};
*/

extern MFRC522           *g_mfrc522[NUM_READERS];
extern ReaderState       g_states[NUM_READERS];
extern ReaderState       g_nextstate[NUM_READERS];
extern unsigned long     g_delayuntil[NUM_READERS];

ReaderState wait_then_do(int cardi, long delay_ms, ReaderState next);
ReaderState handle_error(int cardi, const char *opname, MFRC522::StatusCode status, bool halt_card = false);

void        state_machine_loop(void);
ReaderState connect_to_card(int cardi);
ReaderState select_app(int i);
ReaderState read_and_verify(int i);
ReaderState unlock_lock(int i);
ReaderState unlock_endbeep(int i);
ReaderState unlock_end(int i);
ReaderState err_beeper(int i);

#define SCAN_PERIOD_MS 5
#define UNLOCK_PERIOD_MS 5000
#define ERRBEEP_HZ 1500
#define ERRBEEP_ON_MS 100
#define ERRBEEP_OFF_MS 50
#define GOODBEEP_HZ 900
#define GOODBEEP_ON_MS 600
#define EXPIRE_DETECT ((uint32_t)0x80000000)

#endif // READER_CONFIG_H
