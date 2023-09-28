# Base image with Java 11
FROM maven as jdk

# Install Python 3.11
RUN apt update && apt upgrade -y \
    && apt install -y wget build-essential libncursesw5-dev libssl-dev \
    libsqlite3-dev tk-dev libgdbm-dev libc6-dev libbz2-dev libffi-dev zlib1g-dev \
    software-properties-common \
    && add-apt-repository ppa:deadsnakes/ppa -y \
    && apt install python3.11 -y \
    && apt install python3-pip -y \
    && pip install dbt-bigquery \
    && java -version \
    && mvn --version \
    && python3.11 --version

# run dbt generator and then dbt in our shell script
COPY ./dbtgenerator /app/dbtgenerator
COPY ./poetry.lock /app
COPY ./pyproject.toml /app
COPY ./transform.sh /app

# this should be set by Kubernetes in practice
ARG PROJECT_ID=dsgov-dev
ARG DATASET_NAME=work_manager_cloudsql
ARG SCHEMA_TABLE_NAME=public_dynamic_schema
ARG TRANSACTION_TABLE_NAME=public_transaction

WORKDIR /app/dbtgenerator

# precompile to reduce runtime
RUN mvn compile
WORKDIR ..
#
## we need to go to our transform script to do two things!
RUN chmod +x transform.sh
#
CMD ["./transform.sh"]
