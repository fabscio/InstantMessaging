/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: MenuView
 * Funcao...........: Builds the connection menu interface.
 *************************************************************** */

package view;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class MenuView extends BaseView {

  private final TextField usernameField;
  private final TextField serverField;
  private final Button startButton;
  private final Label statusLabel;

  public MenuView() {
    super("/img/menu/background.png");

    usernameField = createTerminalTextField("Enter your name...");
    place(usernameField, 387, 344, 505, 43);
    add(usernameField);

    serverField = createTerminalTextField("Server IP");
    serverField.setText("127.0.0.1");
    place(serverField, 387, 443, 505, 43);
    add(serverField);

    startButton = createTerminalButton("Start");
    place(startButton, 525, 517, 230, 46);
    add(startButton);

    statusLabel = createTerminalLabel("Ready to connect", 13);
    statusLabel.setStyle(statusLabel.getStyle() + "-fx-text-fill: #76f05a;");
    place(statusLabel, 0, 584, 1280, 24);
    statusLabel.setAlignment(javafx.geometry.Pos.CENTER);
    add(statusLabel);
  }

  public TextField getUsernameField() {
    return usernameField;
  }

  public TextField getServerField() {
    return serverField;
  }

  public Button getStartButton() {
    return startButton;
  }

  public Label getStatusLabel() {
    return statusLabel;
  }
}
