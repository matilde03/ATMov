/*
 * Copyright (c) 2011, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of the Contiki operating system.
 *
 */

#include "contiki.h"
#include "lib/random.h"
#include "sys/ctimer.h"
#include "sys/etimer.h"
#include "net/ip/uip.h"
#include "net/ipv6/uip-ds6.h"
#include "net/ip/uip-debug.h"

#include "simple-udp.h"
#include "servreg-hack.h"
#include "dev/leds.h"
#include "dev/serial-line.h"
#include "protocol.h"

#include "net/rpl/rpl.h"

#include <stdio.h>
#include <string.h>

#define UDP_PORT 1234
#define SERVICE_ID 190

#define MAX_ROOMS 16

struct room_entry {
  uint8_t used;
  uint16_t room_id;
  uip_ipaddr_t addr;
  uint8_t has_seq;
  uint32_t last_seq;
  uint32_t lost_packets;
};

static struct simple_udp_connection unicast_connection;
static struct room_entry rooms[MAX_ROOMS];
static uint32_t total_lost_packets = 0;

/*---------------------------------------------------------------------------*/
PROCESS(sink_net_process, "Sink network process");
PROCESS(sink_led_process, "Sink activity LED process");
PROCESS(sink_console_process, "Sink serial console process");
AUTOSTART_PROCESSES(&sink_net_process, &sink_led_process, &sink_console_process);
/*---------------------------------------------------------------------------*/
static struct room_entry *find_or_add_room(uint16_t room_id, const uip_ipaddr_t *addr)
{
  int i;
  int free_idx = -1;

  for(i = 0; i < MAX_ROOMS; i++) {
    if(rooms[i].used && rooms[i].room_id == room_id) {
      uip_ipaddr_copy(&rooms[i].addr, addr);
      return &rooms[i];
    }
    if(!rooms[i].used && free_idx < 0) {
      free_idx = i;
    }
  }

  if(free_idx >= 0) {
    memset(&rooms[free_idx], 0, sizeof(rooms[free_idx]));
    rooms[free_idx].used = 1;
    rooms[free_idx].room_id = room_id;
    uip_ipaddr_copy(&rooms[free_idx].addr, addr);
    return &rooms[free_idx];
  }

