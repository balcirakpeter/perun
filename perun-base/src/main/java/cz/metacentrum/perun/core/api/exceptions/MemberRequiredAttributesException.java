package cz.metacentrum.perun.core.api.exceptions;

import cz.metacentrum.perun.core.api.Attribute;
import cz.metacentrum.perun.core.api.AttributeDefinition;
import cz.metacentrum.perun.core.api.Member;

/**
 * Member does not have set required attributes correctly
 *
 * @author Peter Balcirak
 */
public class MemberRequiredAttributesException extends PerunException {
    static final long serialVersionUID = 0;

    private Member member;
    private AttributeDefinition attributeDefinition;
    private AttributeDefinition referenceAttribute;
    private Attribute attribute;
    private Object attributeHolder;
    private Object attributeHolderSecondary;

    public MemberRequiredAttributesException( Member member) {
        super(("Attribute: null while setting required attributes for member: " + (member == null ? "null" : member));
        this.member = member;
    }

    public MemberRequiredAttributesException(AttributeDefinition attributeDefinition, Member member) {
        super((attributeDefinition == null ? "Attribute: null" : attributeDefinition) +
                "while setting required attributes for member: " + (member == null ? "null" : member));
        this.attributeDefinition = attribute;
        this.member = member;
    }

    public MemberRequiredAttributesException(AttributeDefinition attributeDefinition, AttributeDefinition referenceAttribute, Member member) {
        super((attributeDefinition == null ? "Attribute: null" : attributeDefinition) +
                " reference attribute " + (referenceAttribute == null ? "null" : referenceAttribute) +
                "while setting required attributes for member: " + (member == null ? "null" : member));
        this.attributeDefinition = attributeDefinition;
        this.referenceAttribute = referenceAttribute;
        this.member = member;
    }

    public MemberRequiredAttributesException(Attribute attribute, Object attributeHolder, Object attributeHolderSecondary, Member member) {
        super((attribute == null ? "Attribute: null" : attribute.toString()) +
                "Set for: " + (attributeHolder == null ? "null primaryHolder" : attributeHolder) +
                " and " + (attributeHolderSecondary == null ? "null secondaryHolder" : attributeHolderSecondary) +
                "while setting required attributes for member: " + (member == null ? "null" : member));
        this.attribute = attribute;
        this.attributeHolder = attributeHolder;
        this.attributeHolderSecondary = attributeHolderSecondary;
        this.member = member;
    }

    public Member getMember() {
        return this.member;
    }

    public AttributeDefinition getAttributeDefinition() {
        return attributeDefinition;
    }
}