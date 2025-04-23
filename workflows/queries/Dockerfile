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
COPY . .

# Make the query.sh script executable
RUN chmod +x /app/typed-query.sh

# Run the query.sh script
ENTRYPOINT ["./typed-query.sh"]