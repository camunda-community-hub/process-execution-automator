// -----------------------------------------------------------
//
// Definition
//
// List of all runners available
//
// -----------------------------------------------------------
import React, {createRef} from 'react';
import {Button, Card} from "react-bootstrap";

import RestCallService from "../services/RestCallService";
import {FileUploader, InlineNotification, Tag} from "carbon-components-react";

import {ArrowRepeat} from "react-bootstrap-icons";
import {ChevronDown, ChevronRight, DataCheck, IbmKnowledgeCatalogStandard, Timer, TrashCan} from '@carbon/icons-react';
import StepDisplay from "../component/StepDisplay";


class Scenario extends React.Component {


    constructor(_props) {
        super();
        this.fileUploaderRef = createRef();

        this.state = {
            scenario: [],
            openIds: new Set(),
            scenarioFiles: [],
            preferateServer: "",
            statusUpload: [],
            statusRun: "",
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
                                    <thead className="table-light sticky-top">
                                    <tr>
                                        <th>Scenario Name</th>
                                        <th>Description</th>
                                        <th>Process</th>
                                        <th>Server</th>
                                        <th>Type Scenario</th>
                                        <th>Version</th>
                                        <th></th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {this.state.scenario ? this.state.scenario.map((item, _index) =>
                                        <React.Fragment key={_index}>
                                            <tr>
                                                <td>{item.name}</td>
                                                <td>{item.description}</td>
                                                <td> {item.processId && item.processId.trim() !== "" ?
                                                    `ID: ${item.processId}`
                                                    : `Name: ${item.processName} / Version: ${item.version}`}</td>
                                                <td>{item.server}</td>
                                                <td>{item.typeScenario}</td>
                                                <td>{item.versionTest}</td>
                                                <td>
                                                    <div style={{display: "flex", alignItems: "center"}}>
                                                        <Button className="btn btn-info btn-sm"
                                                                onClick={() => this.runTest(item.name)}
                                                                disabled={this.state.display.loading || this.state.scenarioFiles.size === 0}>
                                                            Start
                                                        </Button>
                                                        <button onClick={() => this.toggleDetail(item.name)}
                                                                style={{
                                                                    marginLeft: "10px",
                                                                    background: "none",
                                                                    border: "none",
                                                                    cursor: "pointer",
                                                                    padding: 0
                                                                }}>
                                                            {this.state.openIds.has(item.name) ?
                                                                <ChevronDown className="my-chevron"/> :
                                                                <ChevronRight className="my-chevron"/>}
                                                        </button>
                                                    </div>
                                                </td>
                                            </tr>
                                            {this.state.openIds.has(item.name) &&
                                                (Array.isArray(item.executions) ? item.executions : []).map((execution, idx) => (
                                                    <React.Fragment key={idx}>
                                                        <tr>
                                                            <td colSpan="7" style={{paddingLeft: "30px"}}>
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
                                                                            Process
                                                                            Instances: {execution.numberProcessInstances}
                                                                        </td>
                                                                        <td>
                                                                            Threads: {execution.numberOfThreads}
                                                                        </td>
                                                                    </tr>
                                                                </table>
                                                            </td>
                                                        </tr>
                                                        <tr>
                                                            <td colSpan="7" style={{paddingLeft: "30px"}}>
                                                                <div style={{
                                                                    border: "2px solid #3498db",
                                                                    borderRadius: "8px",
                                                                    padding: "16px",
                                                                    width: '100%'
                                                                }}>
                                                                    <h6>Simulation Steps</h6>
                                                                    <table
                                                                        className="table table-striped table-hover w-100">
                                                                        <thead className="table-light sticky-top">
                                                                        <tr>
                                                                            <th>Type</th>
                                                                            <th>TaskId</th>
                                                                            <th>Explanation</th>
                                                                        </tr>
                                                                        </thead>
                                                                        <tbody>
                                                                        {(Array.isArray(execution.steps) ? execution.steps : []).map((step, idx) => (
                                                                            <tr key={idx}>
                                                                                <td className="text-center align-middle">
                                                                                    <StepDisplay type={step.type}/>

                                                                                </td>
                                                                                <td>{step.taskId}</td>
                                                                                <td>{step.synthesis}</td>
                                                                            </tr>
                                                                        ))}
                                                                        </tbody>
                                                                    </table>
                                                                </div>
                                                            </td>
                                                        </tr>

                                                        <tr>
                                                            <td colSpan="7" style={{paddingLeft: "30px"}}>
                                                                <div style={{
                                                                    border: "2px solid #3498db",
                                                                    borderRadius: "8px",
                                                                    padding: "16px",
                                                                    width: '100%'
                                                                }}>
                                                                    <h6>Verifications</h6>
                                                                    <table
                                                                        className="table table-striped table-hover w-100">
                                                                        <thead className="table-light sticky-top">
                                                                        <tr>
                                                                            <th></th>
                                                                            <th>Type</th>
                                                                            <th>Verifications</th>
                                                                        </tr>
                                                                        </thead>
                                                                        <tbody>
                                                                        {/* Loop through verifications activity */}
                                                                        {Array.isArray(execution.verifications?.activities) && execution.verifications?.activities.map((activity, activityIdx) => (
                                                                            <tr key={activityIdx}>
                                                                                <td style={{paddingRight: "20px"}}>
                                                                                    <IbmKnowledgeCatalogStandard/>
                                                                                </td>
                                                                                <td>{activity.type}
                                                                                </td>
                                                                                <td>{activity.synthesis}</td>
                                                                            </tr>
                                                                        ))}

                                                                        {/* Loop through verifications variables */}
                                                                        {Array.isArray(execution.verifications?.variables) && execution.verifications?.variables.map((variable, variableIdx) => (
                                                                            <tr key={variableIdx}>
                                                                                <td style={{paddingRight: "20px"}}>
                                                                                    <DataCheck/></td>
                                                                                <td>VARIABLE</td>
                                                                                <td>{variable.synthesis}</td>
                                                                            </tr>
                                                                        ))}
                                                                        {/* Loop through verifications performance */}
                                                                        {Array.isArray(execution.verifications?.performances) && execution.verifications?.performances.map((performance, performanceIdx) => (
                                                                            <tr key={performanceIdx}>
                                                                                <td style={{paddingRight: "20px"}}>
                                                                                    <Timer/></td>
                                                                                <td>Performance</td>
                                                                                <td>{performance.synthesis}</td>
                                                                            </tr>
                                                                        ))}
                                                                        </tbody>
                                                                    </table>
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

                <div className="row" style={{width: "100%", marginTop: "10px"}}>
                    <div className="col-md-10">
                        <Button className="btn btn-info btn-sm"
                                onClick={() => {
                                    this.runAll()
                                }}
                                disabled={this.state.display.loading || this.state.scenarioFiles.size === 0}>
                            Start All tests
                        </Button>
                        <br/>
                        Default server: {this.state.preferateServer}
                        {this.state.statusRun && <div>Status: {this.state.statusRun}</div>}
                    </div>
                    <div className="col-md-2">
                        <Button className="btn btn-danger btn-sm"
                                onClick={() => {
                                    this.clearAll()
                                }}
                        >
                            <TrashCan/> Clear all scenario
                        </Button>
                    </div>
                </div>

                <div className="row" style={{width: "100%", marginTop: "10px"}}>
                    <div className="col-md-6">
                        <Card>
                            <Card.Header style={{backgroundColor: "rgba(0,0,0,.03)"}}>Upload</Card.Header>
                            <Card.Body>
                                <FileUploader
                                    ref={this.fileUploaderRef}
                                    labelTitle="Upload Scenario files"
                                    labelDescription="Only .json file"
                                    buttonLabel="Add files to upload"
                                    filenameStatus="edit"
                                    accept={['.json']}
                                    onChange={(event) => this.handleFileChange(event)}
                                    multiple
                                    iconDescription="Clear file"
                                    disabled={this.state.display.loading || this.state.scenarioFiles.size === 0}
                                />
                                <Button onClick={() => this.loadScenario()}
                                        disabled={this.state.display.loading}>Upload</Button>
                                <br/>


                                <table id="uploadTable" className="table is-hoverable is-fullwidth"
                                       style={{marginTop: "10px"}}>
                                    <thead className="table-light sticky-top">
                                    <tr>
                                        <th>Status</th>
                                        <th>Filename</th>
                                        <th>Error</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {this.state.statusUpload.map((item, index) => (
                                        <tr key={index}>
                                            <td>
                                                <Tag type={item.status === "ERROR" ? "red" : "green"}>
                                                    {item.status}
                                                </Tag>
                                            </td>
                                            <td className="small">{item.filename}</td>
                                            <td className="small text-danger">{item.error}</td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </Card.Body>
                        </Card>
                    </div>
                </div>
            </div>
        )
            ;
    }


    refreshList() {
        console.log("Definition.refreshList http[pea/api/content/list]");
        this.setState({runners: [], status: "", statusUploadSuccess: '', statusUploadFailed: "", preferateServer: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson('pea/api/content/list?details=true', this, this.refreshListCallback);

        console.log("Definition.refreshList http[pea/api/server/list?details=false]");
        restCallService.getJson('pea/api/server/list?details=false', this, this.refreshPreferateCallback);
    }

    refreshListCallback(httpPayload) {
        this.setDisplayProperty("loading", false);

        if (httpPayload.isError()) {
            this.setState({status: "Error"});
        } else {
            this.setState({scenario: httpPayload.getData()});
        }
    }


    refreshPreferateCallback(httpPayload) {

        if (httpPayload.isError()) {
            this.setState({status: "Error"});
        } else {
            this.setState({preferateServer: httpPayload.getData().preferateServer});
        }
    }

    runAll() {
        console.log("Scenario.runAll http[/pea/api/unittest/runall]");
        this.setState({runners: [], status: ""});
        this.setDisplayProperty("loading", true);

        var restCallService = RestCallService.getInstance();
        var param = {};
        restCallService.postJson('/pea/api/unittest/runall?wait=false', param, this, this.runAllCallback);
    }

    runAllCallback(httpPayload) {
        this.setDisplayProperty("loading", false);
        if (httpPayload.isError()) {
            this.setState({statusRun: "Error; test didn't start"});
        } else {
            this.setState({statusRun: "Started"});
        }
    }

    runTest(scenarioName) {
        console.log("Scenario.runAll http[/pea/api/unittest/run] scenarioName:" + scenarioName);
        this.setState({runners: [], status: ""});
        this.setDisplayProperty("loading", true);

        var restCallService = RestCallService.getInstance();
        var param = {};
        restCallService.postJson('/pea/api/unittest/run?name=' + scenarioName + '&wait=false', param, this, this.runAllCallback);

    }

    clearAll() {
        let uri = 'pea/api/content/clearall?';
        console.log("TestResult.clearAll http[" + uri + "]");

        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.putJson(uri, {}, this, this.clearAllCallback);
    }

    clearAllCallback(httpPayload) {
        console.log("DashBoard.refreshTestResultCallback");

        this.setDisplayProperty("loading", false);
        if (httpPayload.isError()) {
            console.log("TestResult.clearAllCallback: error " + httpPayload.getError());
            this.setState({status: "Error"});
        } else {
            this.setState({scenario: []});

        }
    }

    handleFileChange(event) {
        const fileList = event.target.files;
        this.setState({scenarioFiles: fileList}); // Use spread operator to create a new array
    };


    loadScenario(event) {
        console.log("Load Scenario ", this.state.files);
        var restCallService = RestCallService.getInstance();

        const formData = new FormData();
        Array.from(this.state.scenarioFiles).forEach((file, index) => {
            formData.append(`scenarioFiles`, file);
        });
        /* formData.append("File", this.state.files[0]); */
        this.setDisplayProperty("loading", true);

        restCallService.postUpload('pea/api/content/add?', formData, this, this.operationUploadScenarioCallback);


        // dispatch(connectorService.uploadJar(event.target.files[0]));
    }

    operationUploadScenarioCallback(httpResponse) {
        this.setDisplayProperty("loading", false);

        if (httpResponse.isError()) {
            console.log("operationUploadScenarioCallback.operationUploadScenarioCallback: error " + httpResponse.getError());
            this.setState({statusUploadSuccess: "", statusUploadFailed: httpResponse.getError()});
        } else {
            // Clear the file input field using JavaScript
            if (this.fileUploaderRef.current) {
                this.fileUploaderRef.current.clearFiles();
            }
            this.setState({
                'scenarioFiles': [],
                statusUpload: httpResponse.getData()
            });
        }
        this.refreshList();
    }

    toggleDetail(id) {
        const openIds = new Set(this.state.openIds);

        if (openIds.has(id)) {
            openIds.delete(id);
        } else {
            openIds.add(id);
        }
        console.log("AFTER Toggle [" + id + "] openids:", openIds);

        this.setState({openIds: openIds});
    };

    /**
     * Set the display property
     * @param propertyName name of the property
     * @param propertyValue the value
     */
    setDisplayProperty = (propertyName, propertyValue) => {
        let displayObject = this.state.display;
        displayObject[propertyName] = propertyValue;
        this.setState({display: displayObject});
    }

}

export default Scenario;
