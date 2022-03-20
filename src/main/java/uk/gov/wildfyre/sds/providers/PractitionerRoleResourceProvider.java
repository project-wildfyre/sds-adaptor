package uk.gov.wildfyre.sds.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.wildfyre.sds.dao.PractitionerRoleDaoImpl;

import javax.servlet.http.HttpServletRequest;

import java.util.List;


@Component
public class PractitionerRoleResourceProvider implements IResourceProvider {


    @Autowired
    FhirContext ctx;


    @Autowired
    PractitionerRoleDaoImpl practitionerRoleDao;


    @Override
    public Class<PractitionerRole> getResourceType() {
        return PractitionerRole.class;
    }


    @Read
    public PractitionerRole read(HttpServletRequest request, @IdParam IdType internalId)  {

        return practitionerRoleDao.read(internalId);

    }

    @Search
    public List<PractitionerRole> search(HttpServletRequest request,
                                     @OptionalParam(name = Practitioner.SP_IDENTIFIER)  TokenParam identifier,
                                     @OptionalParam(name = PractitionerRole.SP_PRACTITIONER) ReferenceParam practitioner,
                                     @OptionalParam(name = PractitionerRole.SP_ORGANIZATION) ReferenceParam organisation

    )  {

        return practitionerRoleDao.search(identifier, practitioner, organisation);
    }



}
