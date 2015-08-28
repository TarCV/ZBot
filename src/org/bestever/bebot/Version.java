package org.bestever.bebot;

/**
 * I could have done this better.
 * @author Sean Baggaley
 *
 */
public class Version {
	protected String name;
	protected String path;
	protected boolean isDefault;
	protected String description;
	
	public Version(String name, String path, boolean isDefault, String description) {
		setName(name);
		setPath(path);
		setDefault(isDefault);
		setDescription(description);
	}
	
	protected void setName(String value) {
		name = value;
	}
	
	protected void setPath(String value) {
		path = value;
	}
	
	protected void setDefault(boolean value) {
		isDefault = value;
	}
	
	protected void setDescription(String value) {
		description = value;
	}
}