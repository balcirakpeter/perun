package cz.metacentrum.perun.core.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RoleManagementRules {

	private String roleName;
	private List<Map<String, String>> privilegedRoles;
	private Map<String, String> entitiesToManage;
	private Map<String, String> assignedObjects;

	public RoleManagementRules(String roleName, List<Map<String, String>> privilegedRoles, Map<String, String> entitiesToManage, Map<String, String> assignedObjects) {
		this.roleName = roleName;
		this.privilegedRoles = privilegedRoles;
		this.entitiesToManage = entitiesToManage;
		this.assignedObjects = assignedObjects;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public List<Map<String, String>> getPrivilegedRoles() {
		return privilegedRoles;
	}

	public void setPrivilegedRoles( List<Map<String, String>> privilegedRoles) {
		this.privilegedRoles = privilegedRoles;
	}

	public Map<String, String> getEntitiesToManage() {
		return entitiesToManage;
	}

	public void setEntitiesToManage(Map<String, String> entitiesToManage) {
		this.entitiesToManage = entitiesToManage;
	}

	public Map<String, String> getAssignedObjects() {
		return assignedObjects;
	}

	public void setAssignedObjects(Map<String, String> assignedObjects) {
		this.assignedObjects = assignedObjects;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RoleManagementRules that = (RoleManagementRules) o;
		return Objects.equals(roleName, that.roleName) &&
			Objects.equals(privilegedRoles, that.privilegedRoles) &&
			Objects.equals(entitiesToManage, that.entitiesToManage) &&
			Objects.equals(assignedObjects, that.assignedObjects);
	}

	@Override
	public int hashCode() {
		return Objects.hash(roleName, privilegedRoles, entitiesToManage, assignedObjects);
	}

	@Override
	public String toString() {
		return "RoleManagementRules{" +
			"roleName='" + roleName + '\'' +
			", privilegedRoles=" + privilegedRoles +
			", entitiesToManage=" + entitiesToManage +
			", assignedObjects=" + assignedObjects +
			'}';
	}
}