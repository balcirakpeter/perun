package cz.metacentrum.perun.core.impl;

import cz.metacentrum.perun.core.api.ExtSource;
import cz.metacentrum.perun.core.api.GroupsManager;
import cz.metacentrum.perun.core.api.exceptions.ExtSourceUnsupportedOperationException;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.core.api.exceptions.SubjectNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.rt.InternalErrorRuntimeException;
import cz.metacentrum.perun.core.blImpl.GroupsManagerBlImpl;
import cz.metacentrum.perun.core.blImpl.PerunBlImpl;
import cz.metacentrum.perun.core.implApi.ExtSourceSimpleApi;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import cz.metacentrum.perun.core.api.Attribute;
import cz.metacentrum.perun.core.api.AttributeDefinition;
import cz.metacentrum.perun.core.api.BeansUtils;
import cz.metacentrum.perun.core.api.Group;
import cz.metacentrum.perun.core.api.PerunSession;
import cz.metacentrum.perun.core.api.exceptions.AttributeNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.WrongAttributeAssignmentException;
import cz.metacentrum.perun.core.api.exceptions.WrongAttributeValueException;
import cz.metacentrum.perun.core.api.exceptions.WrongReferenceAttributeValueException;
/**
 * @author Michal Prochazka michalp@ics.muni.cz
 * @author Jan Zverina <zverina@cesnet.cz>
 */
public class ExtSourceSql extends ExtSource implements ExtSourceSimpleApi {

	private final static Logger log = LoggerFactory.getLogger(ExtSourceSql.class);
	private static final Map<String, String> attributeNameMapping = new HashMap<>();
	private Connection con;
	private boolean isOracle = false;
	private boolean isSQLite = false;

	private static PerunBlImpl perunBl;

	// filled by spring (perun-core.xml)
	public static PerunBlImpl setPerunBlImpl(PerunBlImpl perun) {
		perunBl = perun;
		return perun;
	}

	static {
		attributeNameMapping.put("m", "urn:perun:member");
		attributeNameMapping.put("u", "urn:perun:user");
		attributeNameMapping.put("f", "urn:perun:facility");
		attributeNameMapping.put("r", "urn:perun:resource");
		attributeNameMapping.put("g", "urn:perun:group");
		attributeNameMapping.put("v", "urn:perun:vo");
		attributeNameMapping.put("h", "urn:perun:host");
		attributeNameMapping.put("mr", "urn:perun:member_resource");
		attributeNameMapping.put("uf", "urn:perun:user_facility");
		attributeNameMapping.put("gr", "urn:perun:group_resource");
		attributeNameMapping.put("o", ":attribute-def:opt:");
		attributeNameMapping.put("d", ":attribute-def:def:");
	}


	public ExtSourceSql() {
	}

	@Override
	public List<Map<String,String>> findSubjectsLogins(String searchString) throws InternalErrorException {
		return findSubjectsLogins(searchString, 0);
	}

	@Override
	public List<Map<String, String>> findSubjectsLogins(String searchString, int maxResults) throws InternalErrorException {
		String query = getAttributes().get("query");
		if (query == null) {
			throw new InternalErrorException("query attribute is required");
		}
		List<Map<String, String>> subjects = new ArrayList<>();
		this.querySource(query, searchString, maxResults, subjects);
		return subjects;
	}

	@Override
	public Map<String, String> getSubjectByLogin(String login) throws InternalErrorException, SubjectNotExistsException {
		String query = getAttributes().get("loginQuery");
		if (query == null) {
			throw new InternalErrorException("loginQuery attribute is required");
		}

		List<Map<String, String>> subjects = new ArrayList<>();
		this.querySource(query, login, 0, subjects);

		if (subjects.size() < 1) {
			throw new SubjectNotExistsException("Login: " + login);
		}
		if (subjects.size() > 1) {
			throw new InternalErrorException("External source must return exactly one result, search string: " + login);
		}

		return subjects.get(0);
	}

