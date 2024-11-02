import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import java.io.File;
import java.util.Iterator;

public class ConfigReader {
    private final Configuration config;

    public ConfigReader(String filePath) throws Exception {
        Configurations configs = new Configurations();
        config = configs.properties(new File(filePath));
    }

    public String getProperty(String key) {
        return config.getString(key);
    }

    public Iterator<String> getKeys() {
        return config.getKeys();
    }

    public String getSpecialParameterName() {
        return config.getString("Looping.Parameter");
    }

    public String[] getSpecialParameterValues(String specialParamKey) {
        String values = config.getString("Parameter." + specialParamKey);
        return values != null ? values.split(",") : new String[0];
    }
}