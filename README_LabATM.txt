
Some information about ATM development environments
===================================================


User account in Lab PCs (Windows):
----------------------------------

user: ATM
password: atm



Virtual machines installed:
---------------------------
*** (NOTE: This year (2026/2027) the virtual machine XubuntosVM is not installed. If desired it can be provided on request.) ***


XubuntosVM (user: "user"; password: "password")
	--- for TinyOS (micaz, moteist);
	--- for FreeRTOS (moteist)

InstantContiki3.0 (user: "user"; password: "user")
	--- for Contiki (micaz, moteist);
	--- for Contiki (Cooja Simulator);
	--- can also be used to access mbed site using Firefox browser



Execution environment
---------------------

Open a terminal (command line interface).

If the virtual machine was shutdown, you may need to redefine environment variables:

  setxkbmap pt                 --- portuguese keyboard


Make sure that the devices you are connecting (micaz, moteist, mbed) are connected to the virtual machine and not to the host (check in removable devices), and you have read/write permission. If needed do:
  connect (in removable devices)
  sudo chmod 666 /dev/ttyUSB0

(if you have several devices the number may be different from 0 -- e.g. ttyUSB1)



========================
Examples of utilization:
========================

==========
--- Application "Blink" in TinyOS ("XubuntosVM" virtual machine):

  cd ~/Desktop/tinyos-2.x/apps/Blink
  

-- For "micaz" (compilation and programming):

  make micaz install mib520,dev/ttyUSB0  --- using mib520 programmer
or
  make micaz install mib510,dev/ttyUSB0  --- using mib510 programmer (with USB/serial converter)
or
  make micaz install mib510,dev/ttyS0	 --- using mib510 programmer (with normal serial cable)


-- For "moteist":

compilation:
  make moteist5

programming (if there is a timeout you may need to reset moteist, because after a few seconds any previous existing application is started):
  java -jar ~/Desktop/moteist/distv2/MRM2.jar program --fp=build/moteist5/main.ihex --s=512 --p=/dev/ttyUSB0


==========
--- Application "Blink" in Contiki ("InstantContiki3.0" virtual machine):

  cd ~/contiki-3.0/examples/moteist5/blink


-- For "moteist":

compilation:
  make TARGET=moteist5

generate hex file:
  msp430-objcopy blink.moteist5 -O ihex blink.hex

programming (if there is a timeout you may need to reset moteist, because after a few seconds any previous existing application is started):
  java -jar ~/Desktop/distv2/MRM2.jar program --fp=blink.hex --s=512 --p=/dev/ttyUSB0


-- For "micaz" (compilation and programming):

  make TARGET=micaz blink.upload PORT=/dev/ttyUSB0  --- using mib520 programmer or mib510 programmer with USB/serial converter
or
  make TARGET=micaz blink.upload PORT=/dev/ttyS0    --- using mib510 programmer with normal serial cable


==========
--- To start "Cooja Simulator" in Contiki ("InstantContiki3.0" virtual machine):

  cd ~/Desktop/contiki/tools/cooja
  ant run

