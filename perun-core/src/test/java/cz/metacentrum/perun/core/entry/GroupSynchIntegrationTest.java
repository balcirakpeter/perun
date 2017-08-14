package cz.metacentrum.perun.core.entry;

import cz.metacentrum.perun.core.AbstractPerunIntegrationTest;
import cz.metacentrum.perun.core.api.*;
import cz.metacentrum.perun.core.api.exceptions.GroupNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.core.bl.GroupsManagerBl;
import cz.metacentrum.perun.core.bl.ExtSourcesManagerBl;
import cz.metacentrum.perun.core.bl.PerunBl;
import cz.metacentrum.perun.core.blImpl.ExtSourcesManagerBlImpl;
import cz.metacentrum.perun.core.blImpl.GroupsManagerBlImpl;
import cz.metacentrum.perun.core.blImpl.PerunBlImpl;
import cz.metacentrum.perun.core.impl.ExtSourceLdap;
import cz.metacentrum.perun.core.impl.GroupsManagerImpl;
import cz.metacentrum.perun.core.impl.PerunSessionImpl;
import cz.metacentrum.perun.core.implApi.ExtSourceSimpleApi;
import cz.metacentrum.perun.core.implApi.GroupsManagerImplApi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Attr;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by peter on 10/11/17.
 */
public class GroupSynchIntegrationTest extends AbstractPerunIntegrationTest{
	private final static String CLASS_NAME = "GroupsManager.";
	private static final String EXT_SOURCE_NAME = "GroupSyncExtSource";

	// these must be setUp"type" before every method to be in DB
	private Group group1 = new Group("TestGroup1","testovaci1");
	private Group group2 = new Group("TestGroup2","testovaci2");
	private Group group8 = new Group("TestGroup8","testovaci8");
	private Group group3 = new Group("TestGroup3","testovaci3");
	private Group group4 = new Group("TestGroup4","testovaci4");
	private Group group5 = new Group("TestGroup5","testovaci5");
	private Group group6 = new Group("TestGroup6","testovaci6");
	private Group group7 = new Group("TestGroup7","testovaci7");
	private Group group9 = new Group("TestGroup9","testovaci9");
	private Group group10 = new Group("TestGroup10","testovaci10");

	private Map<String, String> subject8 = new HashMap<>();
	private Map<String, String> subject2 = new HashMap<>();
	private Map<String, String> subject3 = new HashMap<>();
	private Map<String, String> subject4 = new HashMap<>();
	private Map<String, String> subject5 = new HashMap<>();
	private Map<String, String> subject6 = new HashMap<>();
	private Map<String, String> subject7 = new HashMap<>();

	private ExtSource extSource = new ExtSource(0, EXT_SOURCE_NAME, ExtSourcesManager.EXTSOURCE_LDAP);

	private Vo vo;

	// exists before every method
	@InjectMocks
	private PerunBl perun = new PerunBlImpl();
	private GroupsManagerBl groupsManagerBl;
	@Spy
	private ExtSourcesManagerBl extSourceManagerBl;
	private AttributesManager attributesManager;
	private GroupsManager groupsManager;
	private ExtSourceSimpleApi essa = mock(ExtSourceLdap.class);
	List<Map<String, String>> subjects = new ArrayList<>();

	@Before
	public void setUpBeforeEveryMethod() throws Exception {
		perun = super.perun;
		groupsManagerBl = perun.getGroupsManagerBl();
		attributesManager = perun.getAttributesManager();
		extSourceManagerBl = perun.getExtSourcesManagerBl();
		groupsManager = perun.getGroupsManager();
		vo = setUpVo();
		setUpGroupStructure(vo);
		MockitoAnnotations.initMocks(this);

	}

	@Test
	public void removeAllGroupsTest() throws Exception {
		System.out.println(CLASS_NAME + "removeAllGroupsTest");

		doReturn(essa).when(extSourceManagerBl).getExtSourceByName(any(PerunSession.class), any(String.class));
		when(essa.getSubjectGroups(any(Map.class))).thenReturn(subjects);
		groupsManagerBl.synchronizeGroupStructure(sess, group1);

		updateGroups();

		int numGr = groupsManagerBl.getAllSubGroups(sess, group1).size();

		assertTrue("All groups should be removed from base group after synchronization!", 0 == numGr);
	}

