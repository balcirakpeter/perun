package cz.metacentrum.perun.core.api;

import java.util.List;
import java.util.Map;

/**
 * @author Vojtech Sassmann <vojtech.sassmann@gmail.com>
 */
public class HashedGenData {
	private final Map<String, List<Attribute>> attributes;
	private final GenDataNode hierarchy;

	public HashedGenData(Map<String, List<Attribute>> attributes, GenDataNode hierarchy) {
		this.attributes = attributes;
		this.hierarchy = hierarchy;
	}

	public Map<String, List<Attribute>> getAttributes() {
		return attributes;
	}

	public GenDataNode getHierarchy() {
		return hierarchy;
	}

	@Override
	public String toString() {
		return "HashedGenData{" +
				"attributes=" + attributes +
				", hierarchy=" + hierarchy +
				'}';
	}
}
