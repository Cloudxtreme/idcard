#ifndef EEPROM_CONFIG_H
# define EEPROM_CONFIG_H

// EEPROM contents
// Max size 192 bytes
struct    s_config {
  const int PROTECTED_CONFIG_SIZE = 192;
  const int PROTECTED_CONFIG_OFFSET_A = 128;
  const int PROTECTED_CONFIG_OFFSET_B = 320;

 public:
  class PointerWrap {
   public:
    int offset;
    boolean is_writing;

    void do_ary(byte *start, byte *end);
    void do_byte(byte &ptr);
    void skip(int bytes);

    template<typename T, int Size>
    void do_int(T &ptr) {
      static_assert(sizeof(T) >= Size, "Wrong size to do_int");
      char tmp[Size];
      if (is_writing) {
        memcpy(tmp, &ptr, Size);
        for (int i = 0; i < Size; i++) {
          EEPROM.write(offset, tmp[i]);
          offset++;
        }
      } else {
        for (int i = 0; i < Size; i++) {
          tmp[i] = EEPROM.read(offset);
          offset++;
        }
        memcpy(&ptr, tmp, Size);
      }
    }
  };

 public:
  void ReadFromEEPROM();
  void WriteToEEPROM();
  void DoState(PointerWrap &p);

  byte id_mac_key[BLAKE2S_KEY_SIZE]; // 32 bytes
  byte tk_mac_key[BLAKE2S_KEY_SIZE]; // 32 bytes

  struct per_door_config {
   public:
    byte door_id;
    uint16_t permission_mode;

    void DoState(PointerWrap &p);
    static void DoSkip(PointerWrap &p);
  };

  per_door_config door_confs[NUM_READERS];
};

#endif
