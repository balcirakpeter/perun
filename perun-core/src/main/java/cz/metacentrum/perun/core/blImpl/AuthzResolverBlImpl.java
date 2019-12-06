package cz.metacentrum.perun.core.blImpl;

import cz.metacentrum.perun.audit.events.AuthorizationEvents.RoleSetForGroup;
import cz.metacentrum.perun.audit.events.AuthorizationEvents.RoleSetForUser;
import cz.metacentrum.perun.audit.events.AuthorizationEvents.RoleUnsetForGroup;
import cz.metacentrum.perun.audit.events.AuthorizationEvents.RoleUnsetForUser;
import cz.metacentrum.perun.audit.events.FacilityManagerEvents.AdminAddedForFacility;
import cz.metacentrum.perun.audit.events.FacilityManagerEvents.AdminGroupAddedForFacility;
import cz.metacentrum.perun.audit.events.FacilityManagerEvents.AdminGroupRemovedForFacility;
import cz.metacentrum.perun.audit.events.FacilityManagerEvents.AdminRemovedForFacility;
import cz.metacentrum.perun.audit.events.GroupManagerEvents.AdminAddedForGroup;
import cz.metacentrum.perun.audit.events.GroupManagerEvents.AdminGroupAddedForGroup;
import cz.metacentrum.perun.audit.events.GroupManagerEvents.AdminGroupRemovedFromGroup;
import cz.metacentrum.perun.audit.events.GroupManagerEvents.AdminRemovedForGroup;
import cz.metacentrum.perun.audit.events.ResourceManagerEvents.AdminGroupAddedForResource;
import cz.metacentrum.perun.audit.events.ResourceManagerEvents.AdminGroupRemovedForResource;
import cz.metacentrum.perun.audit.events.ResourceManagerEvents.AdminUserAddedForResource;
import cz.metacentrum.perun.audit.events.ResourceManagerEvents.AdminUserRemovedForResource;
import cz.metacentrum.perun.audit.events.SecurityTeamsManagerEvents.AdminAddedForSecurityTeam;
import cz.metacentrum.perun.audit.events.SecurityTeamsManagerEvents.AdminGroupAddedForSecurityTeam;
import cz.metacentrum.perun.audit.events.SecurityTeamsManagerEvents.AdminGroupRemovedFromSecurityTeam;
import cz.metacentrum.perun.audit.events.SecurityTeamsManagerEvents.AdminRemovedFromSecurityTeam;
import cz.metacentrum.perun.audit.events.UserManagerEvents.UserPromotedToPerunAdmin;
import cz.metacentrum.perun.audit.events.VoManagerEvents.AdminAddedForVo;
import cz.metacentrum.perun.audit.events.VoManagerEvents.AdminGroupAddedForVo;
import cz.metacentrum.perun.audit.events.VoManagerEvents.AdminGroupRemovedForVo;
import cz.metacentrum.perun.audit.events.VoManagerEvents.AdminRemovedForVo;
import cz.metacentrum.perun.core.api.ActionType;
import cz.metacentrum.perun.core.api.Attribute;
import cz.metacentrum.perun.core.api.AttributeDefinition;
import cz.metacentrum.perun.core.api.AuthzResolver;
import cz.metacentrum.perun.core.api.BeansUtils;
import cz.metacentrum.perun.core.api.Facility;
import cz.metacentrum.perun.core.api.Group;
import cz.metacentrum.perun.core.api.Host;
import cz.metacentrum.perun.core.api.Member;
import cz.metacentrum.perun.core.api.Perun;
import cz.metacentrum.perun.core.api.PerunBean;
import cz.metacentrum.perun.core.api.PerunClient;
import cz.metacentrum.perun.core.api.PerunPolicy;
import cz.metacentrum.perun.core.api.PerunPrincipal;
import cz.metacentrum.perun.core.api.PerunSession;
import cz.metacentrum.perun.core.api.Resource;
import cz.metacentrum.perun.core.api.ResourceTag;
import cz.metacentrum.perun.core.api.RichGroup;
import cz.metacentrum.perun.core.api.RichMember;
import cz.metacentrum.perun.core.api.RichResource;
import cz.metacentrum.perun.core.api.Role;
import cz.metacentrum.perun.core.api.RoleManagementRules;
import cz.metacentrum.perun.core.api.SecurityTeam;
import cz.metacentrum.perun.core.api.Service;
import cz.metacentrum.perun.core.api.Status;
import cz.metacentrum.perun.core.api.User;
import cz.metacentrum.perun.core.api.UserExtSource;
import cz.metacentrum.perun.core.api.Vo;
import cz.metacentrum.perun.core.api.exceptions.ActionTypeNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.AlreadyAdminException;
import cz.metacentrum.perun.core.api.exceptions.AttributeNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.FacilityNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.GroupNotAdminException;
import cz.metacentrum.perun.core.api.exceptions.GroupNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.core.api.exceptions.PolicyNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.ResourceNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.RoleAlreadySetException;
import cz.metacentrum.perun.core.api.exceptions.RoleCannotBeManagedException;
import cz.metacentrum.perun.core.api.exceptions.RoleNotSetException;
import cz.metacentrum.perun.core.api.exceptions.SecurityTeamNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.ServiceNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.UserNotAdminException;
import cz.metacentrum.perun.core.api.exceptions.UserNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.VoNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.WrongAttributeAssignmentException;
import cz.metacentrum.perun.core.bl.AuthzResolverBl;
import cz.metacentrum.perun.core.bl.PerunBl;
import cz.metacentrum.perun.core.bl.VosManagerBl;
import cz.metacentrum.perun.core.impl.Auditer;
import cz.metacentrum.perun.core.impl.AuthzResolverImpl;
import cz.metacentrum.perun.core.impl.AuthzRoles;
import cz.metacentrum.perun.core.impl.Utils;
import cz.metacentrum.perun.core.implApi.AuthzResolverImplApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authorization resolver. It decides if the perunPrincipal has rights to do the provided operation.
 *
 * @author Michal Prochazka <michalp@ics.muni.cz>
 */
public class AuthzResolverBlImpl implements AuthzResolverBl {

	private final static Logger log = LoggerFactory.getLogger(AuthzResolverBlImpl.class);
	private static AuthzResolverImplApi authzResolverImpl;
	private static PerunBl perunBl;

	private static final Pattern columnNamesPattern = Pattern.compile("^[_0-9a-zA-Z]+$");

	private static final String UNSET_ROLE = "UNSET";
	private static final String SET_ROLE = "SET";

	private final static Set<String> extSourcesWithMultipleIdentifiers = BeansUtils.getCoreConfig().getExtSourcesMultipleIdentifiers();

	/**
	 * Prepare necessary structures and resolve access rights for the session's principal.
	 *
	 * @param sess perunSession which contains the principal.
	 * @param policyDefinition is a definition of a policy which will define authorization rules.
	 * @param objects as list of PerunBeans on which will be authorization provided. (e.g. groups, Vos, etc...)
	 * @return true if the principal has particular rights, false otherwise.
	 * @throws PolicyNotExistsException when the given policyDefinition does not exist in the PerunPoliciesContainer.
	 */
	public static boolean authorized(PerunSession sess, String policyDefinition, List<PerunBean> objects) throws PolicyNotExistsException {
		// We need to load additional information about the principal
		if (!sess.getPerunPrincipal().isAuthzInitialized()) {
			refreshAuthz(sess);
		}

		// If the user has no roles, deny access
		if (sess.getPerunPrincipal().getRoles() == null) {
			return false;
		}

		List<PerunPolicy> allPolicies = AuthzResolverImpl.fetchPolicyWithAllIncludedPolicies(policyDefinition);

		List<Map<String, String>> policyRoles = new ArrayList<>();
		for (PerunPolicy policy : allPolicies) policyRoles.addAll(policy.getPerunRoles());

		//Fetch super objects like Vo for group etc.
		Map <String, Set<Integer>> mapOfBeans = fetchAllRelatedObjects(objects);

		return resolveAuthorization(sess, policyRoles, mapOfBeans);
	}

	public static boolean authorizedToManageRole(PerunSession sess, PerunBean object, String roleName) throws PolicyNotExistsException {
		// We need to load additional information about the principal
		if (!sess.getPerunPrincipal().isAuthzInitialized()) {
			refreshAuthz(sess);
		}

		// If the user has no roles, deny access
		if (sess.getPerunPrincipal().getRoles() == null) {
			return false;
		}

		RoleManagementRules rules = AuthzResolverImpl.getRoleManagementRules(roleName);

		Map <String, Set<Integer>> mapOfBeans = new HashMap<>();
		if (object != null)
			//Fetch super objects like Vo for group etc.
			mapOfBeans = fetchAllRelatedObjects(Collections.singletonList(object));

		return resolveAuthorization(sess, rules.getPrivilegedRoles(), mapOfBeans);
	}

	/**
	 * Checks if the principal is authorized.
	 *
	 * @param sess                perunSession
	 * @param role                required role
	 * @param complementaryObject object which specifies particular action of the role (e.g. group)
	 * @return true if the principal authorized, false otherwise
	 * @throws InternalErrorException if something goes wrong
	 */
	public static boolean isAuthorized(PerunSession sess, String role, PerunBean complementaryObject) throws InternalErrorException {
		log.trace("Entering isAuthorized: sess='" + sess + "', role='" + role + "', complementaryObject='" + complementaryObject + "'");
		Utils.notNull(sess, "sess");

		// We need to load additional information about the principal
		if (!sess.getPerunPrincipal().isAuthzInitialized()) {
			refreshAuthz(sess);
		}

		// If the user has no roles, deny access
		if (sess.getPerunPrincipal().getRoles() == null) {
			return false;
		}

		// Perun admin can do anything
		if (sess.getPerunPrincipal().getRoles().hasRole(Role.PERUNADMIN)) {
			return true;
		}

		// If user doesn't have requested role, deny request
		if (!sess.getPerunPrincipal().getRoles().hasRole(role)) {
			return false;
		}

		// Check if the principal has the privileges
		if (complementaryObject != null) {

			String beanName = BeansUtils.convertRichBeanNameToBeanName(complementaryObject.getBeanName());

			// Check various combinations of role and complementary objects
			if (role.equals(Role.VOADMIN) || role.equals(Role.VOOBSERVER)) {
				// VO admin (or VoObserver) and group, get vo id from group and check if the user is vo admin (or VoObserver)
				if (beanName.equals(Group.class.getSimpleName())) {
					return sess.getPerunPrincipal().getRoles().hasRole(role, Vo.class.getSimpleName(), ((Group) complementaryObject).getVoId());
				}
				// VO admin (or VoObserver) and resource, check if the user is vo admin (or VoObserver)
				if (beanName.equals(Resource.class.getSimpleName())) {
					return sess.getPerunPrincipal().getRoles().hasRole(role, Vo.class.getSimpleName(), ((Resource) complementaryObject).getVoId());
				}
				// VO admin (or VoObserver) and member, check if the member is from that VO
				if (beanName.equals(Member.class.getSimpleName())) {
					return sess.getPerunPrincipal().getRoles().hasRole(role, Vo.class.getSimpleName(), ((Member) complementaryObject).getVoId());
				}
			} else if (role.equals(Role.FACILITYADMIN)) {
				// Facility admin and resource, get facility id from resource and check if the user is facility admin
				if (beanName.equals(Resource.class.getSimpleName())) {
					return sess.getPerunPrincipal().getRoles().hasRole(role, Facility.class.getSimpleName(), ((Resource) complementaryObject).getFacilityId());
				}
			} else if (role.equals(Role.RESOURCEADMIN)) {
				// Resource admin, check if the user is admin of resource
				if (beanName.equals(Resource.class.getSimpleName())) {
					return sess.getPerunPrincipal().getRoles().hasRole(role, Resource.class.getSimpleName(), complementaryObject.getId());
				}
			} else if (role.equals(Role.SECURITYADMIN)) {
				// Security admin, check if security admin is admin of the SecurityTeam
				if (beanName.equals(SecurityTeam.class.getSimpleName())) {
					return sess.getPerunPrincipal().getRoles().hasRole(role, SecurityTeam.class.getSimpleName(), complementaryObject.getId());
				}
			} else if (role.equals(Role.GROUPADMIN) || role.equals(Role.TOPGROUPCREATOR)) {
				// Group admin can see some of the date of the VO
				if (beanName.equals(Vo.class.getSimpleName())) {
					return sess.getPerunPrincipal().getRoles().hasRole(role, Vo.class.getSimpleName(), complementaryObject.getId());
				}
			} else if (role.equals(Role.SELF)) {
				// Check if the member belongs to the self role
				if (beanName.equals(Member.class.getSimpleName())) {
					return sess.getPerunPrincipal().getRoles().hasRole(role, User.class.getSimpleName(), ((Member) complementaryObject).getUserId());
				}
			}

			return sess.getPerunPrincipal().getRoles().hasRole(role, complementaryObject);
		} else {
			return true;
		}
	}

	private static Boolean doBeforeAttributeRightsCheck(PerunSession sess, String actionType, AttributeDefinition attrDef) throws InternalErrorException, AttributeNotExistsException {
		Utils.notNull(sess, "sess");
		Utils.notNull(actionType, "ActionType");
		Utils.notNull(attrDef, "AttributeDefinition");
		getPerunBl().getAttributesManagerBl().checkAttributeExists(sess, attrDef);

		// We need to load additional information about the principal
		if (!sess.getPerunPrincipal().isAuthzInitialized()) {
			refreshAuthz(sess);
		}

		// If the user has no roles, deny access
		if (sess.getPerunPrincipal().getRoles() == null) {
			return false;
		}

		// Perun admin can do anything
		if (sess.getPerunPrincipal().getRoles().hasRole(Role.PERUNADMIN)) {
			return true;
		}

		// Engine, Service, RPC and Perunobserver can read attributes
		if ((actionType.equals(ActionType.READ) ||
			actionType.equals(ActionType.READ_PUBLIC) ||
			actionType.equals(ActionType.READ_VO)) &&
			(sess.getPerunPrincipal().getRoles().hasRole(Role.RPC) ||
			sess.getPerunPrincipal().getRoles().hasRole(Role.PERUNOBSERVER) ||
			sess.getPerunPrincipal().getRoles().hasRole(Role.ENGINE))) {
			return true;
		}

		return null;
	}

	/**
	 * From given attributes filter out the ones which are not allowed for the current principal.
	 *
	 * @param sess session
	 * @param bean perun bean
	 * @param attributes attributes
	 * @return list of attributes which can be accessed by current principal.
	 */
	public static List<Attribute> filterNotAllowedAttributes(PerunSession sess, PerunBean bean, List<Attribute> attributes) {
		List<Attribute> allowedAttributes = new ArrayList<>();
		for(Attribute attribute: attributes) {
			try {
				if(AuthzResolver.isAuthorizedForAttribute(sess, ActionType.READ, attribute, bean)) {
					attribute.setWritable(AuthzResolver.isAuthorizedForAttribute(sess, ActionType.WRITE, attribute, bean));
					allowedAttributes.add(attribute);
				}
			} catch (InternalErrorException e) {
				throw new RuntimeException(e);
			}
		}
		return allowedAttributes;
	}

