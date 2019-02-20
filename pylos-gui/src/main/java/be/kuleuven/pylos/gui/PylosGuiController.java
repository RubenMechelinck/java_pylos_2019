package be.kuleuven.pylos.gui;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;
import be.kuleuven.pylos.player.PylosPlayerFactory;
import be.kuleuven.pylos.player.PylosPlayerObserver;
import be.kuleuven.pylos.player.PylosPlayerType;
import be.kuleuven.pylos.player.codes.PlayerFactoryCodes;
import be.kuleuven.pylos.player.student.PlayerFactoryStudent;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.fxml.Initializable;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

public class PylosGuiController implements Initializable, PylosGameObserver, PylosPlayerObserver {

	private static final String COLOR_SHOUT_NORMAL = "rgba(255,255,255,0.2);";
	private static final String COLOR_SHOUT_GOOD = "rgba(0,255,0,0.2);";
	private static final String COLOR_SHOUT_BAD = "rgba(255,0,0,0.2);";

	private static final int CAMERA_RESET_DURATION = 600;
	private static final int CAMERA_ROTATE_DURATION = 1500;
	private static final int IDLE_WIN_DURATION = 800;

	private static final double CAMERA_INITIAL_DISTANCE = -5000;
	private static final double CAMERA_INITIAL_X_ANGLE = 130;
	private static final double CAMERA_INITIAL_Y_ANGLE = 0;
	private static final double CAMERA_INITIAL_Z_ANGLE = 30;
	private static final double CAMERA_NEAR_CLIP = 10;
	private static final double CAMERA_FAR_CLIP = 100000.0;

	private static final double CONTROL_MULTIPLIER = 0.1;
	private static final double SHIFT_MULTIPLIER = 10.0;
	private static final double MOUSE_SPEED = 0.1;
	private static final double ROTATION_SPEED = 3.5;
	private static final double PAN_SPEED = 20;
	private static final double ZOOM_SPEED = 100;

	private static final Random random = new Random(0);

	public Pane panePylosScene;
	public ComboBox<PylosPlayerType> cbPlayerLight;
	public ComboBox<PylosPlayerType> cbPlayerDark;
	public TextArea taLog;
	public ProgressBar pbReservesLight;
	public ProgressBar pbReservesDark;
	public CheckBox cbAnimate;
	public CheckBox cbChecking;
	public Slider slAnimationSpeed;
	public Button btnStart;
	public Button btnReset;
	public VBox vbInfo;
	public Label lblInfo;
	public Button btnPass;
	public VBox vbShoutContainer;
	public Label lblShout;
	public Button btnBattle;
	public TextField tfBattles;

	private Group world = new Group();
	private Group rotateGroup = new Group();
	private Group panGroup = new Group();
	private Camera camera;
	private Rotate rx = new Rotate();
	private Rotate ry = new Rotate();
	private Rotate rz = new Rotate();

	private double mousePosX;
	private double mousePosY;
	private double mouseOldX;
	private double mouseOldY;
	private double mouseDeltaX;
	private double mouseDeltaY;

