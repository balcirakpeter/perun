package cz.metacentrum.perun.core.api;

import java.util.Objects;

public class ActionType {

	public static final String WRITE = "WRITE";
	public static final String WRITE_VO = "WRITE_VO";
	public static final String WRITE_PUBLIC = "WRITE_PUBLIC";
	public static final String READ = "READ";
	public static final String READ_VO = "READ_VO";
	public static final String READ_PUBLIC = "READ_PUBLIC";

	private String actionTypeName;
	private String actionTypeObject;
	private String actionTypeDescription;

	public ActionType() {}

	public ActionType(String actionTypeName, String actionTypeObject, String actionTypeDescription) {
		this.actionTypeName = actionTypeName;
		this.actionTypeObject = actionTypeObject;
		this.actionTypeDescription = actionTypeDescription;
	}

	public ActionType(String actionTypeName, String actionTypeObject) {
		this.actionTypeName = actionTypeName;
		this.actionTypeObject = actionTypeObject;
	}

	public String getActionTypeName() {
		return actionTypeName;
	}

	public void setActionTypeName(String actionTypeName) {
		this.actionTypeName = actionTypeName;
	}

	public String getActionTypeObject() {
		return actionTypeObject;
	}

	public void setActionTypeObject(String actionTypeObject) {
		this.actionTypeObject = actionTypeObject;
	}

	public String getActionTypeDescription() {
		return actionTypeDescription;
	}

	public void setActionTypeDescription(String actionTypeDescription) {
		this.actionTypeDescription = actionTypeDescription;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ActionType)) return false;
		ActionType that = (ActionType) o;
		return Objects.equals(actionTypeName, that.actionTypeName) &&
			Objects.equals(actionTypeObject, that.actionTypeObject);
	}

	@Override
	public int hashCode() {
		return Objects.hash(actionTypeName, actionTypeObject);
	}

	@Override
	public String toString() {
		return "ActionType{" +
			"actionTypeName='" + actionTypeName + '\'' +
			", actionTypeObject='" + actionTypeObject + '\'' +
			", actionTypeDescription='" + actionTypeDescription + '\'' +
			'}';
	}

}
