#!/bin/sh
echo "[*] Running build/local/main.class"
java -Xmx50m \
    -cp build/ \
    Simulation
