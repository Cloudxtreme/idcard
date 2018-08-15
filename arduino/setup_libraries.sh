#!/bin/sh

function rmln() {
	rm -rf $2
	ln -s $1 $2
}

rmln "$(pwd)/libraries/rfid-1.4.0" ~/Documents/Arduino/libraries/rfid-1.4.0
