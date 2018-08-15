#ifndef DESFIRECARD_H
# define DESFIRECARD_H

#include <MFRC522.h>

enum DESFireCommand : byte {
  CMD_AUTHENTICATE              = 0x0A,
  CMD_GET_MANUFACTURING_DATA    = 0x60,
  CMD_GET_APPLICATION_DIRECTORY = 0x6A,
  CMD_GET_FREE_SPACE            = 0x6E,
  CMD_GET_ADDITIONAL_FRAME      = 0xAF,
  CMD_SELECT_APPLICATION        = 0x5A,
  CMD_WRITE_DATA                = 0x3D,
  CMD_READ_DATA                 = 0xBD,
  CMD_READ_RECORD               = 0xBB,
  CMD_GET_VALUE                 = 0x6C,
  CMD_GET_FILES                 = 0x6F,
  CMD_GET_FILE_SETTINGS         = 0xF5,
  CMD_COMMIT_TRANSACTION        = 0xC7,
  CMD_ABORT_TRANSACTION         = 0xA7,
  CMD_CREATE_APPLICATION        = 0xCA,
  CMD_CHANGE_FILE_SETTINGS      = 0x5F,
  CMD_CREATE_BACKUP_FILE        = 0xCB,
  CMD_CREATE_VALUE_FILE         = 0xCC,
  CMD_CREATE_STDDATA_FILE       = 0xCD,
  CMD_CREATE_LINEARRECORD_FILE  = 0xC1,
  CMD_CREATE_CYCLICRECORD_FILE  = 0xC0,
  CMD_CHANGE_KEY_SETTINGS       = 0x54,
  CMD_GET_KEY_SETTINGS          = 0x45,
  CMD_FORMAT_PICC               = 0xFC
};

enum ProtocolStatus : byte {
  STATUS_OPERATION_OK          = 0,
  STATUS_NO_CHANGES            = 0xC,
  STATUS_OUT_OF_MEMORY         = 0xE,
  STATUS_INTEGRITY_ERROR       = 0x1E,
  STATUS_NO_SUCH_KEY           = 0x40,
  STATUS_LENGTH_ERROR          = 0x7E,
  STATUS_PERMISSION_DENIED     = 0x9D,
  STATUS_APPLICATION_NOT_FOUND = 0xA0,
  STATUS_APPL_INTEGRITY_ERROR  = 0xA1,
  STATUS_AUTHENTICATION_ERROR  = 0xAE,
  STATUS_ADDITIONAL_FRAME      = 0xAF,
  STATUS_BOUNDARY_ERROR        = 0xBE,
  STATUS_PICC_INTEGRITY_ERROR  = 0xC1,
  STATUS_COMMAND_ABORTED       = 0xCA,
  STATUS_PICC_DISABLED_ERROR   = 0xCD,
  STATUS_COUNT_ERROR           = 0xCE,
  STATUS_DUPLICATE_ERROR       = 0xDE,
  STATUS_EEPROM_ERROR          = 0xEE,
  STATUS_FILE_NOT_FOUND        = 0xF0,
  STATUS_FILE_INTEGRITY_ERROR  = 0xF1,

  STATUS_UNKNOWN_ERROR_CODE    = 0xFF
};

# define LERROR_ERRNO 0xFFFF
# define LERROR_BAD_RESPONSE 0xFFFE

extern uint16_t g_lerror;

MFRC522::StatusCode send_request(MFRC522 *mfrc522, byte command, byte *sendbuf, byte sendlen, byte **recvbuf, unsigned int *recvlen);
MFRC522::StatusCode send_partial_request(MFRC522 *mfrc522, byte command, byte *sendbuf, byte sendlen, byte **recvbuf, byte *recvlen);
MFRC522::StatusCode select_application(MFRC522 *mfrc522, uint32_t appID);

#endif /* DESFIRECARD_H */
