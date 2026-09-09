/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: Principal
 * Funcao...........: Starts the NetChat Terminal JavaFX client.
 *************************************************************** */

import controller.MenuController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import view.BaseView;

public class Principal extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    stage.initStyle(StageStyle.UNDECORATED);
    stage.setTitle("NetChat Terminal");
    stage.setResizable(false);
    stage
        .getIcons()
        .add(new Image(BaseView.resolveResourceUrl("/img/application_icon.png")));
    stage.setOnCloseRequest(event -> {
      Platform.exit();
      System.exit(0);
    });

    MenuController menuController = new MenuController(stage);
    menuController.setInterface(menuController.getView());
    stage.show();
  }
}
