package uk.gov.fhir.SDS.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.apache.camel.*;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.fhir.SDS.dao.PractitionerDaoImpl;
import uk.gov.fhir.SDS.support.ProviderResponseLibrary;

import javax.naming.directory.SearchResult;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
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

        log.info("boing Start");
        if (name != null) {
            surname = name;
        }
        return practitionerDao.search(identifier, surname);
    }


}
