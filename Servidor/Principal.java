/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: Principal
 * Funcao...........: Starts the NetChat Terminal server.
 *************************************************************** */

public class Principal {

  public static void main(String[] args) {
    System.out.println("NetChat Terminal Server");
    System.out.println("TCP JOIN/LEAVE/BLOCK/UNBLOCK: " + TransportConfig.TCP_PORT);
    System.out.println("UDP SEND: " + TransportConfig.SERVER_UDP_PORT);
    new Servidor().start();
  }
}
