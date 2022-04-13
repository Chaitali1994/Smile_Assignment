import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.util.StopWatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.impl.client.BasicResponseHandler;
import org.checkerframework.common.reflection.qual.GetClass;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
//import org.hl7.fhir.r4.formats.IParser;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SampleClient {

	public static void main(String[] theArgs) throws IOException {

		// Create a FHIR client
		FhirContext fhirContext = FhirContext.forR4();
		IGenericClient client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
		client.registerInterceptor(new LoggingInterceptor(false));

		int[] avgTime = { 0, 0, 0 };

		StopWatch counter = new StopWatch();

		IClientInterceptor interceptor = new IClientInterceptor() {

			@Override
			public void interceptResponse(IHttpResponse arg0) throws IOException {
				// TODO Auto-generated method stub
				counter.endCurrentTask();
			}

			@Override
			public void interceptRequest(IHttpRequest arg0) {
				// TODO Auto-generated method stub
				counter.restart();
			}
		};

		client.registerInterceptor(interceptor);

		// IParser parser = fhirContext.newJsonParser();

		// Search for Patient resources
		Bundle response = client.search().forResource("Patient").where(Patient.FAMILY.matches().value("SMITH")).sort()
				.ascending(Patient.NAME).returnBundle(Bundle.class).execute();
		
		// Basic Task

		for (int i = 0; i < response.getEntry().size(); i++) {
			System.out.println("Patient: " + response.getEntry().get(i).getChildByName("GIVEN"));
		}
		
		// INTERMEDIATE TASK

		for (int i = 0; i < 3; i++) {
			System.out.println("==== Running loop for count: " + i + " ====");
			File file = null;
			try {
				file = new SampleClient().getFileFromResource();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			BufferedReader br = new BufferedReader(new FileReader(file));
			String st;

			while ((st = br.readLine()) != null) {
				// Print the string
				client.search().forResource("Patient").where(Patient.GIVEN.matches().value(st))
						.returnBundle(Bundle.class).cacheControl(new CacheControlDirective().setNoCache(true))
						.execute();
				avgTime[i] += counter.getMillis();
			}
			System.out.println("==== Loop End ====");
		}

		for (int i = 0; i < avgTime.length; i++) {
			System.out.println("Average time for request " + (i + 1) + "took: "
					+ (Long.parseLong(String.valueOf(avgTime[i])) / 20f) + "ms");
		}
	}

	private File getFileFromResource() throws Exception {
		URL resource = getClass().getClassLoader().getResource("patients.txt");
		if (resource == null) {
			throw new IllegalAccessException("File not found.");
		} else {
			return new File(resource.toURI());
		}

	}

}
