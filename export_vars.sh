#!/usr/bin/env bash
while read -r line; do
  export "$line"
done < ./$1
