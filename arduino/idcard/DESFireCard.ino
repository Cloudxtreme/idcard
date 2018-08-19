#include <errno.h>
#include "DESFireCard.h"

uint16_t g_lstatus = 0;

MFRC522::StatusCode send_unwrapped_request(MFRC522 *mfrc522, byte *cmdbuf, byte sendlen, byte *recvbuf, byte *recvlen) {
  byte output[0xFF];
  byte len = 0xFF;
  MFRC522::StatusCode result = mfrc522->ISODEP_Transceive(cmdbuf, sendlen, output, len);

  if (result != MFRC522::STATUS_OK) {
    if (result == MFRC522::STATUS_INTERNAL_ERROR)
      g_lstatus = LERROR_TRANSCEIVE_FAILURE;
    return (result);
  }

  byte status = output[0];
  g_lstatus = status;

  if (!(status == STATUS_OPERATION_OK || status == STATUS_ADDITIONAL_FRAME)) {
    return (MFRC522::STATUS_INTERNAL_ERROR);
  }
  if (recvlen) {
    *recvlen = len - 1;
  }

  if (recvbuf && recvlen && *recvlen != 0) {
    if ((len - 1) > *recvlen) {
      g_lstatus = LERROR_LARGER_RESPONSE;
      return (MFRC522::STATUS_INTERNAL_ERROR);
    }
    else {
      memcpy(recvbuf, output + 1, len - 1);
    }
  }

  return (MFRC522::STATUS_OK);
}

MFRC522::StatusCode send_wrapped_request(MFRC522 *mfrc522, byte *cmdbuf, byte sendlen, byte *recvbuf, byte *recvlen) {
  if (sendlen == 0)
    return (MFRC522::STATUS_INVALID);

  byte *message = (byte *)malloc(5 + sendlen);
  if (!message) {
    g_lstatus = LERROR_ERRNO;
    return (MFRC522::STATUS_INTERNAL_ERROR);
  }

  message[0] = 0x90;
  message[1] = cmdbuf[0];
  message[2] = 0x00;
  message[3] = 0x00;

  message[4] = sendlen - 1;
  for (byte i = 0; i < (sendlen - 1); i++) {
    message[5 + i] = cmdbuf[i + 1];
  }
  message[5 + (sendlen - 1)] = 0x00;

  byte output[0xFF];
  byte len = 0xFF;
  MFRC522::StatusCode result = mfrc522->ISODEP_Transceive(message, sendlen + 5, output, len);
  free(message);

  if (result != MFRC522::STATUS_OK) {
    if (result == MFRC522::STATUS_INTERNAL_ERROR)
      g_lstatus = LERROR_TRANSCEIVE_FAILURE;
    return (result);
  }

  if (len < 2) {
    g_lstatus = LERROR_BAD_RESPONSE;
    return (MFRC522::STATUS_INTERNAL_ERROR);
  }

  byte status_7816 = output[len - 2];
  if (status_7816 != 0x91) {
    g_lstatus = status_7816;
    return (MFRC522::STATUS_INTERNAL_ERROR);
  }

  byte desfire_status = output[len - 1];
  g_lstatus = desfire_status;

  if (!(desfire_status == STATUS_OPERATION_OK || desfire_status == STATUS_ADDITIONAL_FRAME)) {
    return (MFRC522::STATUS_INTERNAL_ERROR);
  }

  // Copy back response
  if (recvlen) {
    *recvlen = len - 2;
  }

  if (recvbuf && recvlen && *recvlen != 0) {
    if ((len - 2) > *recvlen) {
      g_lstatus = LERROR_LARGER_RESPONSE;
      return (MFRC522::STATUS_INTERNAL_ERROR);
    }
    else {
      memcpy(recvbuf, output, len - 2);
    }
  }

  return (MFRC522::STATUS_OK);
}

MFRC522::StatusCode get_additional_frame(MFRC522 *mfrc522, byte *recvbuf, byte *recvlen) {
  byte cmd = CMD_GET_ADDITIONAL_FRAME;
  return (send_wrapped_request(mfrc522, &cmd, 1, recvbuf, recvlen));
}

MFRC522::StatusCode select_7816_app(MFRC522 *mfrc522) {
  byte select_cmd[13];
  MFRC522::StatusCode status;
  byte reply[2];
  byte reply_len;

  // If this is a phone, we need to use a 7816 select
  select_cmd[0] = 0x00;
  select_cmd[1] = 0xA4;
  select_cmd[2] = 0x04;
  select_cmd[3] = 0x00;
  // send ISO_APPID_CARD42 to phone
  select_cmd[4] = 7;
  select_cmd[5] = 0xFB;
  select_cmd[6] = 0x43;
  select_cmd[7] = 0x61;
  select_cmd[8] = 0x72;
  select_cmd[9] = 0x64;
  select_cmd[10] = 0x34;
  select_cmd[11] = 0x32;
  select_cmd[12] = 0;
  reply_len = 2;

  status = mfrc522->ISODEP_Transceive(select_cmd, 13, reply, reply_len);
  if (status != MFRC522::STATUS_OK) {
    if (status == MFRC522::STATUS_INTERNAL_ERROR)
      g_lstatus = LERROR_TRANSCEIVE_FAILURE;
    return (status);
  }

  if (reply_len != 2) {
    return (MFRC522::STATUS_INTERNAL_ERROR);
  }

  if (reply[0] == 0x6A && reply[1] == 0x82) {
    // is DESFire card, probably
    // TODO - store in resetted reader state
    g_lstatus = 0x101;
  }
  else if (reply[0] == 0x91 && reply[1] == 0x00) {
//    Serial.println(reply[0], HEX);
//    Serial.println(reply[1], HEX);
    g_lstatus = 0x102;
  }
  else {
    g_lstatus = LERROR_BAD_RESPONSE;
    return (MFRC522::STATUS_INTERNAL_ERROR);
  }

  return (MFRC522::STATUS_OK);
}

MFRC522::StatusCode select_application(MFRC522 *mfrc522, uint32_t app_id) {
  byte select_cmd[4];
  MFRC522::StatusCode status;

  // Actual DESFire select command
  select_cmd[0] = CMD_SELECT_APPLICATION;
  select_cmd[3] = (byte)(app_id & 0xFF);
  select_cmd[2] = (byte)((app_id >> 8) & 0xFF);
  select_cmd[1] = (byte)((app_id >> 16) & 0xFF);

  status = send_wrapped_request(mfrc522, select_cmd, 4, NULL, NULL);
  if (status != MFRC522::STATUS_OK) {
    return (status);
  }

  return (MFRC522::STATUS_OK);
}

MFRC522::StatusCode read_file(MFRC522 *mfrc522, byte file_id, uint32_t offset, byte *dst, byte length) {
  MFRC522::StatusCode status;
  byte read_command[8];

  if (length > 56) {
    return (MFRC522::STATUS_NO_ROOM);
  }

  read_command[0] = CMD_READ_DATA;
  read_command[1] = file_id;
  read_command[2] = (byte)(offset & 0xFF);
  read_command[3] = (byte)((offset >> 8) & 0xFF);
  read_command[4] = (byte)((offset >> 16) & 0xFF);
  read_command[5] = length;
  read_command[6] = 0; // length
  read_command[7] = 0; // length

  byte actual_len = length;
  status = send_wrapped_request(mfrc522, read_command, 8, dst, &actual_len);
  if (status != MFRC522::STATUS_OK) {
    return (status);
  }

  if (length != actual_len) {
    g_lstatus = STATUS_LENGTH_ERROR;
    return (MFRC522::STATUS_INTERNAL_ERROR);
  }

  return (status);
}


