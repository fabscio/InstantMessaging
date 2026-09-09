/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: ChatClient
 * Funcao...........: Implements client TCP and UDP communication.
 *************************************************************** */

package model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChatClient {

  public interface MessageListener {
    void onMessage(ChatMessage message);
  }

  public interface ErrorListener {
    void onError(String message);
  }

  public interface MembershipListener {
    void onMembers(String group, List<String> members);
  }

  public static class ServerResponse {
    private final String message;
    private final String group;
    private final List<String> members;

    private ServerResponse(String message, String group, List<String> members) {
      this.message = message;
      this.group = group;
      this.members = members;
    }

    public String getMessage() {
      return message;
    }

    public String getGroup() {
      return group;
    }

    public List<String> getMembers() {
      return members;
    }
  }

  private final String username;
  private final String serverHost;
  private InetAddress serverAddress;
  private DatagramSocket udpSocket;
  private Thread udpListenerThread;
  private volatile boolean running;
  private MessageListener messageListener;
  private ErrorListener errorListener;
  private MembershipListener membershipListener;

  public ChatClient(String username, String serverHost) {
    this.username = username.trim();
    this.serverHost = serverHost.trim();
  }

  /**
   * Starts the client transport and registers a unique UDP return port.
   *
   * @throws IOException if the server cannot be reached or rejects the username.
   */
  public void start() throws IOException {
    serverAddress = InetAddress.getByName(serverHost);
    checkServerReachable();

    udpSocket = new DatagramSocket(null);
    udpSocket.setReuseAddress(false);
    udpSocket.bind(new InetSocketAddress(0));

    registerUdpEndpoint();
    running = true;

    udpListenerThread = new Thread(new Runnable() {
      @Override
      public void run() {
        listenForUdpMessages();
      }
    }, "client-udp-listener");
    udpListenerThread.setDaemon(true);
    udpListenerThread.start();
  }

  public String getUsername() {
    return username;
  }

  public String getServerHost() {
    return serverHost;
  }

  public void setMessageListener(MessageListener messageListener) {
    this.messageListener = messageListener;
  }

  public void setErrorListener(ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  public void setMembershipListener(MembershipListener membershipListener) {
    this.membershipListener = membershipListener;
  }

  public ServerResponse joinGroup(String group) throws IOException {
    return sendTcp(Apdu.of("JOIN", group, username));
  }

  public ServerResponse leaveGroup(String group) throws IOException {
    return sendTcp(Apdu.of("LEAVE", group, username));
  }

  public ServerResponse blockUser(String group, String blockedUser) throws IOException {
    return sendTcp(Apdu.of("BLOCK", group, username, blockedUser));
  }

  public ServerResponse unblockUser(String group, String blockedUser) throws IOException {
    return sendTcp(Apdu.of("UNBLOCK", group, username, blockedUser));
  }

  /**
   * Sends the required SEND APDU through UDP.
   *
   * @param group destination group name.
   * @param text message body.
   * @throws IOException if the datagram cannot be sent or the message is invalid.
   */
  public void sendMessage(String group, String text) throws IOException {
    if (text.length() > TransportConfig.MAX_MESSAGE_LENGTH) {
      throw new IOException("Message is too long.");
    }

    Apdu apdu = Apdu.of("SEND", group, username, text);
    byte[] bytes = apdu.encode().getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet =
        new DatagramPacket(bytes, bytes.length, serverAddress, TransportConfig.SERVER_UDP_PORT);
    udpSocket.send(packet);
  }

  /**
   * Leaves joined groups and releases the registered UDP endpoint.
   *
   * @param groups groups joined by this client session.
   */
  public void shutdown(Collection<GroupConversation> groups) {
    if (groups != null) {
      for (GroupConversation group : groups) {
        try {
          leaveGroup(group.getName());
        } catch (IOException ignored) {
        }
      }
    }

    try {
      unregisterUdpEndpoint();
    } catch (IOException ignored) {
      // Shutdown is best-effort; the server also drops the user after LEAVE removes all groups.
    }

    running = false;
    if (udpSocket != null && !udpSocket.isClosed()) {
      udpSocket.close();
    }
  }

  private void registerUdpEndpoint() throws IOException {
    sendUdpControl(Apdu.of("REGISTER", username));
    Apdu response = receiveRegistrationResponse();
    if ("REGISTERED".equals(response.getType())) {
      return;
    }
    if ("ERROR".equals(response.getType())) {
      throw new IOException(response.size() > 0 ? response.param(0) : "Registration failed.");
    }
    throw new IOException("Unexpected registration response: " + response.encode());
  }

  private void unregisterUdpEndpoint() throws IOException {
    if (udpSocket == null || udpSocket.isClosed() || serverAddress == null) {
      return;
    }

    sendUdpControl(Apdu.of("UNREGISTER", username));
  }

  private void sendUdpControl(Apdu apdu) throws IOException {
    byte[] bytes = apdu.encode().getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet =
        new DatagramPacket(bytes, bytes.length, serverAddress, TransportConfig.SERVER_UDP_PORT);
    udpSocket.send(packet);
  }

  private Apdu receiveRegistrationResponse() throws IOException {
    udpSocket.setSoTimeout(TransportConfig.REGISTER_TIMEOUT_MS);
    try {
      byte[] buffer = new byte[TransportConfig.BUFFER_SIZE];
      DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
      udpSocket.receive(responsePacket);
      String rawResponse =
          new String(
              responsePacket.getData(), 0, responsePacket.getLength(), StandardCharsets.UTF_8);
      return Apdu.decode(rawResponse);
    } finally {
      udpSocket.setSoTimeout(0);
    }
  }

  private void checkServerReachable() throws IOException {
    Socket socket = new Socket();
    try {
      socket.connect(
          new InetSocketAddress(serverAddress, TransportConfig.TCP_PORT),
          TransportConfig.CONNECT_TIMEOUT_MS);
    } finally {
      socket.close();
    }
  }

  private ServerResponse sendTcp(Apdu apdu) throws IOException {
    Socket socket = new Socket();
    try {
      socket.connect(
          new InetSocketAddress(serverAddress, TransportConfig.TCP_PORT),
          TransportConfig.CONNECT_TIMEOUT_MS);
      socket.setSoTimeout(TransportConfig.CONNECT_TIMEOUT_MS);

      BufferedWriter writer =
          new BufferedWriter(
              new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
      BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

      writer.write(apdu.encode());
      writer.newLine();
      writer.flush();

      String line = reader.readLine();
      if (line == null) {
        throw new IOException("Server closed the connection without a response.");
      }

      Apdu response = Apdu.decode(line);
      if ("OK".equals(response.getType())) {
        return parseServerResponse(response);
      }
      if ("ERROR".equals(response.getType())) {
        throw new IOException(
            response.size() > 0 ? response.param(0) : "Server reported an error.");
      }
      throw new IOException("Unexpected server response: " + line);
    } catch (SocketTimeoutException e) {
      throw new IOException("Timed out while communicating with the server.", e);
    } finally {
      socket.close();
    }
  }

  private ServerResponse parseServerResponse(Apdu response) {
    String message = response.size() > 0 ? response.param(0) : "OK";
    String group = response.size() > 1 ? response.param(1) : "";
    List<String> members = new ArrayList<String>();
    for (int i = 2; i < response.size(); i++) {
      members.add(response.param(i));
    }
    return new ServerResponse(message, group, members);
  }

  private void listenForUdpMessages() {
    byte[] buffer = new byte[TransportConfig.BUFFER_SIZE];

    while (running) {
      try {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        udpSocket.receive(packet);

        String text = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        Apdu apdu = Apdu.decode(text);
        if ("SEND".equals(apdu.getType()) && apdu.size() == 3) {
          ChatMessage message =
              ChatMessage.userMessage(apdu.param(0), apdu.param(1), apdu.param(2));
          if (messageListener != null) {
            messageListener.onMessage(message);
          }
        } else if ("MEMBERS".equals(apdu.getType()) && apdu.size() >= 1) {
          List<String> members = new ArrayList<String>();
          for (int i = 1; i < apdu.size(); i++) {
            members.add(apdu.param(i));
          }
          if (membershipListener != null) {
            membershipListener.onMembers(apdu.param(0), members);
          }
        }
      } catch (IOException e) {
        if (running) {
          reportError("Failed to receive UDP message: " + e.getMessage());
        }
      } catch (IllegalArgumentException e) {
        reportError("Invalid UDP APDU: " + e.getMessage());
      }
    }
  }

  private void reportError(String message) {
    if (errorListener != null) {
      errorListener.onError(message);
    }
  }
}
