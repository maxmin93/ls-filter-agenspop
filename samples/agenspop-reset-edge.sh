#!/bin/bash

ES_URL="localhost:8087"
ES_IDX="agensedge"

echo "\b** 0) delete index : ${ES_URL}/${ES_IDX}"
curl -X DELETE "${ES_URL}/${ES_IDX}"
sleep 0.5

echo "\n** 1) create index : ${ES_URL}/${ES_IDX}"
curl -X PUT "${ES_URL}/${ES_IDX}" -H 'Content-Type: application/json' -d'
{}
'
sleep 0.5

echo "\n** 2) put mapping : ${ES_URL}/${ES_IDX}"
curl -X PUT "${ES_URL}/${ES_IDX}/_mapping/${ES_IDX}" -H 'Content-Type: application/json' -d'
{
    "properties": {
        "datasource": {
            "type": "keyword"
        },
        "deleted": {
            "type": "boolean"
        },
        "id": {
            "type": "text",
            "fields": {
                "keyword": {
                    "type": "keyword",
                    "ignore_above": 256
                }
            }
        },
        "label": {
            "type": "keyword"
        },
        "properties": {
            "type": "nested",
            "include_in_parent": true,
            "properties": {
                "key": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "type": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "value": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                }
            }
        },
        "sid": {
            "type": "keyword"
        },
        "tid": {
            "type": "keyword"
        },
        "version": {
            "type": "long"
        }
    }
}
'
sleep 0.5

echo "\n** 3) check index : ${ES_URL}"
curl -X GET "${ES_URL}/${ES_IDX}?pretty=true"

echo "\n  ..done, Good-bye\n"