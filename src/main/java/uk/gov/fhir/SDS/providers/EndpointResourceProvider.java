package uk.gov.fhir.SDS.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.apache.camel.CamelContext;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.fhir.SDS.dao.EndpointDaoImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@Component
public class EndpointResourceProvider implements IResourceProvider {


    @Autowired
    FhirContext ctx;

    @Autowired
    CamelContext context;

    @Autowired
    EndpointDaoImpl endpointDao;


    private static final Logger log = LoggerFactory.getLogger(EndpointResourceProvider.class);

    @Override
    public Class<Endpoint> getResourceType() {
        return Endpoint.class;
    }


    @Read
    public Endpoint read(HttpServletRequest request, @IdParam IdType internalId) throws Exception {

        return endpointDao.read(internalId);

    }

    @Search
    public List<Endpoint> search(HttpServletRequest request,
                                     @OptionalParam(name = Endpoint.SP_IDENTIFIER)  TokenParam identifier
    ) throws Exception {


        return endpointDao.search(identifier);
    }

}
