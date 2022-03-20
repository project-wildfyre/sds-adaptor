package uk.gov.wildfyre.sds.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.wildfyre.sds.dao.PractitionerDaoImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@Component
public class PractitionerResourceProvider implements IResourceProvider {


    @Autowired
    FhirContext ctx;

    @Autowired
    PractitionerDaoImpl practitionerDao;

    @Override
    public Class<Practitioner> getResourceType() {
        return Practitioner.class;
    }


    @Read
    public Practitioner read(HttpServletRequest request, @IdParam IdType internalId) {

        return practitionerDao.read(internalId);

    }

    @Search
    public List<Practitioner> search(HttpServletRequest request,
                                     @OptionalParam(name = Practitioner.SP_IDENTIFIER)  TokenParam identifier,
                                     @OptionalParam(name = Practitioner.SP_FAMILY) StringParam surname,
                                     @OptionalParam(name = Practitioner.SP_NAME) StringParam name
    )  {
        return practitionerDao.search(identifier, surname, name);
    }


}
