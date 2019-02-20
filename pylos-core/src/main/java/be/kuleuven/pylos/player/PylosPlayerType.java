package be.kuleuven.pylos.player;

/**
 * Created by Jan on 20/02/2015.
 */
public abstract class PylosPlayerType {

	private final String typeName;

	public PylosPlayerType(String typeName){
		this.typeName = typeName;
	}

	public abstract PylosPlayer create();

	public String toString(){
		return typeName;
	}

}
