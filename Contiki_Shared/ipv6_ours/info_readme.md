## VM Password
`user`

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
