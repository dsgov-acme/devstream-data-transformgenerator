#!/bin/bash

# run the dbt generator
cd dbtgenerator
mvn compile exec:java -Dexec.mainClass="io.nuvalence.Main"

cd output

# run dbt
dbt build --profiles-dir .

echo 'transformation complete!'