	public static boolean isAuthorizedForAttribute(PerunSession sess, String actionType, AttributeDefinition attrDef, Member member, Resource resource) throws InternalErrorException, AttributeNotExistsException, WrongAttributeAssignmentException {

		//TODO create more complex check and change tests accordingly
		if (member == null && resource != null) {
			return isAuthorizedForAttribute(sess, actionType, attrDef, resource);
		} else if (resource == null && member != null) {
			return isAuthorizedForAttribute(sess, actionType, attrDef, member);
		}

		log.trace("Entering isAuthorizedForAttribute: sess='{}', actionType='{}', attrDef='{}', primaryHolder='{}', " +
			"secondaryHolder='{}'", sess, actionType, attrDef, resource, member);

		//This method get all possible roles which can do action on attribute
		Map<String, Set<ActionType>> roles = getRolesPrivilegedToOperateOnAttribute(sess, actionType, attrDef);

		// If the user has no roles and this attribute does not have any rule for MEMBERSHIP, deny access
		if (sess.getPerunPrincipal().getRoles() == null && !roles.containsKey(Role.MEMBERSHIP)) {
			return false;
		}

		//Test if handlers are correct for attribute namespace
		//getPerunBl().getAttributesManagerBl().checkAttributeAssignment(sess, attrDef, resource, member);

		//Get all unique objects from the roles' action types
		Set<String> uniqueObjectTypes = fetchUniqueObjectTypes(roles);

		//Fetch all possible related objects from the member and the resource according the uniqueObjectTypes
		Map<String, Set<Integer>> mapOfObjectsToCheck = new HashMap<>();
		for (String objectType: uniqueObjectTypes) {
			Set<Integer> retrievedObjects = RelatedMemberResourceObjectsResolver.getValue(objectType).callOn(sess, member, resource);
			mapOfObjectsToCheck.put(objectType, retrievedObjects);
		}

		//Resolve principal's the privileges for the attribute according the rules and objects
		return resolveAttributeAuthorization(sess, roles, mapOfObjectsToCheck);
	}

	public static boolean isAuthorizedForAttribute(PerunSession sess, String actionType, AttributeDefinition attrDef, Group group, Resource resource) throws InternalErrorException, AttributeNotExistsException, WrongAttributeAssignmentException {

		//TODO create more complex check and change tests accordingly
		if (group == null && resource != null) {
			return isAuthorizedForAttribute(sess, actionType, attrDef, resource);
		} else if (resource == null && group != null) {
			return isAuthorizedForAttribute(sess, actionType, attrDef, group);
		}

		log.trace("Entering isAuthorizedForAttribute: sess='{}', actionType='{}', attrDef='{}', primaryHolder='{}', " +
			"secondaryHolder='{}'", sess, actionType, attrDef, group, resource);

		//This method get all possible roles which can do action on attribute
		Map<String, Set<ActionType>> roles = getRolesPrivilegedToOperateOnAttribute(sess, actionType, attrDef);

		// If the user has no roles and this attribute does not have any rule for MEMBERSHIP, deny access
		if (sess.getPerunPrincipal().getRoles() == null && !roles.containsKey(Role.MEMBERSHIP)) {
			return false;
		}

		//Test if handlers are correct for attribute namespace
		//getPerunBl().getAttributesManagerBl().checkAttributeAssignment(sess, attrDef, group, resource);

		//Get all unique objects from the roles' action types
		Set<String> uniqueObjectTypes = fetchUniqueObjectTypes(roles);

		//Fetch all possible related objects from the member and the resource according the uniqueObjectTypes
		Map<String, Set<Integer>> mapOfObjectsToCheck = new HashMap<>();
		for (String objectType: uniqueObjectTypes) {
			Set<Integer> retrievedObjects = RelatedGroupResourceObjectsResolver.getValue(objectType).callOn(sess, group, resource);
			mapOfObjectsToCheck.put(objectType, retrievedObjects);
		}

		//Resolve principal's the privileges for the attribute according the rules and objects
		return resolveAttributeAuthorization(sess, roles, mapOfObjectsToCheck);
	}

	public static boolean isAuthorizedForAttribute(PerunSession sess, String actionType, AttributeDefinition attrDef, User user, Facility facility) throws InternalErrorException, AttributeNotExistsException, WrongAttributeAssignmentException {

		//TODO create more complex check and change tests accordingly
		if (user == null && facility != null) {
			return isAuthorizedForAttribute(sess, actionType, attrDef, facility);
		} else if (facility == null && user != null) {
			return isAuthorizedForAttribute(sess, actionType, attrDef, user);
		}

		log.trace("Entering isAuthorizedForAttribute: sess='{}', actionType='{}', attrDef='{}', primaryHolder='{}', " +
			"secondaryHolder='{}'", sess, actionType, attrDef, user, facility);

		//This method get all possible roles which can do action on attribute
		Map<String, Set<ActionType>> roles = getRolesPrivilegedToOperateOnAttribute(sess, actionType, attrDef);

		// If the user has no roles and this attribute does not have any rule for MEMBERSHIP, deny access
		if (sess.getPerunPrincipal().getRoles() == null && !roles.containsKey(Role.MEMBERSHIP)) {
			return false;
		}

		//Test if handlers are correct for attribute namespace
		//getPerunBl().getAttributesManagerBl().checkAttributeAssignment(sess, attrDef, user, facility);

		//Get all unique objects from the roles' action types
		Set<String> uniqueObjectTypes = fetchUniqueObjectTypes(roles);

		//Fetch all possible related objects from the member and the resource according the uniqueObjectTypes
		Map<String, Set<Integer>> mapOfObjectsToCheck = new HashMap<>();
		for (String objectType: uniqueObjectTypes) {
			Set<Integer> retrievedObjects = RelatedUserFacilityObjectsResolver.getValue(objectType).callOn(sess, user, facility);
			mapOfObjectsToCheck.put(objectType, retrievedObjects);
		}

		//Resolve principal's the privileges for the attribute according the rules and objects
		return resolveAttributeAuthorization(sess, roles, mapOfObjectsToCheck);
	}

	public static boolean isAuthorizedForAttribute(PerunSession sess, String actionType, AttributeDefinition attrDef, Member member, Group group) throws InternalErrorException, AttributeNotExistsException, WrongAttributeAssignmentException {

		//TODO create more complex check and change tests accordingly
		if (group == null && member != null) {
			return isAuthorizedForAttribute(sess, actionType, attrDef, member);
		} else if (member == null && group != null) {
			return isAuthorizedForAttribute(sess, actionType, attrDef, group);
		}

		log.trace("Entering isAuthorizedForAttribute: sess='{}', actionType='{}', attrDef='{}', primaryHolder='{}', " +
			"secondaryHolder='{}'", sess, actionType, attrDef, member, group);

		//This method get all possible roles which can do action on attribute
		Map<String, Set<ActionType>> roles = getRolesPrivilegedToOperateOnAttribute(sess, actionType, attrDef);

		// If the user has no roles and this attribute does not have any rule for MEMBERSHIP, deny access
		if (sess.getPerunPrincipal().getRoles() == null && !roles.containsKey(Role.MEMBERSHIP)) {
			return false;
		}

		//Test if handlers are correct for attribute namespace
		//getPerunBl().getAttributesManagerBl().checkAttributeAssignment(sess, attrDef, member, group);

		//Get all unique objects from the roles' action types
		Set<String> uniqueObjectTypes = fetchUniqueObjectTypes(roles);

		//Fetch all possible related objects from the member and the resource according the uniqueObjectTypes
		Map<String, Set<Integer>> mapOfObjectsToCheck = new HashMap<>();
		for (String objectType: uniqueObjectTypes) {
			Set<Integer> retrievedObjects = RelatedMemberGroupObjectsResolver.getValue(objectType).callOn(sess, member, group);
			mapOfObjectsToCheck.put(objectType, retrievedObjects);
		}

		//Resolve principal's the privileges for the attribute according the rules and objects
		return resolveAttributeAuthorization(sess, roles, mapOfObjectsToCheck);
	}

	public static boolean isAuthorizedForAttribute(PerunSession sess, String actionType, AttributeDefinition attrDef, User user) throws InternalErrorException, AttributeNotExistsException, WrongAttributeAssignmentException {
		log.trace("Entering isAuthorizedForAttribute: sess='{}', actionType='{}', attrDef='{}', primaryHolder='{}', " +
			"secondaryHolder='{}'", sess, actionType, attrDef, user, null);

		//This method get all possible roles which can do action on attribute
		Map<String, Set<ActionType>> roles = getRolesPrivilegedToOperateOnAttribute(sess, actionType, attrDef);

		// If the user has no roles and this attribute does not have any rule for MEMBERSHIP, deny access
		if (sess.getPerunPrincipal().getRoles() == null && !roles.containsKey(Role.MEMBERSHIP)) {
			return false;
		}

		//Test if handlers are correct for attribute namespace
		//getPerunBl().getAttributesManagerBl().checkAttributeAssignment(sess, attrDef, user);

		//Get all unique objects from the roles' action types
		Set<String> uniqueObjectTypes = fetchUniqueObjectTypes(roles);

		//Fetch all possible related objects from the member and the resource according the uniqueObjectTypes
		Map<String, Set<Integer>> mapOfObjectsToCheck = new HashMap<>();
		for (String objectType: uniqueObjectTypes) {
			Set<Integer> retrievedObjects = RelatedUserObjectsResolver.getValue(objectType).apply(sess, user);
			mapOfObjectsToCheck.put(objectType, retrievedObjects);
		}

		//Resolve principal's the privileges for the attribute according the rules and objects
		return resolveAttributeAuthorization(sess, roles, mapOfObjectsToCheck);
	}

	public static boolean isAuthorizedForAttribute(PerunSession sess, String actionType, AttributeDefinition attrDef, Member member) throws InternalErrorException, AttributeNotExistsException, WrongAttributeAssignmentException {
		log.trace("Entering isAuthorizedForAttribute: sess='{}', actionType='{}', attrDef='{}', primaryHolder='{}', " +
			"secondaryHolder='{}'", sess, actionType, attrDef, member, null);

		//This method get all possible roles which can do action on attribute
		Map<String, Set<ActionType>> roles = getRolesPrivilegedToOperateOnAttribute(sess, actionType, attrDef);

		// If the user has no roles and this attribute does not have any rule for MEMBERSHIP, deny access
		if (sess.getPerunPrincipal().getRoles() == null && !roles.containsKey(Role.MEMBERSHIP)) {
			return false;
		}

		//Test if handlers are correct for attribute namespace
		//getPerunBl().getAttributesManagerBl().checkAttributeAssignment(sess, attrDef, member);

		//Get all unique objects from the roles' action types
		Set<String> uniqueObjectTypes = fetchUniqueObjectTypes(roles);

		//Fetch all possible related objects from the member and the resource according the uniqueObjectTypes
		Map<String, Set<Integer>> mapOfObjectsToCheck = new HashMap<>();
		for (String objectType: uniqueObjectTypes) {
			Set<Integer> retrievedObjects = RelatedMemberObjectsResolver.getValue(objectType).apply(sess, member);
			mapOfObjectsToCheck.put(objectType, retrievedObjects);
		}

		//Resolve principal's the privileges for the attribute according the rules and objects
		return resolveAttributeAuthorization(sess, roles, mapOfObjectsToCheck);
	}

	public static boolean isAuthorizedForAttribute(PerunSession sess, String actionType, AttributeDefinition attrDef, Vo vo) throws InternalErrorException, AttributeNotExistsException, WrongAttributeAssignmentException {
		log.trace("Entering isAuthorizedForAttribute: sess='{}', actionType='{}', attrDef='{}', primaryHolder='{}', " +
			"secondaryHolder='{}'", sess, actionType, attrDef, vo, null);

		//This method get all possible roles which can do action on attribute
		Map<String, Set<ActionType>> roles = getRolesPrivilegedToOperateOnAttribute(sess, actionType, attrDef);

		// If the user has no roles and this attribute does not have any rule for MEMBERSHIP, deny access
		if (sess.getPerunPrincipal().getRoles() == null && !roles.containsKey(Role.MEMBERSHIP)) {
			return false;
		}

		//Test if handlers are correct for attribute namespace
		//getPerunBl().getAttributesManagerBl().checkAttributeAssignment(sess, attrDef, vo);

		//Get all unique objects from the roles' action types
		Set<String> uniqueObjectTypes = fetchUniqueObjectTypes(roles);

		//Fetch all possible related objects from the member and the resource according the uniqueObjectTypes
		Map<String, Set<Integer>> mapOfObjectsToCheck = new HashMap<>();
		for (String objectType: uniqueObjectTypes) {
			Set<Integer> retrievedObjects = RelatedVoObjectsResolver.getValue(objectType).apply(sess, vo);
			mapOfObjectsToCheck.put(objectType, retrievedObjects);
		}

		//Resolve principal's the privileges for the attribute according the rules and objects
		return resolveAttributeAuthorization(sess, roles, mapOfObjectsToCheck);
	}

	public static boolean isAuthorizedForAttribute(PerunSession sess, String actionType, AttributeDefinition attrDef, Group group) throws InternalErrorException, AttributeNotExistsException, WrongAttributeAssignmentException {
		log.trace("Entering isAuthorizedForAttribute: sess='{}', actionType='{}', attrDef='{}', primaryHolder='{}', " +
			"secondaryHolder='{}'", sess, actionType, attrDef, group, null);

		//This method get all possible roles which can do action on attribute
		Map<String, Set<ActionType>> roles = getRolesPrivilegedToOperateOnAttribute(sess, actionType, attrDef);

		// If the user has no roles and this attribute does not have any rule for MEMBERSHIP, deny access
		if (sess.getPerunPrincipal().getRoles() == null && !roles.containsKey(Role.MEMBERSHIP)) {
			return false;
		}

		//Test if handlers are correct for attribute namespace
		//getPerunBl().getAttributesManagerBl().checkAttributeAssignment(sess, attrDef, group);

		//Get all unique objects from the roles' action types
		Set<String> uniqueObjectTypes = fetchUniqueObjectTypes(roles);

		//Fetch all possible related objects from the member and the resource according the uniqueObjectTypes
		Map<String, Set<Integer>> mapOfObjectsToCheck = new HashMap<>();
		for (String objectType: uniqueObjectTypes) {
			Set<Integer> retrievedObjects = RelatedGroupObjectsResolver.getValue(objectType).apply(sess, group);
			mapOfObjectsToCheck.put(objectType, retrievedObjects);
		}

		//Resolve principal's the privileges for the attribute according the rules and objects
		return resolveAttributeAuthorization(sess, roles, mapOfObjectsToCheck);
	}