	@Override
	public String getGroupSubjects(PerunSession sess, Group group, String status, List<Map<String, String>> subjects) throws InternalErrorException {
		// Get all group attributes and store them to map (info like query, time interval etc.)
		List<Attribute> groupAttributes = perunBl.getAttributesManagerBl().getAttributes(sess, group);
		Map<String, String> groupAttributesMap = new HashMap<>();
		for (Attribute attr: groupAttributes) {
			String value = BeansUtils.attributeValueToString(attr);
			String name = attr.getName();
			groupAttributesMap.put(name, value);
		}

		// Get url of database
		if (getAttributes().get("url") == null) {
			throw new InternalErrorException("url attribute is required");
		}

		// Register driver if the attribute has been defined
		if (getAttributes().get("driver") != null) {
			try {
				Class.forName(getAttributes().get("driver"));
			} catch (ClassNotFoundException e) {
				throw new InternalErrorException("Driver " + getAttributes().get("driver") + " cannot be registered", e);
			}
		}

		// Query for members (can be changed later, if we can get only modified members)
		String queryForMembers = groupAttributesMap.get(GroupsManager.GROUPMEMBERSQUERY_ATTRNAME);
		if (queryForMembers == null || queryForMembers.isEmpty()) {
			throw new InternalErrorException("Attribute " + GroupsManager.GROUPMEMBERSQUERY_ATTRNAME + " must be defined for group " + group + ".");
		}

		// Status of synchronization process (if we get all subjects from extSource or only modified)
		String extSourceStatus = GroupsManager.GROUP_SYNC_STATUS_FULL;
		// Check if we need to save new value of CRC to group change detection value
		boolean saveCheck = false;
		// Get query for group change detection
		String queryForChangeDetection = groupAttributesMap.get(GroupsManager.GROUPCHANGEDETECTIONQUERY_ATTRNAME);
		// String storing new value of CRC
		String crc = null;

		// Prepare statement and result for queries
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			// Check if we have existing connection. In case of Oracle also checks the connection validity
			if (this.con == null || (this.isOracle && !this.con.isValid(0))) {
				this.createConnection();
			}

			// If group has filled group detection query
			if (queryForChangeDetection != null && !queryForChangeDetection.isEmpty()) {
				st = this.con.prepareStatement(queryForChangeDetection);
				rs = st.executeQuery();

				while (rs.next()) {
					crc = rs.getString("crc");
					log.info("CRC: {}", crc);
				}

				// Get format of modifyTimestamp from definition of extSource
				String dateFormat = getAttributes().get("timestampFormat");
				if (dateFormat == null || dateFormat.isEmpty()) {
					throw new InternalErrorException("ExtSourceSQL: Format of timestamp is not defined. Declare timestampFormat attribute in definition of external source: " + this);
				}

				// Get query to get modified members
				String modifiedMembersQuery = groupAttributesMap.get(GroupsManager.GROUPMODIFIEDMEMBERSQUERY_ATTRNAME);
				if (modifiedMembersQuery == null || modifiedMembersQuery.isEmpty()) {
					throw new InternalErrorException("ExtSourceSQL: Group [" + group + "] has filled query for change detection, but doesn't have defined query for modified members.");
				}

				// Get group attribute with change detection value
				String groupChangeDetection = groupAttributesMap.get(GroupsManager.GROUPCHANGEDETECTION_ATTRNAME);
				if (groupChangeDetection != null && !groupChangeDetection.isEmpty()) {
					// Compare obtained CRC with the stored one in change detection value
					if (groupChangeDetection.equals(crc)) {
						// If its Lightweight synchronization, we don't need to gain data from extSource in this case
						if (status.equals(GroupsManager.GROUP_SYNC_STATUS_LIGHTWEIGHT)) {
							return GroupsManager.GROUP_SYNC_STATUS_MODIFIED;
						}

						String startOfLastSuccessSync = groupAttributesMap.get(GroupsManager.GROUPSTARTOFLASTSUCCESSSYNC_ATTRNAME);
						try {
							if (startOfLastSuccessSync != null && !startOfLastSuccessSync.isEmpty()) {
								java.util.Date startDate = BeansUtils.getDateFormatter().parse(startOfLastSuccessSync);
								SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
								String date = sdf.format(startDate);
								String tmp = modifiedMembersQuery.replace("?", date);
								queryForMembers = tmp;
								extSourceStatus = GroupsManager.GROUP_SYNC_STATUS_MODIFIED;
							}
						} catch (ParseException e) {
							// Should not happen
							log.error("ExtSourceSQL: Attribute " + GroupsManager.GROUPSTARTOFLASTSUCCESSSYNC_ATTRNAME + " has bad format: {}", startOfLastSuccessSync);
							throw new InternalErrorException("Error in parsing of date with start of last success sync: " + startOfLastSuccessSync, e);
						}
					} else {
						// We need to update value of change detection
						saveCheck = true;
					}
				} else {
					// We don't have stored CRC value in change detection attribute yet, so we need to store it
					saveCheck = true;
				}
			}

		} catch (SQLException e) {
			log.error("SQL exception during query '{}'", queryForChangeDetection);
			throw new InternalErrorRuntimeException(e);
		} finally {
			try {
				if (rs != null) rs.close();
				if (st != null) st.close();
			} catch (SQLException e) {
				log.error("SQL exception during closing the resultSet {} or statement {}.", rs, st);
				throw new InternalErrorRuntimeException(e);
			}
		}
		log.info("STATUS: {}", extSourceStatus);
		log.info("QUERY: {}", queryForMembers);

