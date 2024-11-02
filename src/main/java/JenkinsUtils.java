import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpHeaders;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class JenkinsUtils {
    public static List<String> getNecessaryParameters(String jenkinsUrl, String jenkinsUsername, String jenkinsApiToken, String jobName, String specialParamKey) throws Exception {
        List<String> necessaryParams = new ArrayList<>();
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(jenkinsUrl + "/job/" + jobName + "/config.xml");

        // Add basic authentication header
        String auth = jenkinsUsername + ":" + jenkinsApiToken;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        String authHeader = "Basic " + new String(encodedAuth);
        get.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

        try (CloseableHttpResponse response = client.execute(get)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new RuntimeException("Failed to get Jenkins job parameters. HTTP error code: " + statusCode);
            }
            String xmlResponse = EntityUtils.toString(response.getEntity());

            // Parse the XML response
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlResponse.getBytes()));

            // Handle different types of parameter definitions
            String[] parameterTypes = {
                    "hudson.model.StringParameterDefinition",
                    "hudson.model.BooleanParameterDefinition",
                    "hudson.model.ChoiceParameterDefinition",
                    "hudson.model.PasswordParameterDefinition",
                    "hudson.model.CascadeChoiceParameterDefinition",
                    "hudson.model.TextParameterDefinition"
            };

            for (String paramType : parameterTypes) {
                NodeList parameterNodes = doc.getElementsByTagName(paramType);
                for (int i = 0; i < parameterNodes.getLength(); i++) {
                    String paramName = parameterNodes.item(i).getChildNodes().item(1).getTextContent();
                    necessaryParams.add("Parameter." + paramName);
                }
            }

            // Add the special parameter key dynamically if not already present
            if (!necessaryParams.contains(specialParamKey)) {
                necessaryParams.add(specialParamKey);
            }
        } finally {
            client.close();
        }
        return necessaryParams;
    }
}