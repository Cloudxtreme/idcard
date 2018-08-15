#include <errno.h>
#include "DESFireCard.h"

uint16_t g_lerror = 0;

static byte *wrap_message(byte command, byte *sendbuf, byte sendlen) {
  size_t len = (sendbuf && sendlen) ? sendlen : 0;
  byte *message = (byte *)malloc(6 + len);

  if (message) {
    message[0] = 0x90;
    message[1] = command;
    message[2] = 0x00;
    message[3] = 0x00;

    if (sendbuf && sendlen) {
      message[4] = sendlen;
      for (byte i = 0; i < sendlen; i++) {
        message[5 + i] = sendbuf[i];
      }
    }
    message[5 + sendlen] = 0x00;
  }

  return (message);
}

MFRC522::StatusCode send_request(MFRC522 *mfrc522, byte command, byte *sendbuf, byte sendlen, byte **recvbuf, unsigned int *recvlen) {
  byte *message = wrap_message(command, sendbuf, sendlen);
  sendlen = (sendbuf && sendlen) ? sendlen : 0;
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

  MFRC522::StatusCode result = mfrc522->PCD_TransceiveData(message, 6 + sendlen, output, &len);
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

    if (recvbuf && recvlen && len - 2) {
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

    message = wrap_message(CMD_GET_ADDITIONAL_FRAME, NULL, 0);
    if (!message) {
      if (recvbuf && recvlen && *recvbuf)
        free(*recvbuf);

      g_lerror = LERROR_ERRNO;
      return (MFRC522::STATUS_INTERNAL_ERROR);
    }

    byte len = 0xFF;
    result = mfrc522->PCD_TransceiveData(message, 6, output, &len);
    free(message);
  }

  if (recvbuf && recvlen)
    *recvlen = offset;
  return (MFRC522::STATUS_OK);
}

MFRC522::StatusCode send_partial_request(MFRC522 *mfrc522, byte command, byte *sendbuf, byte sendlen, byte **recvbuf, byte *recvlen) {
  byte *message = wrap_message(command, sendbuf, sendlen);
  sendlen = (sendbuf && sendlen) ? sendlen : 0;
  if (!message) {
    g_lerror = LERROR_ERRNO;
    return (MFRC522::STATUS_INTERNAL_ERROR);
  }

  if (recvbuf && recvlen) {
    *recvbuf = NULL;
    *recvlen = 0;
  }

  byte output[0xFF];
  byte len = 0xFF;
  MFRC522::StatusCode result = mfrc522->PCD_TransceiveData(message, 6 + sendlen, output, &len);
  free(message);

  if (result != MFRC522::STATUS_OK)
    return (result);

  if (output[len - 2] != (byte) 0x91) {
    g_lerror = LERROR_BAD_RESPONSE;
    return (MFRC522::STATUS_INTERNAL_ERROR);
  }

  byte status = output[len - 1];

  if (status == STATUS_OPERATION_OK || status == STATUS_ADDITIONAL_FRAME) {
    if (recvbuf && recvlen) {
      *recvbuf = (byte *)malloc(len - 2);
      if (!(*recvbuf)) {
        g_lerror = LERROR_ERRNO;
        return (MFRC522::STATUS_INTERNAL_ERROR);
      }

      memcpy(*recvbuf, output, len - 2);
      *recvlen = len - 2;
    }

    return (MFRC522::STATUS_OK);
  }

  g_lerror = status;
  return (MFRC522::STATUS_INTERNAL_ERROR);
}

MFRC522::StatusCode select_application(MFRC522 *mfrc522, uint32_t appID) {
  byte id_frame[3];
  id_frame[2] = (byte)(appID & 0xFF);
  id_frame[1] = (byte)((appID >> 8) & 0xFF);
  id_frame[0] = (byte)((appID >> 16) & 0xFF);
  return (send_request(mfrc522, CMD_SELECT_APPLICATION, id_frame, 3, NULL, NULL));
}

