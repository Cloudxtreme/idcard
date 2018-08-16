
#include "reader_config.h"

// todo: auto cps conversion?


MFRC522           *g_mfrc522[NUM_READERS] = { NULL };
ReaderState       g_states[NUM_READERS] = { STATE_IDLE };
ReaderState       g_nextstate[NUM_READERS] = { STATE_IDLE };
uint32_t          g_delayuntil[NUM_READERS] = { 0 };

ReaderState wait_then_do(int i, long delay_ms, ReaderState next) {
  g_nextstate[i] = next;
  g_delayuntil[i] = millis() + delay_ms;
  return (STATE_WAIT);
}

ReaderState handle_error(int i, const char *opname, MFRC522::StatusCode status, bool halt_card) {
  if (halt_card) {
    g_mfrc522[i]->PICC_HaltA();
  }
  
  SERIAL_PRINT("Card "); SERIAL_PRINT(i); SERIAL_PRINT(" error ");
  if (status == MFRC522::STATUS_INTERNAL_ERROR) {
    if (g_lerror == LERROR_ERRNO) {
      SERIAL_PRINT(opname);
      SERIAL_PRINT(": errno: ");
      SERIAL_PRINTLN(errno);
    }
    else {
      SERIAL_PRINT(opname);
      SERIAL_PRINT(": DESFire Status: ");
      SERIAL_PRINTLN(g_lerror);
    }
  }
  else {
    SERIAL_PRINT(opname);
    SERIAL_PRINT(": MFRC522::StatusCode: ");
    SERIAL_PRINTLN(status);
  }

  g_delayuntil[i] = 0;
  return (STATE_ERRBEEP);
}
