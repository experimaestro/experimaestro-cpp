// @flow

import React, { Component } from 'react';

import logo from './logo.png';
import './App.css';
import Tasks from './Tasks'
import Experiments from './Experiments'

class App extends Component<{}> {
  render() {
    return (
      <div className="App">
        <header className="App-header">
          <img src={logo} className="App-logo" alt="logo" />
          <h1 className="App-title">Experimaestro</h1>
          <h2>Experiment manager</h2>
        </header>
        <Experiments/>
        <Tasks/>
      </div>
    );
  }
}

export default App;
