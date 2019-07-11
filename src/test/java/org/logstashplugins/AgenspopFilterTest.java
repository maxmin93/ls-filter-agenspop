package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.FilterMatchListener;
import org.junit.Assert;
import org.junit.Test;
import org.logstash.plugins.ConfigurationImpl;
import org.logstash.plugins.ContextImpl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AgenspopFilterTest {

    @Test
    public void testJavaExampleFilter() {
        Map<String,Object> params = new HashMap<>();
        params.put("ids", Collections.unmodifiableList(Arrays.asList( "customerid" )));
        params.put("label", "customers");
        params.put("datasource", "northwind");

        Configuration config = new ConfigurationImpl(Collections.unmodifiableMap(params));
        Context context = new ContextImpl(null, null);
        AgenspopFilter filter = new AgenspopFilter("test-id", config, context);

        Event e = new org.logstash.Event();
        e.setField("customerid", "BITNINE");
        e.setField("country", "KR");
        e.setField("email", "agraph@bitnine.net");

        TestMatchListener matchListener = new TestMatchListener();
        Collection<Event> results = filter.filter(Collections.singletonList(e), matchListener);

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(1, matchListener.getMatchCount());
        Assert.assertEquals("northwind_customers_BITNINE", e.getField("id"));
        Assert.assertEquals(3, ((List<Object>) e.getField("properties")).size() );
    }
}

class TestMatchListener implements FilterMatchListener {

    private AtomicInteger matchCount = new AtomicInteger(0);

    @Override
    public void filterMatched(Event event) {
        matchCount.incrementAndGet();
    }

    public int getMatchCount() {
        return matchCount.get();
    }
}