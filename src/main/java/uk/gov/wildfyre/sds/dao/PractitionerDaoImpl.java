package uk.gov.wildfyre.sds.dao;


import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;
import uk.gov.wildfyre.sds.support.NHSDigitalConstants;

import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class PractitionerDaoImpl {

    @Autowired
    private LdapTemplate ldapTemplate;

    @Autowired
    private PractitionerRoleDaoImpl practitionerRoleDao;

    private static final Logger log = LoggerFactory.getLogger(PractitionerDaoImpl.class);

    private class PractitionerAttributesMapper implements AttributesMapper {

        javax.naming.directory.Attributes attributes;
        @Override
        public Object mapFromAttributes(javax.naming.directory.Attributes attributes) throws NamingException {
            this.attributes = attributes;
            Practitioner practitioner = new Practitioner();
            if (hasAttribute("uid")) {
                practitioner.setId(getAttribute("uid"));
                practitioner.addIdentifier()
                        .setSystem(NHSDigitalConstants.SDSUserId)
                        .setValue(getAttribute("uid"));
            } else {
                return null;
            }
            if (hasAttribute("sn") || hasAttribute(NHSDigitalConstants.GIVEN_NAME)) {
                HumanName name = practitioner.addName();

                if (hasAttribute("sn")) {
                        name.setFamily(getAttribute("sn"));
                }
                if (hasAttribute(NHSDigitalConstants.GIVEN_NAME)) {
                        name.addGiven(getAttribute(NHSDigitalConstants.GIVEN_NAME));
                }
                if (hasAttribute("personalTitle")) {
                    name.addPrefix(getAttribute("personalTitle"));
                }
                if (hasAttribute("cn")) {
                    name.setText(getAttribute("cn"));
                }
            }

            if (hasAttribute("nhsOCSPRCode") && getAttribute("nhsOCSPRCode").startsWith("G")) {
                practitioner.addIdentifier()
                        .setSystem(NHSDigitalConstants.GMPNumber)
                        .setValue(getAttribute("nhsOCSPRCode"));
            }

            if (hasAttribute(NHSDigitalConstants.NHS_CONSULTANT)) {
                practitioner.addIdentifier()
                        .setSystem(NHSDigitalConstants.GMCNumber)
                        .setValue(getAttribute(NHSDigitalConstants.NHS_CONSULTANT));
            }

            return practitioner;
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

    public Practitioner read(IdType internalId) {



        log.info(internalId.getIdPart());
        List<Practitioner> practitioners = ldapTemplate.search("ou=People", "(&(objectclass=nhsPerson)(uid="+internalId.getIdPart()+"))", new PractitionerAttributesMapper());

        if (!practitioners.isEmpty()) {
            return practitioners.get(0);
        }
        return null;
    }


    public List<Practitioner> search(TokenParam identifier,
                                             StringParam surname, StringParam name) {

        String ldapFilter = "";
        if (identifier != null) {
            log.info(identifier.getValue());
            /*
            if (identifier.getValue().startsWith("C")) {
               // This is wrong
                ldapFilter = ldapFilter + "(nhsConsultant=*)";
            } else if (identifier.getValue().startsWith("G")) {
                // This is wrong
                ldapFilter = ldapFilter + "(nhsOCSPRCode=*)";
            } else {
                ldapFilter = ldapFilter + "(uid="+identifier.getValue()+")";
            }*/
            ldapFilter = ldapFilter + "(uid="+identifier.getValue()+")";
        }
        if (surname != null) {
            log.info(surname.getValue());
            ldapFilter = ldapFilter + "(sn=*"+surname.getValue()+"*)";
        }
        if (name != null) {
            log.info(name.getValue());
            ldapFilter = ldapFilter + "(cn=*"+name.getValue()+"*)";
        }
        if (ldapFilter.isEmpty()) return Collections.emptyList();
        ldapFilter = "(&(objectclass=nhsPerson)"+ldapFilter+")";
        return ldapTemplate.search("ou=People", ldapFilter, new PractitionerAttributesMapper());
    }

}
