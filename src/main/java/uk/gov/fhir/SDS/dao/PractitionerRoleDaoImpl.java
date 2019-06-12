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
import java.text.SimpleDateFormat;
import java.util.List;

@Component
public class PractitionerRoleDaoImpl {

    @Autowired
    private LdapTemplate ldapTemplate;

    @Autowired
    private OrganizationDaoImpl organizationDao;

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

            if (hasAttribute("uid")) {
                practitionerRole.getPractitioner().setReference("Practitioner/"+getAttribute("uid"));
            }
            if (hasAttribute("cn")) {
                practitionerRole.getPractitioner().setDisplay(getAttribute("cn"));
            }

            if (hasAttribute("nhsOrgOpenDate")) {
                practitionerRole.setActive(true);

            }
            if (hasAttribute("nhsOrgCloseDate")) {
                practitionerRole.setActive(false);

            }

            if (hasAttribute("nhsCountry")) {
                Extension country = new Extension();
                country.setUrl("https://fhir.gov.uk/Extension/nhsCountry");
                CodeableConcept code = new CodeableConcept();
                code.addCoding()
                        .setSystem("https://fhir.gov.uk/CodeSystem/UKCountry")
                        .setCode(getAttribute("nhsCountry"));
                country.setValue(code);
                practitionerRole.addExtension(country);
            }
            if (hasAttribute("nhsIDCode")) {
                practitionerRole.getOrganization().setReference("Organization/"+getAttribute("nhsIDCode"));
                /*
                Organization org = organizationDao.read(false, new IdType(getAttribute("nhsIDCode")));
                if (org != null && org.hasName()) {
                    practitionerRole.getPractitioner().setDisplay(org.getName());
                }

                 */
            }
            if (hasAttribute("nhsRoles")) {
                CodeableConcept code = practitionerRole.addCode();

                code.addCoding().setSystem("https://fhir.nhs.uk/STU3/CodeSystem/CareConnect-SDSJobRoleName-1");

                if (hasAttribute("nhsRoles")) {

                    String[] roles = getAttribute("nhsRoles").split(":");
                    if (roles[2] != null) {
                        code.getCodingFirstRep().setDisplay(roles[2].replace("\"",""));
                    }
                }
            }
            if (hasAttribute("nhsGNC")) {
                practitionerRole.addIdentifier()
                        .setSystem("https://fhir.nhs.uk/Id/sds-user-id")
                        .setValue(getAttribute("nhsGNC"));
            }
            if (hasAttribute("o")) {
                practitionerRole.getOrganization().setDisplay(getAttribute("o"));
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

    public List<PractitionerRole> search( TokenParam identifier,ReferenceParam practitioner, ReferenceParam organisation) {

        String ldapFilter = "";

        if (identifier != null) {
            log.info(identifier.getValue());
            ldapFilter = ldapFilter + "(nhsGNC="+identifier.getValue()+")";
        }
        if (practitioner != null) {
            log.info(practitioner.getValue());
           ldapFilter = ldapFilter + "(uid="+practitioner.getValue()+")";
        }
        if (organisation != null) {
            log.info(organisation.getValue());
            ldapFilter = ldapFilter + "(nhsIdCode="+organisation.getValue()+")";
        }
        if (ldapFilter.isEmpty()) {
            return null;
        }
        ldapFilter = "(&(objectclass=nhsOrgPerson)"+ldapFilter+")";
        return ldapTemplate.search("ou=People", ldapFilter, new PractitionerRoleAttributesMapper());
    }

}
