// -----------------------------------------------------------
//
// Definition
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';
import {Button, Card} from "react-bootstrap";

import RestCallService from "../services/RestCallService";
import {InlineNotification} from "carbon-components-react";
import {ArrowRepeat} from "react-bootstrap-icons";
import {ChevronDown, ChevronRight, DataCheck, IbmKnowledgeCatalogStandard, Timer} from '@carbon/icons-react';


class Scenario extends React.Component {


    constructor(_props) {
        super();

        this.state = {
            scenario: [],
            openIds: new Set(),
            display: {
                loading: false
            },
        };
    }

    componentDidMount() {
        this.refreshList();
    }

    /*           {JSON.stringify(this.state.runners, null, 2) } */
    render() {
        return (
            <div className="container">


                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-10">
                        <h1 className="title">Scenario</h1>
                        <InlineNotification kind="info" hideCloseButton="true" lowContrast="false">
                            Scenario
                        </InlineNotification>
                    </div>

                    <div className="col-md-2">
                        <Button className="btn btn-success btn-sm"
                                onClick={() => {
                                    this.refreshList()
                                }}
                                disabled={this.state.display.loading}>
                            <ArrowRepeat/> Refresh
                        </Button>
                    </div>
                </div>
                <div className="row" style={{width: "100%", marginTop: "10px"}}>
                    <div className="col-md-12">
                        <Card>
                            <Card.Header style={{backgroundColor: "rgba(0,0,0,.03)"}}>Test result</Card.Header>
                            <Card.Body>

                                <table id="runnersTable" className="table is-hoverable is-fullwidth">
                                    <thead>
                                    <tr>
                                        <th>Scenario Name</th>
                                        <th>Server</th>
                                        <th>Type Scenario</th>
                                        <th></th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {this.state.scenario ? this.state.scenario.map((item, _index) =>
                                        <React.Fragment key={_index}>
                                            <tr>
                                                <td>{item.name}</td>
                                                <td>{item.server}</td>
                                                <td>{item.typeScenario}</td>
                                                <td>

                                                    <button onClick={() => this.toggleDetail(item.id)}>
                                                        {this.state.openIds.has(item.id) ? <ChevronDown/> :
                                                            <ChevronRight/>}
                                                    </button>
                                                </td>
                                            </tr>
                                            {this.state.openIds.has(item.id) &&
                                                (Array.isArray(item.executions) ? item.executions : []).map((execution, idx) => (
                                                    <React.Fragment key={idx}>
                                                        <tr>
                                                            <td colSpan="3">
                                                                <table style={{width: '100%'}}>
                                                                    <tr>
                                                                        <td>
                                                                            <h5>Execution: {execution.name}</h5>
                                                                        </td>
                                                                        <td>
                                                                            {execution.description}
                                                                        </td>
                                                                        <td>
                                                                            Policy: {execution.policy}
                                                                        </td>
                                                                        <td>
                                                                            Process Instances: {execution.numberProcessInstances}
                                                                        </td>
                                                                        <td>
                                                                            Threads: {execution.numberOfThreads}
                                                                        </td>
                                                                    </tr>
                                                                </table>
                                                            </td>
                                                        </tr>
                                                        <tr>
                                                            <td colSpan="3">
                                                                <div style={{
                                                                    border: "2px solid #3498db",
                                                                    borderRadius: "8px",
                                                                    padding: "16px",
                                                                    width: '100%'
                                                                }}>
                                                                    <h6>Steps</h6>
                                                                    <table style={{width: '100%'}}
                                                                           className="table is-hoverable is-fullwidth">
                                                                        <tr>
                                                                            <th>Type</th>
                                                                            <th>TaskId</th>
                                                                            <th>Explanation</th>
                                                                        </tr>
                                                                        {(Array.isArray(execution.steps) ? execution.steps : []).map((step, idx) => (
                                                                            <tr>
                                                                                <td>{step.type}</td>
                                                                                <td>{step.taskId}</td>
                                                                                <td>{step.synthesis}</td>
                                                                            </tr>
                                                                        ))}
                                                                    </table>
                                                                </div>
                                                            </td>
                                                        </tr>

                                                        <tr>
                                                            <td colSpan="3">
                                                                <div style={{
                                                                    border: "2px solid #3498db",
                                                                    borderRadius: "8px",
                                                                    padding: "16px",
                                                                    width: '100%'
                                                                }}>
                                                                    <h6>Verification</h6>
                                                                    <table
                                                                        className="table is-hoverable is-fullwidth">
                                                                        <tr>
                                                                            <th></th>
                                                                            <th>Type</th>
                                                                            <th>Verifications</th>
                                                                        </tr>


                                                                        {/* Loop through verifications activity */}
                                                                        {Array.isArray(execution.verifications?.activities) && execution.verifications?.activities.map((activity, activityIdx) => (
                                                                            <tr key={{activityIdx}}>
                                                                                <td style={{paddingRight: "20px"}}>
                                                                                    <IbmKnowledgeCatalogStandard/>
                                                                                </td>
                                                                                <td>Activity
                                                                                </td>
                                                                                <td>{activity.synthesis}</td>
                                                                            </tr>
                                                                        ))}

                                                                        {/* Loop through verifications variables */}
                                                                        {Array.isArray(execution.verifications?.variables) && execution.verifications?.variables.map((variable, variableIdx) => (
                                                                            <tr key={{variableIdx}}>
                                                                                <td style={{paddingRight: "20px"}}>
                                                                                    <DataCheck/></td>
                                                                                <td>Variable</td>
                                                                                <td>{variable.synthesis}</td>
                                                                            </tr>
                                                                        ))}
                                                                        {/* Loop through verifications performance */}
                                                                        {Array.isArray(execution.verifications?.performances) && execution.verifications?.performances.map((performance, performanceIdx) => (
                                                                            <tr key={{performanceIdx}}>
                                                                                <td style={{paddingRight: "20px"}}>
                                                                                    <Timer/></td>
                                                                                <td>Performance</td>
                                                                                <td>{performance.synthesis}</td>
                                                                            </tr>
                                                                        ))}</table>
                                                                </div>
                                                            </td>
                                                        </tr>
                                                    </React.Fragment>
                                                ))}
                                        </React.Fragment>
                                    ) : <div/>}
                                    </tbody>
                                </table>


                            </Card.Body>
                        </Card>
                    </div>
                </div>

                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-2">
                        <Button className="btn btn-warning btn-sm"
                                disabled
                                onClick={() => {
                                    this.startAll()
                                }}
                                disabled={true}>
                            Start All tests
                        </Button>
                    </div>
                </div>
            </div>
        );
    }


    refreshList() {
        console.log("Definition.refreshList http[pea/api/content/list]");
        this.setState({runners: [], status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson('pea/api/content/list?details=true', this, this.refreshListCallback);
    }

    refreshListCallback(httpPayload) {
        if (httpPayload.isError()) {
            this.setState({status: "Error"});
        } else {
            this.setState({scenario: httpPayload.getData()});

        }
    }

    startAll() {
        console.log("Definition.refreshList http[/pea/api/unittest/runall]");
        this.setState({runners: [], status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson('/pea/api/unittest/runall?wait=false&server=Camunda8Ruby', this, this.refreshListCallback);
    }

    toggleDetail(id) {
        const openIds = new Set(this.state.openIds);

        if (openIds.has(id)) {
            openIds.delete(id);
        } else {
            openIds.add(id);
        }
        this.setState({openIds: openIds});
    };
}

export default Scenario;
