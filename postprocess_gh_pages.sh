#!/bin/bash

if [ -z "$1" ]; then
    echo "usage: $0 <prefix>"
    echo "<prefix> will be appended to the base url of ort-wasm files"
    exit 0
fi
PREFIX="$1"
# remove / chars from end
PREFIX=$(echo "$PREFIX" | sed -E 's/(.*[a-zA-Z0-9]).*/\1/')
echo "$PREFIX"

echo "$0": prepending "$PREFIX" to wasm file requests
cd docs/
cat worker.js | sed -E "s/(ort-wasm[a-z-]+\.wasm)/$PREFIX\/\1/g" > worker_processed.js
mv worker_processed.js worker.js