		// Query external sources for members
		this.querySource(queryForMembers, null, 0, subjects);

		// Save new value of change detection if needed
		if (saveCheck) {
			log.info("Saving new Group's change detection value {}", crc);
			// Save new LDAP modify timestamp to group
			try {
				AttributeDefinition attributeDefinition = perunBl.getAttributesManagerBl().getAttributeDefinition(sess, GroupsManager.GROUPCHANGEDETECTION_ATTRNAME);
				Attribute newChangeDetectionValue = new Attribute(attributeDefinition);
				newChangeDetectionValue.setValue(crc);
				perunBl.getAttributesManagerBl().setAttributeInNestedTransaction(sess, group, newChangeDetectionValue);
			} catch (WrongAttributeValueException | WrongAttributeAssignmentException | WrongReferenceAttributeValueException | AttributeNotExistsException e) {
				throw new InternalErrorException("There is a problem with saving of change detection value attribute to group "
						+ group + ". New value is " + crc + ".");
			}
		}
		return extSourceStatus;
	}

	protected void querySource(String query, String searchString, int maxResults, List<Map<String, String>> subjects) throws InternalErrorException {
		PreparedStatement st = null;
		ResultSet rs = null;

		if (getAttributes().get("url") == null) {
			throw new InternalErrorException("url attribute is required");
		}

		//log.debug("Searching for '{}' using query {} in external source 'url:{}'", new Object[] {searchString, query, (String) getAttributes().get("url")});
		log.debug("Searching for '{}' in external source 'url:{}'", searchString, (String) getAttributes().get("url"));

		// Register driver if the attribute has been defined
		if (getAttributes().get("driver") != null) {
			try {
				Class.forName(getAttributes().get("driver"));
			} catch (ClassNotFoundException e) {
				throw new InternalErrorException("Driver " + getAttributes().get("driver") + " cannot be registered", e);
			}
		}

		try {
			// Check if we have existing connection. In case of Oracle also checks the connection validity
			if (this.con == null || (this.isOracle && !this.con.isValid(0))) {
				this.createConnection();
			}

			st = this.con.prepareStatement(query);

			// Substitute the ? in the query by the seachString
			if (searchString != null && !searchString.isEmpty()) {
				for (int i = st.getParameterMetaData().getParameterCount(); i > 0; i--) {
					st.setString(i, searchString);
				}
			}

			// Limit results
			if (maxResults > 0) {
				st.setMaxRows(maxResults);

			}
			rs = st.executeQuery();

			log.trace("Query {}", query);

			while (rs.next()) {
				Map<String, String> map = new HashMap<String, String>();

				try {
					map.put("firstName", rs.getString("firstName"));
				} catch (SQLException e) {
					// If the column doesn't exists, ignore it
					map.put("firstName", null);
				}
				try {
					map.put("lastName", rs.getString("lastName"));
				} catch (SQLException e) {
					// If the column doesn't exists, ignore it
					map.put("lastName", null);
				}
				try {
					map.put("middleName", rs.getString("middleName"));
				} catch (SQLException e) {
					// If the column doesn't exists, ignore it
					map.put("middleName", null);
				}
				try {
					map.put("titleBefore", rs.getString("titleBefore"));
				} catch (SQLException e) {
					// If the column doesn't exists, ignore it
					map.put("titleBefore", null);
				}
				try {
					map.put("titleAfter", rs.getString("titleAfter"));
				} catch (SQLException e) {
					// If the column doesn't exists, ignore it
					map.put("titleAfter", null);
				}
				try {
					map.put("login", rs.getString("login"));
				} catch (SQLException e) {
					// If the column doesn't exists, ignore it
					map.put("login", null);
				}

				for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
					String columnName = rs.getMetaData().getColumnLabel(i);
					log.trace("Iterating through attribute {}", columnName);
					// Now go through all other attributes. If the column name(=attribute name) contains ":", then it represents an attribute
					if (columnName.contains(":")) {
						// Decode the attribute name (column name has limited size, so we need to code the attribute names)
						// Coded attribute name: x:y:z
						// x - m: member, u: user, f: facility, r: resource, mr: member-resource, uf: user-facility, h: host, v: vo, g: group, gr: group-resource
						// y - d: def, o: opt
						String[] attributeRaw = columnName.split(":", 3);
						String attributeName = null;
						if (!attributeNameMapping.containsKey(attributeRaw[0])) {
							log.warn("Unknown attribute type '{}' for user {} {}, attributeRaw {}", attributeRaw[0], map.get("firstName"), map.get("lastName"), attributeRaw);
						} else if (!attributeNameMapping.containsKey(attributeRaw[1])) {
							log.warn("Unknown attribute type '{}' for user {} {}, attributeRaw {}", attributeRaw[1], map.get("firstName"), map.get("lastName"), attributeRaw);
						} else {
							attributeName = attributeNameMapping.get(attributeRaw[0]) + attributeNameMapping.get(attributeRaw[1]) + attributeRaw[2];
							if (!Objects.equals(rs.getMetaData().getColumnTypeName(i), "BLOB")) {
								// trace only string data
								log.trace("Adding attribute {} with value {}", attributeName, rs.getString(i));
							} else {
								log.trace("Adding attribute {} with BLOB value", attributeName);
							}
						}

						String attributeValue = null;
						if (Objects.equals(rs.getMetaData().getColumnTypeName(i), "BLOB")) {
							// source column is binary
							try {
								InputStream inputStream = rs.getBinaryStream(i);
								if (inputStream != null) {
									ByteArrayOutputStream result = new ByteArrayOutputStream();
									byte[] buffer = new byte[1024];
									int length;
									while ((length = inputStream.read(buffer)) != -1) {
										result.write(buffer, 0, length);
									}
									byte[] bytes = Base64.encodeBase64(result.toByteArray());
									attributeValue = new String(bytes, "UTF-8");
								}
							} catch (IOException ex) {
								log.error("Unable to read BLOB for column {}", columnName);
								throw new InternalErrorException("Unable to read BLOB data for column: "+columnName, ex);
							}
						} else {
							// let driver to convert type to string
							attributeValue = rs.getString(i);
						}
						if (rs.wasNull()) {
							map.put(attributeName, null);
						} else {
							map.put(attributeName, attributeValue);
						}
					} else if (columnName.toLowerCase().startsWith(ExtSourcesManagerImpl.USEREXTSOURCEMAPPING)) {
						// additionalUserExtSources, we must do lower case because some DBs changes lower to upper
						map.put(columnName.toLowerCase(), rs.getString(i));
						log.trace("Adding attribute {} with value {}", columnName, rs.getString(i));
					}
				}
				subjects.add(map);
			}
			log.debug("Returning {} subjects from external source {} for searchString {}", new Object[] {subjects.size(), this, searchString});
		} catch (SQLException e) {
			log.error("SQL exception during searching for subject '{}'", query);
			throw new InternalErrorRuntimeException(e);
		} finally {
			try {
				if (rs != null) rs.close();
				if (st != null) st.close();
			} catch (SQLException e) {
				log.error("SQL exception during closing the resultSet or statement, while searching for subject '{}'", query);
				throw new InternalErrorRuntimeException(e);
			}
		}
	}

	protected void createConnection() throws SQLException, InternalErrorException {
		try {

			String connectionUrl = getAttributes().get("url");
			String user = getAttributes().get("user");
			String pass = getAttributes().get("password");
			Properties connectionProperties = new Properties();

			// set user/pass to properties if present
			if (user != null && pass != null) {
				connectionProperties.put("user", user);
				connectionProperties.put("password", pass);
			}

			// set connection read_only for SQLite (doesn't follow JDBC standard)
			if (connectionUrl.startsWith("jdbc:sqlite:")) {
				isSQLite = true;
				connectionProperties.put("open_mode","1");
			}

			// create connection
			this.con = DriverManager.getConnection(connectionUrl, connectionProperties);

			// Set connection to read-only mode for standard JDBC drivers
			if (!isSQLite) {
				this.con.setReadOnly(true);
			}

			if (this.con.getMetaData().getDriverName().toLowerCase().contains("oracle")) {
				this.isOracle = true;
				this.isSQLite = false;
			}

		} catch (SQLException e) {
			log.error("SQL exception during creating the connection to URL", getAttributes().get("url"));
			throw new InternalErrorRuntimeException(e);
		}
	}

	@Override
	public void close() throws InternalErrorException {
		if (this.con != null) {
			try {
				this.con.close();
				this.con = null;
			} catch (SQLException e) {
				throw new InternalErrorException(e);
			}
		}
	}

	@Override
	public List<Map<String, String>> getSubjectGroups(Map<String, String> attributes) throws InternalErrorException, ExtSourceUnsupportedOperationException {
		String sqlQueryForGroup = attributes.get(GroupsManager.GROUPSQUERY_ATTRNAME);

		return this.groupQuery(sqlQueryForGroup, null, 0);
	}

	/**
	 * Get subject groups from an external source
	 *
	 * @param query to select subject groups
	 * @param searchString by which will be ? in query replaced
	 * @param maxResults maximum subjects to get
	 * @return list of subjects
	 * @throws InternalErrorException
	 */
	protected List<Map<String,String>> groupQuery(String query, String searchString, int maxResults) throws InternalErrorException {

		this.checkAndSetPrerequisites();

		try (PreparedStatement st = getPreparedStatement(query, searchString, maxResults)) {
			try (ResultSet rs = st.executeQuery()) {

				List<Map<String, String>> subjects = new ArrayList<>();

				log.trace("Query {}", query);

				while (rs.next()) {
					Map<String, String> map = new HashMap<>();

					try {
						map.put(GroupsManagerBlImpl.GROUP_NAME, rs.getString(GroupsManagerBlImpl.GROUP_NAME));
					} catch (SQLException e) {
						// If the column doesn't exists, ignore it
						map.put(GroupsManagerBlImpl.GROUP_NAME, null);
					}
					try {
						map.put(GroupsManagerBlImpl.PARENT_GROUP_NAME, rs.getString(GroupsManagerBlImpl.PARENT_GROUP_NAME));
					} catch (SQLException e) {
						// If the column doesn't exists, ignore it
						map.put(GroupsManagerBlImpl.PARENT_GROUP_NAME, null);
					}
					try {
						map.put(GroupsManagerBlImpl.GROUP_DESCRIPTION, rs.getString(GroupsManagerBlImpl.GROUP_DESCRIPTION));
					} catch (SQLException e) {
						// If the column doesn't exists, ignore it
						map.put(GroupsManagerBlImpl.GROUP_DESCRIPTION, null);
					}
					subjects.add(map);
				}

				log.debug("Returning {} subjects from external source {} for searchString {}", subjects.size(), this, searchString);
				return subjects;
			}
		} catch (SQLException e) {
			log.error("SQL exception during searching for subject '{}'", query);
			throw new InternalErrorRuntimeException(e);
		}
	}

	protected Map<String,String> getAttributes() throws InternalErrorException {
		return perunBl.getExtSourcesManagerBl().getAttributes(this);
	}

	/**
	 * Check if needed prerequisites are set to be able to call query.
	 *
	 * @throws InternalErrorException if expected attributes are not set
	 */
	private void checkAndSetPrerequisites() throws InternalErrorException {
		if (getAttributes().get("url") == null) {
			throw new InternalErrorException("url attribute is required");
		}

		// Register driver if the attribute has been defined
		if (getAttributes().get("driver") != null) {
			try {
				Class.forName(getAttributes().get("driver"));
			} catch (ClassNotFoundException e) {
				throw new InternalErrorException("Driver " + getAttributes().get("driver") + " cannot be registered", e);
			}
		}

		try {
			// Check if we have existing connection. In case of Oracle also checks the connection validity
			if (this.con == null || (this.isOracle && !this.con.isValid(0))) {
				this.createConnection();
			}
		} catch (SQLException ex) {
			throw new InternalErrorException("Can't check if connection to database is still valid in SQL ExtSource.", ex);
		}
	}

	/**
	 * Prepares query statement from query.
	 * - substitutes character '?' by searchString.
	 * - set max results limit.
	 *
	 * @param query basic query
	 * @param searchString search string
	 * @param maxResults limit of max results where 0 is unlimited
	 * @return
	 * @throws SQLException
	 */
	private PreparedStatement getPreparedStatement(String query, String searchString, int maxResults) throws SQLException {
		PreparedStatement st = this.con.prepareStatement(query);

		// Substitute the ? in the query by the searchString
		if (searchString != null && !searchString.isEmpty()) {
			for (int i = st.getParameterMetaData().getParameterCount(); i > 0; i--) {
				st.setString(i, searchString);
			}
		}

		// Limit results
		if (maxResults > 0) {
			st.setMaxRows(maxResults);

		}

		return st;
	}
}
