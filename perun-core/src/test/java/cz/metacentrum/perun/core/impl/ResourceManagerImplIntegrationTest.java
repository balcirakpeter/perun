package cz.metacentrum.perun.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import cz.metacentrum.perun.core.AbstractPerunIntegrationTest;
import cz.metacentrum.perun.core.api.*;
import cz.metacentrum.perun.core.api.exceptions.AttributeNotExistsException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

/**
 * @author  Peter Balcirak
 */
public class ResourceManagerImplIntegrationTest extends AbstractPerunIntegrationTest {

    private String APPROVED;
    private String DISAPPROVED;
    private String MEMBER_STATUS;
    private static final String EMAIL_URN = AttributesManager.NS_MEMBER_ATTR_DEF + ":mail";
    private static final String TZ_URN = AttributesManager.NS_USER_ATTR_DEF + ":timezone";
    private AttributeDefinition reqAttributeMail;
    private AttributeDefinition reqAttributeTZ;
    private Vo vo = new Vo(0, "TestVo", "SyncTestVo");
    private Resource resource = new Resource();
    private Facility facility = new Facility(0, "TestFacility", "Facility for testing");
    private Service service1 =  new Service(0, "FirstTestService");
    private Service service2 =  new Service(1, "SecondTestService");
    private Service service3 =  new Service(2, "ThirdTestService");
    private Group group = new Group("TestGroup", "Group for testing");
    private Group group2 = new Group("TestGroup2", "Group for testing");
    private Member member1 = new Member();
    private Member member2 = new Member();
    private Member member3 = new Member();
    private Member member4 = new Member();
    private Member member5 = new Member();
    private Member member6 = new Member();
    private Member member7 = new Member();
    private Member member8 = new Member();
    private Member member9 = new Member();
    private Member member10 = new Member();

    @Before
    public void setUp() throws Exception {

        APPROVED = perun.getResourcesManager().APPROVED;
        DISAPPROVED = perun.getResourcesManager().DISAPPROVED;
        MEMBER_STATUS = perun.getResourcesManager().MEMBER_STATUS;
        try {
            perun.getAttributesManager().getAttributeDefinition(sess, MEMBER_STATUS);
        } catch (AttributeNotExistsException ex) {
            setMemberStatusAttribute();
        }
        try {
            reqAttributeMail = perun.getAttributesManager().getAttributeDefinition(sess, EMAIL_URN);
        } catch (AttributeNotExistsException ex) {
            setUpEmailAttribute();
        }
        try {
            reqAttributeTZ = perun.getAttributesManager().getAttributeDefinition(sess, TZ_URN);
        } catch (AttributeNotExistsException ex) {
            setUpTZAttribute();
        }

        setUpVo();

        member1 = setUpMember(0);
        member2 = setUpMember(1);
        member3 = setUpMember(2);
        member4 = setUpMember(3);
        member5 = setUpMember(4);
        member6 = setUpMember(5);
        member7 = setUpMember(6);
        member8 = setUpMember(7);
        member9 = setUpMember(8);
        member10 =setUpMember(9);

        setUpFacility();
        setUpResource();
        setUpServices();
        setUpGroups();

        perun.getGroupsManager().addMember(sess, group, member1);
        perun.getGroupsManager().addMember(sess, group, member2);
        perun.getGroupsManager().addMember(sess, group, member3);

        perun.getGroupsManager().addMember(sess, group2, member6);
        perun.getGroupsManager().addMember(sess, group2, member7);
        perun.getGroupsManager().addMember(sess, group2, member8);
    }

