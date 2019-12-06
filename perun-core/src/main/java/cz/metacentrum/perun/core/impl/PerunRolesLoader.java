package cz.metacentrum.perun.core.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import cz.metacentrum.perun.core.api.BeansUtils;
import cz.metacentrum.perun.core.api.PerunPolicy;
import cz.metacentrum.perun.core.api.Pair;
import cz.metacentrum.perun.core.api.RoleManagementRules;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcPerunTemplate;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The purpose of the PerunRolesLoader is to load perun roles and policies from the perun-roles.yml configuration file.
 *
 * Production configuration file is located in /etc/perun/perun-roles.yml
 * Configuration file which is used during the build is located in perun-base/src/test/resources/perun-roles.yml
 */
public class PerunRolesLoader {

	private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

	private Resource configurationPath;

	//Constants
	private static final String PERUN_ROLES = "perun_roles";
	private static final String ROLES_ID_SEQ = "roles_id_seq";
	private static final String PERUN_POLICIES = "perun_policies";
	private static final String POLICY_ROLES = "policy_roles";
	private static final String INCLUDE_POLICIES = "include_policies";
	private static final String PERUN_ACTION_TYPES = "perun_action_types";
	private static final String NAME = "name";
	private static final String OBJECT = "object";
	private static final String DESCRIPTION = "description";
	private static final String ACTION_TYPES_SEQ = "action_types_seq";
	private static final String PERUN_ROLES_MANAGEMENT = "perun_roles_management";
	private static final String PRIVILEGED_ROLES = "privileged_roles";
	private static final String ENTITIES_TO_MANAGE = "entities_to_manage";
	private static final String ASSIGN_TO_OBJECTS = "assign_to_objects";

	/**
	 * Load perun roles from the configuration file to the database.
	 *
	 * @param jdbc connection to database
	 */
	public void loadPerunRoles(JdbcPerunTemplate jdbc) {

		JsonNode rootNode = loadConfigurationFile();

		JsonNode rolesNode = rootNode.get(PERUN_ROLES);
		List<String> roles = objectMapper.convertValue(rolesNode, new TypeReference<List<String>>() {});

		// Check if all roles defined in class Role exists in the DB
		for (String role : roles) {
			try {
				if (0 == jdbc.queryForInt("select count(*) from roles where name=?", role.toLowerCase())) {
					//Skip creating not existing roles for read only Perun
					if (BeansUtils.isPerunReadOnly()) {
						throw new InternalErrorException("One of default roles not exists in DB - " + role);
					} else {
						int newId = Utils.getNewId(jdbc, ROLES_ID_SEQ);
						jdbc.update("insert into roles (id, name) values (?,?)", newId, role.toLowerCase());
					}
				}
			} catch (RuntimeException e) {
				throw new InternalErrorException(e);
			}
		}
	}

	/**
	 * Load policies from the configuration file as list of PerunPolicies
	 *
	 * @return list of PerunPolicies
	 */
	public List<PerunPolicy> loadPerunPolicies() {
		List<PerunPolicy> policies = new ArrayList<>();
		JsonNode rootNode = loadConfigurationFile();
		//Fetch all policies from the configuration file
		JsonNode policiesNode = rootNode.get(PERUN_POLICIES);

		// For each policy node construct PerunPolicy and add it to the list
		Iterator<String> policyNames = policiesNode.fieldNames();
		while(policyNames.hasNext()) {
			String policyName = policyNames.next();
			JsonNode policyNode = policiesNode.get(policyName);
			List<Map<String, String>> perunRoles = new ArrayList<>();
			JsonNode perunRolesNode = policyNode.get(POLICY_ROLES);

			//Field policy_roles is saved as List of maps in the for loop
			for (JsonNode perunRoleNode : perunRolesNode) {
				Map<String, String> innerRoleMap = createmapFromJsonNode(perunRoleNode);
				perunRoles.add(innerRoleMap);
			}

			//Field include_policies is saved as List of Strings.
			List<String> includePolicies = new ArrayList<>(objectMapper.convertValue(policyNode.get(INCLUDE_POLICIES), new TypeReference<List<String>>() {}));

			policies.add(new PerunPolicy(policyName, perunRoles, includePolicies));
		}

		return policies;
	}