	public static boolean isAuthorizedForAttribute(PerunSession sess, String actionType, AttributeDefinition attrDef, Resource resource) throws InternalErrorException, AttributeNotExistsException, WrongAttributeAssignmentException {
		log.trace("Entering isAuthorizedForAttribute: sess='{}', actionType='{}', attrDef='{}', primaryHolder='{}', " +
			"secondaryHolder='{}'", sess, actionType, attrDef, resource, null);

		//This method get all possible roles which can do action on attribute
		Map<String, Set<ActionType>> roles = getRolesPrivilegedToOperateOnAttribute(sess, actionType, attrDef);

		// If the user has no roles and this attribute does not have any rule for MEMBERSHIP, deny access
		if (sess.getPerunPrincipal().getRoles() == null && !roles.containsKey(Role.MEMBERSHIP)) {
			return false;
		}

		//Test if handlers are correct for attribute namespace
		//getPerunBl().getAttributesManagerBl().checkAttributeAssignment(sess, attrDef, resource);

		//Get all unique objects from the roles' action types
		Set<String> uniqueObjectTypes = fetchUniqueObjectTypes(roles);

		//Fetch all possible related objects from the member and the resource according the uniqueObjectTypes
		Map<String, Set<Integer>> mapOfObjectsToCheck = new HashMap<>();
		for (String objectType: uniqueObjectTypes) {
			Set<Integer> retrievedObjects = RelatedResourceObjectsResolver.getValue(objectType).apply(sess, resource);
			mapOfObjectsToCheck.put(objectType, retrievedObjects);
		}

		//Resolve principal's the privileges for the attribute according the rules and objects
		return resolveAttributeAuthorization(sess, roles, mapOfObjectsToCheck);
	}

	public static boolean isAuthorizedForAttribute(PerunSession sess, String actionType, AttributeDefinition attrDef, Facility facility) throws InternalErrorException, AttributeNotExistsException, WrongAttributeAssignmentException {
		log.trace("Entering isAuthorizedForAttribute: sess='{}', actionType='{}', attrDef='{}', primaryHolder='{}', " +
			"secondaryHolder='{}'", sess, actionType, attrDef, facility, null);

		//This method get all possible roles which can do action on attribute
		Map<String, Set<ActionType>> roles = getRolesPrivilegedToOperateOnAttribute(sess, actionType, attrDef);

		// If the user has no roles and this attribute does not have any rule for MEMBERSHIP, deny access
		if (sess.getPerunPrincipal().getRoles() == null && !roles.containsKey(Role.MEMBERSHIP)) {
			return false;
		}

		//Test if handlers are correct for attribute namespace
		//getPerunBl().getAttributesManagerBl().checkAttributeAssignment(sess, attrDef, facility);

		//Get all unique objects from the roles' action types
		Set<String> uniqueObjectTypes = fetchUniqueObjectTypes(roles);

		//Fetch all possible related objects from the member and the resource according the uniqueObjectTypes
		Map<String, Set<Integer>> mapOfObjectsToCheck = new HashMap<>();
		for (String objectType: uniqueObjectTypes) {
			Set<Integer> retrievedObjects = RelatedFacilityObjectsResolver.getValue(objectType).apply(sess, facility);
			mapOfObjectsToCheck.put(objectType, retrievedObjects);
		}

		//Resolve principal's the privileges for the attribute according the rules and objects
		return resolveAttributeAuthorization(sess, roles, mapOfObjectsToCheck);
	}

	public static boolean isAuthorizedForAttribute(PerunSession sess, String actionType, AttributeDefinition attrDef, Host host) throws InternalErrorException, AttributeNotExistsException, WrongAttributeAssignmentException {
		log.trace("Entering isAuthorizedForAttribute: sess='{}', actionType='{}', attrDef='{}', primaryHolder='{}', " +
			"secondaryHolder='{}'", sess, actionType, attrDef, host, null);

		//This method get all possible roles which can do action on attribute
		Map<String, Set<ActionType>> roles = getRolesPrivilegedToOperateOnAttribute(sess, actionType, attrDef);

		// If the user has no roles and this attribute does not have any rule for MEMBERSHIP, deny access
		if (sess.getPerunPrincipal().getRoles() == null && !roles.containsKey(Role.MEMBERSHIP)) {
			return false;
		}

		//Test if handlers are correct for attribute namespace
		//getPerunBl().getAttributesManagerBl().checkAttributeAssignment(sess, attrDef, host);

		//Get all unique objects from the roles' action types
		Set<String> uniqueObjectTypes = fetchUniqueObjectTypes(roles);

		//Fetch all possible related objects from the member and the resource according the uniqueObjectTypes
		Map<String, Set<Integer>> mapOfObjectsToCheck = new HashMap<>();
		for (String objectType: uniqueObjectTypes) {
			Set<Integer> retrievedObjects = RelatedHostObjectsResolver.getValue(objectType).apply(sess, host);
			mapOfObjectsToCheck.put(objectType, retrievedObjects);
		}

		//Resolve principal's the privileges for the attribute according the rules and objects
		return resolveAttributeAuthorization(sess, roles, mapOfObjectsToCheck);
	}

	public static boolean isAuthorizedForAttribute(PerunSession sess, String actionType, AttributeDefinition attrDef, UserExtSource ues) throws InternalErrorException, AttributeNotExistsException, WrongAttributeAssignmentException {
		log.trace("Entering isAuthorizedForAttribute: sess='{}', actionType='{}', attrDef='{}', primaryHolder='{}', " +
			"secondaryHolder='{}'", sess, actionType, attrDef, ues, null);

		//This method get all possible roles which can do action on attribute
		Map<String, Set<ActionType>> roles = getRolesPrivilegedToOperateOnAttribute(sess, actionType, attrDef);

		// If the user has no roles and this attribute does not have any rule for MEMBERSHIP, deny access
		if (sess.getPerunPrincipal().getRoles() == null && !roles.containsKey(Role.MEMBERSHIP)) {
			return false;
		}

		//Test if handlers are correct for attribute namespace
		//getPerunBl().getAttributesManagerBl().checkAttributeAssignment(sess, attrDef, ues);

		//Get all unique objects from the roles' action types
		Set<String> uniqueObjectTypes = fetchUniqueObjectTypes(roles);

		//Fetch all possible related objects from the member and the resource according the uniqueObjectTypes
		Map<String, Set<Integer>> mapOfObjectsToCheck = new HashMap<>();
		for (String objectType: uniqueObjectTypes) {
			Set<Integer> retrievedObjects = RelatedUserExtSourceObjectsResolver.getValue(objectType).apply(sess, ues);
			mapOfObjectsToCheck.put(objectType, retrievedObjects);
		}

		//Resolve principal's the privileges for the attribute according the rules and objects
		return resolveAttributeAuthorization(sess, roles, mapOfObjectsToCheck);
	}

	public static boolean isAuthorizedForAttribute(PerunSession sess, String actionType, AttributeDefinition attrDef, String key) throws InternalErrorException, AttributeNotExistsException {
		log.trace("Entering isAuthorizedForAttribute: sess='{}', actionType='{}', attrDef='{}', primaryHolder='{}', " +
			"secondaryHolder='{}'", sess, actionType, attrDef, key, null);

		//This method get all possible roles which can do action on attribute
		Map<String, Set<ActionType>> roles = getRolesPrivilegedToOperateOnAttribute(sess, actionType, attrDef);

		// If the user has no roles and this attribute does not have any rule for MEMBERSHIP, deny access
		if (sess.getPerunPrincipal().getRoles() == null && !roles.containsKey(Role.MEMBERSHIP)) {
			return false;
		}

		//Get all unique objects from the roles' action types
		Set<String> uniqueObjectTypes = fetchUniqueObjectTypes(roles);

		//Fetch all possible related objects from the member and the resource according the uniqueObjectTypes
		Map<String, Set<Integer>> mapOfObjectsToCheck = new HashMap<>();
		for (String objectType: uniqueObjectTypes) {
			Set<Integer> retrievedObjects = RelatedEntitylessObjectsResolver.getValue(objectType).apply(sess, key);
			mapOfObjectsToCheck.put(objectType, retrievedObjects);
		}

		//Resolve principal's the privileges for the attribute according the rules and objects
		return resolveAttributeAuthorization(sess, roles, mapOfObjectsToCheck);
	}

	/**
	 * Return map of roles, with allowed actions, which are authorized for doing "action" on "attribute".
	 *
	 * @param sess       perun session
	 * @param actionType type of action on attribute (ex.: write, read, etc...)
	 * @param attrDef    attribute what principal want to work with
	 * @return map of roles with allowed action types
	 */
	public static Map<String, Set<String>> getRolesWhichCanWorkWithAttribute(PerunSession sess, String actionType, AttributeDefinition attrDef) throws InternalErrorException, AttributeNotExistsException, ActionTypeNotExistsException {
		getPerunBl().getAttributesManagerBl().checkAttributeExists(sess, attrDef);
		getPerunBl().getAttributesManagerBl().checkActionTypeExists(sess, new ActionType(actionType, null));
		return cz.metacentrum.perun.core.impl.AuthzResolverImpl.getRolesWhichCanWorkWithAttribute(actionType, attrDef);
	}

	/**
	 * Checks if the principal is authorized.
	 *
	 * @param sess perunSession
	 * @param role required role
	 * @return true if the principal authorized, false otherwise
	 * @throws InternalErrorException if something goes wrong
	 */
	public static boolean isAuthorized(PerunSession sess, String role) throws InternalErrorException {
		return isAuthorized(sess, role, null);
	}


	/**
	 * Returns true if the perunPrincipal has requested role.
	 *
	 * @param perunPrincipal acting person for whom the role is checked
	 * @param role           role to be checked
	 */
	public static boolean hasRole(PerunPrincipal perunPrincipal, String role) {
		return perunPrincipal.getRoles().hasRole(role);
	}

	/**
	 * Set role for user and <b>one</b> complementary object.
	 * <p>
	 * If complementary object is wrong for the role, throw an exception.
	 *
	 * @param sess                perun session
	 * @param user                the user for setting role
	 * @param role                role of user in a session ( PERUNADMIN | VOADMIN | GROUPADMIN | SELF | FACILITYADMIN | VOOBSERVER | TOPGROUPCREATOR | SECURITYADMIN | RESOURCESELFSERVICE | RESOURCEADMIN )
	 * @param complementaryObject object for which role will be set
	 */
	public static void setRole(PerunSession sess, User user, PerunBean complementaryObject, String role) throws InternalErrorException, AlreadyAdminException, RoleCannotBeManagedException {
		if (!objectAndRoleManageableByEntity(user, complementaryObject, role)) {
			throw new RoleCannotBeManagedException("Combination of Role: "+ role +", Object: "+ complementaryObject +" and Entity: "+ user +" cannot be managed.");
		}

		Map<String, Integer> mappingOfValues = createMappingOfValues(user, complementaryObject, role);

		try {
			authzResolverImpl.setRole(sess, mappingOfValues);
		} catch (RoleAlreadySetException e) {
			throw new AlreadyAdminException("User id=" + user.getId() + " is already "+role+" in " + complementaryObject, e);
		}

		getPerunBl().getAuditer().log(sess, new RoleSetForUser(complementaryObject, user, role));

		if (user != null && sess.getPerunPrincipal() != null) {
			if (user.getId() == sess.getPerunPrincipal().getUserId()) {
				AuthzResolverBlImpl.refreshAuthz(sess);
			}
		}
	}

	/**
	 * Set role for authorizedGroup and <b>one</b> complementary object.
	 * <p>
	 * If complementary object is wrong for the role, throw an exception.
	 *
	 * @param sess                perun session
	 * @param authorizedGroup     the group for setting role
	 * @param role                role of user in a session ( PERUNADMIN | VOADMIN | GROUPADMIN | SELF | FACILITYADMIN | VOOBSERVER | TOPGROUPCREATOR | RESOURCESELFSERVICE | RESOURCEADMIN )
	 * @param complementaryObject object for which role will be set
	 */
	public static void setRole(PerunSession sess, Group authorizedGroup, PerunBean complementaryObject, String role) throws InternalErrorException, AlreadyAdminException, RoleCannotBeManagedException {
		if (!objectAndRoleManageableByEntity(authorizedGroup, complementaryObject, role)) {
			throw new RoleCannotBeManagedException("Combination of Role: "+ role +", Object: "+ complementaryObject +" and Entity: "+ authorizedGroup +" cannot be managed.");
		}

		Map<String, Integer> mappingOfValues = createMappingOfValues(authorizedGroup, complementaryObject, role);

		try {
			authzResolverImpl.setRole(sess, mappingOfValues);
		} catch (RoleAlreadySetException e) {
			throw new AlreadyAdminException("Group id=" + authorizedGroup.getId() + " is already "+role+" in " + complementaryObject, e);
		}

		getPerunBl().getAuditer().log(sess, new RoleSetForGroup(complementaryObject, authorizedGroup, role));

		if (authorizedGroup != null && sess.getPerunPrincipal() != null && sess.getPerunPrincipal().getUser() != null) {
			List<Member> groupMembers = perunBl.getGroupsManagerBl().getGroupMembers(sess, authorizedGroup);
			List<Member> userMembers = perunBl.getMembersManagerBl().getMembersByUser(sess, sess.getPerunPrincipal().getUser());
			userMembers.retainAll(groupMembers);
			if (!userMembers.isEmpty()) AuthzResolverBlImpl.refreshAuthz(sess);
		}
	}

	/**
	 * Unset role for user and <b>one</b> complementary object.
	 * <p>
	 * If complementary object is wrong for the role, throw an exception.
	 *
	 * @param sess                perun session
	 * @param user                the user for unsetting role
	 * @param role                role of user in a session ( PERUNADMIN | VOADMIN | GROUPADMIN | SELF | FACILITYADMIN | VOOBSERVER | TOPGROUPCREATOR | RESOURCESELFSERVICE | RESOURCEADMIN )
	 * @param complementaryObject object for which role will be unset
	 */
	public static void unsetRole(PerunSession sess, User user, PerunBean complementaryObject, String role) throws InternalErrorException, UserNotAdminException, RoleCannotBeManagedException {
		if (!objectAndRoleManageableByEntity(user, complementaryObject, role)) {
			throw new RoleCannotBeManagedException("Combination of Role: "+ role +", Object: "+ complementaryObject +" and Entity: "+ user +" cannot be managed.");
		}

		Map<String, Integer> mappingOfValues = createMappingOfValues(user, complementaryObject, role);

		try {
			authzResolverImpl.unsetRole(sess, mappingOfValues);
		} catch (RoleNotSetException e) {
			throw new UserNotAdminException("User id=" + user.getId() + " is not "+role+" in " + complementaryObject, e);
		}

		getPerunBl().getAuditer().log(sess, new RoleUnsetForUser(complementaryObject, user, role));

		if (role.equals(Role.SPONSOR) && complementaryObject.getBeanName().equals("Vo"))
			getPerunBl().getVosManagerBl().handleUserLostVoRole(sess, user, (Vo) complementaryObject, Role.SPONSOR);

		if (user != null && sess.getPerunPrincipal() != null) {
			if (user.getId() == sess.getPerunPrincipal().getUserId()) {
				AuthzResolverBlImpl.refreshAuthz(sess);
			}
		}
	}

