package be.kuleuven.pylos.game;

import be.kuleuven.pylos.gui.PylosGuiController;
import be.kuleuven.pylos.material.Material;
import be.kuleuven.pylos.player.PylosPlayer;
import be.kuleuven.pylos.player.PylosPlayerType;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CubicCurve;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Jan on 13/02/2015.
 */
public class PylosScene {

	public static final PylosPlayerType HUMAN_PLAYER_TYPE = new PylosPlayerType("Human") {
		@Override
		public PylosPlayer create() {
			throw new RuntimeException("Human player should not be created here");
		}
	};

	private static final int MIN_TRANSITION_DURATION = 20;
	private static final int MAX_TRANSITION_DURATION = 400;
	private static final int LIN_TRANSITION_LOCATION = 300;
	private static final int IDLE_DURATION = 100;
	public static final double SPHERE_RADIUS = 200;
//	public static final Color COLOR_BOARD = Color.WHITESMOKE.darker();
	public static final Color COLOR_BOARD = Color.WHITESMOKE;
	public static final Color COLOR_PLAYER_WHITE = Color.BEIGE;
	public static final Color COLOR_PLAYER_WHITE_HOVER = Color.BEIGE.darker();
	public static final Color COLOR_PLAYER_WHITE_HOVER_NOK = Color.RED.brighter();
	public static final Color COLOR_PLAYER_BLACK = Color.MEDIUMPURPLE;
	public static final Color COLOR_PLAYER_BLACK_HOVER = Color.MEDIUMPURPLE.brighter().brighter().brighter();
	public static final Color COLOR_PLAYER_BLACK_HOVER_NOK = Color.RED;

	private final double HEIGHT_DIFF;
	private final double MIN_XY_ON_BOARD;
	private final double MIN_XY_OFF_BOARD;

	private final PylosBoard board;
	private final PylosGuiController controller;
	private final Box boardBox;
	private final PylosSphere3D[] spheresLight;
	private final PylosSphere3D[] spheresDark;
	private final HashMap<PylosLocation, PylosSphere3D> locationSpheres = new HashMap<>();
	private final HashMap<PylosSphere, PhongMaterial[]> materials = new HashMap<>();

	private HumanPlayer playerLight = new HumanPlayer();
	private HumanPlayer playerDark = new HumanPlayer();
	private HumanPlayer currentHumanPlayer;

	private final CubicCurve curve;
	private final Rotate curveRx = new Rotate();
	private final Rotate curveRy = new Rotate();

	public PylosScene(PylosBoard board, Group group, PylosGuiController controller) {
		this.board = board;
		this.controller = controller;
		HEIGHT_DIFF = Math.sin(Math.PI / 4) * SPHERE_RADIUS * 2;
		MIN_XY_ON_BOARD = -SPHERE_RADIUS * this.board.SIZE + SPHERE_RADIUS;
		MIN_XY_OFF_BOARD = -(this.board.SPHERES_PER_PLAYER / 2 + 1) * SPHERE_RADIUS;

		curve = new CubicCurve();
		curve.setVisible(false);
		curve.setFill(null);
		curve.setStroke(Color.RED);
		curve.setStrokeWidth(10);
		curve.setSmooth(true);
		group.getChildren().add(curve);
		curveRx.setAxis(Rotate.X_AXIS);
		curveRx.setAngle(90);
		curveRy.setAxis(Rotate.Y_AXIS);
		curve.getTransforms().addAll(curveRx, curveRy);

		/* create board */
		boardBox = createBoard(this.board.SIZE);
		group.getChildren().add(boardBox);

		/* create spheres */
		spheresLight = new PylosSphere3D[this.board.SPHERES_PER_PLAYER];
		spheresDark = new PylosSphere3D[this.board.SPHERES_PER_PLAYER];
		for (int i = 0; i < this.board.SPHERES_PER_PLAYER; i++) {
			PylosSphere sphereLight = board.getSphere(PylosPlayerColor.LIGHT, i);
			PylosSphere sphereDark = board.getSphere(PylosPlayerColor.DARK, i);
			PylosSphere3D sphereLight3D = spheresLight[i] = PylosSphere3D.boardSphere(sphereLight);
			PylosSphere3D sphereDark3D = spheresDark[i] = PylosSphere3D.boardSphere(sphereDark);
			sphereLight3D.setPosition(getReservePoint(board.getSphere(PylosPlayerColor.LIGHT, i)));
			sphereDark3D.setPosition(getReservePoint(board.getSphere(PylosPlayerColor.DARK, i)));
			sphereLight3D.setOnMouseClicked(evt -> {
				if (currentHumanPlayer != null) currentHumanPlayer.sphereClicked(sphereLight);
			});
			sphereDark3D.setOnMouseClicked(evt -> {
				if (currentHumanPlayer != null) currentHumanPlayer.sphereClicked(sphereDark);
			});
		}
		group.getChildren().addAll(spheresLight);
		group.getChildren().addAll(spheresDark);

		/* create place locations */
		for (PylosLocation bl : board.getLocations()) {
			PylosSphere3D sphere = PylosSphere3D.locationSphere(getBoardPoint(bl));
			group.getChildren().add(sphere);
			locationSpheres.put(bl, sphere);
			sphere.setOnMouseClicked(evt -> {
				if (currentHumanPlayer != null) currentHumanPlayer.locationClicked(bl);
			});
		}

		/* human controls */
		controller.vbInfo.setVisible(false);
		controller.vbInfo.getChildren().remove(controller.btnPass);
		controller.btnPass.setOnAction(evt -> {
			if (currentHumanPlayer != null) {
				currentHumanPlayer.passClicked();
			}
		});
		controller.vbShoutContainer.setVisible(false);
	}

