FROM python:3.9
LABEL maintainer="vcity"

# Install dependencies
RUN apt-get update && apt-get install -q -y

ARG USER_ID=1000
ARG GROUP_ID=1000
RUN addgroup --gid $GROUP_ID user \
	&& adduser --disabled-password --gecos '' --uid $USER_ID --gid $GROUP_ID user

COPY . .

# Install dependencies
RUN pip install -r requirements.txt

RUN mkdir -p /data/triples \
  && mkdir -p /data/quads \
  && chown -R user:user /data

USER user

ENV PYTHONUNBUFFERED=1

# Setup Entrypoint
ENTRYPOINT ["python", "annotate_graph.py"]
