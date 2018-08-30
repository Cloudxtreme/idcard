#!/bin/bash

set -euo pipefail

# requires https://github.com/golang-migrate/migrate/tree/master/cli
# make sure to run setup.sql first

migrate -path card42/migrations -database $(jq -r <card42-config.json .SQLConnect) up $@
