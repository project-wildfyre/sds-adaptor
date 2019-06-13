package uk.gov.fhir.SDS.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.apache.camel.CamelContext;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.fhir.SDS.dao.OrganizationDaoImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@Component
public class OrganizationResourceProvider implements IResourceProvider {


    @Autowired
    FhirContext ctx;

    @Autowired
    CamelContext context;

    @Autowired
    OrganizationDaoImpl organisationDao;


    private static final Logger log = LoggerFactory.getLogger(OrganizationResourceProvider.class);

    @Override
    public Class<Organization> getResourceType() {
        return Organization.class;
    }


    @Read
    public Organization read(HttpServletRequest request, @IdParam IdType internalId) throws Exception {

        return organisationDao.read(true, internalId);

    }

    @Search
    public List<Organization> search(HttpServletRequest request,
                                     @OptionalParam(name = Organization.SP_IDENTIFIER)  TokenParam identifier,
                                     @OptionalParam(name = Organization.SP_NAME) StringParam name,
                                     @OptionalParam(name = Organization.SP_PARTOF) ReferenceParam partOf,
                                     @OptionalParam(name = Organization.SP_ADDRESS_POSTALCODE) StringParam postCode,
                                     @OptionalParam(name = Organization.SP_ADDRESS) StringParam address,
                                     @OptionalParam(name = Organization.SP_TYPE) TokenParam type,
                                     @OptionalParam(name = Organization.SP_ACTIVE) TokenParam active
    ) throws Exception {


        return organisationDao.search(true, identifier,  name, partOf, postCode, address, type, active);
    }


}
