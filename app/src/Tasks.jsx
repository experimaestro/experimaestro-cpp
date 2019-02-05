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
        return <div id="resources">{
            jobs.ids.map((jobId) => {
                let job = jobs.byId[jobId];
                return <div className="resource" key={jobId}>
                    {
                        job.status == "running" ?
                        <span className="status progressbar-container"><span style={{right: `${(1-job.progress)*100}%`}} className="progressbar"></span><div className="status-running">{job.status}</div></span> :
                        <span className={`status status-${job.status}`}>{job.status}</span>
                    }
                    <span className="task-id">{job.taskId}</span>
                    <span className="job-id">{job.jobId}</span>
                </div>
            })
        }</div>;
    }
}

const mapStateToProps = (state: State) : StateProps => ({
    jobs: state.jobs
})
export default connect(mapStateToProps)(Tasks);