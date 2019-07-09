package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.Filter;
import co.elastic.logstash.api.FilterMatchListener;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.PluginConfigSpec;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

// class name must match plugin name
@LogstashPlugin(name = "java_filter_example")
public class JavaFilterExample implements Filter {

    public static final PluginConfigSpec<String> DATASOURCE_CONFIG =
            PluginConfigSpec.stringSetting("datasource", "default");
    public static final PluginConfigSpec<String> ID_CONFIG =
            PluginConfigSpec.stringSetting("id", "_id");
    public static final PluginConfigSpec<String> LABEL_CONFIG =
            PluginConfigSpec.stringSetting("label", "vertex");
    public static final PluginConfigSpec<String> SOURCE_CONFIG =
            PluginConfigSpec.stringSetting("source", "message");

    private String id;
    private String sourceField;

    private String label;
    private String datasource;

    public JavaFilterExample(String id, Configuration config, Context context) {
        // constructors should validate configuration options
        this.id = id;
        this.sourceField = config.get(SOURCE_CONFIG);

        this.label = config.get(LABEL_CONFIG);
        this.datasource = config.get(DATASOURCE_CONFIG);
    }

    @Override
    public Collection<Event> filter(Collection<Event> events, FilterMatchListener matchListener) {
        for (Event e : events) {
            Map<String,Object> meta = e.getMetadata();
            String result = meta.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(", "));
//            System.out.println("meta => [" + result + "]");
            try {
                e.sprintf("meta => [" + result + "]");
            }catch (IOException ie){
            }

            Object f = e.getField(sourceField);
            if (f instanceof String) {
//                AgensProperty property = new AgensProperty(id, label, datasource);
//                e.setField("properties", property);
                e.setField(sourceField, StringUtils.reverse((String)f));
                e.setField("label", label);
                e.setField("datasource", datasource);
                matchListener.filterMatched(e);
            }
        }
        return events;
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        // should return a list of all configuration options for this plugin
        // return Collections.unmodifiableList(Arrays.asList(ID_CONFIG, DATASOURCE_CONFIG));
        return Collections.singletonList(SOURCE_CONFIG);
    }

    @Override
    public String getId() {
        return this.id;
    }
}
