// @flow

import React, { Component } from 'react';
import { connect } from 'react-redux';
import logo from './logo.png';

import './App.css';
import { type State } from './store';
import Tasks from './Tasks'
import Experiments from './Experiments'

type StateProps = { 
  connected: boolean,
  experiment: string
}
type Props = StateProps;

class App extends Component<Props> {
  render() {
    let { connected, experiment } = this.props;
    return (
      <div>
        <header className="App-header">
          <h1 className="App-title">Experimaestro {experiment ? " – " + experiment : ""}  <i className={`fab fa-staylinked ws-status ${connected ? "ws-link" : "ws-no-link" }`} /> </h1>
        </header>
        <Experiments/>
        <Tasks/>
      </div>
    );
  }
}

const mapStateToProps = (state: State) => ({ connected: state.connected, experiment: state.experiment })
export default connect(mapStateToProps)(App);
