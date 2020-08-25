package cz.metacentrum.perun.core.api;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Vojtech Sassmann <vojtech.sassmann@gmail.com>
 */
public class GenDataNode {
	private final List<String> h;
	private final List<GenDataNode> c;

	public GenDataNode(List<String> hashes, List<GenDataNode> childNodes) {
		this.h = hashes;
		this.c = childNodes;
	}

	public void addHashes(Collection<String> hashes) {
		this.h.addAll(hashes);
	}

	public void addChildNode(GenDataNode child) {
		this.c.add(child);
	}

	public List<String> getH() {
		return Collections.unmodifiableList(h);
	}

	public List<GenDataNode> getC() {
		return Collections.unmodifiableList(c);
	}

	@Override
	public String toString() {
		return "GenDataNode{" +
				"h=" + h +
				", c=" + c +
				'}';
	}
}
