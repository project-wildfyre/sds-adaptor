package uk.gov.wildfyre.sds.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.wildfyre.sds.dao.OrganizationDaoImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@Component
public class OrganizationResourceProvider implements IResourceProvider {


    @Autowired
    FhirContext ctx;

    @Autowired
    OrganizationDaoImpl organisationDao;

    @Override
    public Class<Organization> getResourceType() {
        return Organization.class;
    }


    @Read
    public Organization read(HttpServletRequest request, @IdParam IdType internalId) {

        return organisationDao.read(true, internalId);

    }

    @Search
    public List<Organization> search(// HttpServletRequest request,
                                     @OptionalParam(name = Organization.SP_IDENTIFIER)  TokenParam identifier,
                                     @OptionalParam(name = Organization.SP_NAME) StringParam name,
                                     @OptionalParam(name = Organization.SP_PARTOF) ReferenceParam partOf,
                                     @OptionalParam(name = Organization.SP_ADDRESS_POSTALCODE) StringParam postCode,
                                     @OptionalParam(name = Organization.SP_ADDRESS) StringParam address,
                                     @OptionalParam(name = Organization.SP_TYPE) TokenParam type,
                                     @OptionalParam(name = Organization.SP_ACTIVE) TokenParam active
    )  {


        return organisationDao.search(true, identifier,  name, partOf, postCode, address, type, active);
    }


}
