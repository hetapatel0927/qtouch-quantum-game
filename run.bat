echo off
echo '............................................'
echo '..     ALGONQUIN COLLEGE - JAP - 25F      ..'
echo '............................................'
echo '                                            '
echo '  ........=====...........................  '
echo '  ......=+===.............................  '
echo '  ......===+=........=+...................  '
echo '  .......===.........=+...=+.=+..+=...+=..  '
echo '  ...=========....==.=+.+=.=.+=+===.+=.=..  '
echo '  ....==========...==+..====...==...====..  '
echo '  ..==.=======............................  '
echo '  ...=============........................  '
echo '.                   [A6]                    '
echo '.         [Team: Heta &Khusmit]         '
echo '............................................'

set "arg1=%1"
set "arg2=%2"

:: DEFAULT PACKAGE = lowercase qtouch
if "%arg1%"=="" set "arg1=qtouch"
if "%arg2%"=="" set "arg2=net"

echo Parameter value: %arg1%, %arg2%

echo 'Starting Javadoc ...........................'
javadoc -classpath ./src -d doc ./src/qtouch/*.java ./src/qtouch/net/*.java
echo 'Compiling ..................................'
echo MVC...
javac -d bin ./src/%arg1%/*.java

echo C/S...
javac -d bin ./src/%arg1%/*.java ./src/%arg1%/%arg2%/*.java

if %errorlevel% neq 0 (
    echo Compilation failed!
    exit /b 1
)

echo 'Creating Jar ...............................'

echo Creating QTouchclient.jar...
echo Main-Class: %arg1%.%arg2%.QTouchClient > client_manifest.txt
jar cfm QTouchclient.jar client_manifest.txt 
echo Creating QTouchserver.jar...
echo Main-Class: %arg1%.%arg2%.QTouchServer > server_manifest.txt
jar cfm QTouchserver.jar server_manifest.txt echo 'Running Jar ................................'
start java -jar QTouchclient.jar
start java -jar QTouchserver.jar

echo 'End ........................................'
