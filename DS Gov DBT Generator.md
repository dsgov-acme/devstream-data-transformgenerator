# Purpose

[2023-06-19]

This project is designed to take the dynamic schema used by the DSGov project, and, using it, automatically create data pipelines to add analytics to the project out of the box.

## Data

### Schema

DSGov uses dynamic schemas.  These are stored by default in the `public_dynamic_schema` table, and are referred to here as the **schema_table**.

### Transactions

The data of each transaction event, by default found in the `public_transacton` table.  These have a single JSON column, and a schema ID that refers to the **schema_table**.  Referred to in this document as the **transaction_table**.

## Components

### Loader - Data Stream

To bring the operational data and schemas from **postgres** into **BigQuery**, we use the [DataStream](https://cloud.google.com/datastream) managed service in GCP.

## Setup

Setup for Data Stream is located in Git at

### Transformation - DBT and DBTGenerator

Once data is in BigQuery, we use  [DBT](https://www.getdbt.com/) to perform some standard transformations on all the data.

Our standard transformation will:

1) add a primary key to each transaction
2) normalize any sub-objects into sub-tables, with their own primary key and a foreign key to the base transaction
3) type the fields of each table generated in 2
4) copy the tables to a public dataset, so read permissions can be given to consumers
1) This also materializes as a table, to reduce read time of the final data product, at the expense of slightly longer write time on the final table in the pipeline

### Basic generation steps

#### Docker container built

- install JDK 11
- install Maven
- install Python 3.11
- install dbt-bigquery
- add dbtgenerator code to docker container

#### Docker container run

- Environment variables read
  - PROJECT_ID - GCP Project ID
  - DATASET_NAME - BQ dataset within the project
  - SCHEMA_TABLE_NAME - name of the table containing dynamic schemas, `public_dynamic_schema` by default
  - DATA_TABLE_NAME - name of the table containing transactions, `public_transaction` by default
  - runs the program at <https://github.com/Nuvalence/dsgov-data-transformgenerator>
  - runs `dbt build --profiles-dir .` to execute the generated pipeline

### Current Project Status as of [2023-06-20]

**Remaining items:**

- Finish dockerizing the dbt generator.  Basic Dockerfile is there, but unfortunately still having issues with getting Maven, JDK, and python all in the same image so far
- Create a Kubernetes cron job to run this docker image

**Next steps:**

- Add [LookerML](https://cloud.google.com/resources/looker-free-trial) generation to automatically create reports
- Add capability to generate [Dataform](https://cloud.google.com/dataform) as well as DBT versions of SQL transformation pipelines