    @Test
    public void assignGroupsToResourceWithoutServiceTest() throws Exception{

        perun.getResourcesManagerBl().assignGroupToResource(sess, group, resource);

        assertTrue("Resource does not contains group!!" ,perun.getResourcesManager().getAssignedGroups(sess, resource).contains(group));

        List<Member> assignedMembers = perun.getResourcesManager().getAssignedMembers(sess, resource);

        assertTrue("Resource does not contains 3 members!", assignedMembers.size() == 3);

        Attribute status1 = perun.getAttributesManager().getAttribute(sess, resource, member1, MEMBER_STATUS);
        Attribute status2 = perun.getAttributesManager().getAttribute(sess, resource, member2, MEMBER_STATUS);
        Attribute status3 = perun.getAttributesManager().getAttribute(sess, resource, member3, MEMBER_STATUS);

        assertTrue("Member1 has to be approved!!", status1.getValue().equals(APPROVED));
        assertTrue("Member2 has to be approved!!", status2.getValue().equals(APPROVED));
        assertTrue("Member3 has to be approved!!", status3.getValue().equals(APPROVED));

        perun.getResourcesManagerBl().assignGroupToResource(sess, group2, resource);

        status1 = perun.getAttributesManager().getAttribute(sess, resource, member6, MEMBER_STATUS);
        status2 = perun.getAttributesManager().getAttribute(sess, resource, member7, MEMBER_STATUS);
        status3 = perun.getAttributesManager().getAttribute(sess, resource, member8, MEMBER_STATUS);

        assertTrue("Member6 has to be approved!!", status1.getValue().equals(APPROVED));
        assertTrue("Member7 has to be approved!!", status2.getValue().equals(APPROVED));
        assertTrue("Member8 has to be approved!!", status3.getValue().equals(APPROVED));
    }

    @Test
    public void assignServiceToResourceTest() throws Exception{

        perun.getResourcesManagerBl().assignGroupToResource(sess, group2, resource);
        perun.getResourcesManagerBl().assignGroupToResource(sess, group, resource);
        perun.getResourcesManagerBl().assignService(sess, resource, service1);

        assertTrue("Resource does not contains service!!" ,perun.getResourcesManager().getAssignedServices(sess, resource).contains(service1));

        Attribute attr = perun.getAttributesManager().getAttribute(sess, member3, reqAttributeMail.getName());
        attr.setValue("peter.balcirak@gmail.com");
        perun.getAttributesManagerBl().setAttribute(sess, member3, attr);

        perun.getServicesManager().addRequiredAttribute(sess, service1, reqAttributeMail);

        Attribute status1 = perun.getAttributesManager().getAttribute(sess, resource, member1, MEMBER_STATUS);
        Attribute status2 = perun.getAttributesManager().getAttribute(sess, resource, member2, MEMBER_STATUS);
        Attribute status3 = perun.getAttributesManager().getAttribute(sess, resource, member3, MEMBER_STATUS);

        assertTrue("Member1 has to be disapproved!!", status1.getValue().equals(DISAPPROVED));
        assertTrue("Member2 has to be disapproved!!", status2.getValue().equals(DISAPPROVED));
        assertTrue("Member3 has to be approved!!", status3.getValue().equals(APPROVED));

        status1 = perun.getAttributesManager().getAttribute(sess, resource, member6, MEMBER_STATUS);
        status2 = perun.getAttributesManager().getAttribute(sess, resource, member7, MEMBER_STATUS);
        status3 = perun.getAttributesManager().getAttribute(sess, resource, member8, MEMBER_STATUS);

        assertTrue("Member6 has to be disapproved!!", status1.getValue().equals(DISAPPROVED));
        assertTrue("Member7 has to be disapproved!!", status2.getValue().equals(DISAPPROVED));
        assertTrue("Member8 has to be disapproved!!", status3.getValue().equals(DISAPPROVED));

        attr = perun.getAttributesManager().getAttribute(sess, member6, reqAttributeMail.getName());
        attr.setValue("peterBalcirak@zoznam.com");
        perun.getAttributesManagerBl().setAttribute(sess, member6, attr);

        status1 = perun.getAttributesManager().getAttribute(sess, resource, member6, MEMBER_STATUS);

        assertTrue("Member6 has to be approved!!", status1.getValue().equals(APPROVED));
    }

