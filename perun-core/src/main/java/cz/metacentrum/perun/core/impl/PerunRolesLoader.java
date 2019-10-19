package cz.metacentrum.perun.core.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import cz.metacentrum.perun.core.api.BeansUtils;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcPerunTemplate;
import org.springframework.core.io.Resource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PerunRolesLoader {

	private static final Logger log = LoggerFactory.getLogger(PerunBasicDataSource.class);

	private Resource configurationPath;

	public Map<String, JsonNode> loadPerunRoles(JdbcPerunTemplate jdbc) {
		if (BeansUtils.isPerunReadOnly()) log.debug("Loading authzresolver manager init in readOnly version.");

		Map<String, JsonNode> policies = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		JsonNode rootNode;
		try (InputStream is = configurationPath.getInputStream()) {
			rootNode = mapper.readTree(is);
		} catch (FileNotFoundException e) {
			throw new InternalErrorException("Configuration file not found for perun roles. It should be in: " + configurationPath, e);
		} catch (IOException e) {
			throw new InternalErrorException("IO exception was thrown during the processing of the file: " + configurationPath, e);
		}
		JsonNode rolesNode =rootNode.get("perun_roles");
		List<String> roles = new ObjectMapper().convertValue(rolesNode, new TypeReference<List<String>>() {});

		JsonNode policiesNode = rootNode.get("perun_policies");
		Iterator<String> policyNames = policiesNode.fieldNames();
		while(policyNames.hasNext()) {
			String policyname = policyNames.next();
			policies.put(policyname, policiesNode.get(policyname));
		}

		// Check if all roles defined in class Role exists in the DB
		for (String role : roles) {
			try {
				if (0 == jdbc.queryForInt("select count(*) from roles where name=?", role.toLowerCase())) {
					//Skip creating not existing roles for read only Perun
					if (BeansUtils.isPerunReadOnly()) {
						throw new InternalErrorException("One of default roles not exists in DB - " + role);
					} else {
						int newId = Utils.getNewId(jdbc, "roles_id_seq");
						jdbc.update("insert into roles (id, name) values (?,?)", newId, role.toLowerCase());
					}
				}
			} catch (RuntimeException e) {
				throw new InternalErrorException(e);
			}
		}

		return policies;
	}

	public void setConfigurationPath(Resource configurationPath) {
		this.configurationPath = configurationPath;
	}
}
