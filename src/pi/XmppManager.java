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

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;


public class XmppManager {
    private static final int        PACKET_REPLY_TIMEOUT = 5000; // millis
    private Main                    main;
    private String                  server;
    private String                  resource;
    private int                     port;
    private ConnectionConfiguration config;
    private XMPPConnection          connection;
    private ChatManager             chatManager;
    private PacketListener          packetListener;


    // ******************* Constructors ***************************************
    public XmppManager(final Main main, final String server, final String resource, final int port) {
        this.main     = main;
        this.server   = server;
        this.resource = resource;
        this.port     = port;
    }


    // ******************* Initialization *************************************
    public void init() throws XMPPException {
        SmackConfiguration.setPacketReplyTimeout(PACKET_REPLY_TIMEOUT);
        config = new ConnectionConfiguration(server, port);

        //SASLAuthentication.supportSASLMechanism("PLAIN");
        //config.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
        //config.setSASLAuthenticationEnabled(true);
        //config.setReconnectionAllowed(true);
        //config.setRosterLoadedAtLogin(true);
        //config.setSendPresence(false);
        //config.setCompressionEnabled(true);

        connection = new XMPPConnection(config);
        connection.connect();

        chatManager    = connection.getChatManager();
        packetListener = new XmppPacketListener();
    }


    // ******************* Methods ********************************************
    public void login(final String username, final String password) throws XMPPException {
        if (connection != null && connection.isConnected()) {
            connection.login(username, password, resource);
            System.out.println("XMPP connection established as user " + username);
            setStatus(true, username + "(" + resource + ") online");
            connection.getRoster().setSubscriptionMode(Roster.SubscriptionMode.accept_all);
            connection.addPacketListener(packetListener, new PacketTypeFilter(Message.class));
        } else {
            System.out.println("XMPP login failed...!");
        }
    }

    public void setStatus(final boolean available, final String status) {
        Presence.Type type = available ? Presence.Type.available : Presence.Type.unavailable;
        Presence.Mode mode = available ? Presence.Mode.available : Presence.Mode.xa;
        Presence presence  = new Presence(type);
        presence.setFrom(connection.getUser());
        presence.setMode(mode);
        presence.setStatus(status);
        presence.setPriority(0);
        sendPacket(presence);
    }

    public void sendPacket(Packet packet) {
        if (connection != null && connection.isAuthenticated()) {
            connection.sendPacket(packet);
        }
    }

    public void sendMessage(Message message, String jid) throws XMPPException {
        if (connection.isConnected()) {
            Chat chat = chatManager.createChat(jid, null);
            chat.sendMessage(message);
        }
    }

    public void sendMessage(String message, String jid) throws XMPPException {
        if (connection.isConnected()) {
            Chat chat = chatManager.createChat(jid, null);
            chat.sendMessage(message);
        }
    }


    // ******************* Inner classes **************************************
    private class XmppPacketListener implements PacketListener {
        @Override public void processPacket(final Packet PACKET) {
            final String body = ((Message) PACKET).getBody().toLowerCase();
            final String from = PACKET.getFrom();
            if (body.equals("updatejdk")) {
                main.checkForJdkUpdate(from);
            } else if (body.equals("reboot")) {
                main.reboot(from);
            } else if (body.equals("ipaddress")) {
                main.getIpAddress(from);
            }
        }
    }
}
