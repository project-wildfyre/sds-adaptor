package uk.gov.wildfyre.sds.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.wildfyre.sds.dao.EndpointDaoImpl;

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
                                 @OptionalParam(name = Endpoint.SP_IDENTIFIER)  TokenParam identifier,
                                 @OptionalParam(name = Endpoint.SP_ORGANIZATION) ReferenceParam organisation,

                                 @OptionalParam(name = "interaction")  TokenParam interaction
    )  {


        return endpointDao.search(identifier, organisation,interaction);
    }

}
