<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Decentralized Donations CorDapp

The decentralized donations demo presents a new way of contributing to charity through the security and transparency of the Corda platform. Therefore, by decentralizing contributions, the unreliability of middleman charity organizations becomes obsolete in this network, thus providing the donors with a much stronger feeling of security.

To better understand how this CorDapp is designed please see the slides [here](https://r3-cev.atlassian.net/wiki/spaces/FE/pages/3545104707/Decentralized+Donations+CorDapp+Alexandru+Birtoiu).

# Usage

There are 7 processes that you need to be aware of:

- Five Corda nodes: PartyA, PartyB, PartyC, Bank and a Notary
- The backend webserver:  the usual Spring Boot server that comes with Corda
- The frontend webserver: a React app that communicates to the backend server

### Pre-Requisites

If you have never built a corDapp see: https://docs.corda.net/getting-set-up.html.

## Running the nodes

To run the corda nodes you just need to run the `deployNodes` gradle task and then run them with the second commmand. Both of these commands should be used in a terminal such as Git Bash, after moving to this directory.

```
./gradlew deployNodes
./build/nodes/runnodes
```

## Running the Backend Server

To run the Backend Server use the following command in the main directory:
```
./gradlew runBackendServer
```

The Backend Server is configured on the port [localhost:10056](http://localhost:10056). You can find the configuration of the Spring Boot server in `clients/src/main/build.gradle`.

## Running the Frontend React App

First of all, you need to navigate in a terminal to the directory `clients/src/main/charity-demo`. Then run the commands:

```
npm install
npm start
```

The command should start the browser on the correct link automatically. If it doesn't go to [localhost:3000](http://localhost:3000).



