// @flow

import React, { Component } from 'react'
import { connect } from 'react-redux'
import Clipboard from 'react-clipboard.js';
import { toast } from 'react-toastify';

import { type State, type Jobs } from './store'
import confirm from 'util/confirm';
import client from './client'

type StateProps = {
    jobs: Jobs
};

type Props = StateProps;

class Tasks extends Component<Props> {
    kill = (jobId: string) => {
        confirm('Are you sure to kill this job?').then(() => {
            client.send({ type: "kill", payload: jobId}, "cannot kill job " + jobId)
        }, () => {
            toast.info("Action cancelled");
        });
    }

    details = (jobId: string) => {
        client.send({ type: "details", payload: jobId}, "cannot get details for job " + jobId)        
    }

    render() {
        let { jobs } = this.props;
        return <div id="resources">{
            jobs.ids.map((jobId) => {
                let job = jobs.byId[jobId];
                return <div className="resource" key={jobId}>
                    {
                        job.status === "running" ?
                        <React.Fragment>
                            <span className="status progressbar-container" title={`${job.progress*100}%`}>
                                <span style={{right: `${(1-job.progress)*100}%`}} className="progressbar"></span><div className="status-running">{job.status}</div>
                            </span> 
                            <i className="fa fa-skull-crossbones action" onClick={() => this.kill(jobId) }/>
                        </React.Fragment>
                        :
                        <span className={`status status-${job.status}`}>{job.status}</span>
                    }
                    <i className="fas fa-eye action" title="Details" onClick={() => this.details(jobId)}/>
                    <span className="job-id"><Clipboard className="clipboard" data-clipboard-text={`${job.taskId}/${job.jobId}`} onSuccess={() => toast.success(`Job path copied`)}>{job.taskId}</Clipboard></span>
                    {
                        job.tags.map((tv) => {
                            return <span key={tv[0]} className="tag">
                                <span className="name">{tv[0]}</span>
                                <span className="value">{tv[1]}</span>
                            </span>
                        })
                    }
                </div>
            })
        }</div>;
    }
}

const mapStateToProps = (state: State) : StateProps => ({
    jobs: state.jobs
})
export default connect(mapStateToProps)(Tasks);