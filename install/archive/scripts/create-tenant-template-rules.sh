#!/bin/bash

curl -H 'Content-Type: application/json' -XPOST localhost:9000/api/tenants -d '{ "tenantName":"test","tenantId":"test" }'

TEMPLATE_ID=`curl -XPOST localhost:9000/api/templates/test`

curl -H 'Content-Type: application/json' -XPUT localhost:9000/api/templates/test/$TEMPLATE_ID -d '{"templateId":2,"templateName":"rest","destination":"test@xyz.com","media":"mail","subject":"test","body":"test","throttleDuration":300,"throttleLimit":1}'

RULE_ID=`curl -XPOST localhost:9000/api/rules/test`

echo "Created rule id: $RULE_ID"

curl -H 'Content-Type: application/json' -XPUT localhost:9000/api/rules/test/$RULE_ID -d '{ "condition": { "type": "EQUALS", "props": { "value": "app1.symcpe.io", "key": "host" } }, "actions": [ { "type": "ALERT", "props": { "actionId": 0,"templateId": "'$TEMPLATE_ID'"  } } ], "ruleId": "'$RULE_ID'", "name": "test rule", "active": true, "description": "test"}'
