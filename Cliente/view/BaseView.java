/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: BaseView
 * Funcao...........: Builds shared JavaFX view behavior.
 *************************************************************** */

package view;

import java.io.File;
import java.net.URL;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public abstract class BaseView {

  protected final AnchorPane layout;
  private final Button closeButton;
  private final Button minimizeButton;
  private double dragOffsetX;
  private double dragOffsetY;
  private boolean draggingWindow;

  protected BaseView(String backgroundResource) {
    this.layout = new AnchorPane();
    this.layout.setPrefSize(1280, 720);
    this.layout.getChildren().add(createBackground(backgroundResource));

    this.minimizeButton = createWindowButton("-");
    this.closeButton = createWindowButton("X");
    place(this.minimizeButton, 1180, 9, 34, 26);
    place(this.closeButton, 1232, 9, 34, 26);
    this.layout.getChildren().addAll(minimizeButton, closeButton);
  }

  public Parent getLayout() {
    return layout;
  }

  public static String resolveResourceUrl(String resourcePath) {
    URL resource = BaseView.class.getResource(resourcePath);
    if (resource != null) {
      return resource.toExternalForm();
    }

    String normalized =
        resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
    File[] candidates = new File[] {
        new File(normalized),
        new File(".." + File.separator + normalized),
        new File("Cliente" + File.separator + normalized)
    };

    for (File candidate : candidates) {
      if (candidate.isFile()) {
        return candidate.toURI().toString();
      }
    }

    throw new IllegalArgumentException("Resource not found: " + resourcePath);
  }

  public void bindWindowControls(final Stage stage, final Runnable closeAction) {
    closeButton.setOnAction(event -> {
      if (closeAction != null) {
        closeAction.run();
      }
      stage.close();
    });

    minimizeButton.setOnAction(event -> stage.setIconified(true));

    layout.setOnMousePressed(event -> {
      draggingWindow = event.getY() <= 42;
      dragOffsetX = event.getSceneX();
      dragOffsetY = event.getSceneY();
    });

    layout.setOnMouseDragged(event -> {
      if (draggingWindow) {
        stage.setX(event.getScreenX() - dragOffsetX);
        stage.setY(event.getScreenY() - dragOffsetY);
      }
    });

    layout.setOnMouseReleased(event -> draggingWindow = false);
  }

  protected TextField createTerminalTextField(String prompt) {
    TextField field = new TextField();
    field.setPromptText(prompt);
    field.setStyle(
        "-fx-background-color: rgba(7, 13, 12, 0.92);" +
        "-fx-border-color: #58d941;" +
        "-fx-border-width: 1;" +
        "-fx-border-radius: 4;" +
        "-fx-background-radius: 4;" +
        "-fx-text-fill: #e2f4dc;" +
        "-fx-prompt-text-fill: #8b918f;" +
        "-fx-highlight-fill: #58d941;" +
        "-fx-highlight-text-fill: #06100d;" +
        "-fx-font-family: 'Monospaced';" +
        "-fx-font-size: 15px;" +
        "-fx-padding: 0 16;");
    return field;
  }

  protected Button createTerminalButton(String text) {
    Button button = new Button(text);
    button.setStyle(
        "-fx-background-color: rgba(9, 18, 15, 0.94);" +
        "-fx-border-color: #58d941;" +
        "-fx-border-width: 1.2;" +
        "-fx-border-radius: 4;" +
        "-fx-background-radius: 4;" +
        "-fx-text-fill: #76f05a;" +
        "-fx-font-family: 'Monospaced';" +
        "-fx-font-size: 16px;" +
        "-fx-cursor: hand;");
    button.setOnMouseEntered(
        event ->
            button.setStyle(
                button.getStyle() + "-fx-background-color: rgba(28, 55, 38, 0.95);"));
    button.setOnMouseExited(event -> button.setStyle(
        "-fx-background-color: rgba(9, 18, 15, 0.94);" +
        "-fx-border-color: #58d941;" +
        "-fx-border-width: 1.2;" +
        "-fx-border-radius: 4;" +
        "-fx-background-radius: 4;" +
        "-fx-text-fill: #76f05a;" +
        "-fx-font-family: 'Monospaced';" +
        "-fx-font-size: 16px;" +
        "-fx-cursor: hand;"));
    return button;
  }

  protected Label createTerminalLabel(String text, int fontSize) {
    Label label = new Label(text);
    label.setStyle(
        "-fx-text-fill: #76f05a;" +
        "-fx-font-family: 'Monospaced';" +
        "-fx-font-size: " + fontSize + "px;");
    return label;
  }

  protected Pane createPanel(double opacity) {
    Pane panel = new Pane();
    panel.setStyle(
        "-fx-background-color: rgba(5, 12, 10, " + opacity + ");" +
        "-fx-border-color: rgba(88, 217, 65, 0.48);" +
        "-fx-border-width: 1;" +
        "-fx-border-radius: 3;" +
        "-fx-background-radius: 3;");
    return panel;
  }

  protected void add(Node node) {
    layout.getChildren().add(node);
  }

  protected void place(Node node, double x, double y, double width, double height) {
    AnchorPane.setLeftAnchor(node, x);
    AnchorPane.setTopAnchor(node, y);
    if (node instanceof Pane) {
      ((Pane) node).setPrefSize(width, height);
    } else if (node instanceof javafx.scene.control.Control) {
      ((javafx.scene.control.Control) node).setPrefSize(width, height);
    } else if (node instanceof ImageView) {
      ((ImageView) node).setFitWidth(width);
      ((ImageView) node).setFitHeight(height);
    }
  }

  private ImageView createBackground(String backgroundResource) {
    Image image =
        new Image(
            resolveResourceUrl(backgroundResource), 1280, 720, false, true);
    ImageView background = new ImageView(image);
    background.setFitWidth(1280);
    background.setFitHeight(720);
    return background;
  }

  private Button createWindowButton(String text) {
    Button button = new Button();
    button.setStyle(
        "-fx-background-color: transparent;" +
        "-fx-border-color: transparent;" +
        "-fx-text-fill: transparent;" +
        "-fx-cursor: hand;");
    return button;
  }
}
