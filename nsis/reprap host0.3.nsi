; Reprap Host Installer script created by the HM NIS Edit Script Wizard (modified by hand after that)

; Date: 2007-12-31

; HM NIS Edit Wizard helper defines
!define PRODUCT_NAME "Reprap Host"
!define PRODUCT_VERSION "0.8.2-20071231"
!define PRODUCT_PUBLISHER "Bruce Wattendorf and Jonathan Marsden"
!define PRODUCT_WEB_SITE "http://www.reprap.org"
!define PRODUCT_UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}"
!define PRODUCT_UNINST_ROOT_KEY "HKLM"

; MUI 1.67 compatible ------
!include "MUI.nsh"

; MUI Settings
!define MUI_ABORTWARNING
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\modern-install.ico"
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\modern-uninstall.ico"

; Welcome page
!insertmacro MUI_PAGE_WELCOME
; License page
!insertmacro MUI_PAGE_LICENSE "$INSTDIR\reprap-host\LICENSE"
; Directory page
!insertmacro MUI_PAGE_DIRECTORY
; Instfiles page
!insertmacro MUI_PAGE_INSTFILES
; Finish page
!insertmacro MUI_PAGE_FINISH

; Uninstaller pages
!insertmacro MUI_UNPAGE_INSTFILES

; Language files
!insertmacro MUI_LANGUAGE "English"

; MUI end ------

Name "${PRODUCT_NAME} ${PRODUCT_VERSION}"
OutFile "reprap-installer-${PRODUCT_VERSION}.exe"
InstallDir "$PROGRAMFILES\Reprap"
ShowInstDetails show
ShowUnInstDetails show

Section "MainSection" SEC01
  SetOutPath "$INSTDIR"
  SetOverwrite ifnewer
  File "reprap-host\Reprap.jar"
  File "reprap-host\README"
  File "reprap-host\LICENSE"
  File "reprap-host\reprap-host.bat"
  CreateShortCut "$DESKTOP.lnk" "$INSTDIR\reprap-host.bat"
  File "reprap-host\reprap-wv.stl"

; Include reprap-stls.nsi here  if desired -- omitted during testing.  JM 20071231
; Note: Corresponding !include in uninstall section should match this one.
; !include "reprap-stls.nsi"

; Include this if bundling firmware.  Omitted for initial testing.  JM 2007-12-31.
; Note: Corresponding !include in uninstall section should match this one.
; !include "reprap-firmware.nsi"


  Call JVM
  Call Java3D
  SetOutPath "$PROGRAMFILES\Java\jre1.6.0_03\lib\ext"
  File "j3d-org-java3d-all.jar"
  File "RXTXcomm.jar"
  SetOutPath "$PROGRAMFILES\Java\jre1.6.0_03\bin"
  File "rxtxParallel.dll"
  File "rxtxSerial.dll"

SectionEnd

Section -AdditionalIcons
  SetOutPath $INSTDIR
  WriteIniStr "$INSTDIR\${PRODUCT_NAME}.url" "InternetShortcut" "URL" "${PRODUCT_WEB_SITE}"
  CreateDirectory "$SMPROGRAMS\Reprap "
  CreateShortCut "$SMPROGRAMS\Reprap \Website.lnk" "$INSTDIR\${PRODUCT_NAME}.url"
  CreateShortCut "$SMPROGRAMS\Reprap \Uninstall.lnk" "$INSTDIR\uninst.exe"
SectionEnd

Section -Post
  WriteUninstaller "$INSTDIR\uninst.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayName" "$(^Name)"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "UninstallString" "$INSTDIR\uninst.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayVersion" "${PRODUCT_VERSION}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "URLInfoAbout" "${PRODUCT_WEB_SITE}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "Publisher" "${PRODUCT_PUBLISHER}"
SectionEnd

; This is the install script for Java 6 jre
Function JVM
  SetOutPath "$PROGRAMFILES\Temp"
      IfFileExists $PROGRAMFILES\Java\jre1.6.0_03\README.txt endNtBackup beginNtBackup
    Goto endNtBackup
    beginNtBackup:
    MessageBox MB_OK "Your system does not appear to have Java 6 JRE installed.$\n$\nPress Ok to install it."
    File "jre-6u3-windows-i586-p.exe"
    ExecWait "$INSTDIR\Temp\jre-6u3-windows-i586-p.exe"
  endNtBackup:
FunctionEnd

; Java3D Library installation
Function Java3D
  SetOutPath "$PROGRAMFILES\Temp"
      IfFileExists $PROGRAMFILES\Java\Java3D\1.5.1\README.html endNtBackup beginNtBackup
    Goto endNtBackup
    beginNtBackup:
    MessageBox MB_OK "Your system does not appear to have Java 3D installed.$\n$\nPress Ok to install it."
    File "java3d-1_5_1-windows-i586.exe"
    ExecWait "$INSTDIR\Temp\java3d-1_5_1-windows-i586.exe"
  endNtBackup:
FunctionEnd

Function un.onUninstSuccess
  HideWindow
  MessageBox MB_ICONINFORMATION|MB_OK "$(^Name) was successfully removed from your computer."
FunctionEnd

Function un.onInit
  MessageBox MB_ICONQUESTION|MB_YESNO|MB_DEFBUTTON2 "Are you sure you want to completely remove $(^Name) and all of its components?" IDYES +2
  Abort
FunctionEnd


Section Uninstall
  Delete "$INSTDIR\${PRODUCT_NAME}.url"
  Delete "$INSTDIR\uninst.exe"
  Delete "$INSTDIR\reprap-wv.stl"
  Delete "$INSTDIR\reprap-host.bat"
  Delete "$INSTDIR\LICENSE"
  Delete "$INSTDIR\README"
  Delete "$INSTDIR\Reprap.jar"

; Include this if bundling STL files.  Omitted for initial testing.  JM 2007-12-31.
;!include "reprap-stls-uninstall.nsi"

; Include this if bundling firmware.  Omitted for initial testing.  JM 2007-12-31.
; !include "reprap-firmware-uninstall.nsi"

  Delete "$SMPROGRAMS\Reprap \Uninstall.lnk"
  Delete "$SMPROGRAMS\Reprap \Website.lnk"
  Delete "$DESKTOP.lnk"

  RMDir "$SMPROGRAMS\Reprap "
  RMDir "$INSTDIR\firmware\200 step pic"
  RMDir "$INSTDIR"
  RMDir ""

  DeleteRegKey ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}"
  SetAutoClose true
SectionEnd