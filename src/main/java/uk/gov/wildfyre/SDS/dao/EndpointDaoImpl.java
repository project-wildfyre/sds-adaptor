package uk.gov.wildfyre.SDS.dao;

import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import java.util.List;

@Component
public class EndpointDaoImpl {

    @Autowired
    private LdapTemplate ldapTemplate;


    private static final Logger log = LoggerFactory.getLogger(EndpointDaoImpl.class);

    private class SDS {
        private String ASID;

        private String nhsIDCode;

        public String getASID() {
            return ASID;
        }

        public void setASID(String ASID) {
            this.ASID = ASID;
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

            NamingEnumeration e = attributes.getAll();

// Loop through the attributes
            while (e.hasMoreElements())
            {
// Get the next attribute
                Attribute attr = (Attribute) e.nextElement();

// Print out the attribute's value(s)
                System.out.print(attr.getID()+" = ");
                for (int i=0; i < attr.size(); i++)
                {
                    if (i > 0) System.out.print(", ");
                    System.out.print(attr.get(i));
                }
                System.out.println();
            }
            SDS sds = new SDS();

            if (hasAttribute("nhsIDCode")) {
                sds.setNhsIDCode(getAttribute("nhsIDCode"));
            }
            if (hasAttribute("uniqueIdentifier")) {
                sds.setASID(getAttribute("uniqueIdentifier"));
            }
            return sds;
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

    private class EndpointAttributesMapper implements AttributesMapper {

        javax.naming.directory.Attributes attributes;

        String ASID = null;

        EndpointAttributesMapper(String ASID) {
            this.ASID = ASID;
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

            if (ASID != null) {
                endpoint.addIdentifier().setValue(ASID).setSystem("https://fhir.nhs.uk/Ids/ASID");
            }
            if (hasAttribute("nhsMHSPartyKey")) {
                endpoint.addIdentifier().setValue(getAttribute("nhsMHSPartyKey")).setSystem("https://fhir.nhs.uk/Ids/nhsMHSPartyKey");


            }
            if (hasAttribute("nhsIDCode")) {
                endpoint.getManagingOrganization()
                        .setReference("Organization/"+getAttribute("nhsIDCode"))
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
        List<Endpoint> endpoints = ldapTemplate.search("ou=Services", "(&(objectclass=nhsMhs)(uniqueIdentifier="+internalId.getIdPart()+"))", new EndpointAttributesMapper(null));

        if (endpoints.size()>0) {
            Endpoint endpoint = endpoints.get(0);
            String filter =  "(nhsIDCode=" + endpoint.getManagingOrganization().getReference() + ")";

            filter = "(&(objectclass=nhsAs)" + filter + ")";
            List<SDS> sds = ldapTemplate.search("ou=Services", filter, new EndpointAsAttributesMapper());
            if (sds.size()>0) {
                String ASID = sds.get(0).getASID();
                endpoint.addIdentifier()
                        .setValue(ASID)
                        .setSystem("https://fhir.nhs.uk/Ids/ASID");
            }

            return endpoint;
        }
        return null;
    }


    public List<Endpoint> search(TokenParam identifier, ReferenceParam organisation, TokenParam interaction) {



        String ASID = null;

        //  nhsAs search
        String filter = "";
        if (identifier != null) {
            if (identifier.getSystem()!=null && identifier.getSystem().equals("https://fhir.nhs.uk/Ids/ASID")) {
                log.info(identifier.getValue());
                filter =  filter + "(uniqueIdentifier=" + identifier.getValue() + ")";

            }
        } else {
            if (organisation != null) {
                log.info(organisation.getValue());

                filter = filter + "(nhsIDCode=" + organisation.getValue() + ")";

                List<SDS> sds = ldapTemplate.search("ou=Services", filter, new EndpointAsAttributesMapper());
                if (sds.size() > 0) {
                    ASID = sds.get(0).getASID();
                    log.info(ASID);
                }
            } else {
                if (interaction != null) {
                    filter = filter + "(nhsMhsSvcIA=" + interaction.getValue() + ")";
                }
            }
        }

        if (filter.isEmpty()) {
            return null;
        }
        filter = "(&(objectclass=nhsAs)" + filter + ")";
        log.info("nhsAs filter (&(objectclass=nhsAs)= "+filter);
        List<SDS> sds = ldapTemplate.search("ou=Services", filter, new EndpointAsAttributesMapper());
        if (sds.size()>0) {
            if (identifier !=null) {
                organisation = new ReferenceParam().setValue(sds.get(0).getNhsIDCode());
            }
            ASID = sds.get(0).getASID();
            log.info(ASID);
        } else {
            return null;
        }

        //  nhsMhs search

        String ldapFilter = "";

        if (organisation != null) {
              ldapFilter = ldapFilter + "(nhsIDCode="+organisation.getValue()+")";
        }
        if (interaction != null) {
            ldapFilter = ldapFilter + "(nhsMhsSvcIA="+interaction.getValue()+")";
        }

        log.info("nhsMhs filter (&(objectclass=nhsMhs)= "+ldapFilter);
        if (ldapFilter.isEmpty()) return null;
        ldapFilter = "(&(objectclass=nhsMhs)"+ldapFilter+")";
        return ldapTemplate.search("ou=Services", ldapFilter, new EndpointAttributesMapper(ASID));
    }

}
