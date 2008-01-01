; reprap-java3d.nsi

; Java3D library Files for Reprap -- included by reprap-host.nsi

; Author: Jonathan Marsden

; Date: 2007-12-31

  SetOutPath "$PROGRAMFILES\Java\shared"
  SetOverwrite ifnewer
  File "j3dcore.jar"
  File "j3dutils.jar"
  File "vecmath.jar"
  SetOutPath "$SYSDIR"
  File "j3dcore-d3d.dll"
  File "j3dcore-ogl-cg.dll"
  File "j3dcore-ogl-chk.dll"
  File "j3dcore-ogl.dll"

;
; End of reprap-java3d.nsi
