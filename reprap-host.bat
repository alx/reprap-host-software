rem reprap-host -- runs Reprap Java host code with an appropriate classpath

rem Amount of RAM to allow Java VM to use
set RAM_SIZE=384M

rem reprap.jar file and stl file
set REPRAP_DIR=%ProgramFiles%\Reprap

rem Java3D and j3d.org libraries
set JAVA_LIBRARY_DIR=%ProgramFiles%\Java\shared

rem cd so we can find the reprap-wv.stl file.  Can we avoid this??
IF NOT EXIST reprap-wv.stl cd "%REPRAP_DIR%"
java -cp ".;.\reprap.jar;%REPRAP_DIR%\;%REPRAP_DIR%\reprap.jar;%JAVA_LIBRARY_DIR%\*" -Xmx%RAM_SIZE% org/reprap/Main
if ERRORLEVEL 1 pause

