#!/bin/bash

ES_URL="localhost:9200"
ES_IDX="elasticvertex"

echo -e "\n** 0) delete index : ${ES_URL}/${ES_IDX}"
curl -X DELETE "${ES_URL}/${ES_IDX}"
sleep 0.5

echo -e "\n** 1) create index : ${ES_URL}/${ES_IDX}"
curl -X PUT "${ES_URL}/${ES_IDX}" -H 'Content-Type: application/json' -d'
{}
'
sleep 0.5

echo -e "\n** 2) put mapping : ${ES_URL}/${ES_IDX}"
curl -X PUT "${ES_URL}/${ES_IDX}/_mapping/${ES_IDX}" -H 'Content-Type: application/json' -d'
{
  "properties":{
    "datasource" : { "type" : "keyword" },
    "id"         : { "type" : "keyword" },
    "label"      : { "type" : "keyword" },
    "properties" : {
      "type" : "nested",
      "properties": {
        "key"   : { "type": "keyword" },
        "type"  : { "type": "keyword" },
        "value" : { "type" : "text", "fields":{ "keyword": {"type":"keyword", "ignore_above": 256} } }
      }
    }
  }
}
'
sleep 0.5

echo -e "\n** 3) check index : ${ES_URL}"
curl -X GET "${ES_URL}/${ES_IDX}?pretty=true"

echo -e "\n  ..done, Good-bye\n"