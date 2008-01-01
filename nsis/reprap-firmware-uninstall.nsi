; reprap-firmware-uninstall.nsi
;
; Reprap firmware uninstall -- included by reprap-host.nsi
;
; Authors: Bruce Wattendorf and Jonathan Marsden
;
; Date: 2007-12-31
;
  Delete "$INSTDIR\firmware\200 step pic\stepmotor-small.hex"
  Delete "$INSTDIR\firmware\200 step pic\extruder_0_.hex"
  Delete "$INSTDIR\firmware\200 step pic\extruder_1_.hex"
  Delete "$INSTDIR\firmware\200 step pic\iobox.hex"
  Delete "$INSTDIR\firmware\200 step pic\stepmotor.hex"
  Delete "$INSTDIR\firmware\200 step pic\stepmotorb.hex"
  Delete "$INSTDIR\firmware\200 step pic\stepmotorc.hex"
;
  RMDir "$INSTDIR\firmware\200 step pic"
;
; End of reprap-firmware-uninstall.nsi
