# Use an official Ubuntu as a parent image
FROM ubuntu:latest

# Install git, curl, and other dependencies
RUN apt-get update && apt-get install -y \
    git \
    curl \
    bash \
    && rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Clone the repository
RUN git clone https://github.com/VCityTeam/ConVer-G.git

# Change to the repository directory
WORKDIR /app/ConVer-G

# Remove all directories except workflows/queries
RUN find . -mindepth 1 -maxdepth 1 ! -name 'workflows' -exec rm -rf {} + && \
    find workflows -mindepth 1 -maxdepth 1 ! -name 'queries' -exec rm -rf {} +

# Move workflows/queries to /app
RUN mv workflows/queries /app && rm -rf /app/ConVer-G

# Make the query.sh script executable
RUN chmod +x /app/queries/query.sh

# Set the working directory to /app/queries
WORKDIR /app/queries

# Run the query.sh script
ENTRYPOINT ["./query.sh"]