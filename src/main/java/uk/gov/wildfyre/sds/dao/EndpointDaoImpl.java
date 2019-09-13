package uk.gov.wildfyre.sds.dao;

import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.dstu3.model.*;
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
public class EndpointDaoImpl {

    @Autowired
    private LdapTemplate ldapTemplate;
    


    private static final Logger log = LoggerFactory.getLogger(EndpointDaoImpl.class);

    private class SDS {
        private String asid;

        private String nhsIDCode;

        public String getASID() {
            return asid;
        }

        public void setASID(String asid) {
            this.asid = asid;
        }

        public String getNhsIDCode() {
            return nhsIDCode;
        }

        public void setNhsIDCode(String nhsIDCode) {
            this.nhsIDCode = nhsIDCode;
        }
    }

    private class EndpointAsAttributesMapper implements AttributesMapper {

        javax.naming.directory.Attributes attributes;
        @Override
        public Object mapFromAttributes(javax.naming.directory.Attributes attributes) throws NamingException {
            this.attributes = attributes;

            SDS sds = new SDS();

            if (hasAttribute(NHSDigitalLDAPSpineConstants.NHS_ID_CODE)) {
                sds.setNhsIDCode(getAttribute(NHSDigitalLDAPSpineConstants.NHS_ID_CODE));
            }
            if (hasAttribute("uniqueIdentifier")) {
                sds.setASID(getAttribute("uniqueIdentifier"));
            }
            return sds;
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

    private class EndpointAttributesMapper implements AttributesMapper {

        javax.naming.directory.Attributes attributes;

        String asid = null;

        EndpointAttributesMapper(String asid) {
            this.asid = asid;
        }
        
        @Override
        public Object mapFromAttributes(javax.naming.directory.Attributes attributes) throws NamingException {
            this.attributes = attributes;
            Endpoint endpoint = new Endpoint();
            if (hasAttribute("uniqueidentifier")) {
                endpoint.setId(getAttribute("uniqueidentifier"));
            } else {
                return null;
            }

            if (asid != null) {
                endpoint.addIdentifier().setValue(asid).setSystem(NHSDigitalLDAPSpineConstants.IdentifierSystem.ASID);
            }
            if (hasAttribute("nhsMHSPartyKey")) {
                endpoint.addIdentifier().setValue(getAttribute("nhsMHSPartyKey")).setSystem("https://fhir.nhs.uk/Ids/nhsMHSPartyKey");


            }
            if (hasAttribute(NHSDigitalLDAPSpineConstants.NHS_ID_CODE)) {
                endpoint.getManagingOrganization()
                        .setReference("Organization/"+getAttribute(NHSDigitalLDAPSpineConstants.NHS_ID_CODE))
                        .setIdentifier(
                                new Identifier().setValue(getAttribute(NHSDigitalLDAPSpineConstants.NHS_ID_CODE)).setSystem(NHSDigitalLDAPSpineConstants.IdentifierSystem.NHS_ID_CODE)
                        );

            }
            if (hasAttribute("nhsMHsSN")) {
                Coding code = new Coding();
                code.setCode(getAttribute("nhsMHsSN"))
                .setSystem(NHSDigitalLDAPSpineConstants.IdentifierSystem.NHS_MHS_SN);

                endpoint.getConnectionType().addExtension(
                        new Extension()
                        .setUrl("https://fhir.nhs.uk/Extension/nhsMHsSN")
                                .setValue(code)
                );
            }
            if (hasAttribute("nhsMhsSvcIA")) {

                Coding code = new Coding();
                code.setCode(getAttribute("nhsMhsSvcIA"))
                        .setSystem(NHSDigitalLDAPSpineConstants.IdentifierSystem.NHS_SVC_IA);

                endpoint.getConnectionType().addExtension(
                        new Extension()
                                .setUrl("https://fhir.nhs.uk/Extension/nhsMhsSvcIA")
                                .setValue(code)
                );
            }
            if (hasAttribute("nhsEPInteractionType") && getAttribute("nhsEPInteractionType").equals("HL7")) {
                    endpoint.getConnectionType().setSystem("http://terminology.hl7.org/CodeSystem/endpoint-connection-type").setCode(getAttribute("hl7v3"));
            }
            if (hasAttribute("nhsMhsEndPoint")) {
                endpoint.setAddress(getAttribute("nhsMhsEndPoint"));
            }


            return endpoint;
        }

        public boolean hasAttribute(String attrib) {
            return attributes.get(attrib)!= null;
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
        List<Endpoint> endpoints = ldapTemplate.search(NHSDigitalLDAPSpineConstants.OU_SERVICE, "(&(objectclass=nhsMhs)(uniqueIdentifier="+internalId.getIdPart()+"))", new EndpointAttributesMapper(null));

        if (!endpoints.isEmpty()) {
            Endpoint endpoint = endpoints.get(0);
            String filter =  "("+NHSDigitalLDAPSpineConstants.NHS_ID_CODE+ "=" + endpoint.getManagingOrganization().getReference() + ")";

            filter = "(&(objectclass=nhsAs)" + filter + ")";
            List<SDS> sds = ldapTemplate.search(NHSDigitalLDAPSpineConstants.OU_SERVICE, filter, new EndpointAsAttributesMapper());
            if (!sds.isEmpty()) {
                String asid = sds.get(0).getASID();
                endpoint.addIdentifier()
                        .setValue(asid)
                        .setSystem(NHSDigitalLDAPSpineConstants.IdentifierSystem.ASID);
            }

            return endpoint;
        }
        return null;
    }


    public List<Endpoint> search(TokenParam identifier, ReferenceParam organisation, TokenParam interaction) {
        String asid = null;
        //  nhsAs search
        String filter = "";
        if (identifier != null) {
            if (identifier.getSystem()!=null && identifier.getSystem().equals(NHSDigitalLDAPSpineConstants.IdentifierSystem.ASID)) {
                log.info(identifier.getValue());
                filter =  filter + "(uniqueIdentifier=" + identifier.getValue() + ")";
            }
        } else {
            if (organisation != null) {
                log.info(organisation.getValue());

                filter = filter + "("+NHSDigitalLDAPSpineConstants.NHS_ID_CODE+ "=" + organisation.getValue() + ")";

                List<SDS> sds = ldapTemplate.search(NHSDigitalLDAPSpineConstants.OU_SERVICE, filter, new EndpointAsAttributesMapper());
                if (!sds.isEmpty()) {
                    asid = sds.get(0).getASID();
                    log.info(asid);
                }
            } else {
                if (interaction != null) {
                    filter = filter + "(nhsMhsSvcIA=" + interaction.getValue() + ")";
                }
            }
        }

        if (filter.isEmpty()) {
            return Collections.emptyList();
        }
        filter = "(&(objectclass=nhsAs)" + filter + ")";
        log.info("nhsAs filter (&(objectclass=nhsAs)= {}", filter);


        //  nhsMhs search
        return runldapSearch(identifier, organisation,interaction, filter);
    }
    private List<Endpoint> runldapSearch(TokenParam identifier, ReferenceParam organisation, TokenParam interaction,  String filter) {
        String asid = null;
        List<SDS> sds = ldapTemplate.search(NHSDigitalLDAPSpineConstants.OU_SERVICE, filter, new EndpointAsAttributesMapper());
        if (!sds.isEmpty()) {
            if (identifier !=null) {
                organisation = new ReferenceParam().setValue(sds.get(0).getNhsIDCode());
            }
            asid = sds.get(0).getASID();
            log.info(asid);
        } else {
            return Collections.emptyList();
        }

        String ldapFilter = "";

        if (organisation != null) {
            ldapFilter = ldapFilter + "("+NHSDigitalLDAPSpineConstants.NHS_ID_CODE+ "="+organisation.getValue()+")";
        }
        if (interaction != null) {
            ldapFilter = ldapFilter + "(nhsMhsSvcIA="+interaction.getValue()+")";
        }

        log.info("nhsMhs filter (&(objectclass=nhsMhs)= {}", ldapFilter);
        if (ldapFilter.isEmpty()) return Collections.emptyList();
        ldapFilter = "(&(objectclass=nhsMhs)"+ldapFilter+")";

        return ldapTemplate.search(NHSDigitalLDAPSpineConstants.OU_SERVICE, ldapFilter, new EndpointAttributesMapper(asid));
    }
}
