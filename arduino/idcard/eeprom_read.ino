#include "eeprom_config.h"

void s_config::ReadFromEEPROM(void) {
  int ab_switch = EEPROM.read(0);
  PointerWrap p;
  if (ab_switch == 1) {
    p.offset = PROTECTED_CONFIG_OFFSET_A;
  } else {
    p.offset = PROTECTED_CONFIG_OFFSET_B;
  }
  p.is_writing = false;
  DoState(p);
}

void s_config::WriteToEEPROM(void) {
  int ab_switch = EEPROM.read(0);
  PointerWrap p;
  if (ab_switch == 1) {
    p.offset = PROTECTED_CONFIG_OFFSET_B;
    ab_switch = 2;
  } else {
    p.offset = PROTECTED_CONFIG_OFFSET_A;
    ab_switch = 1;
  }
  p.is_writing = true;
  DoState(p);
  Serial.print("Wrote EEPROM up to "); Serial.println(p.offset);
  EEPROM.write(0, ab_switch);
  Serial.print("Switched ProtectedConfig to "); Serial.println(ab_switch);
}

void s_config::DoState(PointerWrap &p) {
  p.do_ary(&id_mac_key[0], &id_mac_key[BLAKE2S_KEY_SIZE]);
  p.do_ary(&tk_mac_key[0], &tk_mac_key[BLAKE2S_KEY_SIZE]);

  if (NUM_READERS <= 1) {
    door_confs[0].DoState(p);
  } else {
    per_door_config::DoSkip(p);
  }
  if (NUM_READERS <= 2) {
    door_confs[1].DoState(p);
  } else {
    per_door_config::DoSkip(p);
  }
}

void s_config::per_door_config::DoState(PointerWrap &p) {
  p.do_int<byte, 1>(door_id);
  p.do_int<uint16_t, 2>(permission_mode);
  p.skip(1);
}

// PointerWrap impls

void s_config::PointerWrap::do_ary(byte *start, byte *end) {
  if (is_writing) {
    for (; start != end; start++) {
      EEPROM.write(offset, *start);
      offset++;
    }
  } else {
    for (; start != end; start++) {
      *start = EEPROM.read(offset);
      offset++;
    }
  }
}

void s_config::PointerWrap::do_byte(byte &ptr) {
  if (is_writing) {
    EEPROM.write(offset, ptr);
    offset++;
  } else {
    ptr = EEPROM.read(offset);
    offset++;
  }
}

void s_config::PointerWrap::skip(int bytes) {
  offset += bytes;
}