	/**
	 * Unset role for group and <b>one</b> complementary object
	 * <p>
	 * If some complementary object is wrong for the role, throw an exception.
	 *
	 * @param sess                perun session
	 * @param authorizedGroup     the group for unsetting role
	 * @param role                role of user in a session ( PERUNADMIN | VOADMIN | GROUPADMIN | SELF | FACILITYADMIN | VOOBSERVER | TOPGROUPCREATOR | RESOURCESELFSERVICE | RESOURCEADMIN )
	 * @param complementaryObject object for which role will be unset
	 */
	public static void unsetRole(PerunSession sess, Group authorizedGroup, PerunBean complementaryObject, String role) throws InternalErrorException, GroupNotAdminException, RoleCannotBeManagedException {
		if (!objectAndRoleManageableByEntity(authorizedGroup, complementaryObject, role)) {
			throw new RoleCannotBeManagedException("Combination of Role: "+ role +", Object: "+ complementaryObject +" and Entity: "+ authorizedGroup +" cannot be managed.");
		}

		Map<String, Integer> mappingOfValues = createMappingOfValues(authorizedGroup, complementaryObject, role);

		try {
			authzResolverImpl.unsetRole(sess, mappingOfValues);
		} catch (RoleNotSetException e) {
			throw new GroupNotAdminException("Group id=" + authorizedGroup.getId() + " is not "+role+" in " + complementaryObject, e);
		}

		getPerunBl().getAuditer().log(sess, new RoleUnsetForGroup(complementaryObject, authorizedGroup, role));

		if (role.equals(Role.SPONSOR) && complementaryObject.getBeanName().equals("Vo"))
			getPerunBl().getVosManagerBl().handleGroupLostVoRole(sess, authorizedGroup, (Vo) complementaryObject, Role.SPONSOR);

		if (authorizedGroup != null && sess.getPerunPrincipal() != null && sess.getPerunPrincipal().getUser() != null) {
			List<Member> groupMembers = perunBl.getGroupsManagerBl().getGroupMembers(sess, authorizedGroup);
			List<Member> userMembers = perunBl.getMembersManagerBl().getMembersByUser(sess, sess.getPerunPrincipal().getUser());
			userMembers.retainAll(groupMembers);
			if (!userMembers.isEmpty()) AuthzResolverBlImpl.refreshAuthz(sess);
		}
	}

	public static void addSpecificUserOwner(PerunSession sess, User user, PerunBean complementaryObject) throws AlreadyAdminException {
		if (user != null && complementaryObject != null) authzResolverImpl.addAdmin(sess, (User) complementaryObject, user);
		else throw new InternalErrorException("Error");
	}

	public static void removeSpecificUserOwner(PerunSession sess, User user, PerunBean complementaryObject) throws UserNotAdminException {
		if (user != null && complementaryObject != null) authzResolverImpl.removeAdmin(sess, (User) complementaryObject, user);
		else throw new InternalErrorException("Error");
	}

	/**
	 * Make user to be PERUNADMIN!
	 *
	 * @param sess
	 * @param user which will get role "PERUNADMIN" in the system
	 * @throws InternalErrorException
	 */
	public static void makeUserPerunAdmin(PerunSession sess, User user) throws InternalErrorException {
		getPerunBl().getAuditer().log(sess, new UserPromotedToPerunAdmin(user));
		authzResolverImpl.makeUserPerunAdmin(sess, user);
	}

	public String toString() {
		return getClass().getSimpleName() + ":[]";
	}

	/**
	 * Returns true if the perun principal inside the perun session is vo admin.
	 *
	 * @param sess perun session
	 * @return true if the perun principal is vo admin
	 */
	public static boolean isVoAdmin(PerunSession sess) {
		return sess.getPerunPrincipal().getRoles().hasRole(Role.VOADMIN);
	}

	/**
	 * Returns true if the perun principal inside the perun session is group admin.
	 *
	 * @param sess perun session
	 * @return true if the perun principal is group admin.
	 */
	public static boolean isGroupAdmin(PerunSession sess) {
		return sess.getPerunPrincipal().getRoles().hasRole(Role.GROUPADMIN);
	}

	/**
	 * Returns true if the perun principal inside the perun session is facility admin.
	 *
	 * @param sess perun session
	 * @return true if the perun principal is facility admin.
	 */
	public static boolean isFacilityAdmin(PerunSession sess) {
		return sess.getPerunPrincipal().getRoles().hasRole(Role.FACILITYADMIN);
	}

	/**
	 * Returns true if the perun principal inside the perun session is resource admin.
	 *
	 * @param sess perun session
	 * @return true if the perun principal is resource admin.
	 */
	public static boolean isResourceAdmin(PerunSession sess) {
		return sess.getPerunPrincipal().getRoles().hasRole(Role.RESOURCEADMIN);
	}

	/**
	 * Returns true if the perun principal inside the perun session is security admin.
	 *
	 * @param sess perun session
	 * @return true if the perun principal is security admin.
	 */
	public static boolean isSecurityAdmin(PerunSession sess) {
		return sess.getPerunPrincipal().getRoles().hasRole(Role.SECURITYADMIN);
	}

	/**
	 * Returns true if the perun principal inside the perun session is vo observer.
	 *
	 * @param sess perun session
	 * @return true if the perun principal is vo observer
	 */
	public static boolean isVoObserver(PerunSession sess) {
		return sess.getPerunPrincipal().getRoles().hasRole(Role.VOOBSERVER);
	}

	/**
	 * Returns true if the perun principal inside the perun session is top group creator.
	 *
	 * @param sess perun session
	 * @return true if the perun principal is top group creator.
	 */
	public static boolean isTopGroupCreator(PerunSession sess) {
		return sess.getPerunPrincipal().getRoles().hasRole(Role.TOPGROUPCREATOR);
	}

	/**
	 * Returns true if the perun principal inside the perun session is perun admin.
	 *
	 * @param sess perun session
	 * @return true if the perun principal is perun admin.
	 */
	public static boolean isPerunAdmin(PerunSession sess) {
		return sess.getPerunPrincipal().getRoles().hasRole(Role.PERUNADMIN);
	}

	/**
	 * Get all principal role names.
	 *
	 * @param sess perun session
	 * @return list of roles.
	 */
	public static List<String> getPrincipalRoleNames(PerunSession sess) throws InternalErrorException {
		// We need to load the principals roles
		if (!sess.getPerunPrincipal().isAuthzInitialized()) {
			refreshAuthz(sess);
		}

		return sess.getPerunPrincipal().getRoles().getRolesNames();
	}

	/**
	 * Get all User's roles.
	 *
	 * @param sess perun session
	 * @param user User
	 * @return list of roles.
	 */
	public static List<String> getUserRoleNames(PerunSession sess,User user) throws InternalErrorException {

		return authzResolverImpl.getRoles(user).getRolesNames();
	}

	/**
	 * Get all roles for a given user.
	 *
	 * @param sess perun session
	 * @param user user
	 * @return AuthzRoles object which contains all roles with perunbeans
	 * @throws InternalErrorException
	 */
	public static AuthzRoles getUserRoles(PerunSession sess, User user) throws InternalErrorException {

		return authzResolverImpl.getRoles(user);
	}

	/**
	 * Get all Group's roles.
	 *
	 * @param sess perun session
	 * @param group Group
	 * @return list of roles.
	 */
	public static List<String> getGroupRoleNames(PerunSession sess,Group group) throws InternalErrorException {

		return authzResolverImpl.getRoles(group).getRolesNames();
	}

	/**
	 * Get all roles for a given group.
	 *
	 * @param sess perun session
	 * @param group group
	 * @return AuthzRoles object which contains all roles with perunbeans
	 * @throws InternalErrorException
	 */
	public static AuthzRoles getGroupRoles(PerunSession sess, Group group) throws InternalErrorException {

		return authzResolverImpl.getRoles(group);
	}

	/**
	 * Returns user which is associated with credentials used to log-in to Perun.
	 *
	 * @param sess perun session
	 * @return currently logged user
	 */
	public static User getLoggedUser(PerunSession sess) throws InternalErrorException {
		// We need to load additional information about the principal
		if (!sess.getPerunPrincipal().isAuthzInitialized()) {
			refreshAuthz(sess);
		}
		return sess.getPerunPrincipal().getUser();
	}

	/**
	 * Returns PerunPrincipal object associated with current session. It contains necessary information,
	 * including user identification, authorization and metadata. Each call of this method refresh the
	 * session including authorization data.
	 *
	 * @param sess perun session
	 * @return perunPrincipal object
	 * @throws InternalErrorException if the PerunSession is not valid.
	 */
	public static PerunPrincipal getPerunPrincipal(PerunSession sess) throws InternalErrorException {
		Utils.checkPerunSession(sess);

		refreshSession(sess);

		return sess.getPerunPrincipal();
	}

	/**
	 * Returns all complementary objects for defined role.
	 *
	 * @param sess perun session
	 * @param role to get object for
	 * @return list of complementary objects
	 */
	public static List<PerunBean> getComplementaryObjectsForRole(PerunSession sess, String role) throws InternalErrorException {
		return AuthzResolverBlImpl.getComplementaryObjectsForRole(sess, role, null);
	}

	/**
	 * Returns only complementary objects for defined role which fits perunBeanClass class.
	 *
	 * @param sess           perun session
	 * @param role           to get object for
	 * @param perunBeanClass particular class ( Vo | Group | ... )
	 * @return list of complementary objects
	 */
	public static List<PerunBean> getComplementaryObjectsForRole(PerunSession sess, String role, Class perunBeanClass) throws InternalErrorException {
		Utils.checkPerunSession(sess);
		Utils.notNull(sess.getPerunPrincipal(), "sess.getPerunPrincipal()");

		List<PerunBean> complementaryObjects = new ArrayList<>();
		if (sess.getPerunPrincipal().getRoles().get(role) != null) {
			for (String beanName : sess.getPerunPrincipal().getRoles().get(role).keySet()) {
				// Do we filter results on particular class?
				if (perunBeanClass == null || beanName.equals(perunBeanClass.getSimpleName())) {

					if (beanName.equals(Vo.class.getSimpleName())) {
						for (Integer beanId : sess.getPerunPrincipal().getRoles().get(role).get(beanName)) {
							try {
								complementaryObjects.add(perunBl.getVosManagerBl().getVoById(sess, beanId));
							} catch (VoNotExistsException ex) {
								//this is ok, vo was probably deleted but still exists in user session, only log it
								log.debug("Vo not find by id {} but still exists in user session when getComplementaryObjectsForRole method was called.", beanId);
							}
						}
					}

					if (beanName.equals(Group.class.getSimpleName())) {
						for (Integer beanId : sess.getPerunPrincipal().getRoles().get(role).get(beanName)) {
							try {
								complementaryObjects.add(perunBl.getGroupsManagerBl().getGroupById(sess, beanId));
							} catch (GroupNotExistsException ex) {
								//this is ok, group was probably deleted but still exists in user session, only log it
								log.debug("Group not find by id {} but still exists in user session when getComplementaryObjectsForRole method was called.", beanId);
							}
						}
					}

					if (beanName.equals(Facility.class.getSimpleName())) {
						for (Integer beanId : sess.getPerunPrincipal().getRoles().get(role).get(beanName)) {
							try {
								complementaryObjects.add(perunBl.getFacilitiesManagerBl().getFacilityById(sess, beanId));
							} catch (FacilityNotExistsException ex) {
								//this is ok, facility was probably deleted but still exists in user session, only log it
								log.debug("Facility not find by id {} but still exists in user session when getComplementaryObjectsForRole method was called.", beanId);
							}
						}
					}

					if (beanName.equals(Resource.class.getSimpleName())) {
						for (Integer beanId : sess.getPerunPrincipal().getRoles().get(role).get(beanName)) {
							try {
								complementaryObjects.add(perunBl.getResourcesManagerBl().getResourceById(sess, beanId));
							} catch (ResourceNotExistsException ex) {
								//this is ok, resource was probably deleted but still exists in user session, only log it
								log.debug("Resource not find by id {} but still exists in user session when getComplementaryObjectsForRole method was called.", beanId);
							}
						}
					}

					if (beanName.equals(Service.class.getSimpleName())) {
						for (Integer beanId : sess.getPerunPrincipal().getRoles().get(role).get(beanName)) {
							try {
								complementaryObjects.add(perunBl.getServicesManagerBl().getServiceById(sess, beanId));
							} catch (ServiceNotExistsException ex) {
								//this is ok, service was probably deleted but still exists in user session, only log it
								log.debug("Service not find by id {} but still exists in user session when getComplementaryObjectsForRole method was called.", beanId);
							}
						}
					}

					if (beanName.equals(SecurityTeam.class.getSimpleName())) {
						for (Integer beanId : sess.getPerunPrincipal().getRoles().get(role).get(beanName)) {
							try {
								complementaryObjects.add(perunBl.getSecurityTeamsManagerBl().getSecurityTeamById(sess, beanId));
							} catch (SecurityTeamNotExistsException e) {
								//this is ok, securityTeam was probably deleted but still exists in user session, only log it
								log.debug("SecurityTeam not find by id {} but still exists in user session when getComplementaryObjectsForRole method was called.", beanId);
							}
						}
					}

				}
			}
		}

		return complementaryObjects;
	}

	/**
	 * Refresh authorization data inside session.
	 * <p>
	 * Fill in proper roles and their relative entities (vos, groups, ....).
	 * User itself or ext source data is NOT updated.
	 *
	 * @param sess perun session to refresh authz for
	 */
	public static synchronized void refreshAuthz(PerunSession sess) throws InternalErrorException {
		Utils.checkPerunSession(sess);
		log.trace("Refreshing authz roles for session {}.", sess);

		//set empty set of roles
		sess.getPerunPrincipal().setRoles(new AuthzRoles());
		//Prepare service roles like engine, service, registrar, perunAdmin etc.
		prepareServiceRoles(sess);

		// if have some of the service principal, we do not need to search further
		if (sess.getPerunPrincipal().getRoles().isEmpty()) {
			User user = sess.getPerunPrincipal().getUser();
			AuthzRoles roles;
			if (user == null) {
				roles = new AuthzRoles();
			} else {
				// Load all user's roles with all possible subgroups
				roles = addAllSubgroupsToAuthzRoles(sess, authzResolverImpl.getRoles(user));
				// Add self role for the user
				roles.putAuthzRole(Role.SELF, user);
				// Add service user role
				if (user.isServiceUser()) {
					roles.putAuthzRole(Role.SERVICEUSER);
				}
			}
			sess.getPerunPrincipal().setRoles(roles);
		}

		//for OAuth clients, do not allow delegating roles not allowed by scopes
		if (sess.getPerunClient().getType() == PerunClient.Type.OAUTH) {
			List<String> oauthScopes = sess.getPerunClient().getScopes();
			log.trace("refreshAuthz({}) oauthScopes={}",sess.getLogId(),oauthScopes);
			if(!oauthScopes.contains(PerunClient.PERUN_ADMIN_SCOPE)) {
				log.debug("removing PERUNADMIN role from session of user {}",sess.getPerunPrincipal().getUserId());
				log.trace("original roles: {}", sess.getPerunPrincipal().getRoles());
				sess.getPerunPrincipal().getRoles().remove(Role.PERUNADMIN);
			}
			if(!oauthScopes.contains(PerunClient.PERUN_API_SCOPE)) {
				log.debug("removing all roles from session {}",sess);
				sess.getPerunPrincipal().getRoles().clear();
			}
		}
		log.trace("Refreshed roles: {}", sess.getPerunPrincipal().getRoles());
		sess.getPerunPrincipal().setAuthzInitialized(true);
	}