    @Test
    public void assignGroupsToResourceWithServiceTest() throws Exception{

        Attribute attr = perun.getAttributesManager().getAttribute(sess, member3, reqAttributeMail.getName());
        attr.setValue("peter.balcirak@gmail.com");
        perun.getAttributesManagerBl().setAttribute(sess, member3, attr);

        attr = perun.getAttributesManager().getAttribute(sess, member6, reqAttributeMail.getName());
        attr.setValue("peterBalcirak@zoznam.com");
        perun.getAttributesManagerBl().setAttribute(sess, member6, attr);

        perun.getResourcesManagerBl().assignService(sess, resource, service1);
        perun.getServicesManager().addRequiredAttribute(sess, service1, reqAttributeMail);

        perun.getResourcesManagerBl().assignGroupToResource(sess, group, resource);
        perun.getResourcesManagerBl().assignGroupToResource(sess, group2, resource);

        Attribute status1 = perun.getAttributesManager().getAttribute(sess, resource, member1, MEMBER_STATUS);
        Attribute status2 = perun.getAttributesManager().getAttribute(sess, resource, member2, MEMBER_STATUS);
        Attribute status3 = perun.getAttributesManager().getAttribute(sess, resource, member3, MEMBER_STATUS);

        assertTrue("Member1 has to be disapproved!!", status1.getValue().equals(DISAPPROVED));
        assertTrue("Member2 has to be disapproved!!", status2.getValue().equals(DISAPPROVED));
        assertTrue("Member3 has to be approved!!", status3.getValue().equals(APPROVED));

        status1 = perun.getAttributesManager().getAttribute(sess, resource, member6, MEMBER_STATUS);
        status2 = perun.getAttributesManager().getAttribute(sess, resource, member7, MEMBER_STATUS);
        status3 = perun.getAttributesManager().getAttribute(sess, resource, member8, MEMBER_STATUS);

        assertTrue("Member6 has to be approved!!", status1.getValue().equals(APPROVED));
        assertTrue("Member7 has to be disapproved!!", status2.getValue().equals(DISAPPROVED));
        assertTrue("Member8 has to be disapproved!!", status3.getValue().equals(DISAPPROVED));
    }

    @Test
    public void setAttributeTest() throws Exception{

        Attribute attrMail = perun.getAttributesManager().getAttribute(sess, member3, reqAttributeMail.getName());
        attrMail.setValue("peter.balcirak@gmail.com");
        perun.getAttributesManagerBl().setAttribute(sess, member3, attrMail);

        perun.getResourcesManagerBl().assignService(sess, resource, service1);
        perun.getServicesManager().addRequiredAttribute(sess, service1, reqAttributeMail);

        perun.getResourcesManagerBl().assignGroupToResource(sess, group, resource);

        perun.getResourcesManagerBl().assignService(sess, resource, service2);
        perun.getServicesManager().addRequiredAttribute(sess, service2, reqAttributeTZ);

        Attribute status3 = perun.getAttributesManager().getAttribute(sess, resource, member3, MEMBER_STATUS);
        assertTrue("1. Member3 has to be disapproved!!", status3.getValue().equals(DISAPPROVED));

        Attribute attrTZ = perun.getAttributesManager().getAttribute(sess, perun.getUsersManager().getUserByMember(sess, member3), reqAttributeTZ.getName());
        attrTZ.setValue("Turkey");
        perun.getAttributesManagerBl().setAttribute(sess, perun.getUsersManager().getUserByMember(sess, member3), attrTZ);

        Attribute status1 = perun.getAttributesManager().getAttribute(sess, resource, member1, MEMBER_STATUS);
        Attribute status2 = perun.getAttributesManager().getAttribute(sess, resource, member2, MEMBER_STATUS);
        status3 = perun.getAttributesManager().getAttribute(sess, resource, member3, MEMBER_STATUS);

        assertTrue("Member1 has to be disapproved!!", status1.getValue().equals(DISAPPROVED));
        assertTrue("Member2 has to be disapproved!!", status2.getValue().equals(DISAPPROVED));
        assertTrue("Member3 has to be approved!!", status3.getValue().equals(APPROVED));
    }

