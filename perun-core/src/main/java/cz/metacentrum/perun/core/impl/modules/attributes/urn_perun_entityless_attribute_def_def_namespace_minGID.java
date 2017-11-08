package cz.metacentrum.perun.core.impl.modules.attributes;

import cz.metacentrum.perun.core.api.Attribute;
import cz.metacentrum.perun.core.api.AttributeDefinition;
import cz.metacentrum.perun.core.api.AttributesManager;
import cz.metacentrum.perun.core.api.exceptions.AttributeNotExistsException;
import cz.metacentrum.perun.core.api.exceptions.ConsistencyErrorException;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.core.api.exceptions.WrongAttributeAssignmentException;
import cz.metacentrum.perun.core.api.exceptions.WrongAttributeValueException;
import cz.metacentrum.perun.core.api.exceptions.WrongReferenceAttributeValueException;
import cz.metacentrum.perun.core.impl.PerunSessionImpl;
import cz.metacentrum.perun.core.implApi.modules.attributes.EntitylessAttributesModuleAbstract;
import cz.metacentrum.perun.core.implApi.modules.attributes.EntitylessAttributesModuleImplApi;
/**
 *
 * @author Michal Stava <stavamichal@gmail.com>
 */
public class  urn_perun_entityless_attribute_def_def_namespace_minGID extends EntitylessAttributesModuleAbstract implements EntitylessAttributesModuleImplApi {


	@Override
	public void checkAttributeValue(PerunSessionImpl perunSession, String key, Attribute attribute) throws InternalErrorException, WrongAttributeValueException, WrongReferenceAttributeValueException, WrongAttributeAssignmentException {
		Integer minGID = (Integer) attribute.getValue();
		if(minGID != null) {
			Attribute maxGIDAttr;
			try {
				maxGIDAttr = perunSession.getPerunBl().getAttributesManagerBl().getAttribute(perunSession, key, AttributesManager.NS_ENTITYLESS_ATTR_DEF + ":namespace-maxGID");
			} catch (AttributeNotExistsException ex) {
				throw new ConsistencyErrorException("Attribute namespace-maxGID is supposed to exist.",ex);
			}
			Integer maxGID = (Integer) maxGIDAttr.getValue();
			if(maxGID != null) {
				if(minGID > maxGID) throw new WrongAttributeValueException(attribute, "Attribute value must be less than maxGID. MaxGID = " + maxGID + ", and minGID try to set = " + minGID);
			}
		}
	}

	@Override
	public void checkAttributeSyntax(PerunSessionImpl perunSession, String key, Attribute attribute) throws WrongAttributeValueException {
		Integer minGID = (Integer) attribute.getValue();
		if(minGID != null) {
			if(minGID < 1) throw new WrongAttributeValueException(attribute, "Attribute value must be min 1.");
		}
	}

	@Override
	public AttributeDefinition getAttributeDefinition() {
		AttributeDefinition attr = new AttributeDefinition();
		attr.setNamespace(AttributesManager.NS_ENTITYLESS_ATTR_DEF);
		attr.setFriendlyName("namespace_minGID");
		attr.setDisplayName("Min GID in namespace");
		attr.setType(Integer.class.getName());
		attr.setDescription("Minimal value of Group ID.");
		return attr;
	}
}
