#!/usr/bin/env sh

# increase the amount of contiguous memory area for buffer allocations
echo "Increasing galcore cma by writing $1 to /sys/module/galcore/parameters/contiguousSize"
echo $1 > /sys/module/galcore/parameters/contiguousSize
