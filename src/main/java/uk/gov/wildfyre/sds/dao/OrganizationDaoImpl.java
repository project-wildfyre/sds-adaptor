package uk.gov.wildfyre.sds.dao;

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
import uk.gov.wildfyre.sds.support.NHSDigitalLDAPSpineConstants;

import javax.naming.NamingException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class OrganizationDaoImpl {

    @Autowired
    private LdapTemplate ldapTemplate;

    private static final Logger log = LoggerFactory.getLogger(OrganizationDaoImpl.class);

    private Map<String, String> orgName = new HashMap<>();

    private class OrganizationAttributesMapper implements AttributesMapper {

        OrganizationAttributesMapper(boolean parent) {
            this.parent = parent;
        }

        javax.naming.directory.Attributes attributes;
        private boolean parent;

        @Override
        public Object mapFromAttributes(javax.naming.directory.Attributes attributes) throws NamingException {
            this.attributes = attributes;


            Organization organisation = new Organization();
            if (hasAttribute("uniqueidentifier")) {
                organisation.setId(getAttribute("uniqueidentifier"));
            } else {
                return null;
            }


            if (hasAttribute(NHSDigitalLDAPSpineConstants.NHS_ID_CODE)) {
                organisation.addIdentifier()
                        .setSystem("https://fhir.nhs.uk/Id/ods-organization-code")
                        .setValue(getAttribute(NHSDigitalLDAPSpineConstants.NHS_ID_CODE));
            }

            if (hasAttribute("o")) {
                organisation.setName(getAttribute("o"));
            }
            if (hasAttribute("ou")) {
                organisation.setName(getAttribute("ou"));
            }
            if (organisation.hasName() && hasAttribute(NHSDigitalLDAPSpineConstants.NHS_ID_CODE) && orgName.get(getAttribute(NHSDigitalLDAPSpineConstants.NHS_ID_CODE))!= null) {
                    orgName.put(getAttribute(NHSDigitalLDAPSpineConstants.NHS_ID_CODE),organisation.getName());
            }

            processDates(organisation);

            if (hasAttribute("mail")) {
                organisation.addTelecom()
                        .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                        .setValue(getAttribute("mail"));
            }
            if (hasAttribute("telephoneNumber")) {
                organisation.addTelecom()
                        .setSystem(ContactPoint.ContactPointSystem.PHONE)
                        .setValue(getAttribute("telephoneNumber"));
            }

            if (hasAttribute("nhsOrgType")) {
                CodeableConcept code=new CodeableConcept();
                code.addCoding().setSystem("https://fhir.gov.uk/CodeSystem/OrgType")
                        .setCode(getAttribute("nhsOrgTypeCode"))
                        .setDisplay(getAttribute("nhsOrgType"));
                organisation.addType(code);
            }

            if (hasAttribute("nhsCountry")) {
                Extension country = new Extension();
                country.setUrl("https://fhir.gov.uk/Extension/nhsCountry");
                CodeableConcept code = new CodeableConcept();
                code.addCoding()
                        .setSystem("https://fhir.gov.uk/CodeSystem/UKCountry")
                        .setCode(getAttribute("nhsCountry"));
                country.setValue(code);
                organisation.addExtension(country);
            }


           processAddress(organisation);
           processParentOrg(organisation);

            return organisation;
        }

        private void processAddress(Organization organisation) {
            Address address = organisation.addAddress();
            if (hasAttribute("postalCode")) {
                address.setPostalCode(getAttribute("postalCode"));
            }
            if (hasAttribute("postalAddress")) {
                String[] lines= getAttribute("postalAddress").split("\\$");
                for (String line : lines) {
                    if (!line.isEmpty()) {
                        address.addLine(line);
                    }
                }

            }
        }
        private void processDates(Organization organisation) {
            Period period = new Period();

            if (hasAttribute("nhsOrgOpenDate")) {
                organisation.setActive(true);
                SimpleDateFormat
                        format = new SimpleDateFormat("yyyymmdd");
                try {
                    period.setStart(format.parse(getAttribute("nhsOrgOpenDate")));
                } catch (Exception ignore) {
                    // No action
                }
            }
            if (hasAttribute("nhsOrgCloseDate")) {
                organisation.setActive(false);
                SimpleDateFormat
                        format = new SimpleDateFormat("yyyymmdd");
                try {
                    period.setEnd(format.parse(getAttribute("nhsOrgCloseDate")));
                } catch (Exception ignore) {
                        // No action
                }
            }

        }

        private void processParentOrg(Organization organisation) {
            if (hasAttribute(NHSDigitalLDAPSpineConstants.NHS_PARENT_ORG_CODE)) {
                organisation.getPartOf().setReference("Organization/"+getAttribute(NHSDigitalLDAPSpineConstants.NHS_PARENT_ORG_CODE));
                organisation.getPartOf().setIdentifier(
                        new Identifier().setSystem("https://fhir.nhs.uk/Id/ods-organization-code")
                                .setValue(getAttribute(NHSDigitalLDAPSpineConstants.NHS_PARENT_ORG_CODE))
                );
                if (this.parent) {

                    if (orgName.get(getAttribute(NHSDigitalLDAPSpineConstants.NHS_PARENT_ORG_CODE))!= null) {
                        organisation.getPartOf().setDisplay(orgName.get(getAttribute(NHSDigitalLDAPSpineConstants.NHS_PARENT_ORG_CODE)));
                    } else {
                        Organization parentOrg = read(false, new IdType(getAttribute(NHSDigitalLDAPSpineConstants.NHS_PARENT_ORG_CODE)));

                        if (parentOrg != null && parentOrg.hasName()) {
                            orgName.put(getAttribute(NHSDigitalLDAPSpineConstants.NHS_PARENT_ORG_CODE),parentOrg.getName());
                            organisation.getPartOf().setDisplay(parentOrg.getName());
                        }
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

    public Organization read(boolean getParent, IdType internalId) {


        log.info(internalId.getIdPart());
        List<Organization> organisations = ldapTemplate.search(NHSDigitalLDAPSpineConstants.OU_ORGANISATIONS, "(&(uniqueIdentifier="+internalId.getIdPart()+"))", new OrganizationAttributesMapper(getParent));

        if (!organisations.isEmpty()) {
            return organisations.get(0);
        }
        return null;
    }


    public List<Organization> search(boolean getParent, TokenParam identifier,
                                             StringParam name,
                                     ReferenceParam partOf,
                                     StringParam postCode,
                                     StringParam address,
                                     TokenParam type,
                                     TokenParam active) {

        String ldapFilter = "";
        if (identifier != null) {
            ldapFilter = ldapFilter + "(nhsIDCode="+identifier.getValue()+")";
            log.info(ldapFilter);
        }
        if (name != null) {
            if (name.getValue() != null) {
                ldapFilter = ldapFilter + "(|(ou=*" + name.getValue() + "*)(o=*" + name.getValue() + "*))";
                log.info(ldapFilter);
            } else {
                return Collections.emptyList();
            }
        }
        String base = NHSDigitalLDAPSpineConstants.OU_ORGANISATIONS;
        if (partOf != null) {
            getParent= false; // Don't return partOf in the resources
            base = "uniqueIdentifier="+partOf.getValue() +", " + base;
            ldapFilter = ldapFilter + "(!(nhsIDCode="+partOf.getValue()+"))";
            log.info(base);
            log.info(ldapFilter);
        }
        if (postCode != null) {
            ldapFilter = ldapFilter + "(postalCode=*"+postCode.getValue()+"*)";
            log.info(ldapFilter);
        }
        if (address != null) {
            ldapFilter = ldapFilter + "(postalAddress=*"+address.getValue()+"*)";
            log.info(ldapFilter);
        }
        if (type != null) {
            ldapFilter = ldapFilter + "(nhsOrgTypeCode="+type.getValue()+")";
            log.info(ldapFilter);
        }
        if (active != null) {
            switch (active.getValue()) {
                case "true":
                    ldapFilter = ldapFilter + "(!(nhsOrgCloseDate=*))";
                    break;
                case "false":
                    ldapFilter = ldapFilter + "(nhsOrgCloseDate=*)";
                    break;
                default:
                    break;
            }

            log.info(ldapFilter);
        }
        return runSearch(ldapFilter, getParent, active, base);
    }

    private List<Organization> runSearch(String ldapFilter, boolean getParent,
                                          TokenParam active, String base) {

        if (ldapFilter.isEmpty()) return Collections.emptyList();
        String ldapFilterOrg = "(&"+ldapFilter+")";

        List<Organization> orgs = ldapTemplate.search(base, ldapFilterOrg, new OrganizationAttributesMapper(getParent));

        if (active != null) {
            List<Organization> nOrgs = new ArrayList<>();

            for (Organization org : orgs) {
                switch (active.getValue()) {
                    case "true":
                        if (org.getActive()) {
                            nOrgs.add(org);
                        }
                        break;
                    case "false":
                        if (!org.getActive()) {
                            nOrgs.add(org);
                        }
                        break;
                    default:
                        break;
                }
            }
            return nOrgs;
        }
        return orgs;
    }

}