	@Test
	public void removeGroupTest() throws Exception {
		System.out.println(CLASS_NAME + "removeGroupTest");

		prepareSubjects();
		subjects.remove(subject4);

		doReturn(essa).when(extSourceManagerBl).getExtSourceByName(any(PerunSession.class), any(String.class));
		when(essa.getSubjectGroups(any(Map.class))).thenReturn(subjects);
		groupsManagerBl.synchronizeGroupStructure(sess, group1);

		updateGroups();

		int numGr = groupsManagerBl.getAllSubGroups(sess, group1).size();

		List <Group> allSubGroups = groupsManagerBl.getAllSubGroups(sess, group1);
		List <Group> subGroups = groupsManagerBl.getSubGroups(sess, group1);
		assertTrue("Group 4 should be removed from group structure after synchronization!", !allSubGroups.contains(group4));
		assertTrue("Group 5 should be moved under Group 1!", subGroups.contains(group5));
	}

	@Test
	public void addGroupTest() throws Exception {
		System.out.println(CLASS_NAME + "addGroupTest");

		prepareSubjects();
		Map<String, String> subject9 = new HashMap<>();
		subject9.put("groupName","TestGroup9");
		subject9.put("parentGroupName","TestGroup4");
		subject9.put("groupDescription","testovaci9");
		subjects.add(subject9);

		doReturn(essa).when(extSourceManagerBl).getExtSourceByName(any(PerunSession.class), any(String.class));
		when(essa.getSubjectGroups(any(Map.class))).thenReturn(subjects);
		groupsManagerBl.synchronizeGroupStructure(sess, group1);

		updateGroups();

		group9 = groupsManagerBl.getGroupByName(sess, vo, group4.getName() +  ":" +group9.getShortName());

		List <Group> subGroups = groupsManagerBl.getSubGroups(sess, group4);

		assertTrue("Group 9 should be created under Group 4!", subGroups.contains(group9));
	}

	@Test
	public void updateGroupTest() throws Exception {
		System.out.println(CLASS_NAME + "updateGroupTest");

		prepareSubjects();
		subjects.remove(subject4);
		subject4.put("parentGroupName",null);
		subject4.put("groupDescription","testGroup4");
		subjects.add(subject4);

		doReturn(essa).when(extSourceManagerBl).getExtSourceByName(any(PerunSession.class), any(String.class));
		when(essa.getSubjectGroups(any(Map.class))).thenReturn(subjects);
		groupsManagerBl.synchronizeGroupStructure(sess, group1);

		updateGroups();

		List <Group> subGroups = groupsManagerBl.getSubGroups(sess, group1);

		assertTrue("Group 4 should have been moved under Group 1!", subGroups.contains(group4));
		assertTrue("Description of Group 4 should have been changed to testGroup4!", group4.getDescription().equals("testGroup4"));
	}

	@Test
	public void addGroupWithLostConnectionTest() throws Exception {
		System.out.println(CLASS_NAME + "addGroupWithLostConnectionTest");

		prepareSubjects();
		Map<String, String> subject9 = new HashMap<>();
		subject9.put("groupName","TestGroup9");
		subject9.put("parentGroupName","TestGroup4");
		subject9.put("groupDescription","testovaci9");
		subjects.add(subject9);

		subjects.remove(subject3);
		subject3.put("parentGroupName","TestGroup9");
		subjects.add(subject3);

		subjects.remove(subject8);
		subject8.put("parentGroupName","TestGroup9");
		subjects.add(subject8);

		/*List<String> subGroupsNames = new ArrayList<>();
		subGroupsNames.add("TestGroup8");
		subGroupsNames.add("TestGroup3");*/

		doReturn(essa).when(extSourceManagerBl).getExtSourceByName(any(PerunSession.class), any(String.class));
		when(essa.getSubjectGroups(any(Map.class))).thenReturn(subjects);
		//when(essa.getSubGroupsNames("TestGroup9")).thenReturn(subGroupsNames);
		groupsManagerBl.synchronizeGroupStructure(sess, group1);

		updateGroups();

		group9 = groupsManagerBl.getGroupByName(sess, vo, group4.getName() +  ":" +group9.getShortName());

		List <Group> allSubGroups = groupsManagerBl.getAllSubGroups(sess, group9);

		assertTrue("Group 9 allSubGroups should have contained Group 8", allSubGroups.contains(group8));
		assertTrue("Group 9 allSubGroups should have contained Group 3", allSubGroups.contains(group3));
		assertTrue("Group 9 allSubGroups should have contained Group 6", allSubGroups.contains(group6));
		assertTrue("Group 9 allSubGroups should have contained Group 7", allSubGroups.contains(group7));
	}

