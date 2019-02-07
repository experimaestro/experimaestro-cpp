// @flow

import { createStore, Reducer, applyMiddleware } from 'redux'
import produce from "immer"
import createSagaMiddleware from 'redux-saga'
import { composeWithDevTools } from 'redux-devtools-extension'
import rootSaga from './sagas'
import _ from 'lodash';

// ---- States

type Experiment = {
    name: string;
}

export type Job = {
    jobId: string;
    taskId: string;

    locator: string;
    status: "running" | "done" | "error" | "waiting";

    start: string;
    end: string;
    submitted: string;

    tags: Array<[string, number|string|boolean]>;

    progress: number;
}
export type Jobs = {
    byId: { [string]: Job },
    ids: Array<string>
}

export type State = {
    connected: boolean;

    experiment: ?string;
    jobs: Jobs,
    experiments: {[string]: Experiment};
}


// ---- Actions

type Action = 
    { type: "CONNECTED", payload: boolean } // true if the connexion is open, false otherwise
    
    | { type: "CLEAN_INFORMATION" }

    | { type: "EXPERIMENT_ADD", payload: Experiment }
    | { type: "EXPERIMENT_SET_MAIN", payload: string }

    | { type: "JOB_ADD", payload: Job }
    | { type: "JOB_UPDATE", payload: $Shape<Job> }
;



// --- Reducer

const initialState : State = {
    connected: false,
    jobs: {
        byId: {},
        ids: []
    },
    experiments: {},
    experiment: null
}

const reducer : Reducer<State,Action> =
    produce((draft: State, action: Action) : void => {
        switch (action.type) {
            case "CONNECTED":
                draft.connected = action.payload;
                break;
            case "CLEAN_INFORMATION":
                draft.jobs = {
                    byId: {},
                    ids: []
                };
                draft.experiments = {};
                break;

            case "EXPERIMENT_SET_MAIN":
                draft.experiment = action.payload;
                break;

            case "EXPERIMENT_ADD":
                draft.experiments[action.payload.name] = action.payload;
                break;

            case "JOB_ADD":
                if (draft.jobs.byId[action.payload.jobId] === undefined) {
                    draft.jobs.ids.push(action.payload.jobId);
                }
                draft.jobs.byId[action.payload.jobId] = action.payload;
                break;

            case "JOB_UPDATE":
                if (draft.jobs.byId[action.payload.jobId] === undefined) {
                } else {
                    _.merge(draft.jobs.byId[action.payload.jobId], action.payload);
                }
                break;

            default: 
                break;
        }
    }, initialState)


// --- Store


const sagaMiddleware = createSagaMiddleware();
const store = createStore(reducer, initialState, composeWithDevTools(applyMiddleware(sagaMiddleware)))
sagaMiddleware.run(rootSaga)

export default store;