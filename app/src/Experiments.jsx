// @flow

import React, { Component } from 'react'
import { connect } from 'react-redux'
import { type State, refreshExperiments } from './store'


type DispatchProps = {
    refreshExperiments: typeof refreshExperiments
}
type Props = DispatchProps;

class Experiments extends Component<Props> {
    componentDidMount() {
        this.props.refreshExperiments();
    }
    render() {
        return <div>Hello</div>;
    }
}

const mapStateToProps = (state: State) => ({})
export default connect(mapStateToProps, { refreshExperiments })(Experiments);