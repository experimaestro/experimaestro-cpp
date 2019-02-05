// @flow

import React, { Component } from 'react'
import { connect } from 'react-redux'
import { type State } from './store'


type DispatchProps = {
}
type Props = { ...DispatchProps,
    experiment: string
}

class Experiments extends Component<Props> {
    render() {
        let { experiment } = this.props;
        return <div>
            <h2>Experiment "{experiment}"</h2>
            </div>;
    }
}

const mapStateToProps = (state: State) => ({
    experiment: state.experiment
})
export default connect(mapStateToProps, {  })(Experiments);