/*
 * FIXME: This is an example, remove after usage.
 */

package se.hms.argos.afutest;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.annotation.Configuration;

import se.hms.argos.afutest.boot.ArgosTestConfig;
import se.hms.argos.afutest.boot.ArgosTestServer;
import se.hms.argos.common.owner.utils.OwnerUtils;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

@Configuration
public class BasicArgosTestTest
{
   private static final Log logger = LogFactory.getLog(BasicArgosTestTest.class);

   private static ArgosTestServer server;
   private static ArgosTestConfig config;
   private static String internalBase;
   private static String operationBase;

   // This is how we can override the default configuration. This class
   // could be extracted and used in other test classes as well. If we
   // needed to run tests with varying property values, we could
   // also extend "Mutable", and update the property within the tests.
   public interface TestArgosTestConfig extends ArgosTestConfig
   {
      @Override
      @DefaultValue("4")
      public int serverThreadpoolSizeMax();
   }

   @BeforeClass
   public static void setup() throws Exception
   {
      logger.info("\n\nStarting service for tests\n\n");

      // Setup SLF4J bridge so that anyone using JUL will be redirected to SLF4
      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();

      // Create default configuration and update it with system properties
      config = OwnerUtils.getOrCreate(TestArgosTestConfig.class);

      internalBase = String.format("http://127.0.0.1:%s/afutest/internal", config.serverInternalPort());
      operationBase = String.format("http://127.0.0.1:%s/afutest/operation", config.serverInternalOperationPort());

      RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

      // Create and start server
      server = new ArgosTestServer(config);
      server.start();
   }

   @AfterClass
   public static void teardown() throws Exception
   {
      logger.info("\n\nStopping service after tests\n\n");
      server.stop();
   }

   @Test
   public void serviceInfo()
   {
      given().
            baseUri(operationBase).
            when().
            get("info").
            then().
            statusCode(200).
            contentType(ContentType.JSON).
            body("version", equalTo(config.serviceVersion())).
            body("name", equalTo(config.serviceName()));
   }

   @Test
   public void serviceConfig()
   {
      given().
            baseUri(operationBase).
            when().
            get("info/config").
            then().
            statusCode(200).
            body("\"server_internal_operation_port\"", equalTo(Integer.toString(config.serverInternalOperationPort())));
   }

   @Test
   public void internalDummy()
   {
      String responseText = given().
            baseUri(internalBase).
            when().
            get("dummy").
            then().
            statusCode(200).
            contentType(ContentType.JSON).
            extract().asString();
      Assert.assertEquals("DUMMY", responseText);
   }

   @Test
   public void metricTest()
   {
      given().
            baseUri(internalBase).
            when().
            get("dummy").
            then().
            statusCode(200);
      given().
            baseUri(operationBase).
            when().
            get("diagnostic/metrics").
            then().
            statusCode(200).
            contentType(ContentType.JSON).
            body("counters.\"example.dummy\".count", greaterThanOrEqualTo(1));
   }

   @Test
   public void operationPingTest()
   {
      given().
            baseUri(operationBase).
            when().
            get("diagnostic/ping").
            then().
            statusCode(200).
            contentType(ContentType.TEXT).
            body(notNullValue());
   }

   @Test
   public void operationDiagnosticMetrics()
   {
      given().
            baseUri(operationBase).
            when().
            get("diagnostic/metrics").
            then().
            statusCode(200).
            contentType(ContentType.JSON).
            body("version", equalTo("3.0.0"));
   }

   @Test
   public void operationDiagnosticHealthcheck()
   {
      given().
            baseUri(operationBase).
            when().
            get("diagnostic/healthcheck").
            then().
            contentType(ContentType.JSON).
            body("\"jvm.dead_lock\".healthy", equalTo(true));
   }
}
