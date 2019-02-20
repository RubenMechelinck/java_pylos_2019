package be.kuleuven.pylos.gui;

import be.kuleuven.pylos.player.codes.PlayerFactoryCodes;
import be.kuleuven.pylos.player.student.PlayerFactoryStudent;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PylosGuiMain extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {

		FXMLLoader fxmlLoader = new FXMLLoader(PylosGuiMain.class.getResource("PylosGui.fxml"));
		Parent root = fxmlLoader.load();
		primaryStage.setMaximized(true);
		primaryStage.setTitle("Pylos - CODeS");
		primaryStage.setScene(new Scene(root));
		primaryStage.show();
		primaryStage.setOnCloseRequest((evt) -> System.exit(0));

		PylosGuiController controller = fxmlLoader.getController();



//		controller.setPlayers(new PlayerFactoryCodes(), new PlayerFactoryStudent());
//		controller.setPlayers(new PlayerFactoryCodes(), new CodesFactory());
//		controller.setPlayers(new PlayerFactoryCodes(), new PlayerFactoryCodes());
	}

	public static void main(String[] args) {
		launch(args);
	}

}
