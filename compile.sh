#!/bin/sh
echo "[*] Compiling src/"
mkdir -p build
javac --release 21 -d build src/*