	public PylosPlayer getHumanPlayer(PylosPlayerColor color) {
		return color == PylosPlayerColor.LIGHT ? playerLight : playerDark;
	}

	public double getReservesState(PylosPlayerColor color) {
		return (double) board.getReservesSize(color) / board.SPHERES_PER_PLAYER;
	}

	private PylosSphere3D getSphere(PylosPlayerColor playerColor, int id) {
		return playerColor == PylosPlayerColor.LIGHT ? spheresLight[id] : spheresDark[id];
	}

	private PylosSphere3D getSphere(PylosSphere pylosSphere) {
		return getSphere(pylosSphere.PLAYER_COLOR, pylosSphere.ID);
	}

	private PylosSphere3D getLocationSphere(PylosLocation pylosLocation) {
		return locationSpheres.get(pylosLocation);
	}

	private void animateSphereBow(PylosSphere3D sphere, Point3D from, Point3D to, CountDownLatch latch) {

		double minZ = Math.min(from.getZ(), to.getZ());
		double maxZ = Math.max(from.getZ(), to.getZ());
		double topZ = maxZ + SPHERE_RADIUS * 2;
		double zRange = topZ - minZ;
		double radRange = Math.PI / 2 + Math.acos((maxZ - minZ) / (topZ - minZ));
		double radFrom = from.getZ() < to.getZ() ? 0 : Math.PI - radRange;

		new Transition() {
			{
				int duration = MAX_TRANSITION_DURATION - (int) (controller.getAnimationSpeed() / 100 * (MAX_TRANSITION_DURATION - MIN_TRANSITION_DURATION));
				setCycleDuration(Duration.millis(duration));
				setOnFinished(evt -> latch.countDown());
			}

			@Override
			protected void interpolate(double frac) {
				double z = minZ + zRange * Math.sin(radRange * frac + radFrom);
				Point3D point = new Point3D(
						(to.getX() - from.getX()) * frac + from.getX(),
						(to.getY() - from.getY()) * frac + from.getY(),
						z);
				sphere.setPosition(point);
			}
		}.play();
	}

	private void animateSphereLinear(PylosSphere3D sphere, Point3D from, Point3D to, CountDownLatch latch) {
		new Transition() {
			{
				setCycleDuration(Duration.millis(LIN_TRANSITION_LOCATION));
				setOnFinished(evt -> latch.countDown());
			}

			@Override
			protected void interpolate(double frac) {
				Point3D point = new Point3D(
						(to.getX() - from.getX()) * frac + from.getX(),
						(to.getY() - from.getY()) * frac + from.getY(),
						(to.getZ() - from.getZ()) * frac + from.getZ());
				sphere.setPosition(point);
			}
		}.play();
	}

	private Point3D getBoardPoint(PylosLocation location) {
		double tX = MIN_XY_ON_BOARD + SPHERE_RADIUS * location.Z + SPHERE_RADIUS * 2 * location.X;
		double tY = MIN_XY_ON_BOARD + SPHERE_RADIUS * location.Z + SPHERE_RADIUS * 2 * location.Y;
		double tZ = SPHERE_RADIUS + HEIGHT_DIFF * location.Z;
		return new Point3D(tX, tY, tZ);
	}

	private Point3D getReservePoint(PylosSphere pylosSphere) {
		double side = pylosSphere.PLAYER_COLOR == PylosPlayerColor.LIGHT ? 1 : -1;
		int id = pylosSphere.ID;
		double tX, tY;
		if (pylosSphere.ID < (board.SPHERES_PER_PLAYER + 1) / 2) {
			tX = side * (MIN_XY_OFF_BOARD + SPHERE_RADIUS * 2 * id);
			tY = side * MIN_XY_OFF_BOARD;
		} else {
			id -= board.SPHERES_PER_PLAYER / 2;
			tX = side * MIN_XY_OFF_BOARD;
			tY = side * (MIN_XY_OFF_BOARD + SPHERE_RADIUS * 2 * id);
		}
		return new Point3D(tX, tY, 0);
	}

