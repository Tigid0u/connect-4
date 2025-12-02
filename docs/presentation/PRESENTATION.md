---
theme: gaia
paginate: true
backgroundColor: #fff
backgroundImage: url('https://marp.app/assets/hero-background.svg')
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
| ![server-1](./img/connect-server-1.png) | ![server-2](./img/connect-server-2.png) |
| -------------------------------- | ------------------------------- |

---
# **Connect 4 Client**

- **TODO**: list client commands

---

# **Connect 4 Client**
| ![client-1](./img/connect-client-1.png) | ![client-2](./img/connect-client-2.png) |
| -------------------------------- | ------------------------------- |

---
<!-- _class: lead -->
# **Any questions ?**
