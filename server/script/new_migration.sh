#!/bin/sh

cd $(git rev-parse --show-cdup)
migrate create -dir server/card42/migrations -ext sql $@
