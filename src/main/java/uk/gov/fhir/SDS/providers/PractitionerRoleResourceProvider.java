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
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.fhir.SDS.dao.PractitionerRoleDaoImpl;
import uk.gov.fhir.SDS.support.ProviderResponseLibrary;

import javax.naming.directory.SearchResult;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
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


        ProducerTemplate template = context.createProducerTemplate();

        PractitionerRole practitionerRole = null;
        IBaseResource resource = null;
        Reader reader = null;
        try {
            InputStream inputStream = null;


            log.info("camelStart");
            Exchange exchange = template.send("direct:LDAPPractitionerRole", ExchangePattern.InOut, new Processor() {
                public void process(Exchange exchange) throws Exception {
                    exchange.getIn().setBody("(sn=dbwgbwdia)");
                }
            });
            log.info("camelEnd");
            if (exchange.getIn().getBody() instanceof InputStream) {
                inputStream = (InputStream) exchange.getIn().getBody();
                reader = new InputStreamReader(inputStream);
            }
            if (exchange.getIn().getBody() instanceof String) {
                log.info("Body = " + exchange.getIn().getBody());
            }
            if (exchange.getIn().getBody() instanceof Collection) {
                Collection<SearchResult> data = (Collection) exchange.getIn().getBody();
                log.info("Body = " + exchange.getIn().getBody());
                log.info("Body = " + data.toString());
            }

            if (reader != null) {
                resource = ctx.newJsonParser().parseResource(reader);
            }
        } catch (Exception ex) {
            if (reader != null) {
                StringBuilder buffer = new StringBuilder();
                char[] arr = new char[8 * 1024];
                int numCharsRead;
                while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
                    buffer.append(arr, 0, numCharsRead);
                }
                reader.close();
                log.error("Output = " + buffer.toString());
            }
            log.error("Apace Camel request error: " + ex.getMessage());
            throw new InternalErrorException(ex.getMessage());
        }
        if (resource instanceof PractitionerRole) {
            practitionerRole = (PractitionerRole) resource;
        } else {
            ProviderResponseLibrary
                    .createException(ctx, resource);
        }

        return practitionerRole;

    }





}
