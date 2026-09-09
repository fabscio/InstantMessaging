/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: GroupManager
 * Funcao...........: Maintains server-side users, groups, and blocks.
 *************************************************************** */

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GroupManager {

  private static final GroupManager INSTANCE = new GroupManager();

  private final Map<String, String> groupDisplayNames;
  private final Map<String, Set<String>> usersByGroup;
  private final Map<String, ClientEndpoint> endpointByUser;
  private final Map<String, Map<String, Set<String>>> blockedUsersByGroupAndBlocker;

  private GroupManager() {
    this.groupDisplayNames = new LinkedHashMap<String, String>();
    this.usersByGroup = new LinkedHashMap<String, Set<String>>();
    this.endpointByUser = new HashMap<String, ClientEndpoint>();
    this.blockedUsersByGroupAndBlocker = new HashMap<String, Map<String, Set<String>>>();
  }

  public static GroupManager getInstance() {
    return INSTANCE;
  }

  /**
   * Adds a user to a group without allowing duplicate user memberships.
   *
   * @param group requested group name.
   * @param user requested username.
   * @param address TCP source address used as a fallback endpoint.
   * @return user-facing result message.
   */
  public synchronized String join(String group, String user, InetAddress address) {
    validateName(group, "group");
    validateName(user, "user");

    String groupKey = normalize(group);
    String userKey = normalize(user);

    ClientEndpoint existingEndpoint = endpointByUser.get(userKey);
    if (existingEndpoint != null && !existingEndpoint.getAddress().equals(address)) {
      throw new IllegalArgumentException("Username is already in use.");
    }

    groupDisplayNames.putIfAbsent(groupKey, group.trim());
    usersByGroup.putIfAbsent(groupKey, new LinkedHashSet<String>());
    InetAddress endpointAddress =
        existingEndpoint == null ? address : existingEndpoint.getAddress();
    int endpointPort = existingEndpoint == null ? -1 : existingEndpoint.getPort();
    endpointByUser.put(userKey, new ClientEndpoint(user.trim(), endpointAddress, endpointPort));

    if (usersByGroup.get(groupKey).contains(userKey)) {
      return user.trim() + " is already in group " + groupDisplayNames.get(groupKey) + ".";
    }

    usersByGroup.get(groupKey).add(userKey);
    return user.trim() + " joined group " + groupDisplayNames.get(groupKey) + ".";
  }

  /**
   * Removes a user from a group and cleans empty groups.
   *
   * @param group group name.
   * @param user username.
   * @return user-facing result message.
   */
  public synchronized String leave(String group, String user) {
    validateName(group, "group");
    validateName(user, "user");

    String groupKey = normalize(group);
    String userKey = normalize(user);
    Set<String> users = usersByGroup.get(groupKey);
    if (users == null || !users.remove(userKey)) {
      throw new IllegalArgumentException("User is not in this group.");
    }

    if (users.isEmpty()) {
      usersByGroup.remove(groupKey);
      groupDisplayNames.remove(groupKey);
    }

    if (!isUserInAnyGroup(userKey)) {
      endpointByUser.remove(userKey);
      removeUserFromBlockLists(userKey);
    }

    return user.trim() + " left group " + group.trim() + ".";
  }

  /**
   * Registers the UDP endpoint that will receive server datagrams.
   *
   * @param user username being registered.
   * @param address UDP source address.
   * @param port UDP source port.
   * @return display name accepted by the server.
   */
  public synchronized String registerUdpEndpoint(String user, InetAddress address, int port) {
    validateName(user, "user");
    String userKey = normalize(user);
    ClientEndpoint endpoint = endpointByUser.get(userKey);
    if (endpoint != null && endpoint.getPort() > 0 && !endpoint.matches(address, port)) {
      throw new IllegalArgumentException("Username is already in use.");
    }

    String displayName = endpoint == null ? user.trim() : endpoint.getUsername();
    endpointByUser.put(userKey, new ClientEndpoint(displayName, address, port));
    return displayName;
  }

  public synchronized void updateUdpEndpoint(String user, InetAddress address, int port) {
    validateName(user, "user");
    String userKey = normalize(user);
    ClientEndpoint endpoint = endpointByUser.get(userKey);
    if (endpoint == null || endpoint.getPort() <= 0 || endpoint.matches(address, port)) {
      String displayName = endpoint == null ? user.trim() : endpoint.getUsername();
      endpointByUser.put(userKey, new ClientEndpoint(displayName, address, port));
    }
  }

  /**
   * Releases a UDP endpoint if the user is not joined to any group.
   *
   * @param user username being released.
   * @param address UDP source address.
   * @param port UDP source port.
   */
  public synchronized void unregisterUdpEndpoint(String user, InetAddress address, int port) {
    validateName(user, "user");
    String userKey = normalize(user);
    ClientEndpoint endpoint = endpointByUser.get(userKey);
    if (endpoint != null && endpoint.matches(address, port) && !isUserInAnyGroup(userKey)) {
      endpointByUser.remove(userKey);
    }
  }

  /**
   * Blocks one group member for the requesting user.
   *
   * @param group group where the block applies.
   * @param blocker user creating the block.
   * @param blocked user being blocked.
   * @return user-facing result message.
   */
  public synchronized String block(String group, String blocker, String blocked) {
    validateName(group, "group");
    validateName(blocker, "user");
    validateName(blocked, "blocked user");

    String groupKey = normalize(group);
    String blockerKey = normalize(blocker);
    String blockedKey = normalize(blocked);

    requireUserInGroup(groupKey, blockerKey, "Requesting user is not in this group.");
    requireUserInGroup(groupKey, blockedKey, "User to block is not in this group.");

    if (blockerKey.equals(blockedKey)) {
      throw new IllegalArgumentException("User cannot block themselves.");
    }

    blockedUsersByGroupAndBlocker.putIfAbsent(
        groupKey, new HashMap<String, Set<String>>());
    blockedUsersByGroupAndBlocker
        .get(groupKey)
        .putIfAbsent(blockerKey, new LinkedHashSet<String>());
    blockedUsersByGroupAndBlocker.get(groupKey).get(blockerKey).add(blockedKey);
    return blocked.trim() + " was blocked by " + blocker.trim() + " in " + group.trim() + ".";
  }

  /**
   * Removes a previous group-scoped block.
   *
   * @param group group where the unblock applies.
   * @param blocker user removing the block.
   * @param blocked user being unblocked.
   * @return user-facing result message.
   */
  public synchronized String unblock(String group, String blocker, String blocked) {
    validateName(group, "group");
    validateName(blocker, "user");
    validateName(blocked, "blocked user");

    String groupKey = normalize(group);
    String blockerKey = normalize(blocker);
    String blockedKey = normalize(blocked);

    requireUserInGroup(groupKey, blockerKey, "Requesting user is not in this group.");
    requireUserInGroup(groupKey, blockedKey, "User to unblock is not in this group.");

    Map<String, Set<String>> blockedByBlocker = blockedUsersByGroupAndBlocker.get(groupKey);
    Set<String> blockedUsers =
        blockedByBlocker == null ? null : blockedByBlocker.get(blockerKey);
    if (blockedUsers != null) {
      blockedUsers.remove(blockedKey);
    }
    return blocked.trim() + " was unblocked by " + blocker.trim() + " in " + group.trim() + ".";
  }

  /**
   * Lists endpoints that should receive a SEND APDU.
   *
   * @param group destination group.
   * @param sender sending user.
   * @return endpoints excluding the sender and users who blocked the sender.
   */
  public synchronized List<ClientEndpoint> recipientsFor(String group, String sender) {
    validateName(group, "group");
    validateName(sender, "user");

    String groupKey = normalize(group);
    String senderKey = normalize(sender);
    requireUserInGroup(groupKey, senderKey, "Sender is not in this group.");

    List<ClientEndpoint> recipients = new ArrayList<ClientEndpoint>();
    Set<String> users = usersByGroup.get(groupKey);
    for (String recipientKey : users) {
      if (recipientKey.equals(senderKey)) {
        continue;
      }
      if (hasBlocked(groupKey, recipientKey, senderKey)) {
        continue;
      }
      ClientEndpoint endpoint = endpointByUser.get(recipientKey);
      if (endpoint != null && endpoint.getPort() > 0) {
        recipients.add(endpoint);
      }
    }
    return recipients;
  }

  public synchronized List<ClientEndpoint> endpointsForGroup(String group) {
    validateName(group, "group");
    String groupKey = normalize(group);
    Set<String> users = usersByGroup.get(groupKey);
    List<ClientEndpoint> endpoints = new ArrayList<ClientEndpoint>();
    if (users == null) {
      return endpoints;
    }

    for (String userKey : users) {
      ClientEndpoint endpoint = endpointByUser.get(userKey);
      if (endpoint != null && endpoint.getPort() > 0) {
        endpoints.add(endpoint);
      }
    }
    return endpoints;
  }

  public synchronized List<String> membersOf(String group) {
    validateName(group, "group");
    String groupKey = normalize(group);
    Set<String> users = usersByGroup.get(groupKey);
    List<String> members = new ArrayList<String>();
    if (users == null) {
      return members;
    }

    for (String userKey : users) {
      ClientEndpoint endpoint = endpointByUser.get(userKey);
      members.add(endpoint == null ? userKey : endpoint.getUsername());
    }
    return members;
  }

  private boolean hasBlocked(String groupKey, String recipientKey, String senderKey) {
    Map<String, Set<String>> blockedByBlocker = blockedUsersByGroupAndBlocker.get(groupKey);
    Set<String> blockedUsers =
        blockedByBlocker == null ? null : blockedByBlocker.get(recipientKey);
    return blockedUsers != null && blockedUsers.contains(senderKey);
  }

  private void removeUserFromBlockLists(String userKey) {
    for (Map<String, Set<String>> blockedByBlocker : blockedUsersByGroupAndBlocker.values()) {
      blockedByBlocker.remove(userKey);
      for (Set<String> blockedUsers : blockedByBlocker.values()) {
        blockedUsers.remove(userKey);
      }
    }
  }

  private void requireUserInGroup(String groupKey, String userKey, String message) {
    Set<String> users = usersByGroup.get(groupKey);
    if (users == null || !users.contains(userKey)) {
      throw new IllegalArgumentException(message);
    }
  }

  private boolean isUserInAnyGroup(String userKey) {
    for (Set<String> users : usersByGroup.values()) {
      if (users.contains(userKey)) {
        return true;
      }
    }
    return false;
  }

  private void validateName(String value, String label) {
    if (value == null
        || value.trim().length() == 0
        || value.trim().length() > TransportConfig.MAX_NAME_LENGTH) {
      throw new IllegalArgumentException("Invalid " + label + " name.");
    }
  }

  private String normalize(String value) {
    return value.trim().toLowerCase(Locale.ROOT);
  }
}
