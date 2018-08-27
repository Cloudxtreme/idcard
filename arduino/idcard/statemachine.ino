#include "DESFireCard.h"
#include "reader_config.h"

// todo: auto cps conversion?

sm::Rdr  g_readers[NUM_READERS];

namespace sm {

void Rdr::loop() {
  switch (m_curstate) {
      case STATE_WAIT:
        check_wait();
        break;

      case STATE_IDLE:
        connect_to_card();
        break;

      case STATE_SELECT:
        select_app();
        break;

      case STATE_READ_START:
      case STATE_READ_POSTPHONEWAIT:
        read_and_verify();
        break;

      case STATE_PHONE_WAIT:
        check_phone_ready();
        break;

      case STATE_UNLOCK_START:
        unlock_start();
        break;

      case STATE_UNLOCK_NOBEEP:
        unlock_endbeep();
        break;

      case STATE_UNLOCK_END:
        unlock_end();
        break;

      case STATE_ERRBEEP:
        err_beeper();
        break;


      default: // If something gets corrupted, just lock the door (potentially set the color of a light?)
        unlock_end();
        m_curstate = STATE_IDLE;
  }
}

ReturnSentinel Rdr::wait_then_do(long delay_ms, ReaderState next) {
  m_delayuntil = millis() + delay_ms;
  m_curstate = STATE_WAIT;
  u.m_nextstate = next;
  return (ReturnSentinel{});
}

ReturnSentinel Rdr::handle_error(const char *opname, MFRC522::StatusCode status, bool halt_card) {
  if (halt_card) {
    mfrc522->PICC_HaltA();
  }
  
  SERIAL_PRINT("Card error ");
  if (status == MFRC522::STATUS_INTERNAL_ERROR) {
    switch (g_lstatus) {
      case LERROR_ERRNO:
        SERIAL_PRINT(opname);
        SERIAL_PRINT(": errno: ");
        SERIAL_PRINTLN(errno);
        break;

      case LERROR_MFRC522_LIBRARY:
        SERIAL_PRINT(opname);
        SERIAL_PRINTLN(": MFRC522 library internal error");
        break;

      case LERROR_LARGER_RESPONSE:
        SERIAL_PRINT(opname);
        SERIAL_PRINTLN(": Specified response buffer is not large enough");
        break;

      case LERROR_BAD_RESPONSE:
        SERIAL_PRINT(opname);
        SERIAL_PRINTLN(": Invalid DESFire response");
        break;

      default:
        SERIAL_PRINT(opname);
        SERIAL_PRINT(": DESFire Status: ");
        SERIAL_PRINTLN(g_lstatus, HEX);
    }
  }
  else {
    SERIAL_PRINT(opname);
    SERIAL_PRINT(": MFRC522::StatusCode: ");
    SERIAL_PRINTLN(status);
  }

  m_delayuntil = 0;
  m_curstate = STATE_ERRBEEP;
  return (ReturnSentinel{});
}

}