    @Test
    public void removeRequiredAttributeTest() throws Exception{

        Attribute attr = perun.getAttributesManager().getAttribute(sess, member3, reqAttributeMail.getName());
        attr.setValue("peter.balcirak@gmail.com");
        perun.getAttributesManagerBl().setAttribute(sess, member3, attr);


        attr = perun.getAttributesManager().getAttribute(sess, member6, reqAttributeMail.getName());
        attr.setValue("peterBalcirak@zoznam.com");
        perun.getAttributesManagerBl().setAttribute(sess, member6, attr);

        perun.getResourcesManagerBl().assignService(sess, resource, service1);
        perun.getServicesManager().addRequiredAttribute(sess, service1, reqAttributeMail);

        perun.getResourcesManagerBl().assignGroupToResource(sess, group, resource);
        perun.getResourcesManagerBl().assignGroupToResource(sess, group2, resource);

        Attribute status1 = perun.getAttributesManager().getAttribute(sess, resource, member1, MEMBER_STATUS);
        Attribute status2 = perun.getAttributesManager().getAttribute(sess, resource, member2, MEMBER_STATUS);
        Attribute status3 = perun.getAttributesManager().getAttribute(sess, resource, member3, MEMBER_STATUS);

        assertTrue("Member1 has to be disapproved!!", status1.getValue().equals(DISAPPROVED));
        assertTrue("Member2 has to be disapproved!!", status2.getValue().equals(DISAPPROVED));
        assertTrue("Member3 has to be approved!!", status3.getValue().equals(APPROVED));

        status1 = perun.getAttributesManager().getAttribute(sess, resource, member6, MEMBER_STATUS);
        status2 = perun.getAttributesManager().getAttribute(sess, resource, member7, MEMBER_STATUS);
        status3 = perun.getAttributesManager().getAttribute(sess, resource, member8, MEMBER_STATUS);

        assertTrue("Member6 has to be approved!!", status1.getValue().equals(APPROVED));
        assertTrue("Member7 has to be disapproved!!", status2.getValue().equals(DISAPPROVED));
        assertTrue("Member8 has to be disapproved!!", status3.getValue().equals(DISAPPROVED));

        perun.getServicesManager().removeRequiredAttribute(sess, service1, reqAttributeMail);

        status1 = perun.getAttributesManager().getAttribute(sess, resource, member1, MEMBER_STATUS);
        status2 = perun.getAttributesManager().getAttribute(sess, resource, member2, MEMBER_STATUS);
        status3 = perun.getAttributesManager().getAttribute(sess, resource, member3, MEMBER_STATUS);

        assertTrue("Member1 has to be approved!!", status1.getValue().equals(APPROVED));
        assertTrue("Member2 has to be approved!!", status2.getValue().equals(APPROVED));
        assertTrue("Member3 has to be approved!!", status3.getValue().equals(APPROVED));

        status1 = perun.getAttributesManager().getAttribute(sess, resource, member6, MEMBER_STATUS);
        status2 = perun.getAttributesManager().getAttribute(sess, resource, member7, MEMBER_STATUS);
        status3 = perun.getAttributesManager().getAttribute(sess, resource, member8, MEMBER_STATUS);

        assertTrue("Member6 has to be approved!!", status1.getValue().equals(APPROVED));
        assertTrue("Member7 has to be approved!!", status2.getValue().equals(APPROVED));
        assertTrue("Member8 has to be approved!!", status3.getValue().equals(APPROVED));
    }

