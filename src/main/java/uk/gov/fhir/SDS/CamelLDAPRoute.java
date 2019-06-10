package uk.gov.fhir.SDS;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.SimpleRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import java.io.InputStream;
import java.util.Properties;

@Component
public class CamelLDAPRoute extends RouteBuilder {

	@Autowired
	protected Environment env;

    @Override
    public void configure() 
    {

        log.info("Camel configure");

        Properties
                props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        props.setProperty(Context.PROVIDER_URL, "ldap://192.168.128.11:389");
        props.setProperty(Context.URL_PKG_PREFIXES, "com.sun.jndi.url");
        props.setProperty(Context.REFERRAL, "ignore");
       // props.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
       // props.setProperty(Context.SECURITY_PRINCIPAL, "cn=Manager");
       // props.setProperty(Context.SECURITY_CREDENTIALS, "secret");

        SimpleRegistry reg = new SimpleRegistry();
        try {
            reg.put("myldap", new InitialLdapContext(props, null));
        } catch (NamingException ex) {
          log.error(ex.getMessage());
        }



        from("direct:FHIRPPractitioner")
                .routeId("Practitioner")
                .log("Practitioner call")
                //.to("ldap:myldap?base=ou=test")
                .to("log:uk.gov.fhir.sds?level=INFO&showHeaders=true&showExchangeId=true")
                .convertBodyTo(InputStream.class);


    }


}
