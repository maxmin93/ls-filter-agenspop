#!/bin/bash

ES_URI="localhost:9200"
IDX_VERTEX="elasticvertex"
IDX_EDGE="elasticedge"

curl -X POST "$ES_URI/$IDX_VERTEX/_delete_by_query?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "match": {
      "datasource": "airroutes"
    }
  }
}
'
sleep 0.5
curl -X POST "$ES_URI/$IDX_EDGE/_delete_by_query?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "match": {
      "datasource": "airroutes"
    }
  }
}
'
sleep 1.5

curl -X GET "$ES_URI/_cat/indices?v"

echo "\n================================================="
echo "** Start bulk-insert to vertex, edge repositories\n"

echo "V1) logstash ==> air-nodes-airport.conf"
logstash -f air-nodes-airport.conf < air-nodes-airport.csv > /dev/null
sleep 0.5

echo "V2) logstash ==> air-nodes-country.conf"
logstash -f air-nodes-country.conf < air-nodes-country.csv > /dev/null
sleep 0.5

echo "V3) logstash ==> air-nodes-continent.conf"
logstash -f air-nodes-continent.conf < air-nodes-continent.csv > /dev/null
sleep 0.5

echo "E1) logstash ==> air-edges-contains.conf"
logstash -f air-edges-contains.conf < air-edges-contains.csv > /dev/null
sleep 0.5

echo "E2) logstash ==> air-edges-route.conf"
logstash -f air-edges-route.conf < air-edges-route.csv > /dev/null
sleep 0.5

curl -X GET "$ES_URI/_cat/indices?v"

echo "\n ..done, Good-bye"
exit 0