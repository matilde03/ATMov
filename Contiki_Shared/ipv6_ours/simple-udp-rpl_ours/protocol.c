#include "protocol.h"

#include <stdio.h>
#include <string.h>

/*---------------------------------------------------------------------------*/
int protocol_encode(const struct wsn_msg *msg, uint8_t *buf, size_t buf_size)
{
  int n;

  if(msg == NULL || buf == NULL || buf_size == 0) {
    return -1;
  }

  switch(msg->type) {
  case MSG_PING:
    n = snprintf((char *)buf, buf_size, "PING:%u:%lu",
                 (unsigned int)msg->room_id,
                 (unsigned long)msg->seq);
    break;

  case MSG_ALARM:
    n = snprintf((char *)buf, buf_size, "ALARM:%u:%lu:%d:%u:%u",
                 (unsigned int)msg->room_id,
                 (unsigned long)msg->seq,
                 (int)msg->temp,
                 (unsigned int)(msg->high_temp ? 1 : 0),
                 (unsigned int)(msg->presence ? 1 : 0));
    break;

  case MSG_CFG:
    n = snprintf((char *)buf, buf_size, "CFG:%lu:%d:%u",
                 (unsigned long)msg->seq,
                 (int)msg->threshold,
                 (unsigned int)(msg->alarms_enabled ? 1 : 0));
    break;

  default:
    return -1;
  }

  if(n < 0 || (size_t)n >= buf_size) {
    return -1;
  }

  /* Include null terminator for Option A (ASCII text) */
  return n + 1;
}
/*---------------------------------------------------------------------------*/
int protocol_decode(const uint8_t *buf, size_t len, struct wsn_msg *msg)
{
  char tmp[PROTOCOL_MAX_BUF];
  unsigned int room_id;
  unsigned long seq;
  int temp;
  int threshold;
  unsigned int high_temp;
  unsigned int presence;
  unsigned int alarms_enabled;

  if(buf == NULL || len == 0 || len >= sizeof(tmp) || msg == NULL) {
    return -1;
  }

  memcpy(tmp, buf, len);
  tmp[len] = '\0';
  memset(msg, 0, sizeof(*msg));

  if(strncmp(tmp, "PING:", 5) == 0) {
    if(sscanf(tmp + 5, "%u:%lu", &room_id, &seq) == 2) {
      msg->type = MSG_PING;
      msg->room_id = (uint16_t)room_id;
      msg->seq = (uint32_t)seq;
      return 0;
    }
  } else if(strncmp(tmp, "ALARM:", 6) == 0) {
    if(sscanf(tmp + 6, "%u:%lu:%d:%u:%u",
              &room_id, &seq, &temp, &high_temp, &presence) == 5) {
      msg->type = MSG_ALARM;
      msg->room_id = (uint16_t)room_id;
      msg->seq = (uint32_t)seq;
      msg->temp = (int16_t)temp;
      msg->high_temp = (uint8_t)(high_temp ? 1 : 0);
      msg->presence = (uint8_t)(presence ? 1 : 0);
      return 0;
    }
  } else if(strncmp(tmp, "CFG:", 4) == 0) {
    if(sscanf(tmp + 4, "%lu:%d:%u",
              &seq, &threshold, &alarms_enabled) == 3) {
      msg->type = MSG_CFG;
      msg->seq = (uint32_t)seq;
      msg->threshold = (int16_t)threshold;
      msg->alarms_enabled = (uint8_t)(alarms_enabled ? 1 : 0);
      return 0;
    }
  }

  return -1;
}
/*---------------------------------------------------------------------------*/
