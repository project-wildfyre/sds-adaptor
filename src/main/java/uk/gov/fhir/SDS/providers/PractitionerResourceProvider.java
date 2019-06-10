package uk.gov.fhir.SDS.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
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
import uk.gov.fhir.SDS.support.ProviderResponseLibrary;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;


@Component
public class PractitionerResourceProvider implements IResourceProvider {


    @Autowired
    FhirContext ctx;

    @Autowired
    CamelContext context;


    private static final Logger log = LoggerFactory.getLogger(PractitionerResourceProvider.class);

    @Override
    public Class<Practitioner> getResourceType() {
        return Practitioner.class;
    }


    @Read
    public Practitioner read(HttpServletRequest request,@IdParam IdType internalId) throws Exception {


        ProducerTemplate template = context.createProducerTemplate();

        Practitioner practitioner = null;
        IBaseResource resource = null;
        Reader reader = null;
        try {
            InputStream inputStream = null;
            if (request != null) {
                inputStream = (InputStream) template.sendBody("direct:FHIRPractitioner",
                        ExchangePattern.InOut, request);
            } else {
                Exchange exchange = template.send("direct:FHIRPractitioner",ExchangePattern.InOut, new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setBody("(uid=test)");
                    }
                });
                inputStream = (InputStream) exchange.getIn().getBody();
            }
            reader = new InputStreamReader(inputStream);
            if (reader != null) {
                resource = ctx.newJsonParser().parseResource(reader);
            }
        } catch(Exception ex) {
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
        if (resource instanceof Practitioner) {
            practitioner = (Practitioner) resource;
        } else {
            ProviderResponseLibrary
                    .createException(ctx,resource);
        }

        return practitioner;

    }
    @Search
    public List<Practitioner> search(HttpServletRequest request,
                                  @OptionalParam(name = Practitioner.SP_IDENTIFIER) TokenParam identifier
    ) throws Exception {

       return null;
    }


}