	private PylosScene pylosScene;
	private PylosBoard pylosBoard;
	private PylosGame game;
	private boolean battleStop;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		TooltipHack.setupCustomTooltipBehavior(100, 100000, 100);
		createCamera();
		createWorld();
		createSubScene();
		createBackground();
		createBoardAndScene();
		controlsPlaying(false);
		setPlayers();
	}

	public double getAnimationSpeed() {
		return slAnimationSpeed.getValue();
	}

	public void resetGame() {
		new Thread(() -> {
			battleStop = true;
			reset();
		}).start();
	}

	public void startGame() {
		controlsPlaying(true);
		new Thread(() -> playGame()).start();
	}

	public void startBattle() {
		controlsPlaying(true);
		new Thread(() -> {
			battleStop = false;
			int n = tfBattles.getText().isEmpty() ? Integer.MAX_VALUE : new Integer(tfBattles.getText());
			for (int i = 0; i < n && !battleStop; i++) {
				playGame();
				resetCamera(true);
				pylosScene.reset(true);
				Platform.runLater(() -> {
					taLog.clear();
					pbReservesLight.setProgress(1);
					pbReservesDark.setProgress(1);
				});
				synchronized (this) {
					try {
						wait(IDLE_WIN_DURATION);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	private void playGame() {
		/* create the game */
		PylosPlayer playerLight = cbPlayerLight.getValue() == PylosScene.HUMAN_PLAYER_TYPE ? pylosScene.getHumanPlayer(PylosPlayerColor.LIGHT) : cbPlayerLight.getValue().create();
		PylosPlayer playerDark = cbPlayerDark.getValue() == PylosScene.HUMAN_PLAYER_TYPE ? pylosScene.getHumanPlayer(PylosPlayerColor.DARK) : cbPlayerDark.getValue().create();
		if (cbPlayerLight.getValue() == PylosScene.HUMAN_PLAYER_TYPE || cbPlayerDark.getValue() == PylosScene.HUMAN_PLAYER_TYPE) {
			Platform.runLater(() -> cbAnimate.setSelected(true));
		}
		game = new PylosGame(pylosBoard, playerLight, playerDark, random, this, this);
		/* play the game */
		game.play();
	}

	private void reset() {
		if (game != null) {
			if (!game.isFinished()) {
				shoutBad("Aborting...");
				game.abort();
			} else {
				controlsPlaying(false);
				pylosScene.reset(true);
			}
			resetCamera(false);
		} else {
			resetCamera(false);
		}
	}

	private void controlsPlaying(boolean playing) {
		Platform.runLater(() -> {
			btnReset.setDisable(!playing);
			btnStart.setDisable(playing);
			cbPlayerLight.setDisable(playing);
			cbPlayerDark.setDisable(playing);
			cbAnimate.setDisable(playing);
			cbChecking.setDisable(playing);
			vbShoutContainer.setVisible(false);
			btnBattle.setDisable(playing);
			tfBattles.setDisable(playing);
			if (!playing) {
				taLog.clear();
				pbReservesLight.setProgress(1);
				pbReservesDark.setProgress(1);
			}
		});
	}

	private void createBackground() {
		RadialGradient shadePaint = new RadialGradient(
				0, 0, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE,
				new Stop(1, Color.BLACK),
				new Stop(0, Color.TRANSPARENT)
		);
		panePylosScene.setBackground(new Background(new BackgroundFill(shadePaint, null, null)));
	}

	private void createWorld() {
		world.getChildren().add(rotateGroup);
		rotateGroup.getChildren().addAll(panGroup);
		world.getChildren().add(camera);
	}

	private void createSubScene() {
		SubScene ss = new SubScene(world, 0, 0, true, SceneAntialiasing.BALANCED);
		ss.widthProperty().bind(panePylosScene.widthProperty());
		ss.heightProperty().bind(panePylosScene.heightProperty());
		ss.setCamera(camera);
		handleMouse(panePylosScene);
		panePylosScene.getChildren().add(ss);
	}

	private void setPlayers() {

		ArrayList<PylosPlayerType> allTypes = new ArrayList<>();
		allTypes.add(PylosScene.HUMAN_PLAYER_TYPE);
		allTypes.addAll(new PlayerFactoryCodes().getTypes());
		allTypes.addAll(new PlayerFactoryStudent().getTypes());

		cbPlayerLight.getItems().addAll(allTypes);
		cbPlayerDark.getItems().addAll(allTypes);
		cbPlayerLight.getSelectionModel().select(0);
		cbPlayerDark.getSelectionModel().select(0);

	}

	public void setPlayers(PylosPlayerFactory factory1, PylosPlayerFactory factory2) {
		cbPlayerLight.getItems().clear();
		cbPlayerDark.getItems().clear();

		ArrayList<PylosPlayerType> allTypes = new ArrayList<>();
		allTypes.addAll(factory1.getTypes());
		allTypes.add(PylosScene.HUMAN_PLAYER_TYPE);
		cbPlayerLight.getItems().addAll(allTypes);
		cbPlayerLight.getSelectionModel().select(0);

		allTypes.clear();
		allTypes.addAll(factory2.getTypes());
		allTypes.add(PylosScene.HUMAN_PLAYER_TYPE);
		cbPlayerDark.getItems().addAll(allTypes);
		cbPlayerDark.getSelectionModel().select(0);
	}

	private void createBoardAndScene() {
		pylosBoard = new PylosBoard();
		pylosScene = new PylosScene(pylosBoard, panGroup, this);
	}

	private void createCamera() {

		PerspectiveCamera cam = new PerspectiveCamera(true);

		rx.setAxis(Rotate.X_AXIS);
		ry.setAxis(Rotate.Y_AXIS);
		rz.setAxis(Rotate.Z_AXIS);
		rx.setAngle(CAMERA_INITIAL_X_ANGLE);
		ry.setAngle(CAMERA_INITIAL_Y_ANGLE);
		rz.setAngle(CAMERA_INITIAL_Z_ANGLE);
		rotateGroup.getTransforms().addAll(rx, ry, rz);

		cam.setNearClip(CAMERA_NEAR_CLIP);
		cam.setFarClip(CAMERA_FAR_CLIP);
		cam.setTranslateZ(CAMERA_INITIAL_DISTANCE);
		cam.setFieldOfView(50);
		cam.setFieldOfView(80);

		camera = cam;
	}

	private void handleMouse(Pane pane) {
		pane.addEventFilter(MouseEvent.MOUSE_PRESSED, me -> {
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseOldX = me.getSceneX();
			mouseOldY = me.getSceneY();
		});
		pane.addEventFilter(MouseEvent.MOUSE_DRAGGED, me -> {
			mouseOldX = mousePosX;
			mouseOldY = mousePosY;
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseDeltaX = (mousePosX - mouseOldX);
			mouseDeltaY = (mousePosY - mouseOldY);

			double modifier = 1.0;
			if (me.isControlDown()) modifier = CONTROL_MULTIPLIER;
			if (me.isShiftDown()) modifier = SHIFT_MULTIPLIER;

			if (me.isPrimaryButtonDown()) {
				/* rotate */
				rz.setAngle(rz.getAngle() + mouseDeltaX * MOUSE_SPEED * modifier * ROTATION_SPEED);
				rx.setAngle(rx.getAngle() + mouseDeltaY * MOUSE_SPEED * modifier * ROTATION_SPEED);
			} else if (me.isSecondaryButtonDown()) {
				/* pan */
				panGroup.setTranslateX(panGroup.getTranslateX() + mouseDeltaX * MOUSE_SPEED * modifier * PAN_SPEED);
				rotateGroup.setTranslateY(rotateGroup.getTranslateY() + mouseDeltaY * MOUSE_SPEED * modifier * PAN_SPEED);
			}
		});

		pane.addEventFilter(ScrollEvent.SCROLL, se -> {
			double modifier = 1.0;
			if (se.isControlDown()) modifier = CONTROL_MULTIPLIER;
			if (se.isShiftDown()) modifier = SHIFT_MULTIPLIER;
			double z = camera.getTranslateZ();
			double newZ = z + MOUSE_SPEED * ZOOM_SPEED * modifier * se.getDeltaY();
			camera.setTranslateZ(newZ);
		});
	}

	private void resetCamera(boolean blocking) {
		animateCameraTo(CAMERA_INITIAL_X_ANGLE, CAMERA_INITIAL_Y_ANGLE, CAMERA_INITIAL_Z_ANGLE, CAMERA_INITIAL_DISTANCE, CAMERA_RESET_DURATION, blocking);
	}

	private void rotateCameraForWin(boolean blocking) {
		double deltaX = CAMERA_INITIAL_X_ANGLE - rx.getAngle();
		double deltaY = CAMERA_INITIAL_Y_ANGLE - ry.getAngle();
		double deltaZ = CAMERA_INITIAL_Z_ANGLE - rz.getAngle() + 360;
		double deltaDist = CAMERA_INITIAL_DISTANCE - camera.getTranslateZ();

		animateCameraDelta(rx.getAngle(), deltaX, ry.getAngle(), deltaY, rz.getAngle() - 360, deltaZ, camera.getTranslateZ(), deltaDist, CAMERA_ROTATE_DURATION, blocking);
	}

	private void animateCameraTo(double xAngle, double yAngle, double zAngle, double distance, int duration, boolean blocking) {

		double deltaX = xAngle - rx.getAngle();
		double deltaY = yAngle - ry.getAngle();
		double deltaZ = zAngle - rz.getAngle();
		double deltaDist = distance - camera.getTranslateZ();

		animateCameraDelta(deltaX, deltaY, deltaZ, deltaDist, duration, blocking);
	}

	private void animateCameraDelta(double deltaX, double deltaY, double deltaZ, double deltaDist, int duration, boolean blocking) {
		double fromX = rx.getAngle();
		double fromY = ry.getAngle();
		double fromZ = rz.getAngle();
		double fromDist = camera.getTranslateZ();
		animateCameraDelta(fromX, deltaX, fromY, deltaY, fromZ, deltaZ, fromDist, deltaDist, duration, blocking);
	}

	private void animateCameraDelta(double fromX, double deltaX, double fromY, double deltaY, double fromZ, double deltaZ, double fromDist, double deltaDist, int duration, boolean blocking) {

		CountDownLatch latch = new CountDownLatch(blocking ? 1 : 0);

		new Transition() {
			{
//				int duration = MAX_TRANSITION_DURATION - (int) (controller.getAnimationSpeed() / 100 * (MAX_TRANSITION_DURATION - MIN_TRANSITION_DURATION));
				setCycleDuration(Duration.millis(duration));
				setOnFinished(evt -> {
					latch.countDown();
				});
			}

			@Override
			protected void interpolate(double frac) {
				double factor = (Math.sin(frac * Math.PI - Math.PI / 2) + 1) / 2;
				rx.setAngle(fromX + deltaX * factor);
				ry.setAngle(fromY + deltaY * factor);
				rz.setAngle(fromZ + deltaZ * factor);
				camera.setTranslateZ(fromDist + deltaDist * factor);
			}
		}.play();

		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void println(String str) {
		Platform.runLater(() -> taLog.appendText(str + "\n"));
	}

	@Override
	public void move(PylosSphere pylosSphere, PylosLocation prevLocation) {
		if (cbAnimate.isSelected()) {
			pylosScene.move(pylosSphere, prevLocation);
			pbReservesLight.setProgress(pylosScene.getReservesState(PylosPlayerColor.LIGHT));
			pbReservesDark.setProgress(pylosScene.getReservesState(PylosPlayerColor.DARK));
		}
	}

	@Override
	public void completed(PylosPlayer winningPlayer) {
		println("WINNER: " + winningPlayer);
		if (!cbAnimate.isSelected()) {
			pylosScene.moveAll(false);
		}
		shout("Winner: " + winningPlayer + " (" + game.getReserveSizeOfWinner() + ")");
		rotateCameraForWin(true);
	}

	@Override
	public void aborted() {
		println("ABORTED");
		shoutBad("Aborted");
		pylosScene.reset(true);
		controlsPlaying(false);
	}

	@Override
	public void draw() {
		println("DRAW");
		shoutBad("Draw");
	}

	@Override
	public void shout(String str) {
		Platform.runLater(() -> vbShoutContainer.setStyle("-fx-background-color: " + COLOR_SHOUT_NORMAL));
		pylosScene.shout(str);
	}

	@Override
	public void shoutGood(String str) {
		Platform.runLater(() -> vbShoutContainer.setStyle("-fx-background-color: " + COLOR_SHOUT_GOOD));
		pylosScene.shout(str);
	}

	@Override
	public void shoutBad(String str) {
		Platform.runLater(() -> vbShoutContainer.setStyle("-fx-background-color: " + COLOR_SHOUT_BAD));
		pylosScene.shout(str);
	}

	@Override
	public void checkingMoveSphere(PylosSphere pylosSphere, PylosLocation toLocation) {
		if (cbAnimate.isSelected() && cbChecking.isSelected()) {
			pylosScene.checkingMoveSphere(pylosSphere, toLocation);
		}
	}

	@Override
	public void checkingRemoveSphere(PylosSphere pylosSphere) {
		if (cbAnimate.isSelected() && cbChecking.isSelected()) {
			pylosScene.checkingRemoveSphere(pylosSphere);
		}
	}

	@Override
	public void checkingPass() {
		if (cbAnimate.isSelected() && cbChecking.isSelected()) {
			pylosScene.checkingPass();
		}
	}

}
