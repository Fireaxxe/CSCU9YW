import java.io.StringReader;

import javax.annotation.Resource;

import java.util.HashMap;
import java.util.Iterator;

import javax.xml.bind.JAXBContext;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import javax.xml.ws.*;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.Service.Mode;

import org.w3c.dom.*;

/**
  RESTful web service to manage a list of courses.
*/

@WebServiceProvider
@ServiceMode(Mode.MESSAGE)
public class CourseService implements Provider<Source>{

  HashMap<String,String> courseMap = new HashMap<String,String>();
  private JAXBContext jaxbContext;
  MessageContext requestContext;
  Transformer messageTransformer;

  @Resource(type=Object.class)
  protected WebServiceContext serviceContext;

  private static final String serviceURI = "http://localhost:8182/stirling/courses/";

  public CourseService() throws Exception {
    jaxbContext = JAXBContext.newInstance(CourseService.class);
    messageTransformer = TransformerFactory.newInstance().newTransformer();
  }

  /**
    Return XML text for currently defined courses in the form
    "<courses><course>name</course>...</courses>", 
    
    @param code		course code
    @return		XML text for currently defined courses
  */
	private String getCourses(String code) {
		String courses;
		if (code != null) {
			String name = courseMap.get(code);
			if (name != null)
				courses = "<courses><course>" + name + "</course></courses>";
			else
				courses = null;
		} else {
			courses = "<courses>";
			Iterator<String> courseIterator = courseMap.keySet().iterator();
			while (courseIterator.hasNext()) {
				code = (String) courseIterator.next();
				String name = courseMap.get(code);
				courses += "<course>" + name + "</course>";
			}
			courses += "</courses>";
		}
		return (courses);
	}

  /**
    Respond to service invocation.

    @param source		service data source
  */
	public Source invoke(Source source) {

		String courses = "<courses/>";
		int httpResponse = 200; // assume success
		MessageContext requestContext = serviceContext.getMessageContext();
		String method = (String) requestContext.get(MessageContext.HTTP_REQUEST_METHOD);
		String code = (String) requestContext.get(MessageContext.PATH_INFO);

		// analyse service call
		try {
			if (method.equals("DELETE")) {
				String deleted = courseMap.remove(code);
				if (deleted == null)
					httpResponse = 404;
			} else if (method.equals("GET")) {
				String courseList = getCourses(code);
				if (courseList != null)
					courses = courseList;
				else
					httpResponse = 404; // set missing response
			} else if (method.equals("POST")) {
				if (!courseMap.containsKey(code)) {
					DOMResult domXML = new DOMResult();
					messageTransformer.transform(source, domXML);
					Element coursesElement = (Element) domXML.getNode().getFirstChild();
					Element courseElement = (Element) coursesElement.getFirstChild();
					String name = courseElement.getFirstChild().getNodeValue();
					courseMap.put(code, name);
					httpResponse = 201;
				} else {
					httpResponse = 405;
				}
			} else if (method.equals("PUT")) {
				if (courseMap.containsKey(code)) {
					DOMResult domXML = new DOMResult();
					messageTransformer.transform(source, domXML);
					Element coursesElement = (Element) domXML.getNode().getFirstChild();
					Element courseElement = (Element) coursesElement.getFirstChild();
					String name = courseElement.getFirstChild().getNodeValue();
					courseMap.put(code, name);
					httpResponse = 200;
				} else {
					httpResponse = 404;
				}
			} else
				throw new WebServiceException("unsupported method: " + method);
		} catch (Exception exception) {
			System.err.println("service invocation exception: " + exception);
		}
		requestContext.put(MessageContext.HTTP_RESPONSE_CODE, httpResponse);
		Source responseSource = new StreamSource(new StringReader(courses));
		return (responseSource);
	}

  /**
    Main method to initialise service.
  */
  public static void main(String arguments[]) throws Exception {
    CourseService service = new CourseService();
    System.out.println("Starting service ...");
    Endpoint endpoint =	Endpoint.create(HTTPBinding.HTTP_BINDING, service);
    endpoint.publish(serviceURI);
  }

}
