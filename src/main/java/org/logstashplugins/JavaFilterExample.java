package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.Filter;
import co.elastic.logstash.api.FilterMatchListener;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.PluginConfigSpec;
import org.apache.commons.lang3.StringUtils;
import org.jruby.RubyObject;
// import org.jruby.RubyNil;

import java.io.IOException;
import java.math.BigDecimal;
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
            PluginConfigSpec.stringSetting("datasource", "default", false, true);
    public static final PluginConfigSpec<String> LABEL_CONFIG =
            PluginConfigSpec.stringSetting("label", "vertex", false, true);
    public static final PluginConfigSpec<List<Object>> IDS_CONFIG =
            PluginConfigSpec.arraySetting("ids", Arrays.asList("_id"), false, true);
    public static final PluginConfigSpec<List<Object>> SID_CONFIG =
            PluginConfigSpec.arraySetting("sid", Collections.EMPTY_LIST, false, false);
    public static final PluginConfigSpec<List<Object>> TID_CONFIG =
            PluginConfigSpec.arraySetting("tid", Collections.EMPTY_LIST, false, false);

    private String id;          // session id (not related with data)

    private List<Object> ids;   // field-names for making id value
    private String label;       // label for agenspop (common)
    private String datasource;  // datasource for agenspop (common)
    private List<Object> sid;   // source vertex-id of edge for agenspop = { <datasource>, <label>, <fieldName> }
    private List<Object> tid;   // target vertex-id of edge for agenspop = { <datasource>, <label>, <fieldName> }

    public JavaFilterExample(String id, Configuration config, Context context) {
        // constructors should validate configuration options
        this.id = id;
        this.ids = config.get(IDS_CONFIG);
        this.label = config.get(LABEL_CONFIG);
        this.datasource = config.get(DATASOURCE_CONFIG);
        this.sid = config.get(SID_CONFIG);
        this.tid = config.get(TID_CONFIG);
    }

    public static String parseTypeName(Object value){
        String valueType = value.getClass().getName();
        if( valueType.startsWith("org.jruby.") ) {
            // **NOTE
            // Reason 1st: Because logstash plugins are made by JRuby,
            // Reason 2nd: Agenspop need Java Compatible Object Type in Elasticsearch Repository
            try {
                // **Caution : type casting
                //   ==> org.logstash.ext.JrubyTimestampExtLibrary$RubyTimestamp
                valueType = ((RubyObject) value).getJavaClass().getName();
            }catch( Exception e ){
                valueType = String.class.getName();
            }
        }
        // convert Primitive type to Object type
        if( !valueType.startsWith("java.") ){
            if( valueType.equals("long") ) valueType = Long.class.getName();
            else if( valueType.equals("double") ) valueType = Double.class.getName();
            else if( valueType.equals("boolean") ) valueType = Boolean.class.getName();
            else if( valueType.equals("char") ) valueType = Character.class.getName();
            else if( valueType.equals("byte") ) valueType = Byte.class.getName();
            else if( valueType.equals("short") ) valueType = Short.class.getName();
            else if( valueType.equals("int") ) valueType = Integer.class.getName();
            else if( valueType.equals("float") ) valueType = Float.class.getName();
            else valueType = String.class.getName();
        }
        return valueType;
    }

    public static Map<String,String> getProperty(String key, Object value){
        Map<String,String> property = new HashMap<>();
        property.put("key", key);
        property.put("type", parseTypeName(value));
        property.put("value", value.toString());
        return property;
    }

    public static String getIdValue(String datasource, String label, List<Object> ids, Event e){
        StringBuilder sb = new StringBuilder().append(datasource).append(ID_DELIMITER).append(label);
        for( Object id : ids ){
            Object value = e.getField(id.toString());
            if( value != null && !value.getClass().getName().equals("org.jruby.RubyNil") )
                sb.append(ID_DELIMITER).append(value.toString());
        }
        return sb.toString();
    }

    @Override
    public Collection<Event> filter(Collection<Event> events, FilterMatchListener matchListener) {
        for (Event e : events) {
            // make new ID using field values of ids
            String idValue = getIdValue(datasource, label, ids, e);
            String sidValue = (sid.size() < 3) ? null
                    : getIdValue(sid.get(0).toString(), sid.get(1).toString()
                        , Collections.singletonList(sid.get(2).toString()), e);
            String tidValue = (tid.size() < 3) ? null
                    : getIdValue(tid.get(0).toString(), tid.get(1).toString()
                    , Collections.singletonList(tid.get(2).toString()), e);

            // remove old fields and create properties
            List<Map<String,String>> properties = new ArrayList<>();
            Set<String> fieldNames = new HashSet<>(e.getData().keySet());
            for( String fieldName : fieldNames ){
                Object fieldValue = e.remove(fieldName);
                // skip removeFields
                if( removeFields.contains(fieldName) ) continue;
                // skip nullValue
                if( fieldValue == null || fieldValue.getClass().getName().equals("org.jruby.RubyNil") ) continue;
                // add field to properties
                properties.add( getProperty(fieldName, fieldValue) );
            }

            // write new fields (common)
            e.setField("id", idValue);
            e.setField("deleted", false);
            e.setField("version", System.currentTimeMillis());
            e.setField("label", label);
            e.setField("datasource", datasource);
            e.setField("properties", properties);
            // write some fields for edge
            if( sidValue != null ) e.setField("sid", sidValue);
            if( tidValue != null ) e.setField("tid", tidValue);

            // apply changes
            matchListener.filterMatched(e);
        }
        return events;
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        // should return a list of all configuration options for this plugin
        return Collections.unmodifiableList(Arrays.asList(IDS_CONFIG, LABEL_CONFIG, DATASOURCE_CONFIG, SID_CONFIG, TID_CONFIG));
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