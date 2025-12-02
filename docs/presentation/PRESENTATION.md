---
theme: gaia
paginate: true
backgroundColor: #fff
backgroundImage: url('https://marp.app/assets/hero-background.svg')
style: |
  .columns {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem;
  }
---
<!-- _class: lead -->
# **Connect 4**

---
# **Demo time !**

Launch the server:

```bash
docker run -it --rm --name c4-server --network connect4-net ghcr.io/tigid0u/connect4-docker:latest server
```

Launch the clients:

```bash
docker run -it --rm --network connect4-net ghcr.io/tigid0u/connect4-docker:latest client -o c4-server
```

---

# **Structure of the application**

![height:500px](../classDiagram/connect4Diagram.png)

---

# **Connect 4 library**

![connect-lib](./img/connect-lib.png)

---
# **Connect 4 Server**

- **TODO**: list server commands
---

# **Connect 4 Server**

<div class="columns">
<div>

![server-1](./img/connect-server-1.png)

</div>
<div>

![server-2](./img/connect-server-2.png)

</div>
</div>

---
# **Connect 4 Client - Commands**

The client supports the following commands:

- **JOIN <username>**: Join the game with the specified username.
- **READY**: Indicate that the player is ready to start the game.
- **PLAY <column>**: Drop a disc into the specified column (0-6).

---

# **Connect 4 Client**
<div class="columns">
<div>

![client-1](./img/connect-client-1.png)

</div>
<div>

![client-2](./img/connect-client-2.png)

</div>
</div>

---
<!-- _class: lead -->
# **Any questions ?**
