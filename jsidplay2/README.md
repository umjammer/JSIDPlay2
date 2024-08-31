Java requirements:
==================
Use Java 17 or newer (Java 11 is NOT supported) or as an alternative Oracle-Java8 (we are still compatible) 

Work with Eclipse:
==================
From the command-line:
mvn -Djavafx.platform=linux clean install								; # Use "linux"/"linux-aarch64", "win" or "mac"/"mac-aarch64" for your platform
mvn eclipse:eclipse
then in Eclipse: Right-Click on Project Maven/"Update Project"

Build JSIDPlay2
===============
mvn -Djavafx.platform=linux clean install
-> target/standalone	-	local PC version
-> target/deploy		-	website version

Deploy JSIDPlay2
================
mvn -Djavafx.platform=linux clean deploy
"builds and uploads target/deploy to web-server"

Build JSIDPlay2 and runs UI-tests
=================================
mvn -Djavafx.platform=linux clean install -P release			"You should disable screensaver to not interfere with the tests"

Set the following optional properties in Eclipse to deploy additional online features:
Open Preferences/Run/Debug/StringSubstitution or at command line: using -D

-Dgb64.mdbfile=<pathToGameBase64.mdb>
-Dhvsc.7z=<pathToHVSC.7z>
-Dcgsc.7z=<pathToCGSZ.7z>
-Dhvmec.zip=<pathToHVMEC.zip>
-Dupx.exe=<pathToUPX>

Launch JSIDPlay2Server:
=======================
mvn exec:java -Dexec.mainClass=server.restful.JSIDPlay2Server

Eclipse JavaFX support:
=======================
1. Install e(fx)clipse
2. To get a content assistant on javafx css files:
Right click on your project -> properties -> Java build path -> libraries -> add library -> javaFX SDK
3. To edit .fxml files with FXML Editor:
Right click on .fxml file: Open With -> Other... -> FXML Editor (Check "Use it for all '*.fxml' files") -> OK
4. To use JavaFX Preview view in Eclipse, change root tag in fxml file (uncomment the commented out alternative version)
Eventually uncomment line in C64VBox.java (see TODO marker), still not sure, why some of the layouts need that.

HardSID in VirtualBox:
======================
Use HardSID4U, HardSID Uno and HardSID UPlay - USB devices:
Connect HardSID device to your real computer.
Allow Windows to use USB hardware with Virtual box Toolbar icon in the bottom right side of the window.
On Linux host your user has to be a member of the group: vboxusers to let virtualbox access USB hardware.
Start JSIDPlay2 and switch from Emulation to HardSID4U.
Check console, that there are no error messages.

Hard-wire HardSID: connect mixed/out into the Microfone/in of your computer:
In Ubuntu Linux with Pulseaudio installed transport Microfone sound to speakers;
Open a terminal and type:
parec --latency-msec=1 | pacat --latency-msec=1
Lower your microphone volume level to prevent overdrive noise

as an alternative use:
pactl load-module module-loopback latency_msec=1
and disable with
pactl unload-module module-loopback

If volume control is not shown in Ubuntu, type:
"gsettings set com.canonical.indicator.sound visible true"
If microphone is greyed out, type:
"kmix"
and change "output device" to "Analog Stereo Duplex".

Grant access to Linux input devices (Joystick support):
=======================================================
sudo usermod -a -G input ken	- replace ken with your username	
Now reboot!

General tips:
=============

How to Create a HTTPS certificate and install easily for your Apache web-server:
https://certbot.eff.org/lets-encrypt/ubuntutrusty-apache

For support of double click on the JAR to start JSIDPlay2:
Right click on jsidplay-<version>.jar
Open with...
Choose a different one...
/home/ken/Downloads/jdk1.8.0_211/bin/java -jar
X Remember file type assignment

How to find out available JavaFX style classes of a node in the scene graph?
	private void dump(Node n, int depth) {
		for (int i = 0; i < depth; i++)
			System.out.print("  ");
		System.out.println(n);
		if (n instanceof Parent)
			for (Node c : ((Parent) n).getChildrenUnmodifiable())
				dump(c, depth + 1);
	}


Heap Memory Analyzer - To find memory leaks you can use:
jmap -dump:format=b,file=heap.bin <JavaProcessId>
Install Eclipse MAT (Heap Memory Analyzer) (https://www.eclipse.org/mat/)
Increase max memory, if required to load huge heap dumps in file MemoryAnalyzer.ini:
-Xmx=5g
In MAT - Load heap dump: File/Open heap.bin

jvisualvm - Monitor JVM:
Java JMX - jvisualvm connection parameters for JSIDPlay2:
-Dcom.sun.management.jmxremote.port=9999
-Dcom.sun.management.jmxremote.ssl=false
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.local.only=false
-Djava.rmi.server.hostname=<server-ip>

Remote Debugging:
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9999

Internal Testing the build (Java8 and newer):
=============================================
cd target/standalone
cp ../deploy/*.sh .
chmod +x *.sh
./jsidplay2-console.sh 
./jsidplay2-console.sh ~/Downloads/Turrican_2-The_Final_Fight.sid 
export PATH=/home/ken/Downloads/jdk1.8.0_211/bin:$PATH
./jsidplay2-java8.sh ~/Downloads/Turrican_2-The_Final_Fight.sid 
./jsidplay2.sh ~/Downloads/Turrican_2-The_Final_Fight.sid 
./sidblastertool.sh 
./recordingtool.sh 
cd ../deploy/
unzip jsiddevice-4.9.zip
java -jar ./jsidplay2-4.9-libsidplay.jar ~/Downloads/Turrican_2-The_Final_Fight.sid 
./jsiddevice-4.9/jsiddevice.sh
./jsiddevice-4.9/jsiddevice-console.sh
