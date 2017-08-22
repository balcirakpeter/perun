package cz.metacentrum.perun.core.entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyInt;

import cz.metacentrum.perun.core.api.*;
import cz.metacentrum.perun.core.api.exceptions.*;
import cz.metacentrum.perun.core.impl.ExtSourceSql;
import cz.metacentrum.perun.core.impl.ExtSourceInternal;
import cz.metacentrum.perun.core.impl.ExtSourceSqlComplex;
import cz.metacentrum.perun.core.impl.PerunSessionImpl;
import cz.metacentrum.perun.core.implApi.ExtSourceSimpleApi;
import org.junit.Before;
import org.junit.Test;

import cz.metacentrum.perun.core.AbstractPerunIntegrationTest;
import cz.metacentrum.perun.core.bl.GroupsManagerBl;

/**
 * Created by peter on 3/2/17.
 */
public class moveGroupIntegrationTest extends AbstractPerunIntegrationTest {

	final ExtSource nonExistingExtSource = new ExtSource(0, "NonExtistingextSource", "cz.metacentrum.perun.core.impl.ExtSourceInternal");
	final Vo nonExistingVo = new Vo(0, "GroupSyncNonExistingTestVo", "GSNETestVo");

	private ExtSource extSourceWithoutSupportedOperation = new ExtSource(0, "ExtSourceWithoutSupportedOperation", "cz.metacentrum.perun.core.impl.ExtSourceInternal");
	private Vo vo;
	private Vo vo2;
	private ExtSource extSource = new ExtSource(0, "testExtSource", "cz.metacentrum.perun.core.impl.ExtSourceSql");
	private String name;
	private String type;
	private int id;
	private Group group1 = new Group("ClassicGroupInVo1", "Classsic group in perun");
	private Group group2 = new Group("ClassicGroupInVo2", "Classsic group in perun");
	private Group group3 = new Group("ClassicGroupInVo3", "Classsic group in perun");
	private Group group4 = new Group("ClassicGroupInVo4", "Classsic group in perun");
	private Group group5 = new Group("ClassicGroupInVo5", "Classsic group in perun");
	private Group group6 = new Group("ClassicGroupInVo6", "Classsic group in perun");
	private Group group7 = new Group("ClassicGroupInVo7", "Classsic group in perun");
	private Group group8 = new Group("ClassicGroupInVo8", "Classsic group in perun");
	private Group group9 = new Group("ClassicGroupInVo9", "Classsic group in perun");
	private Member member1;
	private Member member2;
	private Member member3;
	private Member member4;
	private Member member5;

	// exists before every method
	private GroupsManager groupsManager;
	private GroupsManagerBl groupsManagerBl;
	private AttributesManager attributesManager;

	private static Map<String, String> subjectMap1 = new HashMap<>();
	private static Map<String, String> subjectMap2 = new HashMap<>();
	private static Map<String, String> subjectMap3 = new HashMap<>();
	private static List<String> subGroups1 = new ArrayList<>();
	private static List<String> subGroups2 = new ArrayList<>();
	private static List<String> subGroups3 = new ArrayList<>();

	int subMapId1;
	int subMapId2;
	int subMapId3;
	int subGroupId1;
	int subGroupId2;
	int subGroupId3;

	@Before
	public void setUp() throws Exception {
		//session = mock(PerunSessionImpl.class, RETURNS_DEEP_STUBS);
		vo = setUpVo();
		vo2 = setUpSecondVo();
		setUpExtSource(extSourceWithoutSupportedOperation);
		setUpExtSource(extSource);
		setUpGroup(group1);
		setUpGroup(group2);
		setUpGroupInSecondVo(group9);
		setUpSubGroup(group3, group1);
		setUpSubGroup(group4, group3);
		setUpSubGroup(group5, group2);
		setUpSubGroup(group6, group5);
		setUpSubGroup(group7, group1);
		setUpSubGroup(group8, group2);

		member1 = setUpMember(vo);
		member2 = setUpMember(vo);
		member3 = setUpMember(vo);
		member4 = setUpMember(vo);
		member5 = setUpMember(vo);

		groupsManager = perun.getGroupsManager();
		groupsManagerBl = perun.getGroupsManagerBl();
		attributesManager = perun.getAttributesManager();

		groupsManagerBl.addMember(sess, group4, member1);
		groupsManagerBl.addMember(sess, group4, member2);
		groupsManagerBl.addMember(sess, group3, member3);
		groupsManagerBl.addMember(sess, group6, member4);
		groupsManagerBl.addMember(sess, group5, member5);

		name = extSource.getName();
		type = extSource.getType();
		id = extSource.getId();
		extSource = mock(ExtSourceSql.class, RETURNS_DEEP_STUBS);
		setUpSubjects();
		subGroups1.add("testGroup2");
		subGroups2.add("testGroup3");
	}

	@Test (expected = InternalErrorException.class)
	public void testMoveGroupTodiffVo() throws Exception {
		System.out.println("\ntest moveGroup() to different vo");

		groupsManagerBl.moveGroup(sess , group1, group9);

	}

	@Test (expected = InternalErrorException.class)
	public void testMoveGroupAlreadyInDestination() throws Exception {
		System.out.println("\ntest moveGroup() which is already in destination destination");

		groupsManagerBl.moveGroup(sess , group1, group3);

	}

	@Test (expected = InternalErrorException.class)
	public void testMoveGroupSameGroups() throws Exception {
		System.out.println("\ntest moveGroup() same groups");

		groupsManagerBl.moveGroup(sess , group2, group2);

	}

	@Test (expected = InternalErrorException.class)
	public void testMoveGroupNullMoving() throws Exception {
		System.out.println("\ntest moveGroup() null moving");

		groupsManagerBl.moveGroup(sess , group5, null);

	}

