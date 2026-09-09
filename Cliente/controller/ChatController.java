/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: ChatController
 * Funcao...........: Controls chat UI actions and client state.
 *************************************************************** */

package controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import model.ChatClient;
import model.ChatClient.ServerResponse;
import model.ChatMessage;
import model.GroupConversation;
import view.BaseView;
import view.ChatView;

public class ChatController extends BaseController {

  private final ChatView view;
  private final ChatClient client;
  private final ObservableList<GroupConversation> groups;
  private final FilteredList<GroupConversation> filteredGroups;
  private GroupConversation selectedGroup;

  public ChatController(Stage stage, ChatClient client) {
    super(stage);
    this.client = client;
    this.view = new ChatView();
    this.groups = FXCollections.observableArrayList();
    this.filteredGroups = new FilteredList<GroupConversation>(groups, group -> true);
    setupInteractions();
  }

  @Override
  public BaseView getView() {
    return view;
  }

  @Override
  protected void setupInteractions() {
    view.getGroupListView().setItems(filteredGroups);
    updateConnectionStatus(
        "Connected to " + client.getServerHost() + " as " + client.getUsername());

    view.getJoinButton().setOnAction(event -> joinGroup());
    view.getGroupField().setOnAction(event -> joinGroup());
    view.getSendButton().setOnAction(event -> sendMessage());
    view.getMessageField().setOnAction(event -> sendMessage());
    view.getLeaveButton().setOnAction(event -> leaveSelectedGroup());
    view.getBlockButton().setOnAction(event -> blockSelectedUser());
    view.getUnblockButton().setOnAction(event -> unblockSelectedUser());

    view
        .getGroupListView()
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (newValue != null) {
                selectGroup(newValue);
              }
            });

    view
        .getSearchField()
        .textProperty()
        .addListener((observable, oldValue, newValue) -> applyGroupFilter(newValue));

    client.setMessageListener(message -> Platform.runLater(new Runnable() {
      @Override
      public void run() {
        receiveMessage(message);
      }
    }));

    client.setErrorListener(message -> Platform.runLater(new Runnable() {
      @Override
      public void run() {
        updateConnectionStatus(message);
      }
    }));

    client.setMembershipListener((group, members) -> Platform.runLater(new Runnable() {
      @Override
      public void run() {
        updateGroupMembers(group, members);
      }
    }));

    renderMessages();
  }

  @Override
  protected void onClose() {
    client.shutdown(groups);
    Platform.exit();
  }

  private void joinGroup() {
    String groupName = view.getGroupField().getText().trim();
    if (!isValidName(groupName)) {
      showWarning("Invalid group", "Use a group name with 1 to 24 characters.");
      return;
    }

    try {
      ServerResponse response = client.joinGroup(groupName);
      GroupConversation group = getOrCreateGroup(groupName);
      group.addParticipant(client.getUsername());
      group.setParticipants(response.getMembers());
      group.addMessage(ChatMessage.systemMessage(group.getName(), response.getMessage()));
      view.getGroupField().clear();
      selectGroup(group);
      view.getGroupListView().getSelectionModel().select(group);
      refreshGroups();
    } catch (IOException e) {
      showWarning("JOIN failed", e.getMessage());
    }
  }

  private void sendMessage() {
    if (selectedGroup == null) {
      showWarning("No group selected", "Join a group before sending messages.");
      return;
    }

    String text = view.getMessageField().getText().trim();
    if (text.length() == 0) {
      return;
    }

    try {
      client.sendMessage(selectedGroup.getName(), text);
      selectedGroup.addMessage(
          ChatMessage.userMessage(selectedGroup.getName(), client.getUsername(), text));
      view.getMessageField().clear();
      renderMessages();
      refreshGroups();
    } catch (IOException e) {
      showWarning("SEND failed", e.getMessage());
    }
  }

  private void leaveSelectedGroup() {
    if (selectedGroup == null) {
      return;
    }

    GroupConversation leaving = selectedGroup;
    try {
      client.leaveGroup(leaving.getName());
      groups.remove(leaving);
      selectedGroup = groups.isEmpty() ? null : groups.get(0);
      if (selectedGroup != null) {
        view.getGroupListView().getSelectionModel().select(selectedGroup);
      }
      renderMessages();
      refreshGroups();
    } catch (IOException e) {
      showWarning("LEAVE failed", e.getMessage());
    }
  }

  private void blockSelectedUser() {
    changeBlockState(true);
  }

  private void unblockSelectedUser() {
    changeBlockState(false);
  }

  private void changeBlockState(boolean block) {
    if (selectedGroup == null) {
      showWarning("No group selected", "Select a group before blocking users.");
      return;
    }

    String target = readBlockTarget();
    if (!isValidName(target)) {
      showWarning("Invalid user", "Enter the user to block or unblock.");
      return;
    }

    if (target.equalsIgnoreCase(client.getUsername())) {
      showWarning("Invalid user", "You cannot block yourself.");
      return;
    }

    try {
      ServerResponse response;
      if (block) {
        response = client.blockUser(selectedGroup.getName(), target);
        selectedGroup.block(target);
      } else {
        response = client.unblockUser(selectedGroup.getName(), target);
        selectedGroup.unblock(target);
      }
      selectedGroup.setParticipants(response.getMembers());
      selectedGroup.addMessage(
          ChatMessage.systemMessage(selectedGroup.getName(), response.getMessage()));
      view.getBlockStatusLabel().setText(response.getMessage());
      renderMessages();
      refreshGroups();
    } catch (IOException e) {
      showWarning(block ? "BLOCK failed" : "UNBLOCK failed", e.getMessage());
    }
  }

  private String readBlockTarget() {
    String editorText = view.getBlockTargetBox().getEditor().getText();
    if (editorText != null && editorText.trim().length() > 0) {
      return editorText.trim();
    }
    String value = view.getBlockTargetBox().getValue();
    return value == null ? "" : value.trim();
  }

  private void receiveMessage(ChatMessage message) {
    GroupConversation group = getOrCreateGroup(message.getGroup());
    group.addParticipant(message.getUser());
    group.addMessage(message);

    if (selectedGroup != null && sameName(selectedGroup.getName(), group.getName())) {
      renderMessages();
    } else {
      group.incrementUnreadCount();
    }
    refreshGroups();
  }

  private GroupConversation getOrCreateGroup(String groupName) {
    for (GroupConversation group : groups) {
      if (sameName(group.getName(), groupName)) {
        return group;
      }
    }

    GroupConversation group = new GroupConversation(groupName);
    group.addParticipant(client.getUsername());
    groups.add(group);
    return group;
  }

  private void updateGroupMembers(String groupName, List<String> members) {
    GroupConversation group = getOrCreateGroup(groupName);
    group.setParticipants(members);
    if (selectedGroup != null && sameName(selectedGroup.getName(), groupName)) {
      updateBlockTargets();
      view.getGroupMetaLabel().setText(buildGroupMetadata(group));
    }
    refreshGroups();
  }

  private void selectGroup(GroupConversation group) {
    selectedGroup = group;
    selectedGroup.clearUnreadCount();
    renderMessages();
    refreshGroups();
  }

  private void renderMessages() {
    view.getMessagesBox().getChildren().clear();

    if (selectedGroup == null) {
      view.getGroupTitleLabel().setText("Join a group");
      view.getGroupMetaLabel().setText("Use + to create or join a group");
      view.getBlockStatusLabel().setText("Select a user to block");
      view.getBlockTargetBox().getItems().clear();
      return;
    }

    view.getGroupTitleLabel().setText(selectedGroup.getName());
    view.getGroupMetaLabel().setText(buildGroupMetadata(selectedGroup));

    for (ChatMessage message : selectedGroup.getMessages()) {
      Node node = view.createMessageNode(message, client.getUsername());
      view.getMessagesBox().getChildren().add(node);
    }

    updateBlockTargets();
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        view.getMessagesScroll().setVvalue(1.0);
      }
    });
  }

  private void updateBlockTargets() {
    if (selectedGroup == null) {
      return;
    }

    Set<String> users = new LinkedHashSet<String>();
    for (String participant : selectedGroup.getParticipants()) {
      if (!participant.equalsIgnoreCase(client.getUsername())) {
        users.add(participant);
      }
    }

    List<String> current = new ArrayList<String>(users);
    view.getBlockTargetBox().getItems().setAll(current);

    String target = readBlockTarget();
    if (target.length() == 0) {
      view.getBlockStatusLabel().setText("Type or select a user to block");
    } else if (selectedGroup.isBlocked(target)) {
      view.getBlockStatusLabel().setText(target + " is blocked for you");
    } else {
      view.getBlockStatusLabel().setText(target + " is not blocked");
    }
  }

  private void applyGroupFilter(String text) {
    final String query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    filteredGroups.setPredicate(
        group -> query.length() == 0 || group.getName().toLowerCase(Locale.ROOT).contains(query));
  }

  private String buildGroupMetadata(GroupConversation group) {
    int messageCount = group.getMessages().size();
    int memberCount = group.getParticipants().size();
    return messageCount
        + " message"
        + (messageCount == 1 ? "" : "s")
        + " | "
        + memberCount
        + " member"
        + (memberCount == 1 ? "" : "s");
  }

  private void refreshGroups() {
    view.getGroupListView().refresh();
  }

  private void updateConnectionStatus(String message) {
    view.getConnectionLabel().setText(message);
    if (message.startsWith("Connected")) {
      view.getFooterConnectionLabel().setText("Connected");
    } else if (message.startsWith("Disconnected")) {
      view.getFooterConnectionLabel().setText("Disconnected");
    } else {
      view.getFooterConnectionLabel().setText("Connection issue");
    }
  }

  private boolean sameName(String a, String b) {
    return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
  }

  private boolean isValidName(String value) {
    return value != null && value.trim().length() > 0 && value.trim().length() <= 24;
  }

  private void showWarning(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle(title);
    alert.setHeaderText(title);
    alert.setContentText(message);
    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    alert.showAndWait();
  }
}