  return NULL;
}
/*---------------------------------------------------------------------------*/
static void track_packet_loss(struct room_entry *room, uint32_t seq)
{
  if(room == NULL) {
    return;
  }

  if(!room->has_seq) {
    room->has_seq = 1;
    room->last_seq = seq;
    if(seq > 1) {
      uint32_t lost = seq - 1;
      room->lost_packets += lost;
      total_lost_packets += lost;
      leds_on(LEDS_RED);
      printf("Packet loss detected from room %u: missed %lu packet(s) before seq %lu\n",
             (unsigned int)room->room_id, (unsigned long)lost, (unsigned long)seq);
    }
    return;
  }

  if(seq > room->last_seq + 1) {
    uint32_t lost = seq - (room->last_seq + 1);
    room->lost_packets += lost;
    total_lost_packets += lost;
    leds_on(LEDS_RED);
    printf("Packet loss detected from room %u: expected seq %lu, got %lu (lost %lu)\n",
           (unsigned int)room->room_id,
           (unsigned long)(room->last_seq + 1),
           (unsigned long)seq,
           (unsigned long)lost);
  }

  if(seq > room->last_seq) {
    room->last_seq = seq;
  }
}
/*---------------------------------------------------------------------------*/
static void receiver(struct simple_udp_connection *c,
         const uip_ipaddr_t *sender_addr,
         uint16_t sender_port,
         const uip_ipaddr_t *receiver_addr,
         uint16_t receiver_port,
         const uint8_t *data,
         uint16_t datalen)
{
  struct wsn_msg msg;
  struct room_entry *room;

  if(protocol_decode(data, datalen, &msg) < 0) {
    printf("Invalid packet from ");
    uip_debug_ipaddr_print(sender_addr);
    printf(" (len %d)\n", datalen);
    return;
  }

  if(msg.type == MSG_PING) {
    room = find_or_add_room(msg.room_id, sender_addr);
    track_packet_loss(room, msg.seq);
    printf("PING from room %u (seq %lu) [",
           (unsigned int)msg.room_id, (unsigned long)msg.seq);
    uip_debug_ipaddr_print(sender_addr);
    printf("]\n");
  } else if(msg.type == MSG_ALARM) {
    room = find_or_add_room(msg.room_id, sender_addr);
    track_packet_loss(room, msg.seq);
    leds_on(LEDS_YELLOW);
    printf("ALARM from room %u (seq %lu): temp=%d, high_temp=%u, presence=%u\n",
           (unsigned int)msg.room_id, (unsigned long)msg.seq,
           (int)msg.temp, (unsigned int)msg.high_temp,
           (unsigned int)msg.presence);
  }
}
/*---------------------------------------------------------------------------*/
static uip_ipaddr_t * set_global_address(void)
{
  static uip_ipaddr_t ipaddr;
  int i;
  uint8_t state;

  uip_ip6addr(&ipaddr, 0xaaaa, 0, 0, 0, 0, 0, 0, 0);
  uip_ds6_set_addr_iid(&ipaddr, &uip_lladdr);
  uip_ds6_addr_add(&ipaddr, 0, ADDR_AUTOCONF);

  printf("IPv6 addresses: ");
  for(i = 0; i < UIP_DS6_ADDR_NB; i++) {
    state = uip_ds6_if.addr_list[i].state;
    if(uip_ds6_if.addr_list[i].isused &&
       (state == ADDR_TENTATIVE || state == ADDR_PREFERRED)) {
      uip_debug_ipaddr_print(&uip_ds6_if.addr_list[i].ipaddr);
      printf("\n");
    }
  }

  return &ipaddr;
}
/*---------------------------------------------------------------------------*/
static void create_rpl_dag(uip_ipaddr_t *ipaddr)
{
  struct uip_ds6_addr *root_if;

  root_if = uip_ds6_addr_lookup(ipaddr);
  if(root_if != NULL) {
    rpl_dag_t *dag;
    uip_ipaddr_t prefix;
    
    rpl_set_root(RPL_DEFAULT_INSTANCE, ipaddr);
    dag = rpl_get_any_dag();
    uip_ip6addr(&prefix, 0xaaaa, 0, 0, 0, 0, 0, 0, 0);
    rpl_set_prefix(dag, &prefix, 64);
    PRINTF("created a new RPL dag\n");
  } else {
    PRINTF("failed to create a new RPL DAG\n");
  }
}
/*---------------------------------------------------------------------------*/
static int parse_room_target(const char *arg, uint8_t *all_rooms, uint16_t *room_id)
{
  unsigned int id;
  char extra;

  if(arg == NULL || all_rooms == NULL || room_id == NULL) {
    return -1;
  }

  if(strcmp(arg, "*") == 0) {
    *all_rooms = 1;
    *room_id = 0;
    return 0;
  }

  if(sscanf(arg, "%u%c", &id, &extra) == 1) {
    *all_rooms = 0;
    *room_id = (uint16_t)id;
    return 0;
  }

  return -1;
}
/*---------------------------------------------------------------------------*/
static void cmd_help(void)
{
  printf("Available commands:\n");
  printf("  help                      - Show this help message\n");
  printf("  alarms                    - Show last 5 alarm notifications (clears Yellow LED)\n");
  printf("  rooms                     - List registered rooms and packet-loss stats\n");
  printf("  thresh <room|*> <val>     - Set temperature threshold (C) for room or all (*)\n");
  printf("  alarm <room|*> <on|off>   - Enable/disable alarm notifications for room or all (*)\n");
  printf("  status <room|*>           - Show config & stats for room or all (*)\n");
  printf("  clear                     - Reset packet-loss counters (clears Red LED)\n");
}
/*---------------------------------------------------------------------------*/
static void cmd_alarms(void)
{
  printf("[TODO] alarms: not implemented yet\n");
}
/*---------------------------------------------------------------------------*/
static void cmd_rooms(void)
{
  printf("[TODO] rooms: not implemented yet\n");
}
/*---------------------------------------------------------------------------*/
static void cmd_thresh(const char *target_arg, const char *val_arg)
{
  uint8_t all_rooms;
  uint16_t room_id;
  int val;
  char extra;

  if(parse_room_target(target_arg, &all_rooms, &room_id) < 0 ||
     sscanf(val_arg, "%d%c", &val, &extra) != 1) {
    printf("Usage: thresh <room|*> <val>\n");
    return;
  }

  if(all_rooms) {
    printf("[TODO] thresh: set all rooms (*) to %d C\n", val);
  } else {
    printf("[TODO] thresh: set room %u to %d C\n", (unsigned int)room_id, val);
  }
}
/*---------------------------------------------------------------------------*/
static void cmd_alarm(const char *target_arg, const char *state_arg)
{
  uint8_t all_rooms;
  uint16_t room_id;
  uint8_t enabled;

  if(parse_room_target(target_arg, &all_rooms, &room_id) < 0) {
    printf("Usage: alarm <room|*> <on|off>\n");
    return;
  }

  if(strcmp(state_arg, "on") == 0 || strcmp(state_arg, "1") == 0) {
    enabled = 1;
  } else if(strcmp(state_arg, "off") == 0 || strcmp(state_arg, "0") == 0) {
    enabled = 0;
  } else {
    printf("Usage: alarm <room|*> <on|off>\n");
    return;
  }

  if(all_rooms) {
    printf("[TODO] alarm: set all rooms (*) to %s\n", enabled ? "ON" : "OFF");
  } else {
    printf("[TODO] alarm: set room %u to %s\n",
           (unsigned int)room_id, enabled ? "ON" : "OFF");
  }
}
/*---------------------------------------------------------------------------*/
static void cmd_status(const char *target_arg)
{
  uint8_t all_rooms;
  uint16_t room_id;

  if(parse_room_target(target_arg, &all_rooms, &room_id) < 0) {
    printf("Usage: status <room|*>\n");
    return;
  }

  if(all_rooms) {
    printf("[TODO] status: show all rooms (*)\n");
  } else {
    printf("[TODO] status: show room %u\n", (unsigned int)room_id);
  }
}
/*---------------------------------------------------------------------------*/
static void cmd_clear(void)
{
  int i;
  total_lost_packets = 0;
  for(i = 0; i < MAX_ROOMS; i++) {
    rooms[i].lost_packets = 0;
  }
  leds_off(LEDS_RED);
  printf("Cleared packet loss counters and Red LED\n");
}
/*---------------------------------------------------------------------------*/
static void handle_serial_command(const char *line)
{
  char verb[16];
  char arg1[16];
  char arg2[16];
  int argc;

  argc = sscanf(line, "%15s %15s %15s", verb, arg1, arg2);
  if(argc <= 0) {
    return;
  }

  if(strcmp(verb, "help") == 0) {
    cmd_help();
  } else if(strcmp(verb, "alarms") == 0) {
    cmd_alarms();
  } else if(strcmp(verb, "rooms") == 0) {
    cmd_rooms();
  } else if(strcmp(verb, "thresh") == 0) {
    if(argc != 3) {
      printf("Usage: thresh <room|*> <val>\n");
    } else {
      cmd_thresh(arg1, arg2);
    }
  } else if(strcmp(verb, "alarm") == 0) {
    if(argc != 3) {
      printf("Usage: alarm <room|*> <on|off>\n");
    } else {
      cmd_alarm(arg1, arg2);
    }
  } else if(strcmp(verb, "status") == 0) {
    if(argc != 2) {
      printf("Usage: status <room|*>\n");
    } else {
      cmd_status(arg1);
    }
  } else if(strcmp(verb, "clear") == 0) {
    cmd_clear();
  } else {
    printf("Unknown command '%s'. Type 'help' for commands.\n", verb);
  }
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(sink_net_process, ev, data)
{
  uip_ipaddr_t *ipaddr;

  PROCESS_BEGIN();

  servreg_hack_init();

  ipaddr = set_global_address();

  create_rpl_dag(ipaddr);

  servreg_hack_register(SERVICE_ID, ipaddr);

  simple_udp_register(&unicast_connection, UDP_PORT,
                      NULL, UDP_PORT, receiver);

  while(1) {
    PROCESS_WAIT_EVENT();
  }

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(sink_led_process, ev, data)
{
  static struct etimer led_timer;

  PROCESS_BEGIN();

  etimer_set(&led_timer, CLOCK_SECOND);

  while(1) {
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&led_timer));
    leds_toggle(LEDS_GREEN);
    etimer_reset(&led_timer);
  }

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(sink_console_process, ev, data)
{
  PROCESS_BEGIN();

  printf("Sink console ready. Type 'help' for commands.\n");

  while(1) {
    PROCESS_WAIT_EVENT_UNTIL(ev == serial_line_event_message && data != NULL);
    handle_serial_command((const char *)data);
  }

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
