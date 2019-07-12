#!/bin/bash

#curl -X DELETE "localhost:9200/agensvertex?pretty"
#sleep 0.5
#curl -X DELETE "localhost:9200/agensedge?pretty"
#sleep 0.5

curl -X GET "localhost:9200/_cat/indices?v"

echo "\n================================================="
echo "** Start bulk-insert to agensvertex, agensedge ! \n"

echo "V1) logstash ==> agenspop-vertex-category.conf"
logstash -f agenspop-vertex-category.conf   > /dev/null
sleep 0.5

echo "V2) logstash ==> agenspop-vertex-customer.conf"
logstash -f agenspop-vertex-customer.conf   > /dev/null
sleep 0.5

echo "V3) logstash ==> agenspop-vertex-employee.conf"
logstash -f agenspop-vertex-employee.conf   > /dev/null
sleep 0.5

echo "V4) logstash ==> agenspop-vertex-order.conf"
logstash -f agenspop-vertex-order.conf      > /dev/null
sleep 0.5

echo "V5) logstash ==> agenspop-vertex-product.conf"
logstash -f agenspop-vertex-product.conf    > /dev/null
sleep 0.5

echo "V6) logstash ==> agenspop-vertex-supplier.conf"
logstash -f agenspop-vertex-supplier.conf   > /dev/null
sleep 0.5

echo "E1) logstash ==> agenspop-edge-contains.conf"
logstash -f agenspop-edge-contains.conf     > /dev/null
sleep 0.5

echo "E2) logstash ==> agenspop-edge-part_of.conf"
logstash -f agenspop-edge-part_of.conf      > /dev/null
sleep 0.5

echo "E3) logstash ==> agenspop-edge-purchased.conf"
logstash -f agenspop-edge-purchased.conf    > /dev/null
sleep 0.5

echo "E4) logstash ==> agenspop-edge-reports_to.conf"
logstash -f agenspop-edge-reports_to.conf   > /dev/null
sleep 0.5

echo "E6) logstash ==> agenspop-edge-sold.conf"
logstash -f agenspop-edge-sold.conf         > /dev/null
sleep 0.5

echo "E7) logstash ==> agenspop-edge-supplies.conf"
logstash -f agenspop-edge-supplies.conf     > /dev/null

echo "\n ..done, Good-bye"
exit 0