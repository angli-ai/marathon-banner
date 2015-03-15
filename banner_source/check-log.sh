#!/bin/bash

ls log/*.test | while read line; do
filename=${line%.test}
echo ------ $line ------
cat $line | grep 'tp =\|fp =\|fn =\|precision\|recall\|fmeasure'
echo ------
echo
done
