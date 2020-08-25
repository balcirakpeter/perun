package cz.metacentrum.perun.core.impl;

import cz.metacentrum.perun.core.api.Attribute;
import cz.metacentrum.perun.core.api.Facility;
import cz.metacentrum.perun.core.api.Member;
import cz.metacentrum.perun.core.api.Resource;
import cz.metacentrum.perun.core.api.Service;
import cz.metacentrum.perun.core.api.User;
import cz.metacentrum.perun.core.implApi.GenDataProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author Vojtech Sassmann <vojtech.sassmann@gmail.com>
 */
public class CachingGenDataProvider implements GenDataProvider {

	private final PerunSessionImpl sess;
	private final Service service;
	private final Facility facility;

	private final Map<String, List<Attribute>> attributesByHash = new HashMap<>();

	private Map<Member, List<Attribute>> loadedMemberResourceAttributes = new HashMap<>();
	private final Map<Member, List<Attribute>> loadedMemberAttributes = new HashMap<>();
	private final Map<User, List<Attribute>> loadedUserAttributes = new HashMap<>();
	private final Map<User, List<Attribute>> loadedUserFacilityAttributes = new HashMap<>();
	private final Map<Resource, List<Attribute>> loadedResourceAttributes = new HashMap<>();

	private List<Attribute> loadedFacilityAttributes;

	private final Map<Integer, User> loadedUsersById = new HashMap<>();

	private final Set<Member> processedMembers = new HashSet<>();

	public CachingGenDataProvider(PerunSessionImpl sess, Service service, Facility facility) {
		this.sess = sess;
		this.service = service;
		this.facility = facility;
	}

	@Override
	public void loadFacilitySpecificAttributes() {
		loadedFacilityAttributes =
				sess.getPerunBl().getAttributesManagerBl().getRequiredAttributes(sess, service, facility);
	}

	@Override
	public void loadResourceSpecificAttributes(Resource resource, List<Member> members) {
		if (!loadedResourceAttributes.containsKey(resource)) {
			loadedResourceAttributes.put(resource,
					sess.getPerunBl().getAttributesManagerBl().getRequiredAttributes(sess, service, resource));
		}

		loadedMemberResourceAttributes =
				sess.getPerunBl().getAttributesManagerBl().getRequiredAttributes(sess, service, resource, members);

		// we don't need to load again attributes for the already processed members
		List<Member> notYetProcessedMembers = new ArrayList<>(members);
		notYetProcessedMembers.removeAll(processedMembers);
		processedMembers.addAll(notYetProcessedMembers);

		loadMemberSpecificAttributes(notYetProcessedMembers);
	}

	@Override
	public List<String> getFacilityAttributesHashes() {
		String hash = hashFacility(facility);

		if (!attributesByHash.containsKey(hash)) {
			if (loadedFacilityAttributes == null) {
				throw new IllegalStateException("Facility attributes need to be loaded first.");
			}
			attributesByHash.put(hash, loadedFacilityAttributes);
		}

		return attributesByHash.get(hash).isEmpty() ? emptyList() : singletonList(hash);
	}

	@Override
	public List<String> getResourceAttributesHashes(Resource resource) {
		String hash = hashResource(resource);

		if (!attributesByHash.containsKey(hash)) {
			if (!loadedResourceAttributes.containsKey(resource)) {
				throw new IllegalStateException("Resource attributes for given resource has to be loaded first.");
			}
			attributesByHash.put(hash, loadedResourceAttributes.get(resource));
		}

		return attributesByHash.get(hash).isEmpty() ? emptyList() : singletonList(hash);
	}

	@Override
	public List<String> getAllMemberSpecificAttributesHashes(Resource resource, Member member) {
		List<String> hashes = new ArrayList<>();

		User user = loadedUsersById.get(member.getUserId());

		hashes.addAll(getMemberAttributesHashes(member));
		hashes.addAll(getUserAttributesHashes(user));
		hashes.addAll(getUserFacilityAttributesHashes(user, facility));
		hashes.addAll(getMemberResourceAttributesHashes(member, resource));

		return hashes;
	}

	@Override
	public Map<String, List<Attribute>> getAllFetchedAttributes() {
		return attributesByHash.entrySet().stream()
				.filter(entry -> !entry.getValue().isEmpty())
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private List<String> getMemberResourceAttributesHashes(Member member, Resource resource) {
		String hash = hashMemberResource(member, resource);

		if (!attributesByHash.containsKey(hash)) {
			if (!loadedMemberResourceAttributes.containsKey(member)) {
				return emptyList();
			}
			attributesByHash.put(hash, loadedMemberResourceAttributes.get(member));
		}

		return attributesByHash.get(hash).isEmpty() ? emptyList() : singletonList(hash);
	}

	private List<String> getMemberAttributesHashes(Member member) {
		String hash = hashMember(member);
		if (!attributesByHash.containsKey(hash)) {
			if (!loadedMemberAttributes.containsKey(member)) {
				return emptyList();
			}
			attributesByHash.put(hash, loadedMemberAttributes.get(member));
		}
		return attributesByHash.get(hash).isEmpty() ? emptyList() : singletonList(hash);
	}

	private List<String> getUserAttributesHashes(User user) {
		String hash = hashUser(user);

		if (!attributesByHash.containsKey(hash)) {
			if (!loadedUserAttributes.containsKey(user)) {
				return emptyList();
			}
			attributesByHash.put(hash, loadedUserAttributes.get(user));
		}

		return attributesByHash.get(hash).isEmpty() ? emptyList() : singletonList(hash);
	}

	private List<String> getUserFacilityAttributesHashes(User user, Facility facility) {
		String hash = hashUserFacility(user, facility);

		if (!attributesByHash.containsKey(hash)) {
			if (!loadedUserFacilityAttributes.containsKey(user)) {
				return emptyList();
			}
			attributesByHash.put(hash, loadedUserFacilityAttributes.get(user));
		}

		return attributesByHash.get(hash).isEmpty() ? emptyList() : singletonList(hash);
	}

	private void loadMemberSpecificAttributes(List<Member> members) {
		loadedMemberAttributes.putAll(
				sess.getPerunBl().getAttributesManagerBl().getRequiredAttributes(sess, null, service, members)
		);

		List<Integer> userIds = members.stream()
				.map(Member::getUserId)
				.collect(toList());

		List<User> users = sess.getPerunBl().getUsersManagerBl().getUsersByIds(sess, userIds);

		Map<Integer, User> usersById = users.stream()
				.collect(toMap(User::getId, Function.identity()));
		loadedUsersById.putAll(usersById);

		loadUserSpecificAttributes(users);
	}

	private void loadUserSpecificAttributes(List<User> users) {
		loadedUserAttributes.putAll(
				sess.getPerunBl().getAttributesManagerBl().getRequiredAttributes(sess, service, users)
		);

		loadedUserFacilityAttributes.putAll(
				sess.getPerunBl().getAttributesManagerBl().getRequiredAttributes(sess, service, facility, users)
		);
	}

	private String hashFacility(Facility facility) {
		return "f-" + facility.getId();
	}

	private String hashResource(Resource resource) {
		return "r-" + resource.getId();
	}

	private String hashMember(Member member) {
		return "m-" + member.getId();
	}

	private String hashMemberResource(Member member, Resource resource) {
		return "m-r-" + member.getId() + "-" + resource.getId();
	}

	private String hashUser(User user) {
		return "u-" + user.getId();
	}

	private String hashUserFacility(User user, Facility facility) {
		return "u-f-" + user.getId() + "-" + facility.getId();
	}
}
