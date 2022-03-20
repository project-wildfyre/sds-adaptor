package uk.gov.wildfyre.sds.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.wildfyre.sds.dao.EndpointDaoImpl;
import uk.gov.wildfyre.sds.support.NHSDigitalConstants;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@Component
public class EndpointResourceProvider implements IResourceProvider {


    @Autowired
    FhirContext ctx;

    @Autowired
    EndpointDaoImpl endpointDao;


    @Override
    public Class<Endpoint> getResourceType() {
        return Endpoint.class;
    }


    @Read
    public Endpoint read(HttpServletRequest request, @IdParam IdType internalId) {

        return endpointDao.read(internalId);

    }

    @Search
    public List<Endpoint> search(HttpServletRequest request,
                                 @OptionalParam(name = Endpoint.SP_IDENTIFIER)  TokenParam tokenParam,
                                 @OptionalParam(name = Endpoint.SP_ORGANIZATION) ReferenceParam organisation
    )  {
        TokenParam asid = null;
        TokenParam interaction = null;
        if (tokenParam != null) {
            if (tokenParam.getSystem() != null) {
                if (tokenParam.getSystem().equals(NHSDigitalConstants.IdentifierSystem.ASID)) {
                    asid = tokenParam;
                }
                if (tokenParam.getSystem().equals(NHSDigitalConstants.IdentifierSystem.NHS_SVC_IA)) {
                    interaction = tokenParam;
                }
            } else {
                interaction = tokenParam;
            }
        }

        return endpointDao.search(asid, organisation,interaction);
    }

}
