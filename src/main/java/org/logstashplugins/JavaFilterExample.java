package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.Filter;
import co.elastic.logstash.api.FilterMatchListener;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.PluginConfigSpec;
import org.apache.commons.lang3.StringUtils;
// import org.jruby.RubyNil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

// class name must match plugin name
@LogstashPlugin(name = "java_filter_example")
public class JavaFilterExample implements Filter {

//    public static final List<String> metaFields = Collections.unmodifiableList(
//            Arrays.asList("@version", "@timestamp", "sequence", "host"));

    public static final List<String> removeFields = Collections.unmodifiableList(
            Arrays.asList("@version", "@timestamp"));

    public static final String ID_DELIMITER = "_";
    public static final PluginConfigSpec<String> DATASOURCE_CONFIG =
            PluginConfigSpec.stringSetting("datasource", "default");
    public static final PluginConfigSpec<String> ID_CONFIG =
            PluginConfigSpec.stringSetting("id", "_id");
    public static final PluginConfigSpec<String> LABEL_CONFIG =
            PluginConfigSpec.stringSetting("label", "vertex");

//    public static final PluginConfigSpec<String> SOURCE_CONFIG =
//            PluginConfigSpec.stringSetting("source", "message");
//    private String sourceField;

    private String id;
    private String label;
    private String datasource;

    public JavaFilterExample(String id, Configuration config, Context context) {
        // constructors should validate configuration options
        // this.sourceField = config.get(SOURCE_CONFIG);
        this.id = id;
        this.label = config.get(LABEL_CONFIG);
        this.datasource = config.get(DATASOURCE_CONFIG);
    }

    public static Map<String,String> getProperty(String key, Object value){
        Map<String,String> property = new HashMap<>();
        property.put("key", key);
        property.put("type", value.getClass().getName());
        property.put("value", value.toString());
        return property;
    }

    @Override
    public Collection<Event> filter(Collection<Event> events, FilterMatchListener matchListener) {
        for (Event e : events) {
            Object idValue = e.getField(id);
            if( idValue == null ) continue;

            // remove old fields and create properties
            List<Map<String,String>> properties = new ArrayList<>();
            Set<String> fieldNames = new HashSet<>(e.getData().keySet());
            for( String fieldName : fieldNames ){
                Object fieldValue = e.remove(fieldName);
                // skip removeFields
                if( removeFields.contains(fieldName) ) continue;
                // add not_nil value to properties
                if( fieldValue != null && !fieldValue.getClass().getName().equals("org.jruby.RubyNil") )
                    properties.add( getProperty(fieldName, fieldValue));
            }

            // write new fields
            e.setField("id", datasource+ID_DELIMITER+idValue.toString());
            e.setField("deleted", false);
            e.setField("version", System.currentTimeMillis());
            e.setField("label", label);
            e.setField("datasource", datasource);
            e.setField("properties", properties);

            // apply changes
            matchListener.filterMatched(e);
        }
        return events;
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        // should return a list of all configuration options for this plugin
        return Collections.unmodifiableList(Arrays.asList(ID_CONFIG, LABEL_CONFIG, DATASOURCE_CONFIG));
    }

    @Override
    public String getId() {
        return this.id;
    }
}

/*

"region=, @version=1, fax=(5) 555-3745, @timestamp=2019-07-10T11:39:41.688Z, country=Mexico, address=Avda. de la Constitución 2222, postalcode=05021, city=México D.F., customerid=ANATR, contactname=Ana Trujillo, companyname=Ana Trujillo Emparedados y helados, phone=(5) 555-4729, contacttitle=Owner"

{
      "sequence" => 0,
         "label" => "customers",
       "message" => "!dlrow olleH",
    "properties" => [
        [0] {
              "key" => "name",
            "value" => "David Calson",
             "type" => "java.lang.String"
        },
        [1] {
              "key" => "country",
            "value" => "USA",
             "type" => "java.lang.String"
        }
    ],
      "@version" => "1",
          "host" => "bgmin-pc",
            "id" => "6e73391a73cd79ff04270843886d5d000d88b439808d85fe6db6438235edaeae",
    "datasource" => "northwind",
    "@timestamp" => 2019-07-10T09:41:29.490Z
}

////////////////////////////////////

{
            "id" => "6e73391a73cd79ff04270843886d5d000d88b439808d85fe6db6438235edaeae",
          "meta" => "",                         ## <== e.getMetadata();
       "message" => "!dlrow olleH",
      "@version" => "1",
          "host" => "bgmin-pc",
      "sequence" => 0,
    "@timestamp" => 2019-07-10T09:27:51.070Z,
    "datasource" => "northwind",
         "label" => "customers"
}
 */