    @Test
    public void removeServiceTest() throws Exception{

        Attribute attrMail = perun.getAttributesManager().getAttribute(sess, member3, reqAttributeMail.getName());
        attrMail.setValue("peter.balcirak@gmail.com");
        perun.getAttributesManagerBl().setAttribute(sess, member3, attrMail);

        attrMail = perun.getAttributesManager().getAttribute(sess, member6, reqAttributeMail.getName());
        attrMail.setValue("peterBalcirak@zoznam.com");
        perun.getAttributesManagerBl().setAttribute(sess, member6, attrMail);

        perun.getResourcesManagerBl().assignService(sess, resource, service1);
        perun.getServicesManager().addRequiredAttribute(sess, service1, reqAttributeMail);

        perun.getResourcesManagerBl().assignGroupToResource(sess, group, resource);
        perun.getResourcesManagerBl().assignGroupToResource(sess, group2, resource);

        perun.getResourcesManagerBl().assignService(sess, resource, service2);
        perun.getServicesManager().addRequiredAttribute(sess, service2, reqAttributeTZ);

        Attribute status3 = perun.getAttributesManager().getAttribute(sess, resource, member3, MEMBER_STATUS);
        assertTrue("Member3 has to be disapproved!!", status3.getValue().equals(DISAPPROVED));

        Attribute status1 = perun.getAttributesManager().getAttribute(sess, resource, member6, MEMBER_STATUS);
        assertTrue("Member6 has to be disapproved!!", status1.getValue().equals(DISAPPROVED));

        Attribute attrTZ = perun.getAttributesManager().getAttribute(sess, perun.getUsersManager().getUserByMember(sess, member3), reqAttributeTZ.getName());
        attrTZ.setValue("Turkey");
        perun.getAttributesManagerBl().setAttribute(sess, perun.getUsersManager().getUserByMember(sess, member3), attrTZ);

        attrTZ = perun.getAttributesManager().getAttribute(sess, perun.getUsersManager().getUserByMember(sess, member6), reqAttributeTZ.getName());
        attrTZ.setValue("Turkey");
        perun.getAttributesManagerBl().setAttribute(sess, perun.getUsersManager().getUserByMember(sess, member6), attrTZ);

        status1 = perun.getAttributesManager().getAttribute(sess, resource, member1, MEMBER_STATUS);
        Attribute status2 = perun.getAttributesManager().getAttribute(sess, resource, member2, MEMBER_STATUS);
        status3 = perun.getAttributesManager().getAttribute(sess, resource, member3, MEMBER_STATUS);

        assertTrue("Member1 has to be disapproved!!", status1.getValue().equals(DISAPPROVED));
        assertTrue("Member2 has to be disapproved!!", status2.getValue().equals(DISAPPROVED));
        assertTrue("Member3 has to be approved!!", status3.getValue().equals(APPROVED));

        status1 = perun.getAttributesManager().getAttribute(sess, resource, member6, MEMBER_STATUS);
        status2 = perun.getAttributesManager().getAttribute(sess, resource, member7, MEMBER_STATUS);
        status3 = perun.getAttributesManager().getAttribute(sess, resource, member8, MEMBER_STATUS);

        assertTrue("Member6 has to be approved!!", status1.getValue().equals(APPROVED));
        assertTrue("Member7 has to be disapproved!!", status2.getValue().equals(DISAPPROVED));
        assertTrue("Member8 has to be disapproved!!", status3.getValue().equals(DISAPPROVED));

        perun.getResourcesManagerBl().removeService(sess, resource, service2);
        perun.getResourcesManagerBl().removeService(sess, resource, service1);

        status1 = perun.getAttributesManager().getAttribute(sess, resource, member1, MEMBER_STATUS);
        status2 = perun.getAttributesManager().getAttribute(sess, resource, member2, MEMBER_STATUS);
        status3 = perun.getAttributesManager().getAttribute(sess, resource, member3, MEMBER_STATUS);

        assertTrue("Member1 has to be approved!!", status1.getValue().equals(APPROVED));
        assertTrue("Member2 has to be approved!!", status2.getValue().equals(APPROVED));
        assertTrue("Member3 has to be approved!!", status3.getValue().equals(APPROVED));

        status1 = perun.getAttributesManager().getAttribute(sess, resource, member6, MEMBER_STATUS);
        status2 = perun.getAttributesManager().getAttribute(sess, resource, member7, MEMBER_STATUS);
        status3 = perun.getAttributesManager().getAttribute(sess, resource, member8, MEMBER_STATUS);

        assertTrue("Member6 has to be approved!!", status1.getValue().equals(APPROVED));
        assertTrue("Member7 has to be approved!!", status2.getValue().equals(APPROVED));
        assertTrue("Member8 has to be approved!!", status3.getValue().equals(APPROVED));
    }