	/**
	 * Load action types from the configuration file to the database
	 *
	 * @param jdbc connection to database
	 */
	public void loadActionTypes(JdbcPerunTemplate jdbc) {
		JsonNode rootNode = loadConfigurationFile();
		JsonNode actionTypesNode = rootNode.get(PERUN_ACTION_TYPES);
		List<JsonNode> allActionTypes = new ArrayList<>(new ObjectMapper().convertValue(actionTypesNode, new TypeReference<List<JsonNode>>() {
		}));

		for (JsonNode actionTypeNode : allActionTypes) {
			String actionTypeName = actionTypeNode.get(NAME).isNull() ? null : actionTypeNode.get(NAME).textValue().toLowerCase();
			String actionTypeObject = actionTypeNode.get(OBJECT).isNull() ? null : actionTypeNode.get(OBJECT).textValue();
			String actionTypeDescription = actionTypeNode.get(DESCRIPTION).isNull() ? null : actionTypeNode.get(DESCRIPTION).textValue();
			boolean isMissing = false;
			try {
				if (actionTypeObject == null)
					isMissing = (0 == jdbc.queryForInt("select count(*) from action_types where action_type=? and object is null", actionTypeName));
				else
					isMissing = (0 == jdbc.queryForInt("select count(*) from action_types where action_type=? and object=?", actionTypeName, actionTypeObject));
				if (isMissing) {
					//Skip creating not existing actionTypes for read only Perun
					if (BeansUtils.isPerunReadOnly()) {
						throw new InternalErrorException("One of default actionType not exists in DB - " + actionTypeName + ": " + actionTypeObject);
					} else {
						int newId = Utils.getNewId(jdbc, ACTION_TYPES_SEQ);
						jdbc.update("insert into action_types (id, action_type, description, object) values (?,?,?,?)", newId, actionTypeName, actionTypeDescription, actionTypeObject);
					}
				}
			} catch (RuntimeException e) {
				throw new InternalErrorException(e);
			}
		}
	}

	/**
	 * Load RoleManagementRules from the configuration file
	 *
	 * @return Map of RoleManagementRules
	 */
	public Map<String, RoleManagementRules> loadPerunRolesManagement() {
		Map<String, RoleManagementRules> rolesManagementRules = new HashMap<>();
		JsonNode rootNode = loadConfigurationFile();
		//Fetch all policies from the configuration file
		JsonNode rolesNodes = rootNode.get(PERUN_ROLES_MANAGEMENT);

		// For each role node construct RoleManagementRules and add it to the map
		Iterator<String> roleNames =rolesNodes.fieldNames();
		while(roleNames.hasNext()) {
			String roleName = roleNames.next();
			JsonNode roleNode = rolesNodes.get(roleName);
			List<Map<String, String>> privilegedRoles = new ArrayList<>();
			JsonNode privilegedRolesNode = roleNode.get(PRIVILEGED_ROLES);

			//Field privileged_roles is saved as List of maps in the for loop
			for (JsonNode privilegedRoleNode : privilegedRolesNode) {
				Map<String, String> innerRoleMap = createmapFromJsonNode(privilegedRoleNode);
				privilegedRoles.add(innerRoleMap);
			}

			Map<String, String> entitiesToManage = createmapFromJsonNode(roleNode.get(ENTITIES_TO_MANAGE));
			Map<String, String> objectsToAssign = createmapFromJsonNode(roleNode.get(ASSIGN_TO_OBJECTS));

			rolesManagementRules.put(roleName, new RoleManagementRules(roleName, privilegedRoles, entitiesToManage, objectsToAssign));
		}

		return rolesManagementRules;
	}

	/**
	 * Loads the configuration file according the file path
	 *
	 * @return root node of the file
	 */
	private JsonNode loadConfigurationFile() {

		JsonNode rootNode;
		try (InputStream is = configurationPath.getInputStream()) {
			rootNode = objectMapper.readTree(is);
		} catch (FileNotFoundException e) {
			throw new InternalErrorException("Configuration file not found for perun roles. It should be in: " + configurationPath, e);
		} catch (IOException e) {
			throw new InternalErrorException("IO exception was thrown during the processing of the file: " + configurationPath, e);
		}

		return rootNode;
	}

	/**
	 * Create map from JsonNode keys and their values
	 *
	 * @param node from which will be the map created
	 * @return result map of keys and values
	 */
	private Map<String, String> createmapFromJsonNode(JsonNode node) {
		Map<String, String> resultMap = new HashMap<>();

		Iterator<String> nodeArrayKeys = node.fieldNames();
		while (nodeArrayKeys.hasNext()) {
			String key = nodeArrayKeys.next();
			JsonNode valueNode = node.get(key);
			String value = valueNode.isNull() ? null : valueNode.textValue();
			resultMap.put(key, value);
		}

		return resultMap;
	}

	public void setConfigurationPath(Resource configurationPath) {
		this.configurationPath = configurationPath;
	}
}
