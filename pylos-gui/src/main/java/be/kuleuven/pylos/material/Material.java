package be.kuleuven.pylos.material;

import javafx.scene.image.Image;

/**
 * Created by Jan on 13/02/2015.
 */
public class Material {

	public static Image load(String name) {
		return new Image(Material.class.getResourceAsStream(name));
	}

}
