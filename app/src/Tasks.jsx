// @flow

import React, { Component } from 'react'
import { connect } from 'react-redux'

import { type State, type Jobs } from './store'

type StateProps = {
    jobs: Jobs
};

type Props = StateProps;

class Tasks extends Component<Props> {
    render() {
        let { jobs } = this.props;
        return <div>{
            jobs.ids.map((jobId) => {
                let job = jobs.byId[jobId];
                return <div key={jobId}><span className={`status status-${job.status}`}>{job.status}</span>{job.locator}</div>
            })
        }</div>;
    }
}

const mapStateToProps = (state: State) : StateProps => ({
    jobs: state.jobs
})
export default connect(mapStateToProps)(Tasks);