	@Test (expected = InternalErrorException.class)
	public void testMoveGroupNullDestination() throws Exception {
		System.out.println("\ntest moveGroup() null destination");

		groupsManagerBl.moveGroup(sess , null, group2);

	}

	@Test (expected = InternalErrorException.class)
	public void testMoveGroupToSubGroup() throws Exception {
		System.out.println("\ntest moveGroup() to sub group");

		groupsManagerBl.moveGroup(sess , group5, group2);

	}

	@Test
	public void testMoveGroup() throws Exception {
		System.out.println("\ntest moveGroup()");

		groupsManagerBl.moveGroup(sess , group6, group4);

		assertTrue("group4 has different parent id than group6 id", group6.getId() == group4.getParentGroupId());

		assertEquals("Group4 has wrong name after moving", "ClassicGroupInVo2:ClassicGroupInVo5:ClassicGroupInVo6:ClassicGroupInVo4", group4.getName());

	}

	@Test
	public void testMoveGroupIndirectMembers() throws Exception {
		System.out.println("\ntest moveGroup() with indirect members");

		groupsManagerBl.moveGroup(sess , group6, group4);

		List<Member> members = groupsManagerBl.getGroupMembers(sess, group1);
		assertEquals("Indirect members have to be realocated from group1", 1, members.size());

		members = groupsManagerBl.getGroupMembers(sess, group3);
		assertEquals("Indirect members have to be realocated from group3", 1, members.size());

		members = groupsManagerBl.getGroupMembers(sess, group2);
		assertEquals("Indirect members have to be realocated to group2", 4, members.size());

		members = groupsManagerBl.getGroupMembers(sess, group6);
		assertEquals("Indirect members have to be realocated to group6", 3, members.size());

	}

	// PRIVATE METHODS -------------------------------------------------------------
	private Vo setUpVo() throws Exception {

		Vo newVo = new Vo(0, "GroupSyncTestVo", "GSTestVo");
		Vo returnedVo = perun.getVosManager().createVo(sess, newVo);
		// create test VO in database
		assertNotNull("unable to create testing Vo", returnedVo);

		return returnedVo;

	}

	private Vo setUpSecondVo() throws Exception {

		Vo newVo = new Vo(0, "GroupSyncTestVo2", "GSTestVo2");
		Vo returnedVo = perun.getVosManager().createVo(sess, newVo);
		// create test VO in database
		assertNotNull("unable to create testing Vo", returnedVo);

		return returnedVo;

	}

	private void setUpExtSource(ExtSource es) throws Exception {

		ExtSource returnedExtSource = perun.getExtSourcesManager().createExtSource(sess, es, null);
		// create test ES in database
		assertNotNull("unable to create testing ExtSource", returnedExtSource);


		perun.getExtSourcesManager().addExtSource(sess, vo, returnedExtSource);
		// add real ext source to our VO


	}

	private void setUpGroup(Group gr)  throws Exception {
		Group returnedGroup = perun.getGroupsManager().createGroup(sess, vo, gr);

		assertNotNull("unable to create testing ExtSource", returnedGroup);
	}

	private void setUpGroupInSecondVo(Group gr)  throws Exception {
		Group returnedGroup = perun.getGroupsManager().createGroup(sess, vo2, gr);

		assertNotNull("unable to create testing ExtSource", returnedGroup);
	}

	private void setUpSubGroup(Group gr, Group pgr) throws Exception {
		Group returnedGroup = perun.getGroupsManager().createGroup(sess, pgr, gr);

		assertNotNull("unable to create testing ExtSource", returnedGroup);
	}

	private void setUpSubjects() throws Exception {
		subjectMap1.put("groupName", "testGroup1");
		subjectMap1.put("groupDescription", "testGroup1 for groupSync");
		subjectMap1.put("parentGroup", null);

		subjectMap2.put("groupName", "testGroup2");
		subjectMap2.put("groupDescription", "testGroup2 for groupSync");
		subjectMap2.put("parentGroup", "testGroup1");

		subjectMap3.put("groupName", "testGroup3");
		subjectMap3.put("groupDescription", "testGroup3 for groupSync");
		subjectMap3.put("parentGroup", "testGroup2");
	}

	private void cleanUp() throws Exception{
		groupsManagerBl.deleteAllGroups(sess, vo);
		setUpGroup(group1);
		setUpGroup(group2);
		setUpSubGroup(group3, group1);
	}

	private Member setUpMember(Vo vo) throws Exception {

		Candidate candidate = setUpCandidate();
		Member member = perun.getMembersManagerBl().createMemberSync(sess, vo, candidate);
		// set first candidate as member of test VO
		assertNotNull("No member created", member);
		usersForDeletion.add(perun.getUsersManager().getUserByMember(sess, member));
		// save user for deletion after test
		return member;

	}

	private Candidate setUpCandidate(){

		String userFirstName = Long.toHexString(Double.doubleToLongBits(Math.random()));
		String userLastName = Long.toHexString(Double.doubleToLongBits(Math.random()));
		String extLogin = Long.toHexString(Double.doubleToLongBits(Math.random()));              // his login in external source

		Candidate candidate = new Candidate();  //Mockito.mock(Candidate.class);
		candidate.setFirstName(userFirstName);
		candidate.setId(0);
		candidate.setMiddleName("");
		candidate.setLastName(userLastName);
		candidate.setTitleBefore("");
		candidate.setTitleAfter("");
		ExtSource extSource = new ExtSource(0, "testExtSource", "cz.metacentrum.perun.core.impl.ExtSourceInternal");
		UserExtSource userExtSource = new UserExtSource(extSource, extLogin);
		candidate.setUserExtSource(userExtSource);
		candidate.setAttributes(new HashMap<String,String>());
		return candidate;

	}
}