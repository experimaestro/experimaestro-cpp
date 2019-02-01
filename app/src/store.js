// @flow

import { createStore, Reducer, applyMiddleware } from 'redux'
import produce from "immer"
import createSagaMiddleware from 'redux-saga'
import { composeWithDevTools } from 'redux-devtools-extension'
import rootSaga from './sagas'

// ---- States

export type State = {}


// ---- Actions

type Action = 
    { type: "REFRESH_EXPERIMENTS" };



export const refreshExperiments = () : Action => ({ type: "REFRESH_EXPERIMENTS" });



// --- Reducer


const reducer : Reducer<State,Action> =
    produce((draft: State, action: Action) : void => {
        switch (action.type) {
        }
    }, {})


// --- Store


const sagaMiddleware = createSagaMiddleware();
const store = createStore(reducer, {}, composeWithDevTools(applyMiddleware(sagaMiddleware)))
sagaMiddleware.run(rootSaga)

export default store;