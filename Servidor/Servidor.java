/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: Servidor
 * Funcao...........: Runs the TCP and UDP server services.
 *************************************************************** */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Servidor {

  private final GroupManager groupManager;

  public Servidor() {
    this.groupManager = GroupManager.getInstance();
  }

  /** Starts TCP and UDP listeners for the chat server. */
  public void start() {
    Thread tcpThread = new Thread(new Runnable() {
      @Override
      public void run() {
        startTcpServer();
      }
    }, "server-tcp");

    Thread udpThread = new Thread(new Runnable() {
      @Override
      public void run() {
        startUdpServer();
      }
    }, "server-udp");

    tcpThread.start();
    udpThread.start();
  }

  private void startTcpServer() {
    try (ServerSocket serverSocket = new ServerSocket(TransportConfig.TCP_PORT)) {
      log("TCP listening on port " + TransportConfig.TCP_PORT);
      while (true) {
        final Socket socket = serverSocket.accept();
        Thread handler = new Thread(new Runnable() {
          @Override
          public void run() {
            handleTcp(socket);
          }
        }, "tcp-client-handler");
        handler.start();
      }
    } catch (IOException e) {
      log("TCP server stopped: " + e.getMessage());
    }
  }

  /**
   * Processes one TCP connection carrying JOIN, LEAVE, BLOCK, or UNBLOCK.
   *
   * @param socket accepted client connection.
   */
  private void handleTcp(Socket socket) {
    try {
      BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      BufferedWriter writer =
          new BufferedWriter(
              new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

      String line = reader.readLine();
      if (line == null || line.trim().length() == 0) {
        return;
      }

      log("TCP <= " + line + " from " + socket.getInetAddress().getHostAddress());
      Apdu request = Apdu.decode(line);
      Apdu response = processTcpApdu(request, socket);
      writer.write(response.encode());
      writer.newLine();
      writer.flush();
      log("TCP => " + response.encode());
    } catch (Exception e) {
      try {
        BufferedWriter writer =
            new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        writer.write(
            Apdu.of("ERROR", e.getMessage() == null ? "Unknown error." : e.getMessage())
                .encode());
        writer.newLine();
        writer.flush();
      } catch (IOException ignored) {
      }
      log("TCP error: " + e.getMessage());
    } finally {
      try {
        socket.close();
      } catch (IOException ignored) {
      }
    }
  }

  /**
   * Validates and dispatches the TCP APDU received by the server.
   *
   * @param request decoded TCP APDU.
   * @param socket source socket, used to capture the network address.
   * @return encoded response data.
   */
  private Apdu processTcpApdu(Apdu request, Socket socket) {
    if ("JOIN".equals(request.getType()) && request.size() == 2) {
      String message =
          groupManager.join(request.param(0), request.param(1), socket.getInetAddress());
      notifyMemberList(request.param(0));
      return okWithMembers(message, request.param(0));
    }
    if ("LEAVE".equals(request.getType()) && request.size() == 2) {
      String message = groupManager.leave(request.param(0), request.param(1));
      notifyMemberList(request.param(0));
      return Apdu.of("OK", message, request.param(0));
    }
    if ("BLOCK".equals(request.getType()) && request.size() == 3) {
      String message = groupManager.block(request.param(0), request.param(1), request.param(2));
      return okWithMembers(message, request.param(0));
    }
    if ("UNBLOCK".equals(request.getType()) && request.size() == 3) {
      String message = groupManager.unblock(request.param(0), request.param(1), request.param(2));
      return okWithMembers(message, request.param(0));
    }

    throw new IllegalArgumentException("Unsupported TCP APDU or incorrect parameters.");
  }

  private Apdu okWithMembers(String message, String group) {
    List<String> params = new ArrayList<String>();
    params.add(message);
    params.add(group);
    params.addAll(groupManager.membersOf(group));
    return Apdu.of("OK", params.toArray(new String[params.size()]));
  }

  private void startUdpServer() {
    try (DatagramSocket serverSocket = new DatagramSocket(TransportConfig.SERVER_UDP_PORT)) {
      log("UDP listening on port " + TransportConfig.SERVER_UDP_PORT);
      byte[] buffer = new byte[TransportConfig.BUFFER_SIZE];

      while (true) {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        serverSocket.receive(packet);
        String line = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        handleUdp(serverSocket, line, packet.getAddress(), packet.getPort());
      }
    } catch (IOException e) {
      log("UDP server stopped: " + e.getMessage());
    }
  }

  /**
   * Processes one UDP datagram carrying REGISTER, UNREGISTER, or SEND.
   *
   * @param serverSocket UDP socket used to answer and forward datagrams.
   * @param line raw APDU text.
   * @param sourceAddress datagram source address.
   * @param sourcePort datagram source port.
   */
  private void handleUdp(
      DatagramSocket serverSocket, String line, InetAddress sourceAddress, int sourcePort) {
    try {
      log("UDP <= " + line);
      Apdu request = Apdu.decode(line);
      if ("REGISTER".equals(request.getType()) && request.size() == 1) {
        String registeredUser =
            groupManager.registerUdpEndpoint(request.param(0), sourceAddress, sourcePort);
        sendUdpResponse(
            serverSocket, sourceAddress, sourcePort, Apdu.of("REGISTERED", registeredUser));
        log(
            "REGISTER => "
                + request.param(0)
                + "@"
                + sourceAddress.getHostAddress()
                + ":"
                + sourcePort);
        return;
      }

      if ("UNREGISTER".equals(request.getType()) && request.size() == 1) {
        groupManager.unregisterUdpEndpoint(request.param(0), sourceAddress, sourcePort);
        log(
            "UNREGISTER => "
                + request.param(0)
                + "@"
                + sourceAddress.getHostAddress()
                + ":"
                + sourcePort);
        return;
      }

      if (!"SEND".equals(request.getType()) || request.size() != 3) {
        throw new IllegalArgumentException("Unsupported UDP APDU or incorrect parameters.");
      }

      String group = request.param(0);
      String sender = request.param(1);
      String message = request.param(2);
      if (message.trim().length() == 0
          || message.length() > TransportConfig.MAX_MESSAGE_LENGTH) {
        throw new IllegalArgumentException("Empty message.");
      }

      groupManager.updateUdpEndpoint(sender, sourceAddress, sourcePort);
      sendMemberList(serverSocket, group);

      List<ClientEndpoint> recipients = groupManager.recipientsFor(group, sender);
      byte[] output = request.encode().getBytes(StandardCharsets.UTF_8);
      for (ClientEndpoint endpoint : recipients) {
        DatagramPacket outgoing = new DatagramPacket(
            output,
            output.length,
            endpoint.getAddress(),
            endpoint.getPort());
        serverSocket.send(outgoing);
        logEndpoint("UDP", endpoint);
      }
    } catch (Exception e) {
      log("UDP error: " + e.getMessage());
      try {
        sendUdpResponse(
            serverSocket,
            sourceAddress,
            sourcePort,
            Apdu.of("ERROR", e.getMessage() == null ? "Unknown error." : e.getMessage()));
      } catch (IOException ignored) {
        // Logging already happened; there is no secondary channel for this datagram failure.
      }
    }
  }

  private void sendUdpResponse(
      DatagramSocket serverSocket, InetAddress address, int port, Apdu response)
      throws IOException {
    byte[] output = response.encode().getBytes(StandardCharsets.UTF_8);
    DatagramPacket packet = new DatagramPacket(output, output.length, address, port);
    serverSocket.send(packet);
  }

  private void sendMemberList(DatagramSocket serverSocket, String group) throws IOException {
    List<String> params = new ArrayList<String>();
    params.add(group);
    params.addAll(groupManager.membersOf(group));
    byte[] output =
        Apdu.of("MEMBERS", params.toArray(new String[params.size()]))
            .encode()
            .getBytes(StandardCharsets.UTF_8);

    for (ClientEndpoint endpoint : groupManager.endpointsForGroup(group)) {
      DatagramPacket packet =
          new DatagramPacket(output, output.length, endpoint.getAddress(), endpoint.getPort());
      serverSocket.send(packet);
      logEndpoint("MEMBERS", endpoint);
    }
  }

  private void notifyMemberList(String group) {
    try (DatagramSocket socket = new DatagramSocket()) {
      sendMemberList(socket, group);
    } catch (IOException e) {
      log("MEMBERS notification failed: " + e.getMessage());
    }
  }

  private void log(String message) {
    System.out.println("[Server] " + message);
  }

  private void logEndpoint(String prefix, ClientEndpoint endpoint) {
    log(
        prefix
            + " => "
            + endpoint.getUsername()
            + "@"
            + endpoint.getAddress().getHostAddress()
            + ":"
            + endpoint.getPort());
  }
}
