package be.kuleuven.pylos.game;

import be.kuleuven.pylos.material.Material;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.Random;

/**
 * Created by Jan on 4/03/2015.
 */
public class PylosSphere3D extends Sphere {

	private enum Type {BOARD, LOCATION}

	private static final Random random = new Random(0);
	private static final int SPIN_PERIOD = 30;
	private static final int[] SPIN_DELTA = {1,2,3};

	private Rotate rX = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
	private Rotate rY = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
	private Rotate rZ = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);

	private final Timeline spinTimeline;
	private final Type type;
	private PhongMaterial matOk;
	private PhongMaterial matOkHover;
	private PhongMaterial matNok;
	private PhongMaterial matNokHover;

	private boolean isOk = true;

	private PylosSphere3D(Type type) {
		this(type, PylosScene.SPHERE_RADIUS);
	}

	private PylosSphere3D(Type type, double radius) {
		super(radius);
		getTransforms().addAll(rX, rY, rZ);
		this.type = type;

		spinTimeline = new Timeline(new KeyFrame(Duration.millis(SPIN_PERIOD), evt -> {
			setRotation(rX.getAngle()+SPIN_DELTA[0], rY.getAngle()+SPIN_DELTA[1], rZ.getAngle()+SPIN_DELTA[2]);
		}));
		spinTimeline.setCycleCount(Timeline.INDEFINITE);
	}

	public static PylosSphere3D boardSphere(PylosSphere pylosSphere) {
		PylosSphere3D sphere = new PylosSphere3D(Type.BOARD);

		sphere.matOk = createWoodMaterial(pylosSphere.PLAYER_COLOR == PylosPlayerColor.LIGHT ? PylosScene.COLOR_PLAYER_WHITE : PylosScene.COLOR_PLAYER_BLACK);
		sphere.matOkHover = createWoodMaterial(pylosSphere.PLAYER_COLOR == PylosPlayerColor.LIGHT ? PylosScene.COLOR_PLAYER_WHITE_HOVER: PylosScene.COLOR_PLAYER_BLACK_HOVER);
		sphere.matNok = sphere.matOk;
		sphere.matNokHover = createWoodMaterial(pylosSphere.PLAYER_COLOR == PylosPlayerColor.LIGHT ? PylosScene.COLOR_PLAYER_WHITE_HOVER_NOK: PylosScene.COLOR_PLAYER_BLACK_HOVER_NOK);

		sphere.setOk(true);
		Tooltip.install(sphere, new Tooltip(
				"Player: " + (pylosSphere.PLAYER_COLOR.ordinal() + 1) + "\n" + "Id: " + pylosSphere.ID
		));
		sphere.setOnMouseEntered(evt -> {
			sphere.setMaterial(sphere.isOk ? sphere.matOkHover : sphere.matNokHover);
		});
		sphere.setOnMouseExited(evt -> {
			sphere.setMaterial(sphere.isOk ? sphere.matOk : sphere.matNok);
		});

		sphere.setMouseTransparent(true);
		sphere.setRotation(random.nextInt(180), random.nextInt(180), random.nextInt(180));

		return sphere;
	}

	public static PylosSphere3D locationSphere(Point3D locationPoint) {
		PylosSphere3D sphere = new PylosSphere3D(Type.LOCATION);
		sphere.matOk = createWoodMaterial(Color.GREEN);
		sphere.matNok = createWoodMaterial(Color.RED);
		sphere.setOpacity(0);
		sphere.setMouseTransparent(true);

		sphere.setOnMouseEntered(evt -> {
			sphere.setOpacity(1);
		});
		sphere.setOnMouseExited(evt -> {
			sphere.setOpacity(0);
		});

		sphere.setPosition(locationPoint);
		sphere.setOk(true);

		return sphere;
	}

	public void setOk(boolean ok){
		isOk = ok;
		setMaterial(ok ? matOk : matNok);
	}

	public void setPosition(Point3D p) {
		setTranslateX(p.getX());
		setTranslateY(p.getY());
		setTranslateZ(p.getZ());
	}

	public void setRotation(PylosSphere3D other) {
		rX.setAngle(other.rX.getAngle());
		rY.setAngle(other.rY.getAngle());
		rZ.setAngle(other.rZ.getAngle());
	}

	public void setRotation(double xAx, double yAx, double zAx) {
		rX.setAngle(xAx);
		rY.setAngle(yAx);
		rZ.setAngle(zAx);
	}

	public void setGlow(boolean glow) {
		if (type == Type.BOARD) {
//			matOk.setSpecularColor(glow ? Color.RED : null);
//			matOkHover.setSpecularColor(glow ? Color.RED : null);
//			matNok.setSpecularColor(glow ? Color.RED : null);
//			matNokHover.setSpecularColor(glow ? Color.RED : null);
		} else {

		}
	}

	public void enableSphere(boolean enable){
		if(enable) enableSphere();
		else disableSphere();
	}

	public void enableSphere() {
		setMouseTransparent(false);
		setOk(true);
		if (type == Type.BOARD) {
			setGlow(false);
		} else {
			setOpacity(0);
			spin(true);
		}
	}

	public void disableSphere() {
		setMouseTransparent(true);
		setOk(true);
		spinTimeline.stop();
		if (type == Type.BOARD) {
			setGlow(false);
		} else {
			setOpacity(0);
			spin(false);
		}
	}

	public void spin(boolean spin){
		if(spin) spinTimeline.play();
		else spinTimeline.stop();
	}

	private static PhongMaterial createWoodMaterial(Color color) {
		return new PhongMaterial(color, Material.load("Wood4.jpg"), null, null, null);
	}
}
