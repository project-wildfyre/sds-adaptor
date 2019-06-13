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

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Component
public class OrganizationDaoImpl {

    @Autowired
    private LdapTemplate ldapTemplate;

    private static final Logger log = LoggerFactory.getLogger(OrganizationDaoImpl.class);

    private class OrganizationAttributesMapper implements AttributesMapper {

        OrganizationAttributesMapper(boolean parent) {
            this.parent = parent;
        }

        javax.naming.directory.Attributes attributes;
        private Boolean parent;

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
                        .setSystem("https://fhir.nhs.uk/Id/ods-organization-code")
                        .setValue(getAttribute("nhsIDCode"));
            }

            if (hasAttribute("o")) {
                organisation.setName(getAttribute("o"));
            }
            if (hasAttribute("ou")) {
                organisation.setName(getAttribute("ou"));
            }

            Period period = new Period();

            if (hasAttribute("nhsOrgOpenDate")) {
                organisation.setActive(true);
                SimpleDateFormat
                        format = new SimpleDateFormat("yyyymmdd");
                try {
                    period.setStart(format.parse(getAttribute("nhsOrgOpenDate")));
                } catch (Exception ex) {

                }
            }
            if (hasAttribute("nhsOrgCloseDate")) {
                organisation.setActive(false);
                SimpleDateFormat
                        format = new SimpleDateFormat("yyyymmdd");
                try {
                    period.setEnd(format.parse(getAttribute("nhsOrgCloseDate")));
                } catch (Exception ex) {

                }
            }

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
            if (hasAttribute("nhsParentOrgCode")) {
                organisation.getPartOf().setReference("Organization/"+getAttribute("nhsParentOrgCode"));
                organisation.getPartOf().setIdentifier(
                        new Identifier().setSystem("https://fhir.nhs.uk/Id/ods-organization-code")
                                .setValue(getAttribute("nhsParentOrgCode"))
                );
                if (this.parent) {

                    Organization parent = read(false, new IdType(getAttribute("nhsParentOrgCode")));
                    if (parent != null && parent.hasName()) {
                        organisation.getPartOf().setDisplay(parent.getName());
                    }
                }
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

    public Organization read(boolean getParent, IdType internalId) {


        log.info(internalId.getIdPart());
        List<Organization> organisations = ldapTemplate.search("ou=Organisations", "(&(uniqueIdentifier="+internalId.getIdPart()+"))", new OrganizationAttributesMapper(getParent));

        if (organisations.size()>0) {
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

        String base = "ou=Organisations";
        String ldapFilter = "";
        if (identifier != null) {
            ldapFilter = ldapFilter + "(nhsIDCode="+identifier.getValue()+")";
            log.info(ldapFilter);
        }
        if (name != null) {
            ldapFilter = ldapFilter + "(|(ou=*"+name.getValue()+"*)(o=*"+name.getValue()+"*))";
            log.info(ldapFilter);
        }
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

        if (ldapFilter.isEmpty()) return null;
        String ldapFilterOrg = "(&"+ldapFilter+")";

        List<Organization> orgs = ldapTemplate.search(base, ldapFilterOrg, new OrganizationAttributesMapper(getParent));

        if (active != null) {
            List<Organization> nOrgs = new ArrayList<>();

            for (Organization org : orgs)
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
            }
            return nOrgs;
        }
        return orgs;
    }

}
