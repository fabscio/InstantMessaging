/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: MenuController
 * Funcao...........: Controls the connection screen.
 *************************************************************** */

package controller;

import java.io.IOException;
import javafx.scene.control.Alert;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import model.ChatClient;
import view.BaseView;
import view.MenuView;

public class MenuController extends BaseController {

  private final MenuView view;

  public MenuController(Stage stage) {
    super(stage);
    this.view = new MenuView();
    setupInteractions();
  }

  @Override
  public BaseView getView() {
    return view;
  }

  @Override
  protected void setupInteractions() {
    view.getStartButton().setOnAction(event -> connect());
    view.getUsernameField().setOnAction(event -> connect());
    view.getServerField().setOnAction(event -> connect());
  }

  private void connect() {
    String username = view.getUsernameField().getText().trim();
    String serverIp = view.getServerField().getText().trim();

    if (username.length() == 0) {
      showWarning("Username required", "Enter a username before connecting.");
      return;
    }

    if (serverIp.length() == 0) {
      showWarning("Server IP required", "Enter the server IP. Use 127.0.0.1 for local testing.");
      return;
    }

    view.getStatusLabel().setText("Connecting to " + serverIp + "...");
    ChatClient client = new ChatClient(username, serverIp);
    try {
      client.start();
      ChatController chatController = new ChatController(stage, client);
      chatController.setInterface(chatController.getView());
    } catch (IOException e) {
      view.getStatusLabel().setText("Disconnected");
      showWarning("Connection failed", e.getMessage());
    }
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
