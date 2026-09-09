/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: ChatMessage
 * Funcao...........: Represents one local chat message.
 *************************************************************** */

package model;

import java.time.LocalDateTime;

public class ChatMessage {

  private final String group;
  private final String user;
  private final String text;
  private final LocalDateTime time;
  private final boolean system;

  public ChatMessage(String group, String user, String text, LocalDateTime time, boolean system) {
    this.group = group;
    this.user = user;
    this.text = text;
    this.time = time;
    this.system = system;
  }

  public static ChatMessage userMessage(String group, String user, String text) {
    return new ChatMessage(group, user, text, LocalDateTime.now(), false);
  }

  public static ChatMessage systemMessage(String group, String text) {
    return new ChatMessage(group, "server", text, LocalDateTime.now(), true);
  }

  public String getGroup() {
    return group;
  }

  public String getUser() {
    return user;
  }

  public String getText() {
    return text;
  }

  public LocalDateTime getTime() {
    return time;
  }

  public boolean isSystem() {
    return system;
  }
}
