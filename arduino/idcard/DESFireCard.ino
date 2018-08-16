#include <errno.h>
#include "DESFireCard.h"

uint16_t g_lerror = 0;
byte     g_lstatus = 0;

MFRC522::StatusCode send_request(MFRC522 *mfrc522, byte *cmdbuf, byte sendlen, byte *recvbuf, byte *recvlen) {
  byte output[0xFF];
  byte len = 0xFF;
  MFRC522::StatusCode result = mfrc522->ISODEP_Transceive(cmdbuf, sendlen, output, len);

  if (result != MFRC522::STATUS_OK) {
    if (result == MFRC522::STATUS_INTERNAL_ERROR)
      g_lerror = LERROR_TRANSCEIVE_FAILURE;
    return (result);
  }

  byte status = output[0];

  if (status == STATUS_OPERATION_OK || status == STATUS_ADDITIONAL_FRAME) {
    if (recvlen) {
      *recvlen = len - 1;
    }

    if (recvbuf && recvlen && *recvlen != 0) {
      if ((len - 1) > *recvlen) {
        g_lerror = LERROR_LARGER_RESPONSE;
        return (MFRC522::STATUS_INTERNAL_ERROR);
      }
      else {
        memcpy(recvbuf, output + 1, len - 1);
      }
    }

    g_lstatus = status;
    return (MFRC522::STATUS_OK);
  }

  g_lerror = status;
  return (MFRC522::STATUS_INTERNAL_ERROR);
}

MFRC522::StatusCode get_additional_frame(MFRC522 *mfrc522, byte *recvbuf, byte *recvlen) {
  byte cmd = CMD_GET_ADDITIONAL_FRAME;
  return (send_request(mfrc522, &cmd, 1, recvbuf, recvlen));
}

MFRC522::StatusCode select_application(MFRC522 *mfrc522, uint32_t app_id) {
  byte select_cmd[4];
  select_cmd[3] = (byte)(app_id & 0xFF);
  select_cmd[2] = (byte)((app_id >> 8) & 0xFF);
  select_cmd[1] = (byte)((app_id >> 16) & 0xFF);
  select_cmd[0] = CMD_SELECT_APPLICATION;
  return (send_request(mfrc522, select_cmd, 4, NULL, NULL));
}

MFRC522::StatusCode read_file(MFRC522 *mfrc522, byte file_id, uint32_t offset, byte *dst, byte length) {
  MFRC522::StatusCode status;
  byte read_command[8];

  if (length > 56)
    return (MFRC522::STATUS_NO_ROOM);
  
  read_command[0] = CMD_READ_DATA;
  read_command[1] = file_id;
  read_command[2] = (byte)(offset & 0xFF);
  read_command[3] = (byte)((offset >> 8) & 0xFF);
  read_command[4] = (byte)((offset >> 16) & 0xFF);
  read_command[5] = length;
  read_command[6] = 0; // length
  read_command[7] = 0; // length

  byte actual_len = length;
  status = send_request(mfrc522, read_command, 8, dst, &actual_len);
  if (status != MFRC522::STATUS_OK)
    return (status);

  if (length != actual_len) {
    g_lerror = STATUS_LENGTH_ERROR;
    return (MFRC522::STATUS_INTERNAL_ERROR);
  }

  return (status);
}


