# Connect 4

## Application protocol

The application protocol used for this application can be found [here](./docs/appProtocol/protocol.md)

## Get Started

To get stated with the Connect 4 application, follow these steps:

1. Clone the repository:

   ```bash
   git clone git@github.com:Tigid0u/connect-4.git
   ```

2. Package the application using the included Maven wrapper:

   ```bash
   ./mvnw clean package
   ```

## Run the application:

You have multiple options to run the application:

The recommended way is to use the docker container from the GitHub Container Registry:

```bash
docker run -it --rm ghcr.io/tigid0u/connect4-docker:latest
```

You can also build the docker image yourself using the provided Dockerfile:

```bash
docker build -t connect-4 .
docker run -it --rm connect-4
```

Or locally by running the jar file directly but it is not recommended:

```bash
java -jar target/connect-4-1.0-SNAPSHOT.jar
```

### Run the client:

You can choose any options above to run the application but here we'll use the docker image from the GitHub Container Registry.

To launch the client, run the following command:

```bash
docker run -it --rm ghcr.io/tigid0u/connect4-docker:latest client
```

By default, the client will try to connect to a server running on `localhost:4444`. You can specify a different host and port by providing them as arguments:

```bash
docker run -it --rm -p <host_port>:<container_port> ghcr.io/tigid0u/connect4-docker:latest client -o <host> -p <port>
```

> **Note**: It is required to remap the container's port to the host port using the `-p` option as by default the container only listens on port `4444`.

### Run the server:

Same as the client, you can choose any options above to run the application but here we'll use the docker image from the GitHub Container Registry.

To launch the server, run the following command:

```bash
docker run -it --rm ghcr.io/tigid0u/connect4-docker:latest server
```

By default, the server will listen on port `4444`. You can specify a different port by providing it as an argument:

```bash
docker run -it --rm -p <host_port>:<container_port> ghcr.io/tigid0u/connect4-docker:latest server -p <port>
```

