/* ***************************************************************
 * Autor............: Fabricio da Silva Souza
 * Matricula........: 202411217
 * Inicio...........: 02/07/2026
 * Ultima alteracao.: 03/07/2026
 * Nome.............: BaseController
 * Funcao...........: Defines common controller window behavior.
 *************************************************************** */

package controller;

import javafx.scene.Scene;
import javafx.stage.Stage;
import view.BaseView;

public abstract class BaseController {

  protected final Stage stage;

  protected BaseController(Stage stage) {
    this.stage = stage;
  }

  public void setInterface(BaseView view) {
    Scene scene = new Scene(view.getLayout(), 1280, 720);
    String css = BaseView.resolveResourceUrl("/view/styles.css");
    scene.getStylesheets().add(css);
    stage.setScene(scene);
    view.bindWindowControls(stage, new Runnable() {
      @Override
      public void run() {
        onClose();
      }
    });
  }

  public abstract BaseView getView();

  protected abstract void setupInteractions();

  protected void onClose() {
  }
}
