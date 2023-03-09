package server.netsiddev;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import server.netsiddev.ini.JSIDDeviceConfig;

public class DetectionThread extends Thread {

	private static final String MAGIC_ID = "SidDevice";
	private DatagramSocket socket;
    private byte[] buf = new byte[512];
    private String localIp;
    private String systemName;

    public DetectionThread(final JSIDDeviceConfig config) {
        try {
        	final InetAddress inetAddress = InetAddress.getLocalHost();
        	localIp = inetAddress.getHostAddress();
			systemName = inetAddress.getHostName();
			
			socket = new DatagramSocket(config.jsiddevice().getPort());
		} catch (IOException ex) {
			ex.printStackTrace();
		}
    }

    public void run() {
    	boolean running = true;
        
        final String osName = System.getProperty("os.name");
        final String osArch = is64Bit() ? "64-bit" : "32-bit";

        final byte[] responseData = new StringBuilder()
			.append(MAGIC_ID)
			.append(",")
			.append(systemName)
			.append(",")
			.append(osName)
        	.append(" ")
        	.append(osArch)
        	.toString().getBytes();
        
        final SIDDeviceSettings settings = SIDDeviceSettings.getInstance();
        
    	final DatagramPacket incomingPacket = new DatagramPacket(buf, buf.length);
		final DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length);

    	while (running) {
            try {
				socket.receive(incomingPacket);
	            final String received = new String(incomingPacket.getData(), 0, incomingPacket.getLength());

	            if (received.startsWith(MAGIC_ID) && (isLocalIp(incomingPacket) || settings.getAllowExternalConnections())) {
	                responsePacket.setAddress(incomingPacket.getAddress());
	                responsePacket.setPort(incomingPacket.getPort());
    				socket.send(responsePacket);
	            }
			} catch (IOException ex) {
				ex.printStackTrace();
				running = false;
			}
        }
    	
        socket.close();
    }

	private boolean isLocalIp(DatagramPacket incomingPacket) {
		return incomingPacket.getAddress().getHostAddress().equals(localIp);
	}
    
	private static boolean is64Bit() {
	    String arch = System.getProperty("os.arch");
	    String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
	    return arch != null && arch.contains("64") || wow64Arch != null && wow64Arch.contains("64");
	}
}