    @Test
    public void assignServiceWithReqAtToResourceTest() throws Exception{

        perun.getResourcesManagerBl().assignGroupToResource(sess, group, resource);
        perun.getResourcesManagerBl().assignGroupToResource(sess, group2, resource);

        Attribute attr = perun.getAttributesManager().getAttribute(sess, member3, reqAttributeMail.getName());
        attr.setValue("peter.balcirak@gmail.com");
        perun.getAttributesManagerBl().setAttribute(sess, member3, attr);

        attr = perun.getAttributesManager().getAttribute(sess, member6, reqAttributeMail.getName());
        attr.setValue("peter.lomenak@gmail.com");
        perun.getAttributesManagerBl().setAttribute(sess, member6, attr);

        perun.getServicesManager().addRequiredAttribute(sess, service1, reqAttributeMail);
        perun.getResourcesManagerBl().assignService(sess, resource, service1);

        Attribute status1 = perun.getAttributesManager().getAttribute(sess, resource, member1, MEMBER_STATUS);
        Attribute status2 = perun.getAttributesManager().getAttribute(sess, resource, member2, MEMBER_STATUS);
        Attribute status3 = perun.getAttributesManager().getAttribute(sess, resource, member3, MEMBER_STATUS);

        assertTrue("Member1 has to be disapproved!!", status1.getValue().equals(DISAPPROVED));
        assertTrue("Member2 has to be disapproved!!", status2.getValue().equals(DISAPPROVED));
        assertTrue("Member3 has to be approved!!", status3.getValue().equals(APPROVED));

        status1 = perun.getAttributesManager().getAttribute(sess, resource, member6, MEMBER_STATUS);
        status2 = perun.getAttributesManager().getAttribute(sess, resource, member7, MEMBER_STATUS);
        status3 = perun.getAttributesManager().getAttribute(sess, resource, member8, MEMBER_STATUS);

        assertTrue("Member6 has to be approved!!", status1.getValue().equals(APPROVED));
        assertTrue("Member7 has to be disapproved!!", status2.getValue().equals(DISAPPROVED));
        assertTrue("Member8 has to be disapproved!!", status3.getValue().equals(DISAPPROVED));
    }

    @Test
    public void addMemberToGroupTest() throws Exception{
        perun.getResourcesManagerBl().assignGroupToResource(sess, group, resource);

        Attribute attr = perun.getAttributesManager().getAttribute(sess, member3, reqAttributeMail.getName());
        attr.setValue("peter.balcirak@gmail.com");
        perun.getAttributesManagerBl().setAttribute(sess, member3, attr);

        perun.getServicesManager().addRequiredAttribute(sess, service1, reqAttributeMail);
        perun.getResourcesManagerBl().assignService(sess, resource, service1);

        perun.getGroupsManagerBl().addMember(sess, group, member4);

        attr = perun.getAttributesManager().getAttribute(sess, member5, reqAttributeMail.getName());
        attr.setValue("jurakMrkva@azet.sk");
        perun.getAttributesManagerBl().setAttribute(sess, member5, attr);

        perun.getGroupsManagerBl().addMember(sess, group, member5);

        Attribute status1 = perun.getAttributesManager().getAttribute(sess, resource, member1, MEMBER_STATUS);
        Attribute status2 = perun.getAttributesManager().getAttribute(sess, resource, member2, MEMBER_STATUS);
        Attribute status3 = perun.getAttributesManager().getAttribute(sess, resource, member3, MEMBER_STATUS);
        Attribute status4 = perun.getAttributesManager().getAttribute(sess, resource, member4, MEMBER_STATUS);
        Attribute status5 = perun.getAttributesManager().getAttribute(sess, resource, member5, MEMBER_STATUS);

        assertTrue("Member1 has to be disapproved!!", status1.getValue().equals(DISAPPROVED));
        assertTrue("Member2 has to be disapproved!!", status2.getValue().equals(DISAPPROVED));
        assertTrue("Member3 has to be approved!!", status3.getValue().equals(APPROVED));
        assertTrue("Member4 has to be disapproved!!", status4.getValue().equals(DISAPPROVED));
        assertTrue("Member5 has to be approved!!", status5.getValue().equals(APPROVED));
    }

