#!/bin/bash

if [ $# -eq 0 ]
    then
        java -classpath jrchat.jar client.ClientGuiController
    else
        if [ $1 = "-s" ]
            then
                java -classpath jrchat.jar server.Server
        fi
fi