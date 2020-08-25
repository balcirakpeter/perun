package cz.metacentrum.perun.core.impl;


import cz.metacentrum.perun.core.api.Attribute;
import cz.metacentrum.perun.core.api.Facility;
import cz.metacentrum.perun.core.api.HashedGenData;
import cz.metacentrum.perun.core.api.GenDataNode;
import cz.metacentrum.perun.core.api.Member;
import cz.metacentrum.perun.core.api.Resource;
import cz.metacentrum.perun.core.api.Service;
import cz.metacentrum.perun.core.implApi.HashedDataGenerator;
import cz.metacentrum.perun.core.implApi.GenDataProvider;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Vojtech Sassmann <vojtech.sassmann@gmail.com>
 */
public class HierarchicalHashedDataGenerator implements HashedDataGenerator {

	private final PerunSessionImpl sess;
	private final Service service;
	private final Facility facility;
	private final GenDataProvider dataProvider;
	private final boolean filterExpiredMembers;

	public HierarchicalHashedDataGenerator(PerunSessionImpl sess, Service service, Facility facility, boolean filterExpiredMembers) {
		this.sess = sess;
		this.service = service;
		this.facility = facility;
		this.filterExpiredMembers = filterExpiredMembers;
		dataProvider = new CachingGenDataProvider(sess, service, facility);
	}

	@Override
	public HashedGenData generateData() {
		dataProvider.loadFacilitySpecificAttributes();

		List<Resource> resources = sess.getPerunBl().getFacilitiesManagerBl().getAssignedResources(sess, facility);
		resources.retainAll(sess.getPerunBl().getServicesManagerBl().getAssignedResources(sess, service));

		List<GenDataNode> childNodes = resources.stream()
				.map(this::getDataForResource)
				.collect(Collectors.toList());

		List<String> facilityAttrHashes = dataProvider.getFacilityAttributesHashes();
		Map<String, List<Attribute>> attributes = dataProvider.getAllFetchedAttributes();

		GenDataNode root = new GenDataNode(facilityAttrHashes, childNodes);

		return new HashedGenData(attributes, root);
	}

	private GenDataNode getDataForResource(Resource resource) {
		List<Member> members;
		if (filterExpiredMembers) {
			members = sess.getPerunBl().getResourcesManagerBl().getAllowedMembersNotExpiredInGroups(sess, resource);
		} else {
			members = sess.getPerunBl().getResourcesManagerBl().getAllowedMembers(sess, resource);
		}

		dataProvider.loadResourceSpecificAttributes(resource, members);

		List<String> resourceAttrHashes = dataProvider.getResourceAttributesHashes(resource);

		List<GenDataNode> childNodes = members.stream()
				.map(member -> getDataForMember(resource, member))
				.collect(Collectors.toList());

		return new GenDataNode(resourceAttrHashes, childNodes);
	}

	private GenDataNode getDataForMember(Resource resource, Member member) {
		List<String> memberAttrHashes = dataProvider.getAllMemberSpecificAttributesHashes(resource, member);

		return new GenDataNode(memberAttrHashes, Collections.emptyList());
	}
}
