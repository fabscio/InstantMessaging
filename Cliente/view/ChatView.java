/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: ChatView
 * Funcao...........: Builds the main chat interface.
 *************************************************************** */

package view;

import java.time.format.DateTimeFormatter;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.ChatMessage;
import model.GroupConversation;

public class ChatView extends BaseView {

  private final TextField groupField;
  private final Button joinButton;
  private final TextField searchField;
  private final ListView<GroupConversation> groupListView;
  private final Label connectionLabel;
  private final Label footerConnectionLabel;
  private final Label groupTitleLabel;
  private final Label groupMetaLabel;
  private final ComboBox<String> blockTargetBox;
  private final Button blockButton;
  private final Button unblockButton;
  private final Label blockStatusLabel;
  private final VBox messagesBox;
  private final ScrollPane messagesScroll;
  private final TextField messageField;
  private final Button sendButton;
  private final Button leaveButton;

  public ChatView() {
    super("/img/game/background.png");

    groupField = createTerminalTextField("Group name...");
    styleInputOverlay(groupField);
    place(groupField, 25, 62, 228, 40);
    add(groupField);

    joinButton = createTerminalButton("+");
    styleHotspot(joinButton);
    place(joinButton, 261, 62, 45, 40);
    add(joinButton);

    searchField = createTerminalTextField("Search groups...");
    styleInputOverlay(searchField);
    place(searchField, 25, 114, 281, 36);
    add(searchField);

    groupListView = new ListView<GroupConversation>();
    groupListView.setItems(FXCollections.<GroupConversation>observableArrayList());
    groupListView.setCellFactory(list -> new GroupCell());
    styleGroupList();
    place(groupListView, 25, 213, 281, 410);
    add(groupListView);

    connectionLabel = createTerminalLabel("Disconnected", 12);
    Pane sideStatusCover = createPanel(0.98);
    sideStatusCover.setStyle(
        "-fx-background-color: rgba(5, 12, 10, 0.98);" +
        "-fx-border-color: transparent;");
    place(sideStatusCover, 24, 634, 180, 24);
    add(sideStatusCover);
    place(connectionLabel, 25, 635, 260, 24);
    add(connectionLabel);

    footerConnectionLabel = createTerminalLabel("Disconnected", 12);
    Pane footerStatusCover = createPanel(0.98);
    footerStatusCover.setStyle(
        "-fx-background-color: rgba(5, 12, 10, 0.98);" +
        "-fx-border-color: transparent;");
    place(footerStatusCover, 20, 684, 160, 25);
    add(footerStatusCover);
    place(footerConnectionLabel, 23, 684, 260, 26);
    add(footerConnectionLabel);

    Pane titleCover = createTextCover(0.98);
    place(titleCover, 438, 78, 300, 66);
    add(titleCover);

    groupTitleLabel = createTerminalLabel("Join a group", 24);
    place(groupTitleLabel, 442, 80, 380, 36);
    add(groupTitleLabel);

    groupMetaLabel = createTerminalLabel("No active conversation", 13);
    groupMetaLabel.setStyle(groupMetaLabel.getStyle() + "-fx-text-fill: #a6aaa8;");
    place(groupMetaLabel, 442, 113, 420, 28);
    add(groupMetaLabel);

    blockTargetBox = new ComboBox<String>();
    blockTargetBox.getStyleClass().add("terminal-combo");
    blockTargetBox.setEditable(true);
    blockTargetBox.setPromptText("User...");
    styleComboOverlay();
    place(blockTargetBox, 755, 83, 198, 39);
    add(blockTargetBox);

    blockButton = createTerminalButton("Block");
    styleHotspot(blockButton);
    place(blockButton, 964, 83, 78, 39);
    add(blockButton);

    unblockButton = createTerminalButton("Unblock");
    styleHotspot(unblockButton);
    place(unblockButton, 1054, 83, 94, 39);
    add(unblockButton);

    leaveButton = createTerminalButton("Leave");
    styleHotspot(leaveButton);
    place(leaveButton, 1160, 83, 78, 39);
    add(leaveButton);

    Pane blockStatusCover = createTextCover(0.98);
    place(blockStatusCover, 960, 132, 294, 24);
    add(blockStatusCover);

    blockStatusLabel = createTerminalLabel("Select a user to block", 11);
    blockStatusLabel.setStyle(blockStatusLabel.getStyle() + "-fx-text-fill: #76f05a;");
    place(blockStatusLabel, 962, 132, 295, 24);
    add(blockStatusLabel);

    messagesBox = new VBox(12);
    messagesBox.setPadding(new Insets(18, 18, 18, 18));
    messagesBox.setFillWidth(true);
    messagesScroll = new ScrollPane(messagesBox);
    messagesScroll.setFitToWidth(true);
    messagesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    styleScrollOverlay();
    place(messagesScroll, 344, 164, 910, 406);
    add(messagesScroll);

    messageField = createTerminalTextField("Type a message...");
    styleInputOverlay(messageField);
    place(messageField, 403, 588, 717, 48);
    add(messageField);

    sendButton = createTerminalButton("Send");
    styleHotspot(sendButton);
    place(sendButton, 1129, 586, 125, 51);
    add(sendButton);
  }

