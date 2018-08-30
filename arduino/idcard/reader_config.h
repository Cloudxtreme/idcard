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
  STATE_LOCK,
  STATE_UNLOCK_START,
  STATE_UNLOCK_NOBEEP,
  STATE_UNLOCK_END,
  STATE_ERRBEEP,
  STATE_READ_START,
  STATE_READ_UPDATE,
  STATE_READ_POSTPHONEWAIT,
  STATE_PHONE_WAIT,
};

enum CardType : byte {
  IDCARD,
  TICKET,
  UPDATE
};

namespace sm {
  struct ReturnSentinel {};

  class Rdr {
  public:
    using StateMachineFunc = ReaderState (Rdr::*)(void);

    Rdr() : mfrc522(NULL), m_curstate(STATE_IDLE), m_delayuntil(0) {};

    void loop();

    // STATE_IDLE
    ReturnSentinel connect_to_card();
    // STATE_WAIT
    ReturnSentinel check_wait();
    // STATE_SELECT
    ReturnSentinel select_app();
    // STATE_READ_START
    // STATE_READ_POSTPHONEWAIT
    ReturnSentinel read_and_verify();
    // STATE_PHONE_WAIT
    ReturnSentinel check_phone_ready();
    // STATE_UNLOCK_START
    ReturnSentinel unlock_start();
    // STATE_UNLOCK_NOBEEP
    ReturnSentinel unlock_endbeep();
    // STATE_UNLOCK_END
    ReturnSentinel unlock_end();
    // STATE_ERRBEEP
    ReturnSentinel err_beeper();

  private:
    /*
     * Switch to 'next' after 'delay_ms' milliseconds.
     */
    ReturnSentinel wait_then_do(long delay_ms, ReaderState next);
    /*
     * Print an error to the serial console, if enabled, and switch to ERRBEEP. Optionally, send a HLTA to the card.
     */
    ReturnSentinel handle_error(const char *opname, MFRC522::StatusCode status, bool halt_card = false);
    /*
     * Return from a state machine function.
     */
    inline ReturnSentinel sm_return(ReaderState switchto) {
      m_curstate = switchto;
      return (ReturnSentinel{});
    }

  // Fields
  public:
    MFRC522 *mfrc522;
  private:
    ReaderState m_curstate;
    unsigned long m_delayuntil;
    union {
      ReaderState m_nextstate;
      int m_extra;
    } u;
    /**
     * 1: ID card
     * 2: Phone ticket
     * 3: Phone update
     */
    CardType m_cardtype;
  };

}

extern sm::Rdr g_readers[NUM_READERS];

#define SCAN_PERIOD_MS 5
#define UNLOCK_PERIOD_MS 5000
#define ERRBEEP_HZ 1500
#define ERRBEEP_ON_MS 100
#define ERRBEEP_OFF_MS 50
#define GOODBEEP_HZ 900
#define GOODBEEP_ON_MS 600
#define EXPIRE_DETECT ((uint32_t)0x80000000)

#endif // READER_CONFIG_H
