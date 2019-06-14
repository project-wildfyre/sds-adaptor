package uk.gov.wildfyre.SDS;

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



    }


}
