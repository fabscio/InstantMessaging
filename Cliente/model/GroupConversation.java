/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: GroupConversation
 * Funcao...........: Stores client-side group state.
 *************************************************************** */

package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GroupConversation {

  private final String name;
  private final List<ChatMessage> messages;
  private final Set<String> participants;
  private final Set<String> blockedUsers;
  private int unreadCount;

  public GroupConversation(String name) {
    this.name = name;
    this.messages = new ArrayList<ChatMessage>();
    this.participants = new LinkedHashSet<String>();
    this.blockedUsers = new HashSet<String>();
  }

  public String getName() {
    return name;
  }

  public List<ChatMessage> getMessages() {
    return messages;
  }

  public Set<String> getParticipants() {
    return participants;
  }

  public void addParticipant(String user) {
    if (user != null && user.trim().length() > 0) {
      participants.add(user.trim());
    }
  }

  public void setParticipants(List<String> users) {
    participants.clear();
    for (String user : users) {
      addParticipant(user);
    }
  }

  public void addMessage(ChatMessage message) {
    messages.add(message);
    if (!message.isSystem()) {
      addParticipant(message.getUser());
    }
  }

  public int getUnreadCount() {
    return unreadCount;
  }

  public void incrementUnreadCount() {
    unreadCount++;
  }

  public void clearUnreadCount() {
    unreadCount = 0;
  }

  public String getLastPreview() {
    if (messages.isEmpty()) {
      return "No messages yet.";
    }

    ChatMessage last = messages.get(messages.size() - 1);
    if (last.isSystem()) {
      return last.getText();
    }
    return last.getUser() + ": " + last.getText();
  }

  public boolean isBlocked(String user) {
    return blockedUsers.contains(normalize(user));
  }

  public void block(String user) {
    blockedUsers.add(normalize(user));
  }

  public void unblock(String user) {
    blockedUsers.remove(normalize(user));
  }

  public Set<String> getBlockedUsers() {
    return blockedUsers;
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  @Override
  public String toString() {
    return name;
  }
}
