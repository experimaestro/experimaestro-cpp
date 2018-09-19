// @flow

import React, { Component } from 'react';

import Client from './client';
import logo from './logo.png';
import './App.css';

var { ForceGraph2D } = require('react-force-graph');

class App extends Component<{}> {
  websocket: any;

  componentDidMount() {
    this.websocket = new Client();
  }

  render() {
    let myData = {
      nodes: [{
        "id": "id1",
        "name": "name1",
        "val": 1
      },
      {
        "id": "id2",
        "name": "name2",
        "val": 10
      }],
      links: [
        {
          "source": "id1",
          "target": "id2"
        },
      ]
    }
    return (
      <div className="App">
        <header className="App-header">
          <img src={logo} className="App-logo" alt="logo" />
          <h1 className="App-title">Experimaestro</h1>
          <h2>Experiment manager</h2>
        </header>
        <p className="App-intro">
        </p>
        <ForceGraph2D
          
          graphData={myData}
        />
      </div>
    );
  }
}

export default App;
