#include <errno.h>
#include "DESFireCard.h"

uint16_t g_lerror = 0;

static byte *wrap_message(byte command, byte *sendbuf, byte sendlen, byte *msglen) {
  size_t len = (sendbuf && sendlen) ? sendlen : 0;
  byte *message = (byte *)malloc(1 + len);

  if (message) {
    /*
    message[0] = 0x90;
    message[1] = command;
    message[2] = 0x00;
    message[3] = 0x00;

    message[4] = len;
    */
    message[0] = command;
    for (byte i = 0; i < len; i++) {
      message[1 + i] = sendbuf[i];
    }
    // message[5 + len] = 0x00;
    
  }

  if (msglen)
    *msglen = 1 + len;

  return (message);
}
/*
MFRC522::StatusCode send_request(MFRC522 *mfrc522, byte command, byte *sendbuf, byte sendlen, byte **recvbuf, unsigned int *recvlen) {
  byte msglen;
  byte *message = wrap_message(command, sendbuf, sendlen, &msglen);
  if (!message) {
    g_lerror = LERROR_ERRNO;
    return (MFRC522::STATUS_INTERNAL_ERROR);
  }

  if (recvbuf && recvlen) {
    *recvbuf = NULL;
    *recvlen = 0;
  }

  unsigned int offset = 0;
  byte output[0xFF];
  byte len = 0xFF;

  MFRC522::StatusCode result = mfrc522->PCD_TransceiveData(message, msglen, output, &len);
  free(message);

  while (true) {
    if (result != MFRC522::STATUS_OK)
      return (result);

    if (len < 2 || output[offset + len - 2] != (byte) 0x91) {
      if (recvbuf && recvlen && *recvbuf)
        free(*recvbuf);

      g_lerror = LERROR_BAD_RESPONSE;
      return (MFRC522::STATUS_INTERNAL_ERROR);
    }

    byte status = output[offset + len - 1];

    if (recvbuf && recvlen && (len - 2)) {
      if (*recvbuf) {
        byte *new_ptr = (byte *)realloc(*recvbuf, offset + len - 2);

        if (!new_ptr) {
          int temp_errno = errno;
          free(*recvbuf);
          errno = temp_errno;
        }

        *recvbuf = new_ptr;
      }
      else
        *recvbuf = (byte *)malloc(offset + len - 2);

      if (!(*recvbuf)) {
        g_lerror = LERROR_ERRNO;
        return (MFRC522::STATUS_INTERNAL_ERROR);
      }

      memcpy(*recvbuf + offset, output, len - 2);
      offset += len - 2;
    }

    if (status == STATUS_OPERATION_OK)
      break;
    if (status != STATUS_ADDITIONAL_FRAME) {
      if (recvbuf && recvlen && *recvbuf)
        free(*recvbuf);

      g_lerror = status;
      return (MFRC522::STATUS_INTERNAL_ERROR);
    }

    message = wrap_message(CMD_GET_ADDITIONAL_FRAME, NULL, 0, &msglen);
    if (!message) {
      if (recvbuf && recvlen && *recvbuf)
        free(*recvbuf);

      g_lerror = LERROR_ERRNO;
      return (MFRC522::STATUS_INTERNAL_ERROR);
    }

    len = 0xFF;
    result = mfrc522->PCD_TransceiveData(message, msglen, output, &len);
    free(message);
  }

  if (recvbuf && recvlen)
    *recvlen = offset;
  return (MFRC522::STATUS_OK);
}
*/
MFRC522::StatusCode send_partial_request(MFRC522 *mfrc522, byte *cmdbuf, byte sendlen, byte *recvbuf, byte *recvlen) {
  byte output[0xFF];
  byte len = 0xFF;
  MFRC522::StatusCode result = mfrc522->ISODEP_Transceive(cmdbuf, sendlen, output, len);

  if (result != MFRC522::STATUS_OK)
    return (result);

  byte status = output[0];

  if (status == STATUS_OPERATION_OK || status == STATUS_ADDITIONAL_FRAME) {
    if (recvbuf && recvlen && *recvlen != 0) {
      if ((len - 1) > *recvlen) {
        memcpy(recvbuf, output + 1, *recvlen);
      }
      else {
        memcpy(recvbuf, output + 1, len - 1);
      }
    }
    if (recvlen) {
      *recvlen = len - 1;
    }

    return (MFRC522::STATUS_OK);
  }

  g_lerror = status;
  return (MFRC522::STATUS_INTERNAL_ERROR);
}

MFRC522::StatusCode select_application(MFRC522 *mfrc522, uint32_t app_id) {
  byte select_cmd[4];
  select_cmd[3] = (byte)(app_id & 0xFF);
  select_cmd[2] = (byte)((app_id >> 8) & 0xFF);
  select_cmd[1] = (byte)((app_id >> 16) & 0xFF);
  select_cmd[0] = CMD_SELECT_APPLICATION;
  return (send_partial_request(mfrc522, select_cmd, 4, NULL, NULL));
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
  status = send_partial_request(mfrc522, read_command, 8, dst, &actual_len);
  if (status != MFRC522::STATUS_OK)
    return (status);
  if (length != actual_len)
    return (static_cast<MFRC522::StatusCode>(0x7E));
  return (status);
}


