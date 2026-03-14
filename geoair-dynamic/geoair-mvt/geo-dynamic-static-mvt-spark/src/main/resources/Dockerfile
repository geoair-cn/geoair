# 基于原基础镜像
FROM harbor.host.com/bdh/tile-builder-base:v1.12
ARG SPARK_TARGET_DIR=/Users/will/Documents/Opt/spark-3.5.6-bin-hadoop3
ENV SPARK_HOME=${SPARK_TARGET_DIR}
ENV PATH=${PATH}:${SPARK_HOME}/bin:${SPARK_HOME}/sbin
RUN mkdir -p /Users/will/Documents/Opt/ && \

    chmod -R 755 /Users/will/Documents/Opt/
COPY spark-3.5.6-bin-hadoop3.tgz /tmp/
RUN tar -zxf /tmp/spark-3.5.6-bin-hadoop3.tgz --strip-components=1 -C ${SPARK_TARGET_DIR} && \
    rm -rf /tmp/spark-3.5.6-bin-hadoop3.tgz && \
    chmod +x ${SPARK_HOME}/bin/*

RUN spark-submit --version && echo "Spark environment installed successfully!"

LABEL version="v1.12.1" \
      description="tile-builder-base with Spark 3.5.6 (hadoop3) support"
