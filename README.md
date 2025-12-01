# Connect 4

## Maintainers

This project is maintained by:

- Alberto De Sousa Lopes [@Alb-E](https://github.com/Alb-E)
- Maikol Correia Da Silva [@Maikol-Da-Silva](https://github.com/Maikol-Da-Silva)
- Nolan Evard [@Tigid0u](https://github.com/Tigid0u)

## Description of the project

Who never played Connect 4? It's a two-player connection game in which the players first choose a color and then take turns dropping colored discs from the top into a seven columns, six rows grid. The pieces fall straight down, occupying the lowest available space in the column chosen by the player. The goal of the game is to be the first to form a horizontal, vertical, or diagonal line of four discs of the same color (the color of disks chosen by the player).

This project implements a client-server application that allows two players to play Connect 4 over a network. The server manages the game state and enforces the rules, while the clients provide a user interface for the players to interact with the game.

It implements a simple application protocol (found below) that allows the clients to communicate with the server and vice versa with a set of predefined actions / commands.

The implemented application protocol can be found [here](./docs/appProtocol/protocol.md)

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
  
> **Note**: If you plan on running the application without Docker, it is recommended that you use the included Maven wrapper (`./mvnw`) to ensure you have the correct Maven version and a reproducible build environment.

## Run the application:

First you need to create a network for the docker containers to communicate:

```bash
docker network create connect4-net
```

You then have three options to run the application.

The recommended way is to use the docker container from the GitHub Container Registry as it provides an easy way to get started without having to build the application yourself (it is already pre-built):

```bash
docker run -it --rm ghcr.io/tigid0u/connect4-docker:latest
```

You can also build the docker image yourself using the provided Dockerfile:

```bash
docker build -t connect-4 .
docker run -it --rm connect-4
```

Or locally by running the jar file directly but it is not recommended as the behavior might depend on your local environment:

```bash
java -jar target/connect-4-1.0-SNAPSHOT.jar
```

### Run the server:

You can choose any options above to run the application but here we'll use the docker image from the GitHub Container Registry.

To launch the server, run the following command:

```bash
docker run -it --rm --name c4-server --network connect4-net ghcr.io/tigid0u/connect4-docker:latest server
```

Here we give the container a name `c4-server` that will be its hostname and connect it to the previously created `connect4-net` network.

By default, the server will listen on port `4444`. You can specify a different port by providing it as an argument:

```bash
docker run -it --rm --name c4-server --network connect4-net ghcr.io/tigid0u/connect4-docker:latest server -p <port>
```

### Run the client:

To launch the client, run the following command:

```bash
docker run -it --rm --network connect4-net ghcr.io/tigid0u/connect4-docker:latest client -o c4-server
```

By default, the client will try to connect to a server running on `localhost:4444` (used when connecting locally) but here we must specify the hostname as docker networks are unable to resolve `localhost` hostname.

You can specify a different host and port by providing them as arguments:

```bash
docker run -it --rm --network connect4-net ghcr.io/tigid0u/connect4-docker:latest client -o <host> -p <port>
```

### Run the application over a "public" network:

Running over a "public" (not localhost) network might require opening ports on your firewall or router to allow incoming connections to the server. The additional steps required for this setup to work will not be covered here as they depend on your specific network configuration.

## Usage examples

### Get help

By default the docker image runs the help command that displays all available options for both the client and server. But you can also run it manually for both the client and server:

General help:

```bash
docker run -it --rm ghcr.io/tigid0u/connect4-docker:latest -h
```

Output:

```
Usage: <main class> [-hV] [COMMAND]
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  client
  server
```

Server help:

```bash
docker run -it --rm ghcr.io/tigid0u/connect4-docker:latest server -h
```

Client help:

```bash
docker run -it --rm ghcr.io/tigid0u/connect4-docker:latest client -h
```

### Two clients and one server on a docker network (localhost simulation)

This example is the base use case of the application where two clients connect to a server and play a game of Connect 4.

Start the server:

```bash
docker run -it --rm --name c4-server --network connect4-net ghcr.io/tigid0u/connect4-docker:latest server
```

The output should be similar to this:

```
[Server] listening on port: 4444
```

Then start the first client:

```bash
docker run -it --rm --network connect4-net ghcr.io/tigid0u/connect4-docker:latest client -o c4-server
```

The output should be similar to this:

```
╔════════════════════════════════════════════════════════════════════════════════════════════╗
║                                                                                            ║
║     █████████                                                    █████       █████ █████   ║
║    ███░░░░░███                                                  ░░███       ░░███ ░░███    ║
║   ███     ░░░   ██████  ████████   ████████    ██████   ██████  ███████      ░███  ░███ █  ║
║  ░███          ███░░███░░███░░███ ░░███░░███  ███░░███ ███░░███░░░███░       ░███████████  ║
║  ░███         ░███ ░███ ░███ ░███  ░███ ░███ ░███████ ░███ ░░░   ░███        ░░░░░░░███░█  ║
║  ░░███     ███░███ ░███ ░███ ░███  ░███ ░███ ░███░░░  ░███  ███  ░███ ███          ░███░   ║
║   ░░█████████ ░░██████  ████ █████ ████ █████░░██████ ░░██████   ░░█████           █████   ║
║    ░░░░░░░░░   ░░░░░░  ░░░░ ░░░░░ ░░░░ ░░░░░  ░░░░░░   ░░░░░░     ░░░░░           ░░░░░    ║
║                                                                                            ║
╚════════════════════════════════════════════════════════════════════════════════════════════╝
Please type in your username for the new game >
```

You can then enter your username to join the game. This client will have to wait for the second player to join before starting the game.

Finally, start the second client in a new terminal using the same command as before.

The output should be similar to the first client and the output on the server should look something like:

```
[Server] listening on port: 4444
[Server] new client connected from 172.22.0.3:48482
[Server] new client connected from 172.22.0.4:60160
```

The game will then start once the second player enters their username.

The output on on the client that starts first should be similar to this:

```
┌───┬───┬───┬───┬───┬───┬───┐
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
└───┴───┴───┴───┴───┴───┴───┘
It's your turn! Please enter the column number (0-6) to drop your disc: >
```

And the output on the second client should be similar to this:

```
┌───┬───┬───┬───┬───┬───┬───┐
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
└───┴───┴───┴───┴───┴───┴───┘
Waiting for opponent's move...
```

When a player makes a move, the game board will be updated on both clients and the turn will switch to the other player:

Player 1 makes a move (player 1 POV):

```
┌───┬───┬───┬───┬───┬───┬───┐
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│ ● │   │   │   │   │   │   │
└───┴───┴───┴───┴───┴───┴───┘
Waiting for opponent's move...
```

Player 2 sees the updated board (player 2 POV):

```
┌───┬───┬───┬───┬───┬───┬───┐
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│ ○ │   │   │   │   │   │   │
└───┴───┴───┴───┴───┴───┴───┘
It's your turn! Please enter the column number (0-6) to drop your disc: >
```

The game continues until one player wins or the game ends in a draw.

Player 1 wins (player 1 POV):

```
┌───┬───┬───┬───┬───┬───┬───┐
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │ ● │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │ ○ │ ● │ ● │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│ ○ │ ● │ ● │ ● │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│ ● │ ○ │ ○ │ ○ │   │ ○ │   │
└───┴───┴───┴───┴───┴───┴───┘
You won!
Please type in your username for the new game >
```

Player 2 loses (player 2 POV):

```
┌───┬───┬───┬───┬───┬───┬───┐
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │   │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │   │   │ ○ │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│   │ ● │ ○ │ ○ │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│ ● │ ○ │ ○ │ ○ │   │   │   │
├───┼───┼───┼───┼───┼───┼───┤
│ ○ │ ● │ ● │ ● │   │ ● │   │
└───┴───┴───┴───┴───┴───┴───┘
You lost!
Please type in your username for the new game >
```

## Publish your own Docker image on the GitHub Container Registry

If you wish to publish your own Docker image of this project on the GitHub Container Registry, follow these steps:

First you need to login to the GitHub Container Registry (for this you need a personal access token):

```bash
docker login ghcr.io -u <username>
```

Then build the docker image:

```bash
docker build -t connect-4 .
```

Then tag the image:

```bash
docker tag connect-4 ghcr.io/<username>/connect4-docker:latest
```

Finally push the image to the GitHub Container Registry:

```bash
docker push ghcr.io/<username>/connect4-docker:latest
```

## Contribute

If you wish to help this project evolve, create an issue as follows to be added as a contributor:

- **Title:** [\<your username>] Request to contribute
- **Description:** Briefly state your motivations

Tag the maintainers in a comment to maximize the chances for your request to be reviewed in the shortest delays.

You can also report bugs or suggest new features by creating issues and labeling them accordingly. Pull requests are also welcome!
