/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: ClientEndpoint
 * Funcao...........: Stores one registered client UDP endpoint.
 *************************************************************** */

import java.net.InetAddress;

public class ClientEndpoint {

  private final String username;
  private final InetAddress address;
  private final int port;

  public ClientEndpoint(String username, InetAddress address, int port) {
    this.username = username;
    this.address = address;
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public InetAddress getAddress() {
    return address;
  }

  public int getPort() {
    return port;
  }

  public boolean matches(InetAddress otherAddress, int otherPort) {
    return address.equals(otherAddress) && port == otherPort;
  }
}
