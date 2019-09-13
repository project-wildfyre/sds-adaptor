package uk.gov.wildfyre.sds.providers;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.RestfulServerConfiguration;
import org.hl7.fhir.dstu3.hapi.rest.server.ServerCapabilityStatementProvider;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.gov.wildfyre.sds.HapiProperties;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;


public class ConformanceProvider extends ServerCapabilityStatementProvider {

    private boolean myCache = true;

    private RestfulServerConfiguration
            serverConfiguration;

    private CapabilityStatement capabilityStatement;

    private RestfulServer restfulServer;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConformanceProvider.class);

    private String oauth2authorize ;
    private String oauth2token ;
    private String oauth2register ;
    private String oauth2 ;

    public ConformanceProvider() {
        super();
    }

    @Override
    public void setRestfulServer(RestfulServer theRestfulServer) {

        serverConfiguration = theRestfulServer.createConfiguration();
        restfulServer = theRestfulServer;
        super.setRestfulServer(theRestfulServer);
    }


    @Override
    @Metadata
    public CapabilityStatement getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {

        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(theRequest.getServletContext());
        log.info("restful2 Server not null = {}", ctx.getEnvironment().getProperty("ccri.validate_flag"));

        oauth2authorize = ctx.getEnvironment().getProperty("ccri.oauth2.authorize");
        oauth2token = ctx.getEnvironment().getProperty("ccri.oauth2.token");
        oauth2register = ctx.getEnvironment().getProperty("ccri.oauth2.register");
        oauth2 = ctx.getEnvironment().getProperty("ccri.oauth2");

        if (this.capabilityStatement != null && myCache) {
            return this.capabilityStatement;
        }

        this.capabilityStatement = super.getServerConformance(theRequest, theRequestDetails);

        coreStatement();

        if (restfulServer != null) {
            log.info("restful Server not null");

            processRest();
            log.trace("restful Server not null");

            processRestResources();
        }

        return capabilityStatement;
    }

    private void coreStatement() {
        capabilityStatement.setDateElement(conformanceDate());
        capabilityStatement.setFhirVersion(FhirVersionEnum.DSTU3.getFhirVersionString());
        capabilityStatement.setAcceptUnknown(CapabilityStatement.UnknownContentCode.EXTENSIONS);
        // effort since the parser
        // needs to be modified to actually allow it

        capabilityStatement.setKind(CapabilityStatement.CapabilityStatementKind.INSTANCE);

        capabilityStatement.getSoftware().setName(HapiProperties.getSoftwareName());
        capabilityStatement.getSoftware().setVersion(HapiProperties.getSoftwareVersion());
        capabilityStatement.getImplementation().setDescription(HapiProperties.getSoftwareImplementationDesc());
        capabilityStatement.getImplementation().setUrl(HapiProperties.getSoftwareImplementationUrl());

        // KGM only add if not already present
        if (capabilityStatement.getImplementationGuide().isEmpty()) {
            capabilityStatement.getImplementationGuide().add(new UriType(HapiProperties.getSoftwareImplementationGuide()));
        }
        capabilityStatement.setPublisher("NHS Digital & DWP Digital");

        capabilityStatement.setStatus(Enumerations.PublicationStatus.ACTIVE);
    }
    private void processRest() {
        for (CapabilityStatement.CapabilityStatementRestComponent nextRest : capabilityStatement.getRest()) {
            nextRest.setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);

            // KGM only add if not already present
            if (nextRest.hasSecurity() && nextRest.getSecurity().getService().isEmpty() && oauth2.equals("true")
                    && oauth2token != null && oauth2register != null && oauth2authorize != null) {
                nextRest.getSecurity()
                        .addService().addCoding()
                        .setSystem("http://hl7.org/fhir/restful-security-service")
                        .setDisplay("SMART-on-FHIR")
                        .setSystem("SMART-on-FHIR");
                Extension securityExtension = nextRest.getSecurity().addExtension()
                        .setUrl("http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris");

                securityExtension.addExtension()
                        .setUrl("authorize")
                        .setValue(new UriType(oauth2authorize));

                securityExtension.addExtension()
                        .setUrl("register")
                        .setValue(new UriType(oauth2register));

                securityExtension.addExtension()
                        .setUrl("token")
                        .setValue(new UriType(oauth2token));
            }

        }
    }

    private void processRestResources() {
        for (CapabilityStatement.CapabilityStatementRestComponent nextRest : capabilityStatement.getRest()) {
            for (CapabilityStatement.CapabilityStatementRestResourceComponent restResourceComponent : nextRest.getResource()) {

                if (restResourceComponent.getType().equals("OperationDefinition") || restResourceComponent.getType().equals("StructureDefinition")) {
                    nextRest.getResource().remove(restResourceComponent);
                    break;
                }

                // Start of CRUD operations

                log.trace("restResourceComponent.getType - {}", restResourceComponent.getType());
                for (IResourceProvider provider : restfulServer.getResourceProviders()) {
                    procProvider(provider, restResourceComponent);

                }
            }
        }
    }

    private void procProvider (IResourceProvider provider, CapabilityStatement.CapabilityStatementRestResourceComponent restResourceComponent) {
        log.trace("Provider Resource - {}", provider.getResourceType().getSimpleName());
        if ((restResourceComponent.getType().equals(provider.getResourceType().getSimpleName())
                || (restResourceComponent.getType().contains("List") && provider.getResourceType().getSimpleName().contains("List")))
                && (provider instanceof ICCResourceProvider)) {
            log.trace("ICCResourceProvider - {}", provider.getClass());
            ICCResourceProvider resourceProvider = (ICCResourceProvider) provider;

            Extension extension = restResourceComponent.getExtensionFirstRep();
            if (extension == null) {
                extension = restResourceComponent.addExtension();
            }
            extension.setUrl("http://hl7api.sourceforge.net/hapi-fhir/res/extdefs.html#resourceCount")
                    .setValue(new DecimalType(resourceProvider.count()));
        }

    }

    public DateTimeType conformanceDate() {
        IPrimitiveType<Date> buildDate = serverConfiguration.getConformanceDate();
        if (buildDate != null) {
            try {
                return new DateTimeType(buildDate.getValue());
            } catch (DataFormatException e) {
                // fall through
            }
        }
        return DateTimeType.now();
    }


}
