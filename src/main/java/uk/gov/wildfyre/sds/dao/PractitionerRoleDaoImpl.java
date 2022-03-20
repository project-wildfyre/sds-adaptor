package uk.gov.wildfyre.sds.dao;

import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;
import uk.gov.wildfyre.sds.support.NHSDigitalConstants;

import javax.naming.NamingException;
import java.util.Collections;
import java.util.List;

@Component
public class PractitionerRoleDaoImpl {

    @Autowired
    private LdapTemplate ldapTemplate;

    @Autowired
    private PractitionerDaoImpl practitionerDao;

    private static final Logger log = LoggerFactory.getLogger(PractitionerRoleDaoImpl.class);

    private class PractitionerRoleAttributesMapper implements AttributesMapper {

        javax.naming.directory.Attributes attributes;
        @Override
        public Object mapFromAttributes(javax.naming.directory.Attributes attributes) throws NamingException {
            this.attributes = attributes;
            PractitionerRole practitionerRole = new PractitionerRole();
            if (hasAttribute("uniqueIdentifier")) {
                practitionerRole.setId(getAttribute("uniqueIdentifier"));
                practitionerRole.addIdentifier().setSystem(NHSDigitalConstants.SDSUserRoleProfileId).setValue(getAttribute("uniqueIdentifier"));
            } else {
                return null;
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
            if (hasAttribute(NHSDigitalConstants.NHS_ID_CODE)) {
                practitionerRole.getOrganization()
                        .setReference("Organization/"+getAttribute("nhsIDCode"))
                        .setIdentifier(new Identifier().setSystem(NHSDigitalConstants.ODSCode).setValue(getAttribute("nhsIDCode")));

            }
            if (hasAttribute(NHSDigitalConstants.NHS_ROLES)) {
                CodeableConcept code = practitionerRole.addCode();

                code.addCoding().setSystem(NHSDigitalConstants.SDSRoleCode);

                if (hasAttribute(NHSDigitalConstants.NHS_ROLES)) {

                    String[] roles = getAttribute(NHSDigitalConstants.NHS_ROLES).split(":");
                    if (roles[2] != null) {
                        code.getCodingFirstRep().setDisplay(roles[2].replace("\"",""));
                    }
                }
            }
            addIdentifiers(practitionerRole);
            if (hasAttribute("o")) {
                practitionerRole.getOrganization().setDisplay(getAttribute("o"));
            }


            return practitionerRole;
        }

        private void addIdentifiers(PractitionerRole practitionerRole) {

            List<Practitioner> practitioners = practitionerDao.search(new TokenParam().setValue(getAttribute("uid")),null,null);
            practitionerRole.getPractitioner()
                    .setReference("Practitioner/"+getAttribute("uid"));
            if (practitioners.size()>0) {
                for (Identifier identifier : practitioners.get(0).getIdentifier()) {
                    if (!identifier.getValue().equals(getAttribute("uid"))) {
                        practitionerRole.getPractitioner().setIdentifier(identifier);
                    }
                }
            }
        }

        public boolean hasAttribute(String attrib) {
            return (attributes.get(attrib)!= null);
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

        if (!practitioners.isEmpty()) {
            return practitioners.get(0);
        }
        return null;
    }

    public List<PractitionerRole> search( TokenParam identifier,ReferenceParam practitioner, ReferenceParam organisation) {

        String ldapFilter = "";

        if (identifier != null) {
            log.info(identifier.getValue());
            ldapFilter = ldapFilter + "("+ NHSDigitalConstants.NHS_GNC+"="+identifier.getValue()+")";
        }
        if (practitioner != null) {

            if (practitioner.getChain() == null) {
                log.trace(practitioner.getValue());
                ldapFilter = ldapFilter + "(uid=" + practitioner.getValue() + ")";
            } else {
                String chain =practitioner.getChain();
                switch (chain) {
                    case "name" :


                            ldapFilter = ldapFilter + "(cn=*"+practitioner.getValue()+"*)";
                            break;
                    case "family" :


                        ldapFilter = ldapFilter + "(sn=*"+practitioner.getValue()+"*)";
                        break;
                    default :
                        break;
                }
            }
        }
        if (organisation != null) {
            log.info(organisation.getValue());
            ldapFilter = ldapFilter + "("+ NHSDigitalConstants.NHS_ID_CODE+"="+organisation.getValue()+")";
        }
        if (ldapFilter.isEmpty()) {
            return Collections.emptyList();
        }
        ldapFilter = "(&(objectclass=nhsOrgPerson)"+ldapFilter+")";
        log.info("ldapFilter= {}", ldapFilter);
        return ldapTemplate.search("ou=People", ldapFilter, new PractitionerRoleAttributesMapper());
    }

}
