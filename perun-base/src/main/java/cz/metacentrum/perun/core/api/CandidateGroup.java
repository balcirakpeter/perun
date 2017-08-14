package cz.metacentrum.perun.core.api;

import java.util.*;

/**
 * @date 8/30/17.
 * @author Peter Balcirak peter.balcirak@gmail.com
 */
public class CandidateGroup extends Group {

	private ExtSource extSource;
	private String parentGroupName;
	private List<String> subGroupsNames;

	public CandidateGroup() {
	}

	public CandidateGroup(ExtSource extSource, String parentGroupName, List<String> subGroupsNames) {
		this();
		this.extSource = extSource;
		this.parentGroupName = parentGroupName;
		this.subGroupsNames = subGroupsNames;
	}

	public ExtSource getExtSource() {
		return extSource;
	}

	public void setExtSource(ExtSource extSource) {
		this.extSource = extSource;
	}

	public String getParentGroupName() {
		return parentGroupName;
	}

	public void setSubGroupsNames(List<String> subGroupsNames) {
		this.subGroupsNames = subGroupsNames;
	}

	public List<String> getSubGroupsNames() {
		return  Collections.unmodifiableList(subGroupsNames);
	}

	public void setParentGroupName(String parentGroupName) {
		this.parentGroupName = parentGroupName;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((parentGroupName == null) ? 0 : parentGroupName.hashCode());
		result = prime * result
				+ ((extSource == null) ? 0 : extSource.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		CandidateGroup that = (CandidateGroup) o;

		if (getExtSource() != null ? !getExtSource().equals(that.getExtSource()) : that.getExtSource() != null)
			return false;
		if (getParentGroupName() != null ? !getParentGroupName().equals(that.getParentGroupName()) : that.getParentGroupName() != null)
			return false;
		if (getSubGroupsNames() != null ? getSubGroupsNames().equals(that.getSubGroupsNames()) : that.getSubGroupsNames() != null)
			return false;
		return getName() != null ? getName().equals(that.getName()) : that.getName() == null;
	}

	@Override
	public String serializeToString() {
		StringBuilder str = new StringBuilder();

		return str.append(this.getClass().getSimpleName()).append(":[" +
				"extSource=<").append(getExtSource() == null ? "\\0" : getExtSource().serializeToString()).append(">" +
				", parentGroupName=<").append(getParentGroupName()).append(">" +
				']').toString();
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();

		return str.append(getClass().getSimpleName()+":[userExtSource='").append(getExtSource()).append("', parentGroupName='"
				+ getParentGroupName()).append("']").toString();
	}
}
