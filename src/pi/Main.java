/*
 * Copyright (c) 2013 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pi;

import org.jivesoftware.smack.XMPPException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;


public class Main {
    private static final String JDK_FILE_ON_DROPBOX = "PATH TO YOUR DROPBOX FOLDER/jdk8.gz"; // e.g. "https://dl.dropbox.com/u/12345/jdk8.gz";
    private static final String JDK_FILE_ON_PI      = "PATH TO jdk8.gz FILE ON PI";          // e.g. "/home/pi/jdk8.gz"

    private String      senderName;
    private String      senderPassword;
    private String      server;
    private String      resource;
    private int         port;
    private XmppManager xmppManager;


    // ******************* Constructors ***************************************
    public Main() {
        senderName             = "YOUR JID";           // if your full jid is "YOUR_NAME@jabber.ccc.de" use just "YOUR_NAME" here
        senderPassword         = "YOUR XMPP PASSWORD"; // your xmpp password here
        server                 = "YOUR XMPP SERVER";   // the xmpp server you use e.g. "jabber.ccc.de"
        resource               = "YOUR RESOURCE";      // a xmpp resource for your client e.g. "Home"
        port                   = 5222;                 // default is 5222 and 5223 for SSL

        xmppManager           = new XmppManager(this, server, resource, port);

        init();
    }


    // ******************* Initialization *************************************
    private void init() {
        initializeXmppConnection();
    }

    private void initializeXmppConnection() {
        try {
            xmppManager.init();
            xmppManager.login(senderName, senderPassword);
            xmppManager.setStatus(true, "XMPP Connection established");
        } catch (XMPPException exception) {
            System.out.println("Error connecting to XMPP: XMPPException " + exception);
        }
    }


    // ******************** Methods that answers chat requests ****************
    public void getIpAddress(final String jid) {
        StringBuilder ipAddress = new StringBuilder();
        ipAddress.append("IP address:\n");
        InetAddress                   current_addr = null;
        Enumeration<NetworkInterface> interfaces   = null;
        try { interfaces = NetworkInterface.getNetworkInterfaces(); } catch (SocketException e) {}
        while (interfaces.hasMoreElements()) {
            NetworkInterface current = interfaces.nextElement();
            try { if (!current.isUp() || current.isLoopback() || current.isVirtual()) continue;} catch (SocketException e) {}
            Enumeration<InetAddress> addresses = current.getInetAddresses();
            while (addresses.hasMoreElements()) {
                current_addr = addresses.nextElement();
                if (current_addr.isLoopbackAddress()) continue;
                if (current_addr instanceof Inet4Address) {
                    ipAddress.append(current_addr.getHostAddress()).append("\n");
                } else if (current_addr instanceof Inet6Address) {
                    ipAddress.append(current_addr.getHostAddress()).append("\n");
                }
            }
        }
        sendMessage(ipAddress.toString(), jid);
    }

    public void checkForJdkUpdate(final String jid) {
        sendMessage("Start downloading jdk...please wait", jid);
        try (InputStream in = URI.create(JDK_FILE_ON_DROPBOX).toURL().openStream()) {
            Files.copy(in, Paths.get(JDK_FILE_ON_PI), StandardCopyOption.REPLACE_EXISTING);
            if (isFileCopyFinished(JDK_FILE_ON_PI)) {
                sendMessage("Download of jdk successful, please reboot", jid);
            } else {
                sendMessage("Copy jdk file from DropBox failed", jid);
            }
        } catch (IOException e) {
            sendMessage("Download of jdk failed", jid);
        }
    }

    public void reboot(final String jid) {
        try {
            Runtime run = Runtime.getRuntime();
            run.exec("sudo shutdown -r now");
        } catch (IOException exception) {
            sendMessage("Rebooting the Pi failed", jid);
        }
    }


    // ******************* Private Methods ************************************
    private void sendMessage(final String message, final String jid) {
        new Thread(() -> {
            try {
                xmppManager.sendMessage(message, jid);
            } catch (XMPPException exception) {}
        }).start();
    }

    private boolean isFileCopyFinished(final String FILE_NAME) {
        File ff = new File(FILE_NAME);
        // Wait up to 30 seconds for file copy to be finished
        if (ff.exists()) {
            for (int timeout = 300 ; timeout > 0 ; timeout--) {
                RandomAccessFile ran = null;
                try {
                    ran = new RandomAccessFile(ff, "rw");
                    break; // no errors, done waiting
                } catch(FileNotFoundException exception) {
                    // timeout
                    return false;
                } finally {
                    if (null != ran) {
                        try { ran.close(); } catch (IOException exception) {}
                        ran = null;
                    }
                    try { Thread.sleep(100); } catch (InterruptedException exception) {}
                }
            }
            if (ff.exists()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println("Temperature monitor started");
        new Main();
    }
}
