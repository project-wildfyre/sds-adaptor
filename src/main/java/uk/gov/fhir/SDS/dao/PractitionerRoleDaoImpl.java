package uk.gov.fhir.SDS.dao;

import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import java.util.List;

@Component
public class PractitionerRoleDaoImpl {

    @Autowired
    private LdapTemplate ldapTemplate;

    private static final Logger log = LoggerFactory.getLogger(PractitionerRoleDaoImpl.class);

    private class PractitionerRoleAttributesMapper implements AttributesMapper {

        javax.naming.directory.Attributes attributes;
        @Override
        public Object mapFromAttributes(javax.naming.directory.Attributes attributes) throws NamingException {
            this.attributes = attributes;
            PractitionerRole practitionerRole = new PractitionerRole();
            if (hasAttribute("uniqueIdentifier")) {
                practitionerRole.setId(getAttribute("uniqueIdentifier"));
            } else {
                return null;
            }

            if (hasAttribute("nhsIDCode")) {
                CodeableConcept code = practitionerRole.addCode();

                code.addCoding().setSystem("https://fhir.nhs.uk/STU3/CodeSystem/CareConnect-SDSJobRoleName-1")
                        .setCode(getAttribute("nhsIDCode"));
                if (hasAttribute("nhsRoles")) {
                    code.getCodingFirstRep().setDisplay(getAttribute("nhsRoles"));
                }
            }


            return practitionerRole;
        }

        public Boolean hasAttribute(String attrib) {
            if (attributes.get(attrib)!= null) return true;
            return false;
        }

        public String getAttribute(String attrib) {
            if (attributes.get(attrib) == null) return null;
            try {
                return (String) attributes.get(attrib).get();
            } catch (NamingException ex) {
                return null;
            }

        }
    }

    public PractitionerRole read(IdType internalId) {

        log.info(internalId.getIdPart());
        List<PractitionerRole> practitioners = ldapTemplate.search("ou=People", "(&(objectclass=nhsOrgPerson)(uniqueIdentifier="+internalId.getIdPart()+"))", new PractitionerRoleAttributesMapper());

        if (practitioners.size()>0) {
            return practitioners.get(0);
        }
        return null;
    }

    public List<PractitionerRole> search(ReferenceParam practitioner) {


        if (practitioner != null) {
            log.info(practitioner.getValue());
            return ldapTemplate.search("ou=People", "(&(objectclass=nhsOrgPerson)(uid="+practitioner.getValue()+"))", new PractitionerRoleAttributesMapper());
        }
        return ldapTemplate.search("ou=People", "(objectclass=person)", new PractitionerRoleAttributesMapper());
    }

}
