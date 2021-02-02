package cz.metacentrum.perun.core.impl.modules.attributes;

import cz.metacentrum.perun.core.api.Attribute;
import cz.metacentrum.perun.core.api.AttributeDefinition;
import cz.metacentrum.perun.core.api.AttributesManager;
import cz.metacentrum.perun.core.api.ExtSource;
import cz.metacentrum.perun.core.api.User;
import cz.metacentrum.perun.core.api.UserExtSource;
import cz.metacentrum.perun.core.api.exceptions.ExtSourceNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.core.api.exceptions.UserExtSourceExistsException;
import cz.metacentrum.perun.core.impl.PerunSessionImpl;
import cz.metacentrum.perun.core.impl.modules.ModulesConfigLoader;
import cz.metacentrum.perun.core.impl.modules.ModulesYamlConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * Class for checking logins uniqueness in the namespace and filling umbrellaid-persistent id.
 * It is only storage! Use module login umbrellaid_persistent for access the value.
 *
 */
public class urn_perun_user_attribute_def_def_login_namespace_umbrellaid_persistent_shadow
	extends urn_perun_user_attribute_def_def_login_namespace {

	private final static Logger log = LoggerFactory.getLogger(
		urn_perun_user_attribute_def_def_login_namespace_umbrellaid_persistent_shadow.class);

	private final static String CONFIG_EXT_SOURCE_NAME_UMBRELLA_ID = "extSourceNameUmbrellaID";
	private final static String FRIENDLY_NAME = "login-namespace:umbrellaid-persistent-shadow";
	private final static String FRIENDLY_NAME_PARAMETER = "umbrellaid-persistent-shadow";

	private final ModulesConfigLoader loader = new ModulesYamlConfigLoader();

	/**
	 * fillAttribute will generate a version 1 UUID
	 */
	@Override
	public Attribute fillAttribute(PerunSessionImpl perunSession, User user, AttributeDefinition attribute) {

		Attribute filledAttribute = new Attribute(attribute);

		if (filledAttribute.getValue() != null && !filledAttribute.valueAsString().isEmpty()) return filledAttribute;

		if (attribute.getFriendlyName().equals(FRIENDLY_NAME)) {
			long most64SigBits = get64MostSignificantBits();
			long least64SigBits = get64LeastSignificantBits();
			UUID uuid = new UUID(most64SigBits, least64SigBits);

			filledAttribute.setValue(uuid.toString());
			return filledAttribute;
		} else {
			// without value
			return filledAttribute;
		}
	}

	/**
	 * ChangedAttributeHook() sets UserExtSource with following properties:
	 *  - extSourceType is IdP
	 *  - extSourceName is {getExtSourceName()}
	 *  - user's extSource login is the same as his persistent attribute
	 */
	@Override
	public void changedAttributeHook(PerunSessionImpl session, User user, Attribute attribute) {
		try {
			String userNamespace = attribute.getFriendlyNameParameter();

			if(userNamespace.equals(FRIENDLY_NAME_PARAMETER) && attribute.getValue() != null && !attribute.valueAsString().isEmpty()){
				ExtSource extSource = session.getPerunBl()
					.getExtSourcesManagerBl()
					.getExtSourceByName(session, getExtSourceName());
				UserExtSource userExtSource = new UserExtSource(extSource, 0, attribute.getValue().toString());

				session.getPerunBl().getUsersManagerBl().addUserExtSource(session, user, userExtSource);
			}
		} catch (UserExtSourceExistsException ex) {
			log.warn("Attribute: {}, External source already exists for the user.", FRIENDLY_NAME_PARAMETER, ex);
		} catch (ExtSourceNotExistsException ex) {
			throw new InternalErrorException("Attribute: " + FRIENDLY_NAME_PARAMETER +
				", IdP external source doesn't exist.", ex);
		}
	}

	@Override
	public AttributeDefinition getAttributeDefinition() {
		AttributeDefinition attr = new AttributeDefinition();
		attr.setNamespace(AttributesManager.NS_USER_ATTR_DEF);
		attr.setFriendlyName(FRIENDLY_NAME);
		attr.setDisplayName("umbrellaID login");
		attr.setType(String.class.getName());
		attr.setDescription("Login for umbrellaID. Do not use it directly! " +
			"Use \"user:virt:login-namespace:umbrellaid-persistent\" attribute instead.");
		return attr;
	}

	/**
	 * Get name of the extSource where the login will be set.
	 *
	 * @return extSource name for the login
	 */
	private String getExtSourceName() {
		return loader.loadString(getClass().getSimpleName(), CONFIG_EXT_SOURCE_NAME_UMBRELLA_ID);
	}

	/**
	 * Generate the 64 least significant bits as long values.
	 *
	 * @return 64 bits as long values
	 */
	private static long get64LeastSignificantBits() {
		Random random = new Random();
		long random63BitLong = random.nextLong() & 0x3FFFFFFFFFFFFFFFL;
		long variant3BitFlag = 0x8000000000000000L;
		return random63BitLong + variant3BitFlag;
	}

	/**
	 * Generate the 64 most significant bits as long values.
	 *
	 * @return 64 bits as long values
	 */
	private static long get64MostSignificantBits() {
		LocalDateTime start = LocalDateTime.of(1582, 10, 15, 0, 0, 0);
		Duration duration = Duration.between(start, LocalDateTime.now());
		long seconds = duration.getSeconds();
		long nanos = duration.getNano();
		long timeForUuidIn100Nanos = seconds * 10000000 + nanos * 100;
		long least12SignificantBitOfTime = (timeForUuidIn100Nanos & 0x000000000000FFFFL) >> 4;
		long version = 1 << 12;
		return (timeForUuidIn100Nanos & 0xFFFFFFFFFFFF0000L) + version + least12SignificantBitOfTime;
	}
}