	@Test
	public void complexGroupSynchronizationTest() throws Exception {
		System.out.println(CLASS_NAME + "complexGroupSynchronizationTest");

		prepareSubjects();

		subjects.remove(subject6);
		subject6.put("parentGroupName","TestGroup8");
		subjects.add(subject6);

		Map<String, String> subject9 = new HashMap<>();
		subject9.put("groupName","TestGroup9");
		subject9.put("parentGroupName","TestGroup5");
		subject9.put("groupDescription","testovaci9");
		subjects.add(subject9);

		subjects.remove(subject3);
		subject3.put("parentGroupName","TestGroup9");
		subjects.add(subject3);

		subjects.remove(subject8);
		subject8.put("parentGroupName","TestGroup9");
		subjects.add(subject8);

		/*List<String> subGroupsNames9 = new ArrayList<>();
		subGroupsNames9.add("TestGroup8");
		subGroupsNames9.add("TestGroup3");*/

		Map<String, String> subject10 = new HashMap<>();
		subject10.put("groupName","TestGroup10");
		subject10.put("parentGroupName",null);
		subject10.put("groupDescription","testovaci10");
		subjects.add(subject10);

		subjects.remove(subject4);
		subject4.put("parentGroupName","TestGroup10");

		/*List<String> subGroupsNames10 = new ArrayList<>();
		subGroupsNames10.add("TestGroup4");*/

		doReturn(essa).when(extSourceManagerBl).getExtSourceByName(any(PerunSession.class), any(String.class));
		when(essa.getSubjectGroups(any(Map.class))).thenReturn(subjects);
		/*when(essa.getSubGroupsNames("TestGroup9")).thenReturn(subGroupsNames9);
		when(essa.getSubGroupsNames("TestGroup10")).thenReturn(subGroupsNames10);*/
		groupsManagerBl.synchronizeGroupStructure(sess, group1);

		updateGroups();

		group9 = groupsManagerBl.getGroupByName(sess, vo, group5.getName() +  ":" +group9.getShortName());
		group10 = groupsManagerBl.getGroupByName(sess, vo, group1.getName() +  ":" +group10.getShortName());

		List <Group> subGroups1 = groupsManagerBl.getSubGroups(sess, group1);
		List <Group> subGroups2 = groupsManagerBl.getSubGroups(sess, group2);
		List <Group> subGroups10 = groupsManagerBl.getSubGroups(sess, group10);
		List <Group> subGroups5 = groupsManagerBl.getSubGroups(sess, group5);
		List <Group> subGroups9 = groupsManagerBl.getSubGroups(sess, group9);
		List <Group> subGroups8 = groupsManagerBl.getSubGroups(sess, group8);
		List <Group> subGroups3 = groupsManagerBl.getSubGroups(sess, group3);
		List <Group> subGroups6 = groupsManagerBl.getSubGroups(sess, group6);
		List <Group> subGroups7 = groupsManagerBl.getSubGroups(sess, group7);

		assertTrue("Group 1 subGroups should have contained Group 2", subGroups1.contains(group2));
		assertTrue("Group 1 subGroups should have contained Group 10", subGroups1.contains(group10));
		assertTrue("Group 1 subGroups should have contained Group 5", subGroups1.contains(group5));
		assertTrue("Group 2 subGroups should have contained 0 Groups", subGroups2.size() == 0);
		assertTrue("Group 10 subGroups should have contained 0 Groups", subGroups10.size() == 0);
		assertTrue("Group 5 subGroups should have contained Group 9", subGroups5.contains(group9));
		assertTrue("Group 9 subGroups should have contained Group 8", subGroups9.contains(group8));
		assertTrue("Group 9 subGroups should have contained Group 3", subGroups9.contains(group3));
		assertTrue("Group 8 subGroups should have contained Group 6", subGroups8.contains(group6));
		assertTrue("Group 3 subGroups should have contained Group 7", subGroups3.contains(group7));
		assertTrue("Group 6 subGroups should have contained 0 Groups", subGroups6.size() == 0);
		assertTrue("Group 7 subGroups should have contained 0 Groups", subGroups7.size() == 0);

	}

	@Test
	public void changeGroupNameTest() throws Exception {
		System.out.println(CLASS_NAME + "updateGroupTest");

		prepareSubjects();
		subjects.remove(subject4);
		subject4.put("groupName","TestGroup12");
		subjects.add(subject4);

		subjects.remove(subject5);
		subject5.put("parentGroupName","TestGroup12");
		subjects.add(subject5);

		/*List<String> subGroupsNames12 = new ArrayList<>();
		subGroupsNames12.add("TestGroup5");*/

		doReturn(essa).when(extSourceManagerBl).getExtSourceByName(any(PerunSession.class), any(String.class));
		when(essa.getSubjectGroups(any(Map.class))).thenReturn(subjects);
		//when(essa.getSubGroupsNames("TestGroup12")).thenReturn(subGroupsNames12);
		groupsManagerBl.synchronizeGroupStructure(sess, group1);

		updateGroups();

		Group group12 = groupsManagerBl.getGroupByName(sess, vo, group2.getName() +  ":TestGroup12");

		List <Group> subGroups = groupsManagerBl.getSubGroups(sess, group2);
		List <Group> subGroups12 = groupsManagerBl.getSubGroups(sess, group12);

		assertTrue("Group 4 should have been just renamed to TestGroup12!", subGroups.contains(group12));
		assertTrue("Group 12 should have contained group 5 !", subGroups12.contains(group5));
	}

