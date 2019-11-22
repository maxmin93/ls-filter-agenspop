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

import java.util.*;
import java.util.stream.Collectors;

// class name must match plugin name
@LogstashPlugin(name = "agenspop_filter")
public class AgenspopFilter implements Filter {

    public static final List<String> removeFields = Collections.unmodifiableList(
            Arrays.asList("@version", "@timestamp", "message", "sequence"));

    public static final String ID_DELIMITER = "_";
    public static final PluginConfigSpec<String> DATASOURCE_CONFIG =
            PluginConfigSpec.stringSetting("datasource", "default", false, true);
    public static final PluginConfigSpec<String> LABEL_CONFIG =
            PluginConfigSpec.stringSetting("label", "vertex", false, true);
    public static final PluginConfigSpec<List<Object>> IDS_CONFIG =
            PluginConfigSpec.arraySetting("ids", Arrays.asList("_id"), false, true);
    public static final PluginConfigSpec<List<Object>> SRC_CONFIG =
            PluginConfigSpec.arraySetting("src", Collections.EMPTY_LIST, false, false);
    public static final PluginConfigSpec<List<Object>> DST_CONFIG =
            PluginConfigSpec.arraySetting("dst", Collections.EMPTY_LIST, false, false);
    public static final PluginConfigSpec<String> NIL_CONFIG =
            PluginConfigSpec.stringSetting("nil_value", null, false, false);
    public static final PluginConfigSpec<Boolean> EXCLUDE_LABEL_CONFIG =
            PluginConfigSpec.booleanSetting("exclude_label", false, false, false);

    private String id;          // session id (not related with data)

    private List<Object> ids;   // field-names for making id value
    private String label;       // label for agenspop (common)
    private String datasource;  // datasource for agenspop (common)
    private List<Object> src;   // source vertex-id of edge for agenspop = { <datasource>, <label>, <fieldName> }
    private List<Object> dst;   // target vertex-id of edge for agenspop = { <datasource>, <label>, <fieldName> }

    private Boolean excludeLabel;   // excludeLabel for propagating id
    private String nil_value;       // nil value ==> skip property

    public AgenspopFilter(String id, Configuration config, Context context) {
        // constructors should validate configuration options
        this.id = id;
        this.ids = config.get(IDS_CONFIG);
        this.label = config.get(LABEL_CONFIG);
        this.datasource = config.get(DATASOURCE_CONFIG);
        this.src = config.get(SRC_CONFIG);
        this.dst = config.get(DST_CONFIG);
        this.nil_value = config.get(NIL_CONFIG);
        this.excludeLabel = config.get(EXCLUDE_LABEL_CONFIG);
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

    public static String getFieldOrElse(String label, Event e, String value) {
        return e.includes(label) ? e.getField(label).toString() : value;
    }

    public static String getIdValue(String datasource, String label, List<Object> ids, Event e, Boolean excludeLabel){
        StringBuilder sb = new StringBuilder().append(datasource);
        if( !excludeLabel ) sb.append(ID_DELIMITER).append(label);
        for( Object id : ids ){
            Object value = e.getField(id.toString());
            if( value != null && !value.getClass().getName().equals("org.jruby.RubyNil") )
                sb.append(ID_DELIMITER).append(value.toString());
        }
        return sb.toString();
    }

    public static String getVidValue(String datasource, List<Object> list, Event e, Boolean excludeLabel){
        String label = "";
        List<Object> ids = list;
        if( !excludeLabel && ids.size() > 1 ){
            label = ids.get(0).toString();
            ids = list.subList(1, list.size());
        }
        return getIdValue(datasource, label, ids, e, label.isEmpty());
    }

    @Override
    public Collection<Event> filter(Collection<Event> events, FilterMatchListener matchListener) {
        for (Event e : events) {
            String labelValue = getFieldOrElse(label, e, label);
            // make new ID using field values of ids
            String idValue = getIdValue(datasource, labelValue, ids, e, excludeLabel);
            // vertex id for edge : source or target
            String sidValue = (src.size() == 0) ? null : getVidValue(datasource, src, e, excludeLabel);
            String tidValue = (dst.size() == 0) ? null : getVidValue(datasource, dst, e, excludeLabel);

            // remove old fields and create properties
            List<Map<String,String>> properties = new ArrayList<>();
            Set<String> fieldNames = new HashSet<>(e.getData().keySet());
            for( String fieldName : fieldNames ){
                Object fieldValue = e.remove(fieldName);
                // skip removeFields
                if( removeFields.contains(fieldName) ) continue;
                // skip nullValue
                if( fieldValue == null
                        || fieldValue.getClass().getName().equals("org.jruby.RubyNil")
                        || fieldValue.toString().length() == 0
                        || fieldValue.toString().isEmpty()
                        || (nil_value != null && fieldValue.toString().equals(nil_value) )
                ) continue;
                // add field to properties
                properties.add( getProperty(fieldName, fieldValue) );
            }

            // write new fields (common)
            e.setField("id", idValue);
            e.setField("label", labelValue);
            e.setField("datasource", datasource);
            e.setField("properties", properties);
            // write some fields for edge
            if( sidValue != null ) e.setField("src", sidValue);
            if( tidValue != null ) e.setField("dst", tidValue);

            // apply changes
            matchListener.filterMatched(e);
        }
        return events;
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        // should return a list of all configuration options for this plugin
        return Collections.unmodifiableList(Arrays.asList(IDS_CONFIG, LABEL_CONFIG, DATASOURCE_CONFIG, SRC_CONFIG, DST_CONFIG, NIL_CONFIG, EXCLUDE_LABEL_CONFIG));
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
 */