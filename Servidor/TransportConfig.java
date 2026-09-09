/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: TransportConfig
 * Funcao...........: Stores network constants used by the server.
 *************************************************************** */

public final class TransportConfig {

  public static final int TCP_PORT = 6788;
  public static final int SERVER_UDP_PORT = 6789;
  public static final int BUFFER_SIZE = 8192;
  public static final int MAX_NAME_LENGTH = 24;
  public static final int MAX_MESSAGE_LENGTH = 1200;

  private TransportConfig() {
  }
}
