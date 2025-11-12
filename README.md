# Connect 4 - Simple application protocol

## Overview

The goal of this protocol is to allow clients to play connect 4 together with a server in the middle to host the game.

There's no specific protocol for connect 4 so we'll implement one.

## Transport protocol

This application protocol will be using TCP as a transport protocol as we need reliability for the commands that are sent by the clients.

The server will listen on TCP port 4444.

The messages sent and received by the server are treated as text encoded as UTF-8 and they are delimited by a `\n` new line character.

The initial connection must be established by the client. Once established, the client can choose a username and start a game. It must wait for another client to join the server to start a game. 

A finished game doesn't disconnect the client. It offers to start a new game but once finished playing, the client is responsible for closing the connection with the server.

On an unknown command or mis-formated command, the server sends an error message to the client with a code indicating the error type.

## Commands

### Join the server

Once the TCP connection is established, the client can send a `JOIN` command to join the server:

**Request**

```
JOIN <username>
```
- `<username>` : the username of the client

**Response:**

- `OK`: The client has joined the server
- `ERROR <code>`: an error during the joining process
  - `1` : The username is already in use
  - `2` : The server is already full (2 players have already joined the server)

### Tell the server the client is ready to play

Once the client joined the server and wants to start a game. It tells the server it is `READY`.

**Request**

```
READY
```

**Response**

None

### The game starts

Once all clients are ready, the server sends `GAME_STARTS` to all the ready clients.

**Request**

```
GAME_STARTS <op_username> <your_turn>
```

- `<op_username>`: the username of the opponent
- `<your_turn>`: boolean defining if it's your turn. If true (1) it is your turn.

**Response**

No response needed from the clients.

### Play a turn

Once the game started, each player can, in turn, place a disc by sending the `PLAY` command.

**Request**

```
PLAY <column>
```

- `<column>`: the column number to place the disk

**Response**

- `ERROR <code>`
  - `1`: This is not your turn
  - `2`: Index out of range
  - `3`: Column full
  - `4`: Too few / too much argument or invalid format.
- `OK` : the play was registered successfully

### It's your turn

Once a play was played by a player, the server sends the other player a `YOUR_TURN` announcement signaling the play that was made.

**Request**

```
YOUR_TURN <column>
```

- `<column>`: the column in which the opponent played

**Response**

None

### End of a game

Once one player won or all the columns are full, the server sends a `END_OF_GAME` request to announce that the game is finished and who's the winner in case there's one or if it's a draw in announces the draw. Each client receives a different announcement (unless it's a draw) as one wins and the other looses.

**Request**

```
END_OF_GAME <code>
```

- `<code>`
  - `WIN`: the player has won the game
  - `LOOSE`: the player lost the game
  - `DRAW`: it's a draw

**Response**

None

### Quit the server

To quit the server, the client can simply closes the connections. As the protocol is TCP based, the server will notice the connection has been closed, remove the client from the list of connected clients and close the connection on its side too. The server should never close the connection by itself, the client is responsible for closing the connection.