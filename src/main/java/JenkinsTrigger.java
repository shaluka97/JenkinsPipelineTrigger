import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpHeaders;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

public class JenkinsTrigger {
    public static void trigger() {
        try {
            ConfigReader configReader = new ConfigReader("src/main/resources/config.properties");
            String jenkinsUrl = configReader.getProperty("Jenkins.url");
            String jenkinsUsername = configReader.getProperty("Jenkins.username");
            String jenkinsApiToken = configReader.getProperty("Jenkins.apiToken");
            String jobName = configReader.getProperty("Jenkins.jobName");

            if (jenkinsUrl == null || jenkinsUsername == null || jenkinsApiToken == null || jobName == null) {
                throw new IllegalArgumentException("Jenkins URL, username, API token, or job name is missing in the configuration.");
            }

            // Get the special parameter key dynamically
            String specialParamKey = configReader.getSpecialParameterName();
            if (specialParamKey == null) {
                throw new IllegalArgumentException("Special parameter key is missing in the configuration.");
            }

            // Get the necessary parameters from the Jenkins pipeline
            List<String> necessaryParams = JenkinsUtils.getNecessaryParameters(jenkinsUrl, jenkinsUsername, jenkinsApiToken, jobName, specialParamKey);

            // Check if all necessary parameters are present, excluding the special parameter
            for (String param : necessaryParams) {
                if (!param.equals(specialParamKey) && configReader.getProperty(param) == null) {
                    throw new IllegalArgumentException("Necessary parameter " + param + " is missing in the configuration.");
                }
            }

            String[] specialParamValues = configReader.getSpecialParameterValues(specialParamKey);

            for (String specialParamValue : specialParamValues) {
                CloseableHttpClient client = HttpClients.createDefault();
                HttpPost post = new HttpPost(jenkinsUrl + "/job/" + jobName + "/buildWithParameters");

                // Add basic authentication header
                String auth = jenkinsUsername + ":" + jenkinsApiToken;
                byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
                String authHeader = "Basic " + new String(encodedAuth);
                post.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

                // Add parameters from config file
                List<NameValuePair> params = new ArrayList<>();
                Iterator<String> keys = configReader.getKeys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (key.startsWith("Parameter.") && !key.equals("Parameter." + specialParamKey)) {
                        String paramName = key.substring("Parameter.".length());
                        params.add(new BasicNameValuePair(paramName, configReader.getProperty(key)));
                    }
                }
                // Add the special parameter with the current value
                params.add(new BasicNameValuePair(specialParamKey, specialParamValue));
                post.setEntity(new UrlEncodedFormEntity(params));

                try (CloseableHttpResponse response = client.execute(post)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode != 200 && statusCode != 201) {
                        throw new RuntimeException("Failed to trigger Jenkins job. HTTP error code: " + statusCode);
                    }
                    System.out.println(EntityUtils.toString(response.getEntity()));
                    // Print the message for each looping parameter only on success
                    System.out.println("Jenkins Pipeline triggered for " + specialParamKey + ": " + specialParamValue);
                } catch (Exception e) {
                    System.err.println("Error executing Jenkins job: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    client.close();
                }
            }
        } catch (Exception e) {
            System.err.println("Error in JenkinsTrigger: " + e.getMessage());
            e.printStackTrace();
        }
    }
}