	private Box createBoard(int gameSize) {
		/* board */
		double height = SPHERE_RADIUS / 2;
		double widthDepth = SPHERE_RADIUS * 2 * (gameSize + 1);
		Box board = new Box(widthDepth, widthDepth, height);
		board.setTranslateZ(-height / 2);
		board.setMaterial(createBoardWoodMaterial(COLOR_BOARD));
		return board;
	}

	private static PhongMaterial createBoardWoodMaterial(Color color) {
		return new PhongMaterial(color, Material.load("Wood5.jpg"), null, null, null);
	}

	public void moveAll(boolean blocking) {
		CountDownLatch latch = new CountDownLatch(blocking ? board.getSpheres().length : 0);
		for (PylosSphere sphere : board.getSpheres()) {
			PylosSphere3D s = getSphere(sphere);
			Point3D from = new Point3D(s.getTranslateX(), s.getTranslateY(), s.getTranslateZ());
			Point3D to = sphere.isReserve() ? getReservePoint(sphere) : getBoardPoint(sphere.getLocation());
			animateSphereLinear(s, from, to, latch);
		}
		if (blocking) {
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void move(final PylosSphere pylosSphere, final PylosLocation prevLocation) {
		CountDownLatch latch = new CountDownLatch(1);
		disableSpheres();
		Platform.runLater(() -> {
			PylosLocation newLocation = pylosSphere.getLocation();
			PylosSphere3D sphere = getSphere(pylosSphere);
			Point3D from = prevLocation == null ? getReservePoint(pylosSphere) : getBoardPoint(prevLocation);
			Point3D to = newLocation == null ? getReservePoint(pylosSphere) : getBoardPoint(newLocation);

//			double dist = Math.sqrt(Math.pow(to.getX() - from.getX(), 2) + Math.pow(to.getY() - from.getY(), 2));
//
//			double topZ = Math.max(from.getZ(), to.getZ()) + SPHERE_RADIUS * 4;
//			double angle = Math.acos(from.getX() - to.getX() / dist) * (180d / Math.PI);
////			curveRy.setAngle(0);
////			curveRx.setAngle(0);
//
//			Point3D center;
//			Point3D rotated;
//			if(from.getY() < to.getY()){
//				center = from;
//				rotated = to;
//			}else{
//				center = to;
//				rotated = from;
//			}
////			if(center.getX()>rotated.getX()){
////				dist = -dist;
////			}
//
//			curve.setStartX(0);
//			curve.setStartY(center.getZ());
//			curve.setControlX1(0);
//			curve.setControlY1(topZ);
//
//			curve.setEndX(dist);
//			curve.setEndY(rotated.getY());
//			curve.setControlX2(dist);
//			curve.setControlY2(topZ);
//
////			curveRx.setAngle(90);
//			curveRy.setAngle(angle);
//			curve.setTranslateX(Math.min(from.getX(), to.getX()));
//			curve.setTranslateY(Math.min(from.getY(), to.getY()));
//			curve.setTranslateZ(Math.min(from.getZ(), to.getZ() + curve.getLayoutBounds().getHeight() / 2));
//			curve.setVisible(true);
////			curveRy.setAngle(angle);

			animateSphereBow(sphere, from, to, latch);

		});
		try {
			latch.await();
			synchronized (this) {
				try {
					wait(IDLE_DURATION);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void reset(boolean blocking) {
		playerLight.reset();
		playerDark.reset();
		board.reset();
		disableSpheres();
		moveAll(blocking);
	}

	public void checkingMoveSphere(PylosSphere pylosSphere, PylosLocation toLocation) {
		disableSpheres();
		Platform.runLater(() -> {
			getSphere(pylosSphere).spin(true);
			getLocationSphere(toLocation).spin(true);
			getLocationSphere(toLocation).setOpacity(1);
		});
	}

	public void checkingRemoveSphere(PylosSphere pylosSphere) {
		disableSpheres();
		Platform.runLater(() ->{
			getSphere(pylosSphere).spin(true);
		});
	}

	public void checkingPass() {
		disableSpheres();
	}

	private void disableSpheres() {
		Platform.runLater(() -> {
			for (PylosSphere ps : board.getSpheres()) {
				getSphere(ps).disableSphere();
			}
			for (PylosLocation bl : board.getLocations()) {
				getLocationSphere(bl).disableSphere();
			}
		});
	}

	private void showTask(PylosGameState gameState) {
		Platform.runLater(() -> {
			switch (gameState) {
				case MOVE:
					controller.lblInfo.setText("Move / Add");
					controller.vbInfo.getChildren().remove(controller.btnPass);
					break;
				case REMOVE_FIRST:
					controller.lblInfo.setText("Remove");
					controller.vbInfo.getChildren().remove(controller.btnPass);
					break;
				case REMOVE_SECOND:
					controller.lblInfo.setText("Remove / Pass");
					controller.vbInfo.getChildren().add(controller.btnPass);
					break;
			}
			controller.vbInfo.setVisible(true);
		});
	}

	private void hideTask() {
		Platform.runLater(() -> controller.vbInfo.setVisible(false));
	}

	public void shout(String str) {
		if (str == null) {
			hideShout();
		} else {
			Platform.runLater(() -> {
				controller.lblShout.setText(str);
				controller.vbShoutContainer.setVisible(true);
			});
		}
	}

	private void hideShout() {
		Platform.runLater(() -> controller.vbShoutContainer.setVisible(false));
	}

	private class HumanPlayer extends PylosPlayer {

		private PylosGameIF game;
		private PylosBoard board;
		private CountDownLatch latch;
		private PylosSphere sphere;
		private PylosLocation toLocation;
		private Boolean pass;

		@Override
		public void doMove(PylosGameIF game, PylosBoard board) {
			initMove(game, board);
		}

		@Override
		public void doRemove(PylosGameIF game, PylosBoard board) {
			initMove(game, board);
		}

		@Override
		public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
			initMove(game, board);
		}

		public void reset() {
			if (latch != null) {
				latch.countDown();
				hideShout();
				hideTask();
			}
		}

		public void sphereClicked(PylosSphere sphere) {
			this.sphere = sphere;
			checkMoveComplete();
		}

		public void locationClicked(PylosLocation location) {
			this.toLocation = location;
			checkMoveComplete();
		}

		public void passClicked() {
			this.pass = true;
			checkMoveComplete();
		}

		private void checkMoveComplete() {
			new Thread(() -> {
				switch (game.getState()) {
					case MOVE:
						if (sphere != null && toLocation != null) {
							disableSpheres();
							game.moveSphere(sphere, toLocation);
							latch.countDown();
						} else {
							enableSpheres();
						}
						break;
					case REMOVE_FIRST:
						if (sphere != null) {
							disableSpheres();
							game.removeSphere(sphere);
							latch.countDown();
						}
						break;
					case REMOVE_SECOND:
						if (sphere != null) {
							disableSpheres();
							game.removeSphere(sphere);
							latch.countDown();
						}
						if (pass != null && pass) {
							disableSpheres();
							game.pass();
							latch.countDown();
						}
						break;
				}
			}).start();
		}

		private void enableSpheres() {
			Platform.runLater(() -> {
				switch (game.getState()) {
					case MOVE:
						if (sphere == null && toLocation == null) {
							/* spheres which can be moved */
							for (PylosSphere ps : board.getSpheres(this)) {
								PylosSphere3D s = getSphere(ps);
								s.enableSphere(ps.canMove());
							}
						} else if (sphere != null && toLocation == null) {
							/* glow/spin selected sphere */
							for (PylosSphere ps : board.getSpheres(this)) {
								getSphere(ps).setGlow(ps == sphere);
								getSphere(ps).spin(ps == sphere);
							}
							PylosSphere3D selectedSphere3D = getSphere(sphere);
							/* locations for this selected sphere */
							for (PylosLocation bl : board.getLocations()) {
								PylosSphere3D locSphere = getLocationSphere(bl);
								if (sphere.canMoveTo(bl)) {
									locSphere.enableSphere();
									locSphere.setOk(!game.moveSphereIsDraw(sphere, bl));
									locSphere.setRotation(selectedSphere3D);
								} else {
									getLocationSphere(bl).disableSphere();
								}
							}
						}
						break;
					case REMOVE_FIRST:
					case REMOVE_SECOND:
						/* spheres which can be removed */
						for (PylosSphere ps : board.getSpheres(this)) {
							PylosSphere3D ps3d = getSphere(ps);
							if (ps.canRemove()) {
								ps3d.enableSphere(true);
								ps3d.setOk(!game.removeSphereIsDraw(ps));
							} else {
								ps3d.enableSphere(false);
							}
						}
						break;
					default:
				}
			});
		}

		private void initMove(PylosGameIF game, PylosBoard board) {
			currentHumanPlayer = this;
			this.game = game;
			this.board = board;
			this.latch = new CountDownLatch(1);
			this.sphere = null;
			this.toLocation = null;
			this.pass = null;

			showTask(game.getState());
			enableSpheres();

			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			hideTask();
			hideShout();

			currentHumanPlayer = null;
		}

	}

}
