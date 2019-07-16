package uk.gov.wildfyre.SDS.providers;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.RestulfulServerConfiguration;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.dstu3.hapi.rest.server.ServerCapabilityStatementProvider;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.json.JSONObject;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.gov.wildfyre.SDS.HapiProperties;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Date;

// 16/June/2019
public class ConformanceProvider extends ServerCapabilityStatementProvider {

    private boolean myCache = true;
    private volatile CapabilityStatement capabilityStatement;

    private RestulfulServerConfiguration serverConfiguration;

    private RestfulServer restfulServer;

    private JSONObject openIdObj;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConformanceProvider.class);


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
    public CapabilityStatement getServerConformance(HttpServletRequest theRequest) {

        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(theRequest.getServletContext());
        log.info("restful2 Server not null = " + ctx.getEnvironment().getProperty("ccri.validate_flag"));


        if (capabilityStatement != null && myCache) {
            return capabilityStatement;
        }
        CapabilityStatement capabilityStatement = super.getServerConformance(theRequest);

        capabilityStatement.setPublisher("NHS Digital & DWP Digital");
        capabilityStatement.setDateElement(conformanceDate());
        capabilityStatement.setFhirVersion(FhirVersionEnum.DSTU3.getFhirVersionString());
        capabilityStatement.setAcceptUnknown(CapabilityStatement.UnknownContentCode.EXTENSIONS); // TODO: make this configurable - this is a fairly big
        // effort since the parser
        // needs to be modified to actually allow it

        capabilityStatement.getImplementation().setDescription(serverConfiguration.getImplementationDescription());
        capabilityStatement.setKind(CapabilityStatement.CapabilityStatementKind.INSTANCE);


        capabilityStatement.getSoftware().setName(HapiProperties.getSoftwareName());
        capabilityStatement.getSoftware().setVersion(HapiProperties.getSoftwareVersion());
        capabilityStatement.getImplementation().setDescription(HapiProperties.getSoftwareImplementationDesc());
        capabilityStatement.getImplementation().setUrl(HapiProperties.getSoftwareImplementationUrl());

        // KGM only add if not already present
        if (capabilityStatement.getImplementationGuide().size() == 0) {
            capabilityStatement.getImplementationGuide().add(new UriType(HapiProperties.getSoftwareImplementationGuide()));
        }
        capabilityStatement.setPublisher("NHS Digital & DWP Digital");

        if (restfulServer != null) {
            log.info("restful Server not null");
            for (CapabilityStatement.CapabilityStatementRestComponent nextRest : capabilityStatement.getRest()) {
                nextRest.setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);

                if (HapiProperties.getSecurityOauth()) {

                    nextRest.getSecurity()
                            .addService().addCoding()
                            .setSystem("http://hl7.org/fhir/restful-security-service")
                            .setDisplay("SMART-on-FHIR")
                            .setSystem("SMART-on-FHIR");

                    if (HapiProperties.getSecurityOpenidConfig() != null) {
                        Extension securityExtension = nextRest.getSecurity().addExtension()
                                .setUrl("http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris");
                        HttpClient client = getHttpClient();
                        HttpGet request = new HttpGet(HapiProperties.getSecurityOpenidConfig());
                        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                        request.setHeader(HttpHeaders.ACCEPT, "application/json");
                        if (openIdObj == null) {
                            try {

                                HttpResponse response = client.execute(request);
                                //System.out.println(response.getStatusLine());
                                if (response.getStatusLine().toString().contains("200")) {
                                    InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
                                    BufferedReader bR = new BufferedReader(reader);
                                    String line = "";

                                    StringBuilder responseStrBuilder = new StringBuilder();
                                    while ((line = bR.readLine()) != null) {

                                        responseStrBuilder.append(line);
                                    }
                                    openIdObj = new JSONObject(responseStrBuilder.toString());
                                }
                            } catch (UnknownHostException e) {
                                System.out.println("Host not known");
                            } catch (Exception ex) {
                                System.out.println(ex.getMessage());
                            }
                        }
                        if (openIdObj != null) {
                            if (openIdObj.has("token_endpoint")) {
                                securityExtension.addExtension()
                                        .setUrl("token")
                                        .setValue(new UriType(openIdObj.getString("token_endpoint")));
                            }
                            if (openIdObj.has("authorization_endpoint")) {
                                securityExtension.addExtension()
                                        .setUrl("authorize")
                                        .setValue(new UriType(openIdObj.getString("authorization_endpoint")));
                            }
                            if (openIdObj.has("register_endpoint")) {
                                securityExtension.addExtension()
                                        .setUrl("register")
                                        .setValue(new UriType(openIdObj.getString("register_endpoint")));
                            }
                        }
                    } else {
                        if (HapiProperties.getSecurityOauth2Authorize() != null && HapiProperties.getSecurityOauth2Register() != null && HapiProperties.getSecurityOauth2Token() != null) {

                            Extension securityExtension = nextRest.getSecurity().addExtension()
                                    .setUrl("http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris");

                            securityExtension.addExtension()
                                    .setUrl("authorize")
                                    .setValue(new UriType(HapiProperties.getSecurityOauth2Authorize()));

                            securityExtension.addExtension()
                                    .setUrl("register")
                                    .setValue(new UriType(HapiProperties.getSecurityOauth2Register()));

                            securityExtension.addExtension()
                                    .setUrl("token")
                                    .setValue(new UriType(HapiProperties.getSecurityOauth2Token()));
                        }
                    }
                }
                log.trace("restful Server not null");

                for (CapabilityStatement.CapabilityStatementRestResourceComponent restResourceComponent : nextRest.getResource()) {

                    if (restResourceComponent.getType().equals("OperationDefinition")) {
                        nextRest.getResource().remove(restResourceComponent);
                        break;
                    }
                    if (restResourceComponent.getType().equals("StructureDefinition")) {
                        nextRest.getResource().remove(restResourceComponent);
                        break;
                    }
                    // Start of CRUD operations

                    log.trace("restResourceComponent.getType - " + restResourceComponent.getType());
                    for (IResourceProvider provider : restfulServer.getResourceProviders()) {

                        log.trace("Provider Resource - " + provider.getResourceType().getSimpleName());
                        if (restResourceComponent.getType().equals(provider.getResourceType().getSimpleName())
                                || (restResourceComponent.getType().contains("List") && provider.getResourceType().getSimpleName().contains("List")))
                            if (provider instanceof ICCResourceProvider) {
                                log.trace("ICCResourceProvider - " + provider.getClass());
                                ICCResourceProvider resourceProvider = (ICCResourceProvider) provider;

                                Extension extension = restResourceComponent.getExtensionFirstRep();
                                if (extension == null) {
                                    extension = restResourceComponent.addExtension();
                                }
                                extension.setUrl("http://hl7api.sourceforge.net/hapi-fhir/res/extdefs.html#resourceCount")
                                        .setValue(new DecimalType(resourceProvider.count()));
                            }
                    }
                }
            }
        }


        return capabilityStatement;
    }

    private HttpClient getHttpClient() {
        final HttpClient httpClient = HttpClientBuilder.create().build();
        return httpClient;
    }

    private DateTimeType conformanceDate() {
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
