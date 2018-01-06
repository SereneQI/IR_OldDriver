#!/bin/bash  
  
while true  
do   
    procnum=` ps -ef | grep "SohuSpider.py"  | wc -l`  
   if [ $procnum -eq 1 ]; then  
   		python SohuSpider.py >> spider.log &
   fi  
   sleep 10m
done