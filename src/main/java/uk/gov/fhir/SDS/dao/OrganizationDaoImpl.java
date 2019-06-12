package uk.gov.fhir.SDS.dao;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import java.util.List;

@Component
public class OrganizationDaoImpl {

    @Autowired
    private LdapTemplate ldapTemplate;

    private static final Logger log = LoggerFactory.getLogger(OrganizationDaoImpl.class);

    private class OrganizationAttributesMapper implements AttributesMapper {

        javax.naming.directory.Attributes attributes;
        @Override
        public Object mapFromAttributes(javax.naming.directory.Attributes attributes) throws NamingException {
            this.attributes = attributes;
            Organization organisation = new Organization();
            if (hasAttribute("uniqueidentifier")) {
                organisation.setId(getAttribute("uniqueidentifier"));
            } else {
                return null;
            }

            if (hasAttribute("nhsIDCode")) {
                organisation.addIdentifier()
                        .setSystem("https://fhir.nhs.uk/Id/sds-user-id")
                        .setValue(getAttribute("nhsIDCode"));
            }

            return organisation;
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

    public Organization read(IdType internalId) {



        log.info(internalId.getIdPart());
        List<Organization> organisations = ldapTemplate.search("ou=People", "(&(objectclass=nhsPerson)(uid="+internalId.getIdPart()+"))", new OrganizationAttributesMapper());

        if (organisations.size()>0) {
            return organisations.get(0);
        }
        return null;
    }


    public List<Organization> search(TokenParam identifier,
                                             StringParam name) {

        String ldapFilter = "";
        if (identifier != null) {

            ldapFilter = ldapFilter + "(nhsIDCode="+identifier.getValue()+")";
            log.info(ldapFilter);
        }

        if (ldapFilter.isEmpty()) return null;
        ldapFilter = "(&(objectClass=nhsAS)"+ldapFilter+")";
        return ldapTemplate.search("ou=Services", ldapFilter, new OrganizationAttributesMapper());
    }

}
