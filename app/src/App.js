// @flow

import React, { Component } from 'react';
import { connect } from 'react-redux';
import logo from './logo.png';

import './App.css';
import { type State } from './store';
import Tasks from './Tasks'
import Experiments from './Experiments'

type StateProps = { connected: boolean }
type Props = StateProps;

class App extends Component<Props> {
  render() {
    let { connected } = this.props;
    return (
      <div className={`App ${connected ? "connected" : "not-connected"}`}>
        <header className="App-header">
          <img src={logo} className="App-logo" alt="logo"/>
          <h1 className="App-title">Experimaestro</h1>
          <h2>Experiment manager</h2>
        </header>
        <Experiments/>
        <Tasks/>
      </div>
    );
  }
}

const mapStateToProps = (state: State) => ({ connected: state.connected })
export default connect(mapStateToProps)(App);
