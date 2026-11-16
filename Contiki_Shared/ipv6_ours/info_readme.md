## VM Password
`user`

## VirtualBox Shared Folder Setup
To keep all paths consistent across machines, configure the VirtualBox shared folder with:
* **Folder Name:** `Contiki_Shared` (mounts automatically inside the VM at `/media/sf_Contiki_Shared/`)
* **Folder Path (Host):** point it to your local `Contiki_Shared` directory containing `ipv6_ours/` so the project is accessible at:
  `/media/sf_Contiki_Shared/ipv6_ours`

## To start "Cooja Simulator" in Contiki ("InstantContiki3.0" virtual machine):
```
cd ~/Desktop/contiki/tools/cooja
ant run
```

## Mote type
Use **cooja mote**. moteist5 is for if we want to program hardware, which we don't want

## Starting Point Firmware (`_our` files)
`simple-udp-rpl_ours/unicast-receiver_our.c` and `simple-udp-rpl_ours/unicast-sender_our.c` are copies of the original `unicast-receiver.c` and `unicast-sender.c` examples, created as our working starting point.

### How they map to the project
1. **`unicast-receiver_our.c` — Base for the Sink Node:**
   * Keep `set_global_address()`, `create_rpl_dag()`, and `servreg_hack_register()`.
   * Expand `receiver()` to parse incoming room alarms and store the last 5 notifications (along with each room's `sender_addr` so the sink can reply to them).
   * Add a serial-line input handler (`serial_line_event_message`) in the main `PROCESS_THREAD` for the console menu (view last 5 notifications, change temperature threshold, enable/disable alarms, and send those updates to rooms via `simple_udp_sendto`).

2. **`unicast-sender_our.c` — Base for the Room Nodes:**
   * Keep `set_global_address()`, `servreg_hack_lookup(SERVICE_ID)`, and `simple_udp_register()`.
   * Use `receiver()` on the room node to receive threshold and alarm-enable/disable commands from the sink.
   * Replace the single periodic `"Message %d"` loop with the room logic (1-second timer for green LED blink, sensor polling/emulation for temperature + red LED and presence + yellow LED, and sending an alarm via `simple_udp_sendto` when enabled and triggered).

## Node Architecture

### Overview
The WSN uses a **star/tree (RPL) topology** with one central sink node and multiple room nodes (with optional multi-hop forwarding through intermediate room nodes):

```text
[room-node-1] ──┐
[room-node-2] ──┼──(UDP / IPv6 + RPL)──▶ [sink-node] ◀──▶ Serial Console (User)
[room-node-3] ──┘         ▲
     ▲                    │ (Threshold & Enable/Disable commands)
     └────────────────────┘
```

### Node Roles & Naming

1. **`sink-node`** (Firmware: `simple-udp-rpl_ours/unicast-receiver_our.c`, 1 node)
   * **Network Role:** RPL DAG Root (`aaaa::/64`) and `servreg-hack` service provider (`SERVICE_ID 190`).
   * **Modules:**
     * **Notification Buffer:** Stores the last **5 alarm notifications** (FIFO/circular buffer) with room ID/address and event details.
     * **Room Registry:** Keeps track of `sender_addr` for each reporting room node so configuration commands can be sent back.
     * **Serial Console (`serial_line_event_message`):** Interactive CLI over Cooja's serial port to:
       * List/consult the last 5 notifications.
       * Set the global (or per-room) temperature threshold.
       * Enable or disable alarm notifications.

2. **`room-node-1`, `room-node-2`, ... `room-node-N`** (Firmware: `simple-udp-rpl_ours/unicast-sender_our.c`, ≥ 2 nodes)
   * **Network Role:** RPL leaf/router nodes discovering `sink-node` via `servreg_hack_lookup(SERVICE_ID)`.
   * **Modules:**
     * **Activity Blinker:** 1-second `etimer` toggling the **Green LED** to signal node activity.
     * **Temperature Monitor:** Reads/emulates temperature; sets **Red LED** ON when above threshold (OFF otherwise).
     * **Presence & Light Control:** Reads/emulates people presence; sets **Yellow LED** ON when presence is detected (OFF otherwise).
     * **Alarm Reporter:** Sends unicast UDP alarm messages to `sink-node` when notifications are enabled and high temperature or presence is triggered.
     * **Config Listener (`receiver`):** Receives threshold and enable/disable commands from `sink-node` on `UDP_PORT` (`1234`).

3. **`relay-node`** *(Optional — for multi-hop testing)*
   * **Role:** An intermediate `room-node` (or forwarding-only node) placed between `sink-node` and a distant `room-node` outside direct radio range to demonstrate RPL multi-hop routing.

---

## TODO List

### Receiver — `sink-node` (`unicast-receiver_our.c`)
- [ ] **Define message format / protocol:** Agree on a shared struct or text format for alarms (room ID, alarm type, values) and config commands (threshold, enable/disable).
- [ ] **Notification buffer (last 5):** Store the last 5 received alarm notifications in a circular/FIFO buffer inside `receiver()`.
- [ ] **Room node registry:** Save each reporting room's `sender_addr` so the sink can send configuration updates back.
- [ ] **Serial console (`serial_line_event_message`):** Add interactive commands on the sink's serial port to:
  - [ ] Print/consult the last 5 notifications.
  - [ ] Modify the temperature threshold and push it to registered room nodes (`simple_udp_sendto`).
  - [ ] Enable/disable alarm notifications and push the state to registered room nodes.
- [ ] *(Optional — basic security)* Filter incoming alarms in `receiver()` against an allowlist of predefined room IPv6 addresses (`uip_ipaddr_cmp`) or node IDs.

### Sender — `room-node` (`unicast-sender_our.c`)
- [ ] **Activity indicator:** Add a 1-second `etimer` to toggle the **Green LED** continuously.
- [ ] **Temperature monitoring:** Read/emulate room temperature and turn the **Red LED** ON when above the threshold (OFF otherwise).
- [ ] **Presence & light control:** Read/emulate people presence and turn the **Yellow LED** ON when people are detected (OFF otherwise).
- [ ] **Alarm reporting:** When alarm notifications are enabled and high temperature or presence is triggered, send an alarm packet to the sink via `simple_udp_sendto`.
- [ ] **Config listener:** Parse incoming threshold and enable/disable commands from the sink inside `receiver()`.
- [ ] *(Optional — basic security)* In `receiver()`, verify `uip_ipaddr_cmp(sender_addr, sink_addr)` (using the address from `servreg_hack_lookup`) before applying config updates.


