package uk.gov.wildfyre.sds.dao;


import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;
import uk.gov.wildfyre.sds.support.NHSDigitalLDAPSpineConstants;

import javax.naming.NamingException;
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
            } else {
                return null;
            }
            if (hasAttribute("sn") || hasAttribute(NHSDigitalLDAPSpineConstants.GIVEN_NAME)) {
                HumanName name = practitioner.addName();

                if (hasAttribute("sn")) {
                        name.setFamily(getAttribute("sn"));
                }
                if (hasAttribute(NHSDigitalLDAPSpineConstants.GIVEN_NAME)) {
                        name.addGiven(getAttribute(NHSDigitalLDAPSpineConstants.GIVEN_NAME));
                }
                if (hasAttribute("personalTitle")) {
                    name.addPrefix(getAttribute("personalTitle"));
                }
                if (hasAttribute("cn")) {
                    name.setText(getAttribute("cn"));
                }
            }
            if (hasAttribute("nhsOCSPRCode")) {
                practitioner.addIdentifier()
                        .setSystem("https://fhir.nhs.uk/Id/sds-user-id")
                        .setValue(getAttribute("nhsOCSPRCode"));
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
            List<PractitionerRole> roles = practitionerRoleDao.search(identifier,null, null);
            if (roles.isEmpty()) return Collections.emptyList();

            PractitionerRole role = roles.get(0);
            if (!role.hasPractitioner() || !role.getPractitioner().hasReference()) return Collections.emptyList();

            String[] ids = role.getPractitioner().getReference().split("/");
            if (ids[1]== null) return Collections.emptyList();
            ldapFilter = ldapFilter + "(uid="+ids[1]+")";
            log.info(ldapFilter);
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
