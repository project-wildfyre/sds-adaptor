package uk.gov.fhir.SDS;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.SimpleRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class CamelLDAPRoute extends RouteBuilder {

	@Autowired
	protected Environment env;

    @Override
    public void configure() 
    {





        from("direct:LDAPPractitioner")
                .routeId("Practitioner")
                .log("Practitioner call")
                .to("ldap:fhirldap?base=o=nhs")
                .to("log:uk.gov.fhir.sds?level=INFO&showHeaders=true&showExchangeId=true");

    }


}
