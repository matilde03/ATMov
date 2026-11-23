#ifndef PROTOCOL_H_
#define PROTOCOL_H_

#include <stddef.h>
#include <stdint.h>

#define PROTOCOL_MAX_BUF 64

enum msg_type {
  MSG_PING = 1,
  MSG_ALARM = 2,
  MSG_CFG = 3
};

struct wsn_msg {
  uint8_t type;
  uint16_t room_id;
  uint32_t seq;
  int16_t temp;
  int16_t threshold;
  uint8_t high_temp;
  uint8_t presence;
  uint8_t alarms_enabled;
};

/* Encode msg into buf. Returns number of bytes to send (>0), or -1 on error. */
int protocol_encode(const struct wsn_msg *msg, uint8_t *buf, size_t buf_size);

/* Decode buf of length len into msg. Returns 0 on success, or -1 on error. */
int protocol_decode(const uint8_t *buf, size_t len, struct wsn_msg *msg);

#endif /* PROTOCOL_H_ */
