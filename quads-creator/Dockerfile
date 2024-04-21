FROM python:3.9
LABEL maintainer="vcity"

# Install dependencies
RUN apt-get update && apt-get install -q -y

COPY . .

# Install dependencies
RUN pip install -r requirements.txt

RUN mkdir -p /data/triples \
  && mkdir -p /data/quads

# Setup Entrypoint
ENTRYPOINT ["python", "annotate_graph.py"]
