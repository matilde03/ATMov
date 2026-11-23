# Design Decisions

## 1. Protocol Abstraction (`protocol.h` & `protocol.c`)
To keep the application logic decoupled from the wire format, both the sink and room nodes will share a common protocol module:

* **`protocol.h`** — Defines the message types (`MSG_PING`, `MSG_ALARM`, `MSG_CFG`), the in-memory `struct wsn_msg`, and the encode/decode API:
  * `int protocol_encode(const struct wsn_msg *msg, uint8_t *buf, size_t buf_size);`
  * `int protocol_decode(const uint8_t *buf, size_t len, struct wsn_msg *msg);`
* **`protocol.c`** — Implements `protocol_encode()` and `protocol_decode()`.
  * **Current choice:** **Option A (Delimited ASCII Text)** for human-readable debugging in Cooja's Log Listener and Radio Messages view.
  * **Future switch to Option B (Binary Struct):** Only the encode/decode functions inside `protocol.c` need to change; `unicast-receiver_our.c` and `unicast-sender_our.c` stay untouched.
* **`Makefile`** — Will include `PROJECT_SOURCEFILES += protocol.c` so Contiki automatically compiles and links `protocol.c` for both motes.

---

## 2. Message Types & Current Packet Format (Option A — Text)

Every packet includes a monotonically increasing **sequence number (`seq`)** per sender. Putting `seq` in *all* room-to-sink packets (both `PING` and `ALARM`) allows the sink to track exact packet loss per room (`expected_seq = last_seq + 1`) rather than only guessing from periodic pings. Including `seq` on `CFG` also allows room nodes to ignore out-of-order or duplicate config updates if retransmissions are added later.

| Direction | Type | Wire Format (ASCII) | Example | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| Room $\rightarrow$ Sink | `PING` | `PING:<room_id>:<seq>` | `PING:2:1` | Registers room IPv6 address at sink & acts as a heartbeat / packet-loss probe. |
| Room $\rightarrow$ Sink | `ALARM` | `ALARM:<room_id>:<seq>:<temp>:<high_temp>:<presence>` | `ALARM:2:2:29:1:1` | Reports high-temperature (`0`/`1`) and/or people presence (`0`/`1`) along with current temperature. |
| Sink $\rightarrow$ Room | `CFG` | `CFG:<seq>:<threshold>:<alarms_enabled>` | `CFG:1:25:1` | Pushes both the temperature threshold and alarm-notification enable state (`0`/`1`) in one stateless packet. |

---

## 3. Planned Project Files (`simple-udp-rpl_ours/`)

* `unicast-receiver_our.c` — Sink node (`sink-node`): RPL DAG root, room registry, 5-notification buffer, packet-loss counter (future), and serial console.
* `unicast-sender_our.c` — Room nodes (`room-node-1..N`): LEDs (green activity, red temp, yellow presence), emulated sensors, `PING` & `ALARM` sender, and `CFG` listener.
* `protocol.h` / `protocol.c` — Shared message struct and encode/decode layer (currently ASCII, swappable to binary).
* `Makefile` — Build configuration pointing to `/home/user/Desktop/contiki` and linking `protocol.c`.
* `project_2_sim1.csc` — Cooja simulation setup.

---

## 4. Sink Node LEDs (`sink-node`)

* **Green LED (`LEDS_GREEN`):** Toggles every **1 second** via an `etimer` to indicate the sink node is active.
* **Red LED (`LEDS_RED`):** **Latched packet-loss indicator** — turns **ON** whenever a sequence gap (`seq > last_seq + 1`) is detected from any room node, and turns **OFF** when cleared/acknowledged via the serial console.
* **Yellow LED (`LEDS_YELLOW`):** **Unread alarm indicator** — turns **ON** when a new `ALARM` notification arrives, and turns **OFF** when the user consults the notifications on the serial console.

---

## 5. Sink Serial Console & 5-Notification Buffer (`sink-node`)

### Serial Commands
`thresh`, `alarm`, and `status` require a mandatory `<room|*>` target argument (`*` applies to all registered rooms):

| Command | Example | Action |
| :--- | :--- | :--- |
| `help` | `help` | Prints all available commands. |
| `alarms` | `alarms` | Prints the **last 5 alarm notifications** (oldest $\rightarrow$ newest) and turns **`LEDS_YELLOW` OFF**. |
| `rooms` | `rooms` | Lists all registered rooms (`room_id`, IPv6 address, `last_seq`, `lost_packets`) and total packet loss. |
| `thresh <room\|*> <val>` | `thresh 2 28` / `thresh * 28` | Sets the temperature threshold (°C) for one room or all rooms (`*`), increments `cfg_seq`, and sends `MSG_CFG`. |
| `alarm <room\|*> <on\|off>` | `alarm 2 off` / `alarm * on` | Enables (`on`/`1`) or disables (`off`/`0`) alarm notifications for one room or all rooms (`*`), increments `cfg_seq`, and sends `MSG_CFG`. |
| `status <room\|*>` | `status 2` / `status *` | Shows current `threshold`, `alarms_enabled`, and packet-loss stats for one room or all rooms (`*`). |
| `clear` | `clear` | Resets packet-loss counters and turns **`LEDS_RED` OFF**. |

### 5-Notification Buffer & Per-Room Config
* **Circular FIFO Buffer (`alarm_buf[5]`):** Stores the last 5 received `MSG_ALARM` notifications (`room_id`, `seq`, `timestamp`, `temp`, `high_temp`, `presence`), overwriting the oldest entry when full.
* **Per-Room State:** Each registered `room_entry` tracks its own `threshold` (default `25`) and `alarms_enabled` (default `1`) so individual rooms or all rooms (`*`) can be configured independently.