    @Test
    public void getAllowedMembersTest() throws Exception{
        perun.getResourcesManagerBl().assignGroupToResource(sess, group, resource);
        perun.getResourcesManagerBl().assignGroupToResource(sess, group2, resource);

        Attribute attr = perun.getAttributesManager().getAttribute(sess, member3, reqAttributeMail.getName());
        attr.setValue("peter.balcirak@gmail.com");
        perun.getAttributesManagerBl().setAttribute(sess, member3, attr);

        perun.getServicesManager().addRequiredAttribute(sess, service1, reqAttributeMail);
        perun.getResourcesManagerBl().assignService(sess, resource, service1);

        perun.getGroupsManagerBl().addMember(sess, group, member4);

        attr = perun.getAttributesManager().getAttribute(sess, member5, reqAttributeMail.getName());
        attr.setValue("jurakMrkva@azet.sk");
        perun.getAttributesManagerBl().setAttribute(sess, member5, attr);

        perun.getGroupsManagerBl().addMember(sess, group, member5);

        attr = perun.getAttributesManager().getAttribute(sess, member9, reqAttributeMail.getName());
        attr.setValue("jurakLiska@azetko.sk");
        perun.getAttributesManagerBl().setAttribute(sess, member9, attr);

        perun.getGroupsManagerBl().addMember(sess, group2, member9);

        List<Member> allowedMembers = perun.getResourcesManagerBl().getAllowedMembers(sess, resource);
        assertEquals("There has to be two members in result", 3, allowedMembers.size());

        attr = perun.getAttributesManager().getAttribute(sess, member4, reqAttributeMail.getName());
        attr.setValue("lukas111@azet.sk");
        perun.getAttributesManagerBl().setAttribute(sess, member4, attr);

        allowedMembers = perun.getResourcesManagerBl().getAllowedMembers(sess, resource);
        assertEquals("There has to be 3 members in result", 4, allowedMembers.size());
    }



    //PRIVATE SET UP METHODS--------------------------------------------------
    private AttributeDefinition setMemberStatusAttribute() throws Exception {

        AttributeDefinition attr = new AttributeDefinition();
        attr.setNamespace(AttributesManager.NS_MEMBER_RESOURCE_ATTR_DEF);
        attr.setFriendlyName("memberStatus");
        attr.setDisplayName("Member status");
        attr.setType(String.class.getName());
        attr.setDescription("Member status to resource");

        return perun.getAttributesManager().createAttribute(sess, attr);
    }

    private Member setUpMember(int id) throws Exception {

        Candidate candidate = new Candidate();
        candidate.setFirstName(Long.toHexString(Double.doubleToLongBits(Math.random())));
        candidate.setId(id);
        candidate.setMiddleName("");
        candidate.setLastName(Long.toHexString(Double.doubleToLongBits(Math.random())));
        candidate.setTitleBefore("");
        candidate.setTitleAfter("");

        candidate.setAttributes(new HashMap<String,String>());

        return perun.getMembersManagerBl().createMemberSync(sess, vo, candidate);

    }

    private void setUpFacility() throws Exception{
        facility = perun.getFacilitiesManager().createFacility(sess, facility);
    }

    private void setUpResource()throws Exception{
        resource.setId(0);
        resource.setName("TestResource");
        resource.setFacilityId(facility.getId());
        resource.setVoId(vo.getId());

        resource = perun.getResourcesManager().createResource(sess, resource, vo, facility);
    }

    private void setUpVo() throws Exception {
        vo = perun.getVosManager().createVo(sess, vo);
    }

    private void setUpServices() throws Exception {
        service1 = perun.getServicesManager().createService(sess, service1);
        service2 = perun.getServicesManager().createService(sess, service2);
        service3 = perun.getServicesManager().createService(sess, service3);
    }

    private void setUpGroups() throws Exception {
        group = perun.getGroupsManager().createGroup(sess, vo, group);
        group2 = perun.getGroupsManager().createGroup(sess, vo, group2);
    }

    private void setUpEmailAttribute() throws Exception {

        AttributeDefinition attr = new AttributeDefinition();
        attr.setNamespace(AttributesManager.NS_MEMBER_ATTR_DEF);
        attr.setFriendlyName("mail");
        attr.setDisplayName("Mail");
        attr.setType(String.class.getName());
        attr.setDescription("Member's trusted mail.");

        reqAttributeMail = perun.getAttributesManager().createAttribute(sess, attr);

    }

    private void setUpTZAttribute() throws Exception {

        AttributeDefinition attr = new AttributeDefinition();
        attr.setNamespace(AttributesManager.NS_USER_ATTR_DEF);
        attr.setFriendlyName("timezone");
        attr.setDisplayName("Timezone");
        attr.setType(String.class.getName());
        attr.setDescription("User's timezone described by ±[hh] (ISO 8601).");

        reqAttributeTZ = perun.getAttributesManager().createAttribute(sess, attr);

    }

}