	/**
	 * Refresh all session data excluding Ext. Source and additional information.
	 * <p>
	 * This method update user in session (try to find user by ext. source data).
	 * Then it updates authorization data in session.
	 *
	 * @param sess Perun session to refresh data for
	 */
	public static synchronized void refreshSession(PerunSession sess) throws InternalErrorException {
		Utils.checkPerunSession(sess);
		log.trace("Refreshing session data for session {}.", sess);

		PerunPrincipal principal = sess.getPerunPrincipal();

		try {
			User user;
				if(extSourcesWithMultipleIdentifiers.contains(principal.getExtSourceName())) {
					UserExtSource ues = perunBl.getUsersManagerBl().getUserExtSourceFromMultipleIdentifiers(sess, principal);
					user = perunBl.getUsersManagerBl().getUserByUserExtSource(sess, ues);
				} else {
					user = perunBl.getUsersManagerBl().getUserByExtSourceNameAndExtLogin(sess, principal.getExtSourceName(), principal.getActor());
				}
			sess.getPerunPrincipal().setUser(user);
		} catch (Exception ex) {
			// we don't care that user was not found - clear it from session
			sess.getPerunPrincipal().setUser(null);
		}

		AuthzResolverBlImpl.refreshAuthz(sess);

	}

	/**
	 * For role GroupAdmin with association to "Group" add also all subgroups to authzRoles.
	 * If authzRoles is null, return empty AuthzRoles.
	 * If there is no GroupAdmin role or Group object for this role, return not changed authzRoles.
	 *
	 * @param sess       perun session
	 * @param authzRoles authzRoles for some user
	 * @return authzRoles also with subgroups of groups
	 */
	public static AuthzRoles addAllSubgroupsToAuthzRoles(PerunSession sess, AuthzRoles authzRoles) throws InternalErrorException {
		if (authzRoles == null) return new AuthzRoles();
		if (authzRoles.hasRole(Role.GROUPADMIN)) {
			Map<String, Set<Integer>> groupAdminRoles = authzRoles.get(Role.GROUPADMIN);
			Set<Integer> groupsIds = groupAdminRoles.get("Group");
			Set<Integer> newGroupsIds = new HashSet<>(groupsIds);
			for (Integer id : groupsIds) {
				Group parentGroup;
				try {
					parentGroup = getPerunBl().getGroupsManagerBl().getGroupById(sess, id);
				} catch (GroupNotExistsException ex) {
					log.debug("Group with id=" + id + " not exists when initializing rights for user: " + sess.getPerunPrincipal().getUser());
					continue;
				}
				List<Group> subGroups = getPerunBl().getGroupsManagerBl().getAllSubGroups(sess, parentGroup);
				for (Group g : subGroups) {
					newGroupsIds.add(g.getId());
				}
			}
			groupAdminRoles.put("Group", newGroupsIds);
			authzRoles.put(Role.GROUPADMIN, groupAdminRoles);
		}
		return authzRoles;
	}

	public static void removeAllAuthzForVo(PerunSession sess, Vo vo) throws InternalErrorException {
		authzResolverImpl.removeAllAuthzForVo(sess, vo);
	}

	static List<Vo> getVosForGroupInRole(PerunSession sess, Group group, String role) throws InternalErrorException {
		List<Vo> vos = new ArrayList<>();
		for (Integer voId : authzResolverImpl.getVoIdsForGroupInRole(sess, group, role)) {
			try {
				vos.add(getPerunBl().getVosManagerBl().getVoById(sess, voId));
			} catch (VoNotExistsException e) {
				log.error("vo " + voId + " not found", e);
			}
		}
		return vos;
	}

	static void removeAllUserAuthz(PerunSession sess, User user) throws InternalErrorException {
		//notify vosManager that the deleted user had SPONSOR role for some VOs
		List<Integer> sponsoredVoIds = authzResolverImpl.getVoIdsForUserInRole(sess, user, Role.SPONSOR);
		for (Integer voId : sponsoredVoIds) {
			VosManagerBl vosManagerBl = getPerunBl().getVosManagerBl();
			try {
				vosManagerBl.handleUserLostVoRole(sess, user, vosManagerBl.getVoById(sess, voId),Role.SPONSOR);
			} catch (VoNotExistsException e) {
				log.error("Vo {} has user {} in role SPONSOR, but does not exist",voId,user.getId());
			}
		}
		//remove all roles from the user
		authzResolverImpl.removeAllUserAuthz(sess, user);
	}

	static void removeAllSponsoredUserAuthz(PerunSession sess, User sponsoredUser) throws InternalErrorException {
		authzResolverImpl.removeAllSponsoredUserAuthz(sess, sponsoredUser);
	}

	public static void removeAllAuthzForGroup(PerunSession sess, Group group) throws InternalErrorException {
		//notify vosManager that the deleted group had SPONSOR role for some VOs
		for (Vo vo : getVosForGroupInRole(sess, group, Role.SPONSOR)) {
			getPerunBl().getVosManagerBl().handleGroupLostVoRole(sess, group, vo ,Role.SPONSOR);
		}
		//remove all roles from the group
		authzResolverImpl.removeAllAuthzForGroup(sess, group);
	}

	public static void removeAllAuthzForFacility(PerunSession sess, Facility facility) throws InternalErrorException {
		authzResolverImpl.removeAllAuthzForFacility(sess, facility);
	}

	public static void removeAllAuthzForResource(PerunSession sess, Resource resource) throws InternalErrorException {
		authzResolverImpl.removeAllAuthzForResource(sess, resource);
	}

	public static void removeAllAuthzForService(PerunSession sess, Service service) throws InternalErrorException {
		authzResolverImpl.removeAllAuthzForService(sess, service);
	}

	public static void removeAllAuthzForSecurityTeam(PerunSession sess, SecurityTeam securityTeam) throws InternalErrorException {
		authzResolverImpl.removeAllAuthzForSecurityTeam(sess, securityTeam);
	}

	public static void addAdmin(PerunSession sess, SecurityTeam securityTeam, User user) throws InternalErrorException, AlreadyAdminException {
		authzResolverImpl.addAdmin(sess, securityTeam, user);
	}

	public static void addAdmin(PerunSession sess, SecurityTeam securityTeam, Group group) throws InternalErrorException, AlreadyAdminException {
		authzResolverImpl.addAdmin(sess, securityTeam, group);
	}

	public static void removeAdmin(PerunSession sess, SecurityTeam securityTeam, User user) throws InternalErrorException, UserNotAdminException {
		authzResolverImpl.removeAdmin(sess, securityTeam, user);
	}

	public static void removeAdmin(PerunSession sess, SecurityTeam securityTeam, Group group) throws InternalErrorException, GroupNotAdminException {
		authzResolverImpl.removeAdmin(sess, securityTeam, group);
	}

	public static boolean roleExists(String role) {
		return authzResolverImpl.roleExists(role);
	}

	public static void loadAuthorizationComponents() { authzResolverImpl.loadAuthorizationComponents(); }

	/**
	 * Checks whether the user is in role for Vo.
	 *
	 * @param session perun session
	 * @param user user
	 * @param role role of user
	 * @param vo virtual organization
	 * @return true if user is in role for VO, false otherwise
	 */
	static boolean isUserInRoleForVo(PerunSession session, User user, String role, Vo vo) {
		return authzResolverImpl.isUserInRoleForVo(session, user, role, vo);
	}

	/**
	 * Checks whether the group is in role for Vo.
	 *
	 * @param session perun session
	 * @param group group
	 * @param role role of group
	 * @param vo virtual organization
	 * @return true if group is in role for VO, false otherwise
	 */
	static boolean isGroupInRoleForVo(PerunSession session, Group group, String role, Vo vo) {
		return authzResolverImpl.isGroupInRoleForVo(session, group, role, vo);
	}

	// Filled by Spring
	public static AuthzResolverImplApi setAuthzResolverImpl(AuthzResolverImplApi authzResolverImpl) {
		AuthzResolverBlImpl.authzResolverImpl = authzResolverImpl;
		return authzResolverImpl;
	}

	//Filled by Spring
	public static PerunBl setPerunBl(PerunBl perunBl) {
		AuthzResolverBlImpl.perunBl = perunBl;
		return perunBl;
	}

	private static PerunBl getPerunBl() {
		return perunBl;
	}

	/**
	 * Prepare service roles to session AuthzRoles (PERUNADMIN, SERVICE, RPC, ENGINE etc.)
	 *
	 * @param sess use session to add roles
	 */
	private static void prepareServiceRoles(PerunSession sess) {
		// Load list of perunAdmins from the configuration, split the list by the comma
		List<String> perunAdmins = BeansUtils.getCoreConfig().getAdmins();

		// Check if the PerunPrincipal is in a group of Perun Admins
		if (perunAdmins.contains(sess.getPerunPrincipal().getActor())) {
			sess.getPerunPrincipal().getRoles().putAuthzRole(Role.PERUNADMIN);
			sess.getPerunPrincipal().setAuthzInitialized(true);
			// We can quit, because perun admin has all privileges
			log.trace("AuthzResolver.init: Perun Admin {} loaded", sess.getPerunPrincipal().getActor());
			return;
		}

		String perunRpcAdmin = BeansUtils.getCoreConfig().getRpcPrincipal();
		if (sess.getPerunPrincipal().getActor().equals(perunRpcAdmin)) {
			sess.getPerunPrincipal().getRoles().putAuthzRole(Role.RPC);
			log.trace("AuthzResolver.init: Perun RPC {} loaded", perunRpcAdmin);
		}

		List<String> perunEngineAdmins = BeansUtils.getCoreConfig().getEnginePrincipals();
		if (perunEngineAdmins.contains(sess.getPerunPrincipal().getActor())) {
			sess.getPerunPrincipal().getRoles().putAuthzRole(Role.ENGINE);
			log.trace("AuthzResolver.init: Perun Engine {} loaded", perunEngineAdmins);
		}

		List<String> perunNotifications = BeansUtils.getCoreConfig().getNotificationPrincipals();
		if (perunNotifications.contains(sess.getPerunPrincipal().getActor())) {
			sess.getPerunPrincipal().getRoles().putAuthzRole(Role.NOTIFICATIONS);

			log.trace("AuthzResolver.init: Perun Notifications {} loaded", perunNotifications);
		}

		List<String> perunRegistrars = BeansUtils.getCoreConfig().getRegistrarPrincipals();
		if (perunRegistrars.contains(sess.getPerunPrincipal().getActor())) {
			sess.getPerunPrincipal().getRoles().putAuthzRole(Role.REGISTRAR);

			//FIXME ted pridame i roli plneho admina
			sess.getPerunPrincipal().getRoles().putAuthzRole(Role.PERUNADMIN);

			log.trace("AuthzResolver.init: Perun Registrar {} loaded", perunRegistrars);
		}
	}

	/**
	 * Decide whether a principal has sufficient rights according the the given roles and objects.
	 *
	 * @param sess perunSession which contains the principal.
	 * @param policyRoles is a list of maps where each map entry consists from a role name as a key and a role object as a value.
	 *                    Relation between each map in the list is logical OR and relation between each entry in the map is logical AND.
	 *                    Example list - (Map1, Map2...)
	 *                    Example map - key: VOADMIN ; value: Vo
	 *                                 key: GROUPADMIN ; value: Group
	 * @param mapOfBeans is a map of objects against which will be authorization done.
	 *                    Example map entry - key: Member ; values: (10,15,26)
	 * @return true if the principal has particular rights, false otherwise.
	 */
	private static boolean resolveAuthorization(PerunSession sess, List<Map<String, String>> policyRoles, Map <String, Set<Integer>> mapOfBeans) {
		//Traverse through outer role list which works like logical OR
		for (Map<String, String> roleArray: policyRoles) {

			boolean authorized = true;
			//Traverse through inner role list which works like logical AND
			Set<String> roleArrayKeys = roleArray.keySet();
			for (String role : roleArrayKeys) {

				//fetch the object which is connected with the role
				String roleObject = roleArray.get(role);

				// If policy role is not connected to any object
				if (roleObject == null) {
					//If principal does not have the role, this inner list's result is false
					if (!sess.getPerunPrincipal().getRoles().hasRole(role)) authorized = false;
				//If there is no corresponding type of object in the perunBeans map
				} else if (!mapOfBeans.containsKey(roleObject)) {
					authorized = false;
				// If policy role is connected to some object, like VOADMIN->Vo
				} else {
					//traverse all related objects from perun which are relevant for the authorized method
					for (Integer objectId : mapOfBeans.get(roleObject)) {
						//If the principal does not have rights on role-object, this inner list's result is false
						if (!sess.getPerunPrincipal().getRoles().hasRole(role, roleObject, objectId)) {
							authorized = false;
							break;
						}
					}
				}
				//Some inner role check failed so jump out of the while loop
				if (!authorized) break;
			}
			// If all checks for inner role list pass, return true. Otherwise proceed to another inner role list
			if (authorized) return true;
		}
		//If no check passed, return false. The principal doesn't have sufficient rights.
		return false;
	}

	/**
	 * Fetch all possible PerunBeans for each of the objects from the list according to the id of the bean in the object.
	 *
	 * @param objects for which will be related objects fetched.
	 * @return all related objects together with the objects from the input as a map of PerunBean names and ids.
	 */
	private static Map<String, Set<Integer>> fetchAllRelatedObjects(List<PerunBean> objects) {
		List<PerunBean> relatedObjects = new ArrayList<>();
		//Create a map from objects for easier manipulation and duplicity prevention
		Map<String, Set<Integer>> mapOfBeans = new HashMap<>();

		for (PerunBean object: objects) {
			relatedObjects.add(object);
			List<PerunBean> retrievedObjects = RelatedObjectsResolver.getValue(object.getBeanName()).apply(object);
			relatedObjects.addAll(retrievedObjects);
		}

		//Fill map with PerunBean names as keys and a set of unique ids as value for each bean name
		for (PerunBean object : relatedObjects) {
			if (!mapOfBeans.containsKey(object.getBeanName())) mapOfBeans.put(object.getBeanName(), new HashSet<>());
			mapOfBeans.get(object.getBeanName()).add(object.getId());
		}

		return mapOfBeans;
	}