	// PRIVATE METHODS -------------------------------------------------------------

	private Vo setUpVo() throws Exception {

		Vo newVo = new Vo(0, "UserManagerTestVo", "UMTestVo");
		Vo returnedVo = perun.getVosManagerBl().createVo(sess, newVo);
		// create test VO in database
		assertNotNull("unable to create testing Vo",returnedVo);

		return returnedVo;

	}

	private void setUpGroupStructure(Vo vo) throws Exception {

		extSource = extSourceManagerBl.createExtSource(sess, extSource, null);
		groupsManagerBl.createGroup(sess, vo, group1);
		extSourceManagerBl.addExtSource(sess, vo, extSource);
		Attribute attr = attributesManager.getAttribute(sess, group1, groupsManager.GROUPEXTSOURCE_ATTRNAME);
		attr.setValue(extSource.getName());
		attributesManager.setAttribute(sess, group1, attr);
		extSourceManagerBl.addExtSource(sess, group1, extSource);
		groupsManagerBl.createGroup(sess, group1, group2);
		groupsManagerBl.createGroup(sess, group1, group3);
		groupsManagerBl.createGroup(sess, group2, group4);
		groupsManagerBl.createGroup(sess, group4, group5);
		groupsManagerBl.createGroup(sess, group3, group6);
		groupsManagerBl.createGroup(sess, group3, group7);
		groupsManagerBl.createGroup(sess, group1, group8);
	}

	private void prepareSubjects(){
		subject8.put("groupName","TestGroup8");
		subject8.put("parentGroupName","TestGroup1");
		subject8.put("groupDescription","testovaci8");
		subjects.add(subject8);
		subject2.put("groupName","TestGroup2");
		subject2.put("parentGroupName","TestGroup1");
		subject2.put("groupDescription","testovaci2");
		subjects.add(subject2);
		subject3.put("groupName","TestGroup3");
		subject3.put("parentGroupName","TestGroup1");
		subject3.put("groupDescription","testovaci3");
		subjects.add(subject3);
		subject4.put("groupName","TestGroup4");
		subject4.put("parentGroupName","TestGroup2");
		subject4.put("groupDescription","testovaci4");
		subjects.add(subject4);
		subject5.put("groupName","TestGroup5");
		subject5.put("parentGroupName","TestGroup4");
		subject5.put("groupDescription","testovaci5");
		subjects.add(subject5);
		subject6.put("groupName","TestGroup6");
		subject6.put("parentGroupName","TestGroup3");
		subject6.put("groupDescription","testovaci6");
		subjects.add(subject6);
		subject7.put("groupName","TestGroup7");
		subject7.put("parentGroupName","TestGroup3");
		subject7.put("groupDescription","testovaci7");
		subjects.add(subject7);
	}

	private void updateGroups() {
		try {
			group1 = groupsManagerBl.getGroupById(sess, group1.getId());
		} catch (InternalErrorException e) {
		} catch (GroupNotExistsException e) {
		}
		try {
			group2 = groupsManagerBl.getGroupById(sess, group2.getId());
		} catch (InternalErrorException e) {
		} catch (GroupNotExistsException e) {
		}
		try {
			group8 = groupsManagerBl.getGroupById(sess, group8.getId());
		} catch (InternalErrorException e) {
		} catch (GroupNotExistsException e) {
		}
		try {
			group3 = groupsManagerBl.getGroupById(sess, group3.getId());
		} catch (InternalErrorException e) {
		} catch (GroupNotExistsException e) {
		}
		try {
			group4 = groupsManagerBl.getGroupById(sess, group4.getId());
		} catch (InternalErrorException e) {
		} catch (GroupNotExistsException e) {
		}
		try {
			group5 = groupsManagerBl.getGroupById(sess, group5.getId());
		} catch (InternalErrorException e) {
		} catch (GroupNotExistsException e) {
		}
		try {
			group6 = groupsManagerBl.getGroupById(sess, group6.getId());
		} catch (InternalErrorException e) {
		} catch (GroupNotExistsException e) {
		}
		try {
			group7 = groupsManagerBl.getGroupById(sess, group7.getId());
		} catch (InternalErrorException e) {
		} catch (GroupNotExistsException e) {
		}
		try {
			group9 = groupsManagerBl.getGroupById(sess, group9.getId());
		} catch (InternalErrorException e) {
		} catch (GroupNotExistsException e) {
		}
	}
}
