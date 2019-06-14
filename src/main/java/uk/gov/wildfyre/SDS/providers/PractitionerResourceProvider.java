package uk.gov.wildfyre.SDS.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.apache.camel.*;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.wildfyre.SDS.dao.PractitionerDaoImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@Component
public class PractitionerResourceProvider implements IResourceProvider {


    @Autowired
    FhirContext ctx;

    @Autowired
    CamelContext context;

    @Autowired
    PractitionerDaoImpl practitionerDao;


    private static final Logger log = LoggerFactory.getLogger(PractitionerResourceProvider.class);

    @Override
    public Class<Practitioner> getResourceType() {
        return Practitioner.class;
    }


    @Read
    public Practitioner read(HttpServletRequest request, @IdParam IdType internalId) throws Exception {

        return practitionerDao.read(internalId);

    }

    @Search
    public List<Practitioner> search(HttpServletRequest request,
                                     @OptionalParam(name = Practitioner.SP_IDENTIFIER)  TokenParam identifier,
                                     @OptionalParam(name = Practitioner.SP_FAMILY) StringParam surname,
                                     @OptionalParam(name = Practitioner.SP_NAME) StringParam name
    ) throws Exception {


        return practitionerDao.search(identifier, surname, name);
    }


}