	/**
	 * Enum defines PerunBean's name and action. The action retrieves all related objects for the object with that name.
	 */
	private enum RelatedObjectsResolver implements Function<PerunBean, List<PerunBean>> {
		Member((object) -> {
			User user = new User();
			user.setId(((Member) object).getUserId());
			Vo vo = new Vo();
			vo.setId(((Member) object).getVoId());
			return Arrays.asList(user,vo);
		}),
		Group((object) -> {
			Vo vo = new Vo();
			vo.setId(((Group) object).getVoId());
			return Collections.singletonList(vo);
		}),
		Resource((object) -> {
			Vo vo = new Vo();
			vo.setId(((Resource) object).getVoId());
			Facility facility = new Facility();
			facility.setId(((Resource) object).getFacilityId());
			return Arrays.asList(vo, facility);
		}),
		ResourceTag((object) -> {
			Vo vo = new Vo();
			vo.setId(((ResourceTag) object).getVoId());
			return Collections.singletonList(vo);
		}),
		RichMember((object) -> {
			User user = new User();
			user.setId(((RichMember) object).getUserId());
			Vo vo = new Vo();
			vo.setId(((Member) object).getVoId());
			return Arrays.asList(user,vo);
		}),
		RichGroup((object) -> {
			Vo vo = new Vo();
			vo.setId(((RichGroup) object).getVoId());
			return Collections.singletonList(vo);
		}),
		RichResource((object) -> {
			Vo vo = new Vo();
			vo.setId(((RichResource) object).getVoId());
			Facility facility = new Facility();
			facility.setId(((Resource) object).getFacilityId());
			return Arrays.asList(vo, facility);
		}),
		Default((object) -> {
			return Collections.emptyList();
		});

		private Function<PerunBean, List<PerunBean>> function;

		RelatedObjectsResolver(final Function<PerunBean, List<PerunBean>> function) {
			this.function = function;
		}

		/**
		 * Get RelatedObjectsResolver value by the given name or default value if the name does not exist.
		 *
		 * @param name of the value which will be retrieved if exists.
		 * @return RelatedObjectsResolver value.
		 */
		public static RelatedObjectsResolver getValue(String name) {
			try {
				return RelatedObjectsResolver.valueOf(name);
			} catch (IllegalArgumentException ex) {
				return RelatedObjectsResolver.Default;
			}
		}

		@Override
		public List<PerunBean> apply(PerunBean object) {
			return function.apply(object);
		}
	}

	//PRIVATE METHODS FOR ATTRIBUTE AUTHORIZATION//

	private static Map<String, Set<ActionType>> getRolesPrivilegedToOperateOnAttribute(PerunSession sess, String actionType, AttributeDefinition attrDef) throws AttributeNotExistsException {
		Utils.notNull(sess, "sess");
		Utils.notNull(actionType, "ActionType");
		Utils.notNull(attrDef, "AttributeDefinition");
		getPerunBl().getAttributesManagerBl().checkAttributeExists(sess, attrDef);

		// We need to load additional information about the principal
		if (!sess.getPerunPrincipal().isAuthzInitialized()) {
			refreshAuthz(sess);
		}

		return AuthzResolverImpl.getRolesPrivilegedToOperateOnAttribute(actionType, attrDef);
	}


	private static boolean resolveAttributeAuthorization(PerunSession sess, Map<String, Set<ActionType>> roles, Map<String, Set<Integer>> mapOfObjectsToCheck) {
		for (String role : roles.keySet()) {
			Set<ActionType> roleActionTypes = roles.get(role);
			for (ActionType roleActionType : roleActionTypes) {
				if (roleActionType.getActionTypeObject() == null) {
					if (sess.getPerunPrincipal().getRoles().hasRole(role)) return true;
				} else {
					String objectType = roleActionType.getActionTypeObject();
					if (mapOfObjectsToCheck.containsKey(objectType)) {
						Set<Integer> objectsToCheck = mapOfObjectsToCheck.get(objectType);
						for (Integer objectId : objectsToCheck) {
							if (sess.getPerunPrincipal().getRoles().hasRole(role, objectType, objectId))
								return true;
						}
					}
				}
			}
		}

		return resolveMembershipPrivileges(sess, roles, mapOfObjectsToCheck);
	}

	private static boolean resolveMembershipPrivileges(PerunSession sess, Map<String, Set<ActionType>> roles, Map<String, Set<Integer>> mapOfObjectsToCheck) {
		if (roles.containsKey(Role.MEMBERSHIP)) {
			Set<ActionType> membershipActions = roles.get(Role.MEMBERSHIP);

			for (ActionType membershipAction : membershipActions) {
				String object = membershipAction.getActionTypeObject();
				if (object == null) return true;

				if (MembershipPrivilegesResolver.getValue(object).apply(sess, mapOfObjectsToCheck.get(object)))
					return true;
			}
		}

		return false;
	}

	private static Set<String> fetchUniqueObjectTypes(Map<String, Set<ActionType>> roles) {
		Set<String> uniqueObjectTypes = new HashSet<>();

		for (String roleName: roles.keySet()) {
			Set<ActionType> roleActionTypes = roles.get(roleName);
			roleActionTypes.forEach(actionType -> {
				if (actionType.getActionTypeObject() != null)
					uniqueObjectTypes.add(actionType.getActionTypeObject());
			});
		}

		return uniqueObjectTypes;
	}

	/**
	 * Functional interface defining action for member-resource related objects
	 */
	@FunctionalInterface
	private interface MemberResourceRelatedObjectAction<TA extends PerunSession, TS extends Member, TM extends Resource, TV extends Set<Integer>> {
		TV callOn(TA session, TS member, TM resource) throws InternalErrorException;
	}

	private enum RelatedMemberResourceObjectsResolver implements MemberResourceRelatedObjectAction<PerunSession, Member, Resource, Set<Integer>> {
		Vo((sess, member, resource) -> {
			return Collections.singleton(member.getVoId());
		}),
		Facility((sess, member, resource) -> {
			return Collections.singleton(resource.getFacilityId());
		}),
		User((sess, member, resource) -> {
			return Collections.singleton(member.getUserId());
		}),
		Group((sess, member, resource) -> {
			List<Group> groups = getPerunBl().getGroupsManagerBl().getGroupsByPerunBean(sess, resource);
			Set<Integer> ids = new HashSet<>();
			groups.forEach(group -> ids.add(group.getId()));
			return ids;
		}),
		Member((sess, member, resource) -> {
			return Collections.singleton(member.getId());
		}),
		Resource((sess, member, resource) -> {
			return Collections.singleton(resource.getId());
		}),
		Default((sess, member, resource) -> {
			return Collections.emptySet();
		});

		private MemberResourceRelatedObjectAction<PerunSession, Member, Resource, Set<Integer>> function;

		RelatedMemberResourceObjectsResolver(final MemberResourceRelatedObjectAction<PerunSession, Member, Resource, Set<Integer>> function) {
			this.function = function;
		}

		/**
		 * Get RelatedObjectsResolver value by the given name or default value if the name does not exist.
		 *
		 * @param name of the value which will be retrieved if exists.
		 * @return RelatedObjectsResolver value.
		 */
		public static RelatedMemberResourceObjectsResolver getValue(String name) {
			try {
				return RelatedMemberResourceObjectsResolver.valueOf(name);
			} catch (IllegalArgumentException ex) {
				return RelatedMemberResourceObjectsResolver.Default;
			}
		}

		@Override
		public Set<Integer> callOn(PerunSession sess, Member member, Resource resource) {
			return function.callOn(sess, member, resource);
		}
	}

	/**
	 * Functional interface defining action for group-resource related objects
	 */
	@FunctionalInterface
	private interface GroupResourceRelatedObjectAction<TA extends PerunSession, TS extends Group, TM extends Resource, TV extends Set<Integer>> {
		TV callOn(TA session, TS group, TM resource) throws InternalErrorException;
	}

	private enum RelatedGroupResourceObjectsResolver implements GroupResourceRelatedObjectAction<PerunSession, Group, Resource, Set<Integer>> {
		Vo((sess, group, resource) -> {
			return Collections.singleton(resource.getVoId());
		}),
		Facility((sess, group, resource) -> {
			return Collections.singleton(resource.getFacilityId());
		}),
		User((sess, group, resource) -> {
			List<User> users = perunBl.getUsersManagerBl().getUsersByPerunBean(sess, group);
			Set<Integer> userIds = new HashSet<>();
			users.forEach(user -> userIds.add(user.getId()));
			return userIds;
		}),
		Group((sess, group, resource) -> {
			return Collections.singleton(group.getId());
		}),
		Member((sess, group, resource) -> {
			List<Member> members = perunBl.getGroupsManagerBl().getGroupMembersExceptInvalid(sess, group);
			Set<Integer> memberIds = new HashSet<>();
			members.forEach(member -> memberIds.add(member.getId()));
			return memberIds;
		}),
		Resource((sess, group, resource) -> {
			return Collections.singleton(resource.getId());
		}),
		Default((sess, group, resource) -> {
			return Collections.emptySet();
		});

		private GroupResourceRelatedObjectAction<PerunSession, Group, Resource, Set<Integer>> function;

		RelatedGroupResourceObjectsResolver(final GroupResourceRelatedObjectAction<PerunSession, Group, Resource, Set<Integer>> function) {
			this.function = function;
		}

		/**
		 * Get RelatedObjectsResolver value by the given name or default value if the name does not exist.
		 *
		 * @param name of the value which will be retrieved if exists.
		 * @return RelatedObjectsResolver value.
		 */
		public static RelatedGroupResourceObjectsResolver getValue(String name) {
			try {
				return RelatedGroupResourceObjectsResolver.valueOf(name);
			} catch (IllegalArgumentException ex) {
				return RelatedGroupResourceObjectsResolver.Default;
			}
		}

		@Override
		public Set<Integer> callOn(PerunSession sess, Group group, Resource resource) {
			return function.callOn(sess, group, resource);
		}
	}

	/**
	 * Functional interface defining action for user-facility related objects
	 */
	@FunctionalInterface
	private interface UserFacilityRelatedObjectAction<TA extends PerunSession, TS extends User, TM extends Facility, TV extends Set<Integer>> {
		TV callOn(TA session, TS user, TM facility) throws InternalErrorException;
	}

	private enum RelatedUserFacilityObjectsResolver implements UserFacilityRelatedObjectAction<PerunSession, User, Facility, Set<Integer>> {
		Vo((sess, user, facility) -> {
			List<Member> membersFromUser = getPerunBl().getMembersManagerBl().getMembersByUser(sess, user);
			HashSet<Resource> resourcesFromUser = new HashSet<>();
			for (Member memberElement : membersFromUser) {
				resourcesFromUser.addAll(getPerunBl().getResourcesManagerBl().getAssignedResources(sess, memberElement));
			}
			resourcesFromUser.retainAll(getPerunBl().getFacilitiesManagerBl().getAssignedResources(sess, facility));
			Set<Integer> voIds = new HashSet<>();
			resourcesFromUser.forEach(resource -> voIds.add(resource.getVoId()));
			return voIds;
		}),
		Facility((sess, user, facility) -> {
			return Collections.singleton(facility.getId());
		}),
		User((sess, user, facility) -> {
			return Collections.singleton(user.getId());
		}),
		Group((sess, user, facility) -> {
			List<Group> userGroups = getPerunBl().getGroupsManagerBl().getGroupsByPerunBean(sess, user);
			List<Group> facilityGroups = getPerunBl().getGroupsManagerBl().getGroupsByPerunBean(sess, facility);
			userGroups.retainAll(facilityGroups);
			Set<Integer> groupIds = new HashSet<>();
			userGroups.forEach(group -> groupIds.add(group.getId()));
			return groupIds;
		}),
		Member((sess, user, facility) -> {
			List<Member> membersFromUser = getPerunBl().getMembersManagerBl().getMembersByUser(sess, user);
			Set<Integer> memberIds = new HashSet<>();
			membersFromUser.forEach(member -> memberIds.add(member.getId()));
			return memberIds;
		}),
		Resource((sess, user, facility) -> {
			List<Member> membersFromUser = getPerunBl().getMembersManagerBl().getMembersByUser(sess, user);
			HashSet<Resource> resourcesFromUser = new HashSet<>();
			for (Member memberElement : membersFromUser) {
				resourcesFromUser.addAll(getPerunBl().getResourcesManagerBl().getAssignedResources(sess, memberElement));
			}
			resourcesFromUser.retainAll(getPerunBl().getFacilitiesManagerBl().getAssignedResources(sess, facility));
			Set<Integer> resourceIds = new HashSet<>();
			resourcesFromUser.forEach(resource -> resourceIds.add(resource.getId()));
			return resourceIds;
		}),
		Default((sess, user, facility) -> {
			return Collections.emptySet();
		});

		private UserFacilityRelatedObjectAction<PerunSession, User, Facility, Set<Integer>> function;

		RelatedUserFacilityObjectsResolver(final UserFacilityRelatedObjectAction<PerunSession, User, Facility, Set<Integer>> function) {
			this.function = function;
		}

		/**
		 * Get RelatedObjectsResolver value by the given name or default value if the name does not exist.
		 *
		 * @param name of the value which will be retrieved if exists.
		 * @return RelatedObjectsResolver value.
		 */
		public static RelatedUserFacilityObjectsResolver getValue(String name) {
			try {
				return RelatedUserFacilityObjectsResolver.valueOf(name);
			} catch (IllegalArgumentException ex) {
				return RelatedUserFacilityObjectsResolver.Default;
			}
		}

		@Override
		public Set<Integer> callOn(PerunSession sess, User user, Facility facility) {
			return function.callOn(sess, user, facility);
		}
	}

	/**
	 * Functional interface defining action for member-group related objects
	 */
	@FunctionalInterface
	private interface MemberGroupRelatedObjectAction<TA extends PerunSession, TS extends Member, TM extends Group, TV extends Set<Integer>> {
		TV callOn(TA session, TS member, TM group) throws InternalErrorException;
	}

	private enum RelatedMemberGroupObjectsResolver implements MemberGroupRelatedObjectAction<PerunSession, Member, Group, Set<Integer>> {
		Vo((sess, member, group) -> {
			return Collections.singleton(member.getVoId());
		}),
		Facility((sess, member, group) -> {
			List<Resource> memberResources = getPerunBl().getResourcesManagerBl().getAssignedResources(sess, member);
			List<Resource> groupResources = getPerunBl().getResourcesManagerBl().getAssignedResources(sess, group);
			memberResources.retainAll(groupResources);
			Set<Integer> facilityIds = new HashSet<>();
			memberResources.forEach(resource -> facilityIds.add(resource.getFacilityId()));
			return facilityIds;
		}),
		User((sess, member, group) -> {
			return Collections.singleton(member.getUserId());
		}),
		Group((sess, member, group) -> {
			return Collections.singleton(group.getId());
		}),
		Member((sess, member, group) -> {
			return Collections.singleton(member.getId());
		}),
		Resource((sess, member, group) -> {
			List<Resource> memberResources = getPerunBl().getResourcesManagerBl().getAssignedResources(sess, member);
			List<Resource> groupResources = getPerunBl().getResourcesManagerBl().getAssignedResources(sess, group);
			memberResources.retainAll(groupResources);
			Set<Integer> resourceIds = new HashSet<>();
			memberResources.forEach(resource -> resourceIds.add(resource.getId()));
			return resourceIds;
		}),
		Default((sess, member, group) -> {
			return Collections.emptySet();
		});

