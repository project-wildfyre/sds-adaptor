package uk.gov.wildfyre.sds.support;

public final class NHSDigitalConstants {

    private NHSDigitalConstants(){

    }

    public static final  class IdentifierSystem {

        private IdentifierSystem () {}

        public static final String ASID =  "https://fhir.nhs.uk/Ids/ASID";

        public static final String NHS_ID_CODE = "https://fhir.nhs.uk/Ids/nhsIDCode";

        public static final String NHS_MHS_SN = "https://fhir.nhs.uk/Ids/nhsMHsSN";

        public static final String NHS_SVC_IA = "https://fhir.nhs.uk/Ids/nhsMhsSvcIA";

    }

    public static final String NHS_ID_CODE = "nhsIDCode";

    public static final String NHS_ROLES = "nhsRoles";

    public static final String NHS_GNC = "nhsGNC";

    public static final String OU_SERVICE = "ou=Services";

    public static final String OU_ORGANISATIONS ="ou=Organisations";

    public static final String NHS_PARENT_ORG_CODE ="nhsParentOrgCode";

    public static final String GIVEN_NAME = "givenName";

    public static final String ODSCode = "https://fhir.nhs.uk/Id/ods-organization-code";

    public static final String SDSUserId = "https://fhir.nhs.uk/Id/sds-user-id";

    public static final String SDSUserRoleProfileId = "https://fhir.nhs.uk/Id/sds-role-profile-id";

    public static final String SDSRoleCode = "https://fhir.hl7.org.uk/CodeSystem/UKCore-SDSJobRoleName";

    public static final String GMPNumber = "https://fhir.hl7.org.uk/Id/gmp-number";
    public static final String GMCNumber = "https://fhir.hl7.org.uk/Id/gmc-number";
}
