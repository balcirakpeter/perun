package cz.metacentrum.perun.core.impl.modules.attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.metacentrum.perun.core.api.Attribute;
import cz.metacentrum.perun.core.api.AttributeDefinition;
import cz.metacentrum.perun.core.api.AttributesManager;
import cz.metacentrum.perun.core.api.Facility;
import cz.metacentrum.perun.core.api.exceptions.WrongAttributeValueException;
import cz.metacentrum.perun.core.impl.PerunSessionImpl;
import cz.metacentrum.perun.core.implApi.modules.attributes.FacilityAttributesModuleAbstract;
import cz.metacentrum.perun.core.implApi.modules.attributes.FacilityAttributesModuleImplApi;

/**
 *
 * @author Milan Halenar <255818@mail.muni.cz>
 * @date 27.4.2011
 */
public class urn_perun_facility_attribute_def_def_homeMountPoints extends FacilityAttributesModuleAbstract implements FacilityAttributesModuleImplApi {

	private Pattern pattern = Pattern.compile("^/[-a-zA-Z.0-9_/]*$");

	@Override
	public void checkAttributeValue(PerunSessionImpl perunSession, Facility facility, Attribute attribute) throws WrongAttributeValueException {

		if(attribute.getValue() == null) {
			throw new WrongAttributeValueException(attribute);
		}
		List<String> homeMountPoints = (List<String>) attribute.getValue();
		if (homeMountPoints.isEmpty()) {
			throw new WrongAttributeValueException(attribute,"Attribute can't be empty");
		}
	}

	@Override
	public void checkAttributeSyntax(PerunSessionImpl perunSession, Facility facility, Attribute attribute) throws WrongAttributeValueException {

		if(attribute.getValue() == null) {
			return;
		}
		List<String> homeMountPoints = (List<String>) attribute.getValue();
		if (!homeMountPoints.isEmpty()) {
			for (String st : homeMountPoints) {
				Matcher match = pattern.matcher(st);
				if (!match.matches()) {
					throw new WrongAttributeValueException(attribute, "Bad homeMountPoints attribute format " + st);
				}
			}
		}
	}

	@Override
	public Attribute fillAttribute(PerunSessionImpl session, Facility facility, AttributeDefinition attribute) {
		return new Attribute(attribute);
	}

	@Override
	public AttributeDefinition getAttributeDefinition() {
		AttributeDefinition attr = new AttributeDefinition();
		attr.setNamespace(AttributesManager.NS_FACILITY_ATTR_DEF);
		attr.setFriendlyName("homeMountPoints");
		attr.setDisplayName("Home mount points");
		attr.setType(ArrayList.class.getName());
		attr.setDescription("All available home mount points.");
		return attr;
	}
}