		private MemberGroupRelatedObjectAction<PerunSession, Member, Group, Set<Integer>> function;

		RelatedMemberGroupObjectsResolver(final MemberGroupRelatedObjectAction<PerunSession, Member, Group, Set<Integer>> function) {
			this.function = function;
		}

		/**
		 * Get RelatedMemberGroupObjectsResolver value by the given name or default value if the name does not exist.
		 *
		 * @param name of the value which will be retrieved if exists.
		 * @return RelatedMemberGroupObjectsResolver value.
		 */
		public static RelatedMemberGroupObjectsResolver getValue(String name) {
			try {
				return RelatedMemberGroupObjectsResolver.valueOf(name);
			} catch (IllegalArgumentException ex) {
				return RelatedMemberGroupObjectsResolver.Default;
			}
		}

		@Override
		public Set<Integer> callOn(PerunSession sess, Member member, Group group) {
			return function.callOn(sess, member, group);
		}
	}

	private enum RelatedUserObjectsResolver implements BiFunction<PerunSession, User, Set<Integer>> {
		Vo((sess, user) -> {
			List<Vo> vosFromUser = getPerunBl().getUsersManagerBl().getVosWhereUserIsMember(sess, user);
			Set<Integer> voIds = new HashSet<>();
			vosFromUser.forEach(vo -> voIds.add(vo.getId()));
			return voIds;
		}),
		Facility((sess, user) -> {
			List<Facility> userFacilities = getPerunBl().getFacilitiesManagerBl().getFacilitiesByPerunBean(sess, user);
			Set<Integer> facilityIds = new HashSet<>();
			userFacilities.forEach(facility -> facilityIds.add(facility.getId()));
			return facilityIds;
		}),
		User((sess, user) -> {
			return Collections.singleton(user.getId());
		}),
		Group((sess, user) -> {
			List<Group> userGroups = getPerunBl().getGroupsManagerBl().getGroupsByPerunBean(sess, user);
			Set<Integer> groupIds = new HashSet<>();
			userGroups.forEach(group -> groupIds.add(group.getId()));
			return groupIds;
		}),
		Member((sess, user) -> {
			List<Member> userMembers = getPerunBl().getMembersManagerBl().getMembersByUser(sess, user);
			Set<Integer> memberIds = new HashSet<>();
			userMembers.forEach(member -> memberIds.add(member.getId()));
			return memberIds;
		}),
		Resource((sess, user) -> {
			List<Resource> userResources = getPerunBl().getUsersManagerBl().getAssignedResources(sess, user);
			Set<Integer> resourceIds = new HashSet<>();
			userResources.forEach(resource -> resourceIds.add(resource.getId()));
			return resourceIds;
		}),
		Default((sess, user) -> {
			return Collections.emptySet();
		});

		private BiFunction<PerunSession, User, Set<Integer>> function;

		RelatedUserObjectsResolver(final BiFunction<PerunSession, User, Set<Integer>> function) {
			this.function = function;
		}

		/**
		 * Get RelatedUserObjectsResolver value by the given name or default value if the name does not exist.
		 *
		 * @param name of the value which will be retrieved if exists.
		 * @return RelatedObjectsResolver value.
		 */
		public static RelatedUserObjectsResolver getValue(String name) {
			try {
				return RelatedUserObjectsResolver.valueOf(name);
			} catch (IllegalArgumentException ex) {
				return RelatedUserObjectsResolver.Default;
			}
		}

		@Override
		public Set<Integer> apply(PerunSession sess, User user) {
			return function.apply(sess, user);
		}
	}

	private enum RelatedMemberObjectsResolver implements BiFunction<PerunSession, Member, Set<Integer>> {
		Vo((sess, member) -> {
			List<Vo> vosFromMember = getPerunBl().getVosManagerBl().getVosByPerunBean(sess, member);
			Set<Integer> voIds = new HashSet<>();
			vosFromMember.forEach(vo -> voIds.add(vo.getId()));
			return voIds;
		}),
		Facility((sess, member) -> {
			List<Facility> memberFacilities = getPerunBl().getFacilitiesManagerBl().getFacilitiesByPerunBean(sess, member);
			Set<Integer> facilityIds = new HashSet<>();
			memberFacilities.forEach(facility -> facilityIds.add(facility.getId()));
			return facilityIds;
		}),
		User((sess, member) -> {
			return Collections.singleton(member.getUserId());
		}),
		Group((sess, member) -> {
			List<Group> memberGroups = getPerunBl().getGroupsManagerBl().getGroupsByPerunBean(sess, member);
			Set<Integer> groupIds = new HashSet<>();
			memberGroups.forEach(group -> groupIds.add(group.getId()));
			return groupIds;
		}),
		Member((sess, member) -> {
			return Collections.singleton(member.getId());
		}),
		Resource((sess, member) -> {
			List<Resource> memberResources = getPerunBl().getResourcesManagerBl().getAssignedResources(sess, member);
			Set<Integer> resourceIds = new HashSet<>();
			memberResources.forEach(resource -> resourceIds.add(resource.getId()));
			return resourceIds;
		}),
		Default((sess, member) -> {
			return Collections.emptySet();
		});

		private BiFunction<PerunSession, Member, Set<Integer>> function;

		RelatedMemberObjectsResolver(final BiFunction<PerunSession, Member, Set<Integer>> function) {
			this.function = function;
		}

		/**
		 * Get RelatedUserObjectsResolver value by the given name or default value if the name does not exist.
		 *
		 * @param name of the value which will be retrieved if exists.
		 * @return RelatedObjectsResolver value.
		 */
		public static RelatedMemberObjectsResolver getValue(String name) {
			try {
				return RelatedMemberObjectsResolver.valueOf(name);
			} catch (IllegalArgumentException ex) {
				return RelatedMemberObjectsResolver.Default;
			}
		}

		@Override
		public Set<Integer> apply(PerunSession sess, Member member) {
			return function.apply(sess, member);
		}
	}

	private enum RelatedVoObjectsResolver implements BiFunction<PerunSession, Vo, Set<Integer>> {
		Vo((sess, vo) -> {
			return Collections.singleton(vo.getId());
		}),
		Facility((sess, vo) -> {
			List<Facility> voFacilities = getPerunBl().getFacilitiesManagerBl().getFacilitiesByPerunBean(sess, vo);
			Set<Integer> facilityIds = new HashSet<>();
			voFacilities.forEach(facility -> facilityIds.add(facility.getId()));
			return facilityIds;
		}),
		User((sess, vo) -> {
			List<User> voUsers = getPerunBl().getUsersManagerBl().getUsersByPerunBean(sess, vo);
			Set<Integer> userIds = new HashSet<>();
			voUsers.forEach(user -> userIds.add(user.getId()));
			return userIds;
		}),
		Group((sess, vo) -> {
			List<Group> memberGroups = getPerunBl().getGroupsManagerBl().getGroups(sess, vo);
			Set<Integer> groupIds = new HashSet<>();
			memberGroups.forEach(group -> groupIds.add(group.getId()));
			return groupIds;
		}),
		Member((sess, vo) -> {
			List<Member> voMembers = getPerunBl().getMembersManagerBl().getMembers(sess, vo);
			Set<Integer> memberIds = new HashSet<>();
			voMembers.forEach(member -> memberIds.add(member.getId()));
			return memberIds;
		}),
		Resource((sess, vo) -> {
			List<Resource> voResources = getPerunBl().getResourcesManagerBl().getResources(sess, vo);
			Set<Integer> resourceIds = new HashSet<>();
			voResources.forEach(resource -> resourceIds.add(resource.getId()));
			return resourceIds;
		}),
		Default((sess, vo) -> {
			return Collections.emptySet();
		});

		private BiFunction<PerunSession, Vo, Set<Integer>> function;

		RelatedVoObjectsResolver(final BiFunction<PerunSession, Vo, Set<Integer>> function) {
			this.function = function;
		}

		/**
		 * Get RelatedVoObjectsResolver value by the given name or default value if the name does not exist.
		 *
		 * @param name of the value which will be retrieved if exists.
		 * @return RelatedObjectsResolver value.
		 */
		public static RelatedVoObjectsResolver getValue(String name) {
			try {
				return RelatedVoObjectsResolver.valueOf(name);
			} catch (IllegalArgumentException ex) {
				return RelatedVoObjectsResolver.Default;
			}
		}

		@Override
		public Set<Integer> apply(PerunSession sess, Vo vo) {
			return function.apply(sess, vo);
		}
	}

	private enum RelatedGroupObjectsResolver implements BiFunction<PerunSession, Group, Set<Integer>> {
		Vo((sess, group) -> {
			return Collections.singleton(group.getVoId());
		}),
		Facility((sess, group) -> {
			List<Facility> groupFacilities = getPerunBl().getFacilitiesManagerBl().getFacilitiesByPerunBean(sess, group);
			Set<Integer> facilityIds = new HashSet<>();
			groupFacilities.forEach(facility -> facilityIds.add(facility.getId()));
			return facilityIds;
		}),
		User((sess, group) -> {
			List<User> groupUsers = getPerunBl().getUsersManagerBl().getUsersByPerunBean(sess, group);
			Set<Integer> userIds = new HashSet<>();
			groupUsers.forEach(user -> userIds.add(user.getId()));
			return userIds;
		}),
		Group((sess, group) -> {
			return Collections.singleton(group.getId());
		}),
		Member((sess, group) -> {
			List<Member> groupMembers = getPerunBl().getGroupsManagerBl().getGroupMembers(sess, group);
			Set<Integer> memberIds = new HashSet<>();
			groupMembers.forEach(member -> memberIds.add(member.getId()));
			return memberIds;
		}),
		Resource((sess, group) -> {
			List<Resource> groupResources = getPerunBl().getResourcesManagerBl().getAssignedResources(sess, group);
			Set<Integer> resourceIds = new HashSet<>();
			groupResources.forEach(resource -> resourceIds.add(resource.getId()));
			return resourceIds;
		}),
		Default((sess, group) -> {
			return Collections.emptySet();
		});

		private BiFunction<PerunSession, Group, Set<Integer>> function;

		RelatedGroupObjectsResolver(final BiFunction<PerunSession, Group, Set<Integer>> function) {
			this.function = function;
		}

		/**
		 * Get RelatedGroupObjectsResolver value by the given name or default value if the name does not exist.
		 *
		 * @param name of the value which will be retrieved if exists.
		 * @return RelatedGroupObjectsResolver value.
		 */
		public static RelatedGroupObjectsResolver getValue(String name) {
			try {
				return RelatedGroupObjectsResolver.valueOf(name);
			} catch (IllegalArgumentException ex) {
				return RelatedGroupObjectsResolver.Default;
			}
		}

		@Override
		public Set<Integer> apply(PerunSession sess, Group group) {
			return function.apply(sess, group);
		}
	}

	private enum RelatedResourceObjectsResolver implements BiFunction<PerunSession, Resource, Set<Integer>> {
		Vo((sess, resource) -> {
			return Collections.singleton(resource.getVoId());
		}),
		Facility((sess, resource) -> {
			return Collections.singleton(resource.getFacilityId());
		}),
		User((sess, resource) -> {
			List<User> resourceUsers = getPerunBl().getUsersManagerBl().getUsersByPerunBean(sess, resource);
			Set<Integer> userIds = new HashSet<>();
			resourceUsers.forEach(user -> userIds.add(user.getId()));
			return userIds;
		}),
		Group((sess, resource) -> {
			List<Group> resourceGroups = getPerunBl().getGroupsManagerBl().getGroupsByPerunBean(sess, resource);
			Set<Integer> groupIds = new HashSet<>();
			resourceGroups.forEach(group -> groupIds.add(group.getId()));
			return groupIds;
		}),
		Member((sess, resource) -> {
			List<Member> resourceMembers = getPerunBl().getResourcesManagerBl().getAssignedMembers(sess, resource);
			Set<Integer> memberIds = new HashSet<>();
			resourceMembers.forEach(member -> memberIds.add(member.getId()));
			return memberIds;
		}),
		Resource((sess, resource) -> {
			return Collections.singleton(resource.getId());
		}),
		Default((sess, resource) -> {
			return Collections.emptySet();
		});

		private BiFunction<PerunSession, Resource, Set<Integer>> function;

		RelatedResourceObjectsResolver(final BiFunction<PerunSession, Resource, Set<Integer>> function) {
			this.function = function;
		}

		/**
		 * Get RelatedResourceObjectsResolver value by the given name or default value if the name does not exist.
		 *
		 * @param name of the value which will be retrieved if exists.
		 * @return RelatedResourceObjectsResolver value.
		 */
		public static RelatedResourceObjectsResolver getValue(String name) {
			try {
				return RelatedResourceObjectsResolver.valueOf(name);
			} catch (IllegalArgumentException ex) {
				return RelatedResourceObjectsResolver.Default;
			}
		}

		@Override
		public Set<Integer> apply(PerunSession sess, Resource resource) {
			return function.apply(sess, resource);
		}
	}

	private enum RelatedFacilityObjectsResolver implements BiFunction<PerunSession, Facility, Set<Integer>> {
		Vo((sess, facility) -> {
			List<Vo> vosFromMember = getPerunBl().getVosManagerBl().getVosByPerunBean(sess, facility);
			Set<Integer> voIds = new HashSet<>();
			vosFromMember.forEach(vo -> voIds.add(vo.getId()));
			return voIds;
		}),
		Facility((sess, facility) -> {
			return Collections.singleton(facility.getId());
		}),
		User((sess, facility) -> {
			List<User> resourceUsers = getPerunBl().getUsersManagerBl().getUsersByPerunBean(sess, facility);
			Set<Integer> userIds = new HashSet<>();
			resourceUsers.forEach(user -> userIds.add(user.getId()));
			return userIds;
		}),
		Group((sess, facility) -> {
			List<Group> resourceGroups = getPerunBl().getGroupsManagerBl().getGroupsByPerunBean(sess, facility);
			Set<Integer> groupIds = new HashSet<>();
			resourceGroups.forEach(group -> groupIds.add(group.getId()));
			return groupIds;
		}),
		Member((sess, facility) -> {
			List<Resource> facilityResources = getPerunBl().getFacilitiesManagerBl().getAssignedResources(sess, facility);
			List<Member> resourceMembers = new ArrayList<>();
			facilityResources.forEach(resource -> resourceMembers.addAll(getPerunBl().getResourcesManagerBl().getAssignedMembers(sess, resource)));
			Set<Integer> memberIds = new HashSet<>();
			resourceMembers.forEach(member -> memberIds.add(member.getId()));
			return memberIds;
		}),
		Resource((sess, facility) -> {
			List<Resource> facilityResources = getPerunBl().getFacilitiesManagerBl().getAssignedResources(sess, facility);
			Set<Integer> resourceIds = new HashSet<>();
			facilityResources.forEach(resource -> resourceIds.add(resource.getId()));
			return resourceIds;
		}),
		Default((sess, facility) -> {
			return Collections.emptySet();
		});