  public TextField getGroupField() {
    return groupField;
  }

  public Button getJoinButton() {
    return joinButton;
  }

  public TextField getSearchField() {
    return searchField;
  }

  public ListView<GroupConversation> getGroupListView() {
    return groupListView;
  }

  public Label getConnectionLabel() {
    return connectionLabel;
  }

  public Label getFooterConnectionLabel() {
    return footerConnectionLabel;
  }

  public Label getGroupTitleLabel() {
    return groupTitleLabel;
  }

  public Label getGroupMetaLabel() {
    return groupMetaLabel;
  }

  public ComboBox<String> getBlockTargetBox() {
    return blockTargetBox;
  }

  public Button getBlockButton() {
    return blockButton;
  }

  public Button getUnblockButton() {
    return unblockButton;
  }

  public Label getBlockStatusLabel() {
    return blockStatusLabel;
  }

  public VBox getMessagesBox() {
    return messagesBox;
  }

  public ScrollPane getMessagesScroll() {
    return messagesScroll;
  }

  public TextField getMessageField() {
    return messageField;
  }

  public Button getSendButton() {
    return sendButton;
  }

  public Button getLeaveButton() {
    return leaveButton;
  }

  public Node createMessageNode(ChatMessage message, String currentUser) {
    if (message.isSystem()) {
      Label label = createTerminalLabel(message.getText(), 12);
      label.setStyle(label.getStyle() + "-fx-text-fill: #8da98a;");
      HBox wrapper = new HBox(label);
      wrapper.setAlignment(Pos.CENTER);
      return wrapper;
    }

    boolean own = message.getUser().equalsIgnoreCase(currentUser);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    Label userLabel =
        createTerminalLabel(
            formatter.format(message.getTime()) + "  [" + message.getUser() + "]", 13);
    userLabel.setStyle(userLabel.getStyle() + "-fx-text-fill: " + (own ? "#76f05a;" : "#61e64b;"));

    Label textLabel = new Label(message.getText());
    textLabel.setWrapText(true);
    textLabel.setMaxWidth(520);
    textLabel.setStyle(
        "-fx-background-color: " + (own ? "rgba(20, 56, 31, 0.95);" : "rgba(25, 31, 32, 0.96);") +
        "-fx-border-color: " + (own ? "#58d941;" : "rgba(206, 217, 204, 0.45);") +
        "-fx-border-radius: 4;" +
        "-fx-background-radius: 4;" +
        "-fx-text-fill: #eef6ea;" +
        "-fx-font-family: 'Monospaced';" +
        "-fx-font-size: 14px;" +
        "-fx-padding: 10 14;");

    VBox bubble = new VBox(5, userLabel, textLabel);
    HBox wrapper = new HBox(bubble);
    wrapper.setAlignment(own ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
    HBox.setHgrow(bubble, Priority.NEVER);
    return wrapper;
  }

  private class GroupCell extends ListCell<GroupConversation> {
    @Override
    protected void updateItem(GroupConversation group, boolean empty) {
      super.updateItem(group, empty);
      if (empty || group == null) {
        setGraphic(null);
        setText(null);
        setStyle("-fx-background-color: transparent;");
        return;
      }

      Label name = createTerminalLabel(group.getName(), 14);
      name.setMaxWidth(170);
      Label preview = new Label(group.getLastPreview());
      preview.setMaxWidth(190);
      preview.setStyle(
          "-fx-text-fill: #a9b1ae;" +
          "-fx-font-family: 'Monospaced';" +
          "-fx-font-size: 11px;");

      VBox texts = new VBox(4, name, preview);
      Label unread =
          createTerminalLabel(
              group.getUnreadCount() > 0 ? String.valueOf(group.getUnreadCount()) : "", 12);
      unread.setAlignment(Pos.CENTER);
      unread.setMinWidth(28);

      HBox row = new HBox(10, texts, unread);
      row.setAlignment(Pos.CENTER_LEFT);
      row.setPadding(new Insets(8, 8, 8, 8));
      HBox.setHgrow(texts, Priority.ALWAYS);

      setGraphic(row);
      setText(null);
      setStyle("-fx-background-color: transparent;");
    }
  }

  private Pane createTextCover(double opacity) {
    Pane cover = new Pane();
    cover.setStyle(
        "-fx-background-color: rgba(5, 12, 10, " + opacity + ");" +
        "-fx-border-color: transparent;");
    return cover;
  }

  private void styleInputOverlay(TextField field) {
    field.setStyle(
        "-fx-background-color: rgba(5, 12, 10, 0.94);" +
        "-fx-border-color: transparent;" +
        "-fx-background-radius: 3;" +
        "-fx-border-radius: 3;" +
        "-fx-text-fill: #e2f4dc;" +
        "-fx-prompt-text-fill: #8b918f;" +
        "-fx-highlight-fill: #58d941;" +
        "-fx-highlight-text-fill: #06100d;" +
        "-fx-font-family: 'Monospaced';" +
        "-fx-font-size: 15px;" +
        "-fx-padding: 0 16;" +
        "-fx-focus-color: transparent;" +
        "-fx-faint-focus-color: transparent;");
  }

  private void styleComboOverlay() {
    blockTargetBox.setStyle(
        "-fx-background-color: rgba(5, 12, 10, 0.96);" +
        "-fx-border-color: transparent;" +
        "-fx-background-radius: 3;" +
        "-fx-border-radius: 3;" +
        "-fx-font-family: 'Monospaced';" +
        "-fx-font-size: 13px;" +
        "-fx-focus-color: transparent;" +
        "-fx-faint-focus-color: transparent;");
    blockTargetBox.getEditor().setStyle(
        "-fx-background-color: transparent;" +
        "-fx-text-fill: #e2f4dc;" +
        "-fx-prompt-text-fill: #8b918f;" +
        "-fx-font-family: 'Monospaced';" +
        "-fx-font-size: 13px;");
  }

  private void styleHotspot(Button button) {
    button.setText("");
    button.setOnMouseEntered(null);
    button.setOnMouseExited(null);
    button.setStyle(
        "-fx-background-color: transparent;" +
        "-fx-border-color: transparent;" +
        "-fx-text-fill: transparent;" +
        "-fx-cursor: hand;" +
        "-fx-focus-color: transparent;" +
        "-fx-faint-focus-color: transparent;");
  }

  private void styleGroupList() {
    groupListView.setStyle(
        "-fx-background-color: transparent;" +
        "-fx-control-inner-background: transparent;" +
        "-fx-border-color: transparent;" +
        "-fx-font-family: 'Monospaced';" +
        "-fx-focus-color: transparent;" +
        "-fx-faint-focus-color: transparent;");
  }

  private void styleScrollOverlay() {
    messagesScroll.setStyle(
        "-fx-background: transparent;" +
        "-fx-background-color: transparent;" +
        "-fx-control-inner-background: transparent;" +
        "-fx-border-color: transparent;" +
        "-fx-focus-color: transparent;" +
        "-fx-faint-focus-color: transparent;");
  }
}
