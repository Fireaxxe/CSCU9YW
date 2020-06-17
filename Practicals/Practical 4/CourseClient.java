import java.io.StringReader;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import javax.xml.ws.*;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;

import org.w3c.dom.*;

/**
  RESTful web client to manage a list of courses.
*/
public class CourseClient {

  private static final String SERVICE_PATH = "/stirling/courses/";
  private static final QName SERVICE_QNAME = new QName("urn:Courses", "");
  private static final String SERVICE_URI = "http://localhost:8182" + SERVICE_PATH;

  Transformer messageTransformer;
  private static Dispatch<Source> serviceDispatcher;

	public CourseClient() throws Exception {
		Service service = Service.create(SERVICE_QNAME);
		service.addPort(SERVICE_QNAME, HTTPBinding.HTTP_BINDING, SERVICE_URI);
		serviceDispatcher = service.createDispatch(SERVICE_QNAME, Source.class, Service.Mode.MESSAGE);
		messageTransformer = TransformerFactory.newInstance().newTransformer();
	}

  /**
    Call course service with given method for course code and name (optional).

    @param method	HTTP method ("DELETE"/"GET"/"POST"/"PUT")
    @param arguments	course code and name, with parameters used as follows:
			  "DELETE",<code> (delete course);
			  "GET" (list all courses);
			  "GET",<code> (list course)
			  "POST",<code>,<name> (create course);
			  "PUT",<code>,<name> (update course)
  */
	private void invoke(String method, String... arguments) {
		try {
			String code = "";
			String name = "";
			switch (arguments.length) {
			case 2:
				name = arguments[1];
			case 1:
				code = arguments[0];
			}
			String courses = "<courses/>";
			if (method.equals("DELETE")) {
				System.out.println("delete course:   " + code);
			} else if (method.equals("GET")) {
				System.out.println("retrieve course: " + (code.equals("") ? "All" : code));
			} else if (method.equals("POST")) {
				System.out.println("create course:   " + code + " = " + name);
				courses = "<courses><course>" + name + "</course></courses>";
			} else if (method.equals("PUT")) {
				System.out.println("update course:   " + code + " = " + name);
				courses = "<courses><course>" + name + "</course></courses>";
			} else
				throw (new Exception("unrecognised method: " + method));

			Source requestSource = new StreamSource(new StringReader(courses));
			Map<String, Object> requestContext = serviceDispatcher.getRequestContext();
			requestContext.put(MessageContext.HTTP_REQUEST_METHOD, method);
			requestContext.put(MessageContext.PATH_INFO, SERVICE_PATH + code);
			Source responseSource = serviceDispatcher.invoke(requestSource);

			if (method.equals("GET")) {
				DOMResult domXML = new DOMResult();
				messageTransformer.transform(responseSource, domXML);
				Node topNode = domXML.getNode();
				Element coursesNode = (Element) domXML.getNode().getFirstChild();
				NodeList children = coursesNode.getChildNodes();
				for (int i = 0; i < children.getLength(); i++) {
					Element course = (Element) children.item(i);
					name = course.getFirstChild().getNodeValue();
					System.out.println("  course name:   " + name);
				}
			}
		} catch (Exception exception) {
			if (!(exception instanceof WebServiceException))
				exception.printStackTrace();
		}

		// analyse HTTP response
		Map<String, Object> responseContext = serviceDispatcher.getResponseContext();
		int httpResponse = (Integer) responseContext.get(MessageContext.HTTP_RESPONSE_CODE);
		String httpResult = httpResponse == 200 ? "success"
				: httpResponse == 201 ? "created"
				: httpResponse == 404 ? "does not exist"
				: httpResponse == 405 ? "already exists" 
				: httpResponse + "";
		System.out.println("request result:  " + httpResult + "\n");
	}

  public static void main(String arguments[]) throws Exception {
    CourseClient client = new CourseClient();

    client.invoke("POST", "CSCU9YW", "Web Services UG");
    client.invoke("POST", "ITNP02B", "Web Services PG");
    client.invoke("PUT", "CSCU9YW", "Web Services");
    client.invoke("PUT", "ITNP02B", "Communication Systems and Services");
    client.invoke("GET");
    client.invoke("GET", "CSCU9YW");
  }

}