		private BiFunction<PerunSession, Facility, Set<Integer>> function;

		RelatedFacilityObjectsResolver(final BiFunction<PerunSession, Facility, Set<Integer>> function) {
			this.function = function;
		}

		/**
		 * Get RelatedFacilityObjectsResolver value by the given name or default value if the name does not exist.
		 *
		 * @param name of the value which will be retrieved if exists.
		 * @return RelatedFacilityObjectsResolver value.
		 */
		public static RelatedFacilityObjectsResolver getValue(String name) {
			try {
				return RelatedFacilityObjectsResolver.valueOf(name);
			} catch (IllegalArgumentException ex) {
				return RelatedFacilityObjectsResolver.Default;
			}
		}

		@Override
		public Set<Integer> apply(PerunSession sess, Facility facility) {
			return function.apply(sess, facility);
		}
	}

	private enum RelatedHostObjectsResolver implements BiFunction<PerunSession, Host, Set<Integer>> {
		Vo((sess, host) -> {
			Facility facility = getPerunBl().getFacilitiesManagerBl().getFacilityForHost(sess, host);
			List<Vo> vosFromMember = getPerunBl().getVosManagerBl().getVosByPerunBean(sess, facility);
			Set<Integer> voIds = new HashSet<>();
			vosFromMember.forEach(vo -> voIds.add(vo.getId()));
			return voIds;
		}),
		Facility((sess, host) -> {
			Facility facility = getPerunBl().getFacilitiesManagerBl().getFacilityForHost(sess, host);
			return Collections.singleton(facility.getId());
		}),
		User((sess, host) -> {
			Facility facility = getPerunBl().getFacilitiesManagerBl().getFacilityForHost(sess, host);
			List<User> resourceUsers = getPerunBl().getUsersManagerBl().getUsersByPerunBean(sess, facility);
			Set<Integer> userIds = new HashSet<>();
			resourceUsers.forEach(user -> userIds.add(user.getId()));
			return userIds;
		}),
		Group((sess, host) -> {
			Facility facility = getPerunBl().getFacilitiesManagerBl().getFacilityForHost(sess, host);
			List<Group> resourceGroups = getPerunBl().getGroupsManagerBl().getGroupsByPerunBean(sess, facility);
			Set<Integer> groupIds = new HashSet<>();
			resourceGroups.forEach(group -> groupIds.add(group.getId()));
			return groupIds;
		}),
		Member((sess, host) -> {
			Facility facility = getPerunBl().getFacilitiesManagerBl().getFacilityForHost(sess, host);
			List<Resource> facilityResources = getPerunBl().getFacilitiesManagerBl().getAssignedResources(sess, facility);
			List<Member> resourceMembers = new ArrayList<>();
			facilityResources.forEach(resource -> resourceMembers.addAll(getPerunBl().getResourcesManagerBl().getAssignedMembers(sess, resource)));
			Set<Integer> memberIds = new HashSet<>();
			resourceMembers.forEach(member -> memberIds.add(member.getId()));
			return memberIds;
		}),
		Resource((sess, host) -> {
			Facility facility = getPerunBl().getFacilitiesManagerBl().getFacilityForHost(sess, host);
			List<Resource> facilityResources = getPerunBl().getFacilitiesManagerBl().getAssignedResources(sess, facility);
			Set<Integer> resourceIds = new HashSet<>();
			facilityResources.forEach(resource -> resourceIds.add(resource.getId()));
			return resourceIds;
		}),
		Default((sess, host) -> {
			return Collections.emptySet();
		});

		private BiFunction<PerunSession, Host, Set<Integer>> function;

		RelatedHostObjectsResolver(final BiFunction<PerunSession, Host, Set<Integer>> function) {
			this.function = function;
		}

		/**
		 * Get RelatedHostObjectsResolver value by the given name or default value if the name does not exist.
		 *
		 * @param name of the value which will be retrieved if exists.
		 * @return RelatedHostObjectsResolver value.
		 */
		public static RelatedHostObjectsResolver getValue(String name) {
			try {
				return RelatedHostObjectsResolver.valueOf(name);
			} catch (IllegalArgumentException ex) {
				return RelatedHostObjectsResolver.Default;
			}
		}

		@Override
		public Set<Integer> apply(PerunSession sess, Host host) {
			return function.apply(sess, host);
		}
	}

	private enum RelatedUserExtSourceObjectsResolver implements BiFunction<PerunSession, UserExtSource, Set<Integer>> {
		Vo((sess, ues) -> {
			User user;
			try {
				user = getPerunBl().getUsersManagerBl().getUserByUserExtSource(sess, ues);
			} catch (UserNotExistsException e) {
				log.warn("User not exists for the userExtSource: " + ues);
				return Collections.emptySet();
			}
			List<Vo> vosFromUser = getPerunBl().getUsersManagerBl().getVosWhereUserIsMember(sess, user);
			Set<Integer> voIds = new HashSet<>();
			vosFromUser.forEach(vo -> voIds.add(vo.getId()));
			return voIds;
		}),
		Facility((sess, ues) -> {
			User user;
			try {
				user = getPerunBl().getUsersManagerBl().getUserByUserExtSource(sess, ues);
			} catch (UserNotExistsException e) {
				log.warn("User not exists for the userExtSource: " + ues);
				return Collections.emptySet();
			}
			List<Facility> userFacilities = getPerunBl().getFacilitiesManagerBl().getFacilitiesByPerunBean(sess, user);
			Set<Integer> facilityIds = new HashSet<>();
			userFacilities.forEach(facility -> facilityIds.add(facility.getId()));
			return facilityIds;
		}),
		User((sess, ues) -> {
			User user;
			try {
				user = getPerunBl().getUsersManagerBl().getUserByUserExtSource(sess, ues);
			} catch (UserNotExistsException e) {
				log.warn("User not exists for the userExtSource: " + ues);
				return Collections.emptySet();
			}
			return Collections.singleton(user.getId());
		}),
		Group((sess, ues) -> {
			User user;
			try {
				user = getPerunBl().getUsersManagerBl().getUserByUserExtSource(sess, ues);
			} catch (UserNotExistsException e) {
				log.warn("User not exists for the userExtSource: " + ues);
				return Collections.emptySet();
			}
			List<Group> userGroups = getPerunBl().getGroupsManagerBl().getGroupsByPerunBean(sess, user);
			Set<Integer> groupIds = new HashSet<>();
			userGroups.forEach(group -> groupIds.add(group.getId()));
			return groupIds;
		}),
		Member((sess, ues) -> {
			User user;
			try {
				user = getPerunBl().getUsersManagerBl().getUserByUserExtSource(sess, ues);
			} catch (UserNotExistsException e) {
				log.warn("User not exists for the userExtSource: " + ues);
				return Collections.emptySet();
			}
			List<Member> userMembers = getPerunBl().getMembersManagerBl().getMembersByUser(sess, user);
			Set<Integer> memberIds = new HashSet<>();
			userMembers.forEach(member -> memberIds.add(member.getId()));
			return memberIds;
		}),
		Resource((sess, ues) -> {
			User user;
			try {
				user = getPerunBl().getUsersManagerBl().getUserByUserExtSource(sess, ues);
			} catch (UserNotExistsException e) {
				log.warn("User not exists for the userExtSource: " + ues);
				return Collections.emptySet();
			}
			List<Resource> userResources = getPerunBl().getUsersManagerBl().getAssignedResources(sess, user);
			Set<Integer> resourceIds = new HashSet<>();
			userResources.forEach(resource -> resourceIds.add(resource.getId()));
			return resourceIds;
		}),
		Default((sess, user) -> {
			return Collections.emptySet();
		});

		private BiFunction<PerunSession, UserExtSource, Set<Integer>> function;

		RelatedUserExtSourceObjectsResolver(final BiFunction<PerunSession, UserExtSource, Set<Integer>> function) {
			this.function = function;
		}

		/**
		 * Get RelatedHostObjectsResolver value by the given name or default value if the name does not exist.
		 *
		 * @param name of the value which will be retrieved if exists.
		 * @return RelatedHostObjectsResolver value.
		 */
		public static RelatedUserExtSourceObjectsResolver getValue(String name) {
			try {
				return RelatedUserExtSourceObjectsResolver.valueOf(name);
			} catch (IllegalArgumentException ex) {
				return RelatedUserExtSourceObjectsResolver.Default;
			}
		}

		@Override
		public Set<Integer> apply(PerunSession sess, UserExtSource ues) {
			return function.apply(sess, ues);
		}
	}

	private enum RelatedEntitylessObjectsResolver implements BiFunction<PerunSession, String, Set<Integer>> {
		Default((sess, key) -> {
			return Collections.emptySet();
		});

		private BiFunction<PerunSession, String, Set<Integer>> function;

		RelatedEntitylessObjectsResolver(final BiFunction<PerunSession, String, Set<Integer>> function) {
			this.function = function;
		}

		/**
		 * Get RelatedHostObjectsResolver value by the given name or default value if the name does not exist.
		 *
		 * @param name of the value which will be retrieved if exists.
		 * @return RelatedHostObjectsResolver value.
		 */
		public static RelatedEntitylessObjectsResolver getValue(String name) {
			try {
				return RelatedEntitylessObjectsResolver.valueOf(name);
			} catch (IllegalArgumentException ex) {
				return RelatedEntitylessObjectsResolver.Default;
			}
		}

		@Override
		public Set<Integer> apply(PerunSession sess, String key) {
			return function.apply(sess, key);
		}
	}

	private enum MembershipPrivilegesResolver implements BiFunction<PerunSession, Set<Integer>, Boolean> {
		Vo((sess, objectIds) -> {
			if (sess.getPerunPrincipal().getUser() == null) return false;
			List<Member> principalUserMembers = getPerunBl().getMembersManagerBl().getMembersByUser(sess, sess.getPerunPrincipal().getUser());
			for (Integer objectId: objectIds) {
				for (Member userMember : principalUserMembers) {
					if (userMember.getVoId() == objectId && userMember.getStatus() == Status.VALID) {
						return true;
					}
				}
			}
			return false;
		}),
		Facility((sess, objectIds) -> {
			if (sess.getPerunPrincipal().getUser() == null) return false;
			HashSet<Resource> resourcesFromUser = new HashSet<>();
			List<Member> principalUserMembers = getPerunBl().getMembersManagerBl().getMembersByUser(sess, sess.getPerunPrincipal().getUser());
			for (Member member : principalUserMembers) {
				if (member.getStatus() != Status.VALID) continue;
				resourcesFromUser.addAll(getPerunBl().getResourcesManagerBl().getAssignedResources(sess, member));
			}
			for (Resource resource: resourcesFromUser) {
				if (objectIds.contains(resource.getFacilityId())) return true;
			}
			return false;
		}),
		Group((sess, objectIds) -> {
			if (sess.getPerunPrincipal().getUser() == null) return false;
			List<Member> principalUserMembers = getPerunBl().getMembersManagerBl().getMembersByUser(sess, sess.getPerunPrincipal().getUser());
			for (Member member : principalUserMembers) {
				if (member.getStatus() != Status.VALID) continue;
				List<Group> memberGroups = getPerunBl().getGroupsManagerBl().getGroupsByPerunBean(sess, member);
				for (Group group : memberGroups) {
					if (objectIds.contains(group.getId())) return true;
				}
			}
			return false;
		}),
		Default((sess, objectIds) -> false);

		private BiFunction<PerunSession, Set<Integer>, Boolean> function;

		MembershipPrivilegesResolver(final BiFunction<PerunSession, Set<Integer>, Boolean> function) {
			this.function = function;
		}

		/**
		 * Get MembershipPrivilegesResolver value by the given name or default value if the name does not exist.
		 *
		 * @param name of the value which will be retrieved if exists.
		 * @return MembershipPrivilegesResolver value.
		 */
		public static MembershipPrivilegesResolver getValue(String name) {
			try {
				return MembershipPrivilegesResolver.valueOf(name);
			} catch (IllegalArgumentException ex) {
				return MembershipPrivilegesResolver.Default;
			}
		}

		@Override
		public Boolean apply(PerunSession sess, Set<Integer> objectIds) {
			return function.apply(sess, objectIds);
		}

	}

	private static boolean objectAndRoleManageableByEntity(PerunBean entityToManage, PerunBean complementaryObject, String role) {
		RoleManagementRules rules;
		try {
			rules = AuthzResolverImpl.getRoleManagementRules(role);
		} catch (PolicyNotExistsException e) {
			throw new InternalErrorException("Management rules not exist for the role " + role, e);
		}

		Set<String> necessaryObjects = rules.getAssignedObjects().keySet();

		if (!rules.getEntitiesToManage().containsKey(entityToManage.getBeanName())) return false;
		if (complementaryObject != null && necessaryObjects.isEmpty()) return false;
		if (complementaryObject == null && !necessaryObjects.isEmpty()) return false;
		if (complementaryObject == null) return true;
		if (!necessaryObjects.contains(complementaryObject.getBeanName())) return false;

		if (necessaryObjects.size() > 1) {
			//Fetch super objects like Vo for group etc.
			Map<String, Set<Integer>> mapOfBeans = fetchAllRelatedObjects(Collections.singletonList(complementaryObject));
			for (String object : necessaryObjects) {
				if (!mapOfBeans.containsKey(object) || mapOfBeans.get(object).isEmpty()) return false;
			}
		}

		return true;
	}

	private static Map<String, Integer> createMappingOfValues(PerunBean entityToManage, PerunBean complementaryObject, String role) {
		Map<String, Integer> mapping = new HashMap<>();

		RoleManagementRules rules;
		try {
			rules = AuthzResolverImpl.getRoleManagementRules(role);
		} catch (PolicyNotExistsException e) {
			throw new InternalErrorException("Management rules not exist for the role " + role, e);
		}

		Integer role_id = authzResolverImpl.getRoleId(role);
		mapping.put("role_id", role_id);
		mapping.put(rules.getEntitiesToManage().get(entityToManage.getBeanName()), entityToManage.getId());

		Map <String, Set<Integer>> mapOfBeans = new HashMap<>();
		if (complementaryObject != null)
			//Fetch super objects like Vo for group etc.
			mapOfBeans = fetchAllRelatedObjects(Collections.singletonList(complementaryObject));


		for (String objectType : rules.getAssignedObjects().keySet()) {
			if (!mapOfBeans.containsKey(objectType)) {
				throw new InternalErrorException("Cannot create a mapping for role management, because object of type: " + objectType + " cannot be obtained.");
			}

			if (mapOfBeans.get(objectType).size() != 1) {
				throw new InternalErrorException("Cannot create a mapping for role management, because there is more than one object of type: " + objectType + ".");
			}

			String definition = rules.getAssignedObjects().get(objectType);

			Matcher matcher = columnNamesPattern.matcher(definition);
			if (!matcher.matches()) {
				throw new InternalErrorException("Cannot create a mapping for role management, because column name: " + definition + " contains forbidden characters. Allowed are only [1-9a-zA-Z_].");
			}

			mapping.put(definition, mapOfBeans.get(objectType).iterator().next());
		}

		return mapping;
	}
}
