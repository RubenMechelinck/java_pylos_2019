package be.kuleuven.pylos.player;

import java.util.ArrayList;

/**
 * Created by Jan on 17/02/2015.
 */
public abstract class PylosPlayerFactory {

	private final String name;
	private final ArrayList<PylosPlayerType> types = new ArrayList<>();

	public PylosPlayerFactory(String name){
		this.name = name;
		createTypes();
	}

	protected abstract void createTypes();

	protected final void add(PylosPlayerType type) {
		types.add(type);
	}

	public String getName(){
		return name;
	}

	public ArrayList<PylosPlayerType> getTypes() {
		return types;
	}

	public PylosPlayerType getType(String name){
		for(PylosPlayerType type : types){
			if(type.toString().equals(name)){
				return type;
			}
		}
		return null;
	}

}
