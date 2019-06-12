package uk.gov.fhir.SDS.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.apache.camel.*;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.fhir.SDS.dao.PractitionerRoleDaoImpl;

import javax.servlet.http.HttpServletRequest;

import java.util.List;


@Component
public class PractitionerRoleResourceProvider implements IResourceProvider {


    @Autowired
    FhirContext ctx;

    @Autowired
    CamelContext context;

    @Autowired
    PractitionerRoleDaoImpl practitionerRoleDao;


    private static final Logger log = LoggerFactory.getLogger(PractitionerRoleResourceProvider.class);

    @Override
    public Class<PractitionerRole> getResourceType() {
        return PractitionerRole.class;
    }


    @Read
    public PractitionerRole read(HttpServletRequest request, @IdParam IdType internalId) throws Exception {

        return practitionerRoleDao.read(internalId);

    }

    @Search
    public List<PractitionerRole> search(HttpServletRequest request,
                                         @OptionalParam(name = Practitioner.SP_IDENTIFIER)  TokenParam identifier,
                                     @OptionalParam(name = PractitionerRole.SP_PRACTITIONER) ReferenceParam practitioner


    ) throws Exception {

        log.info("boing Start");
        return practitionerRoleDao.search(identifier, practitioner);
    }



}
