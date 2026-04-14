// -----------------------------------------------------------
//
// Step display
//
// Display a step, wuth an image
//
// -----------------------------------------------------------
import React from 'react';
import {BarElement, CategoryScale, Chart as ChartJS, Legend, LinearScale, Title, Tooltip} from 'chart.js'
import {Bar, Doughnut} from 'react-chartjs-2';


class StepDisplay extends React.Component {



    /** Caller must declare  this.props.changePasswordCallback(isCorrect, password ) */
    constructor(props) {
        super();
        this.state = {
            type: props.type,

        };


    }


    render() {
        // console.log("Chart.render: type=" + this.state.type + " state=" + JSON.stringify(this.state))


        // ------------------------------------------- Doughnut

        return (<div>
                {this.state.type === "STARTEVENT" ? (
                    <div>
                        <img src="/img/StartEvent.png" alt="Start Event" width="40" />
                    </div>
                ) : null}
                {this.state.type === "SERVICETASK" ? (
                    <div>
                        <img src="/img/ServiceTask.png" alt="Start Event" width="40" />
                    </div>
                ) : null}

                {this.state.type === "USERTASK" ? (
                    <div>
                        <img src="/img/UserTask.png" alt="User task" width="40" />
                    </div>
                ) : null}
                {this.state.type === "MESSAGE" ? (
                    <div>
                        <img src="/img/Message.png" alt="User task" width="40" />
                    </div>
                ) : null}
                {this.state.type === "ENDEVENT" ? (
                    <div>
                        <img src="/img/EndEvent.png" alt="User task" width="40" />
                    </div>
                ) : null}

                {this.state.type === "EXCLUSIVEGATEWAY" ? (
                    <div>
                        <img src="/img/ExclusiveGateway.png" alt="User task" width="40" />
                    </div>
                ) : null}
                {this.state.type === "PARALLELGATEWAY" ? (
                    <div>
                        <img src="/img/ParallelGateway.png" alt="User task" width="40" />
                    </div>
                ) : null}
                {this.state.type === "TASK" ? (
                    <div>
                        <img src="/img/Task.png" alt="User task" width="40" />
                    </div>
                ) : null}
                {this.state.type === "SCRIPTTASK" ? (
                    <div>
                        <img src="/img/ScriptTask.png" alt="User task" width="40" />
                    </div>
                ) : null}

                <div style={{fontSize: "0.75rem"}}>{this.state.type}</div>
            </div>
        )


    }



}

export default StepDisplay;


/* target Doughnut*/
