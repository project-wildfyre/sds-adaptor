package uk.gov.fhir.SDS.dao;

import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.dstu3.model.*;
import org.jsoup.select.Evaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import java.util.List;

@Component
public class EndpointDaoImpl {

    @Autowired
    private LdapTemplate ldapTemplate;


    private static final Logger log = LoggerFactory.getLogger(EndpointDaoImpl.class);

    private class EndpointAttributesMapper implements AttributesMapper {

        javax.naming.directory.Attributes attributes;
        @Override
        public Object mapFromAttributes(javax.naming.directory.Attributes attributes) throws NamingException {
            this.attributes = attributes;
            Endpoint endpoint = new Endpoint();
            if (hasAttribute("uniqueidentifier")) {
                endpoint.setId(getAttribute("uniqueidentifier"));
            } else {
                return null;
            }
            if (hasAttribute("nhsMHSPartyKey")) {
                endpoint.addIdentifier().setValue(getAttribute("nhsMHSPartyKey")).setSystem("https://fhir.nhs.uk/Ids/nhsMHSPartyKey");


            }
            if (hasAttribute("nhsIDCode")) {
                endpoint.getManagingOrganization()
                        .setIdentifier(
                                new Identifier().setValue(getAttribute("nhsIDCode")).setSystem("https://fhir.nhs.uk/Ids/nhsIDCode")
                        );

            }
            if (hasAttribute("nhsMHsSN")) {
                Coding code = new Coding();
                code.setCode(getAttribute("nhsMHsSN"))
                .setSystem("https://fhir.nhs.uk/Ids/nhsMHsSN");

                endpoint.getConnectionType().addExtension(
                        new Extension()
                        .setUrl("https://fhir.nhs.uk/Extension/nhsMHsSN")
                                .setValue(code)
                );
            }
            if (hasAttribute("nhsMhsSvcIA")) {

                Coding code = new Coding();
                code.setCode(getAttribute("nhsMhsSvcIA"))
                        .setSystem("https://fhir.nhs.uk/Ids/nhsMhsSvcIA");

                endpoint.getConnectionType().addExtension(
                        new Extension()
                                .setUrl("https://fhir.nhs.uk/Extension/nhsMhsSvcIA")
                                .setValue(code)
                );
            }
            if (hasAttribute("nhsEPInteractionType")) {
                switch (getAttribute("nhsEPInteractionType")) {
                    case "HL7" :
                        endpoint.getConnectionType().setSystem("http://terminology.hl7.org/CodeSystem/endpoint-connection-type").setCode(getAttribute("hl7v3"));
                        break;

                }
            }
            if (hasAttribute("nhsMhsEndPoint")) {
                endpoint.setAddress(getAttribute("nhsMhsEndPoint"));
            }


            return endpoint;
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

    public Endpoint read(IdType internalId) {



        log.info(internalId.getIdPart());
        List<Endpoint> endpoints = ldapTemplate.search("ou=Services", "(&(objectclass=nhsMhs)(uniqueIdentifier="+internalId.getIdPart()+"))", new EndpointAttributesMapper());

        if (endpoints.size()>0) {
            return endpoints.get(0);
        }
        return null;
    }


    public List<Endpoint> search(TokenParam identifier, ReferenceParam organisation) {

        String ldapFilter = "";

        if (identifier != null) {
            log.info(identifier.getValue());
            ldapFilter = ldapFilter + "(nhsIDCode="+identifier.getValue()+")";
        }
        if (organisation != null) {
            log.info(organisation.getValue());
            ldapFilter = ldapFilter + "(nhsIDCode="+organisation.getValue()+")";
        }
        if (ldapFilter.isEmpty()) return null;
        ldapFilter = "(&(objectclass=nhsMhs)"+ldapFilter+")";
        return ldapTemplate.search("ou=Services", ldapFilter, new EndpointAttributesMapper());
    }

}
