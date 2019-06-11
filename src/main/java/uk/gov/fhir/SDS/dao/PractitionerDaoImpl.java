package uk.gov.fhir.SDS.dao;

import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;
import uk.gov.fhir.SDS.SDSAdaptor;

import javax.naming.NamingException;
import java.util.List;

@Component
public class PractitionerDaoImpl {

    @Autowired
    private LdapTemplate ldapTemplate;

    private static final Logger log = LoggerFactory.getLogger(PractitionerDaoImpl.class);

    private class PractitonerAttributesMapper implements AttributesMapper {

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
            if (hasAttribute("sn") && hasAttribute("cn")) {
                practitioner.addName()
                        .setFamily(getAttribute("sn"))
                        .addGiven(getAttribute("cn"));
            }
            if (hasAttribute("nhsOCSPRCode")) {
                practitioner.addIdentifier()
                        .setSystem("https://fhir.nhs.uk/Id/sds-user-id")
                        .setValue(getAttribute("nhsOCSPRCode"));
            }

            return practitioner;
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

    public Practitioner read(IdType internalId) {



        log.info(internalId.getIdPart());
        List<Practitioner> practitioners = ldapTemplate.search("ou=People", "(&(objectclass=nhsPerson)(uid="+internalId.getIdPart()+"))", new PractitonerAttributesMapper());

        if (practitioners.size()>0) {
            return practitioners.get(0);
        }
        return null;
    }


    public List<Practitioner> search(TokenParam identifier,
                                             StringParam surname) {

        if (identifier != null) {
            log.info(identifier.getValue());
            return ldapTemplate.search("ou=People", "(&(objectclass=nhsPerson)(nhsOCSPRCode=*"+identifier.getValue()+"*))", new PractitonerAttributesMapper());
        }
        if (surname != null) {
            log.info(surname.getValue());
            return ldapTemplate.search("ou=People", "(&(objectclass=nhsPerson)(sn=*"+surname.getValue()+"*))", new PractitonerAttributesMapper());
        }
        return ldapTemplate.search("ou=People", "(objectclass=person)", new PractitonerAttributesMapper());
    }

}
