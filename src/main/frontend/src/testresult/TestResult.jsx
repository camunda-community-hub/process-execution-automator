// -----------------------------------------------------------
//
// TestResult
//
// Manage the dashboard. Root component
//
// -----------------------------------------------------------

import React from 'react';

import {Button, InlineNotification, Tag} from "carbon-components-react";
import {ChevronDown, ChevronRight, IbmKnowledgeCatalogStandard, DataCheck, Timer, TrashCan } from '@carbon/icons-react';


import {Card} from 'react-bootstrap';
import RestCallService from "../services/RestCallService";
import {ArrowRepeat} from "react-bootstrap-icons";


class TestResult extends React.Component {


    constructor(_props) {
        super();
        this.state = {
            testresults: [],
            openIds: new Set(),
            display: {
                loading: false
            },
        };
        this.schedule = this.schedule.bind(this);
        this.setDisplayProperty = this.setDisplayProperty.bind(this);
    }

    componentDidMount() {
        this.refreshTestResult();


        // Set up the interval to call schedule() every 30 seconds
        this.intervalId = setInterval(this.schedule, 120000);
    }

    // Cleanup to clear the interval when the component unmounts
    componentWillUnmount() {
        clearInterval(this.intervalId);
    }

    render() {
        // console.log("dashboard.render display="+JSON.stringify(this.state.display));
        return (<div className={"container"}>

                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-10">
                        <h1 className="title">Test Result</h1>
                        <InlineNotification kind="info" hideCloseButton="true" lowContrast="false">
                            Test result for each execution
                        </InlineNotification>
                    </div>

                    <div className="col-md-2">
                        <Button className="btn btn-success btn-sm"
                                onClick={() => {
                                    this.refreshTestResult()
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
                                        <th>ID</th>
                                        <th>Status</th>
                                        <th>Start</th>
                                        <th>End</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {this.state.testresults ? this.state.testresults.map((item, _index) =>
                                        <React.Fragment key={_index}>
                                            <tr>
                                                <td>{item.scenarioName}</td>
                                                <td>{item.id}</td>
                                                <td>
                                                    {item.status === "INPROGRESS" &&
                                                        <Tag type="blue">In progress</Tag>
                                                    }

                                                    {item.result === "FAIL" &&
                                                            <Tag type="red">Fail</Tag>
                                                    }
                                                    {item.result === "SUCCESS" &&
                                                        <Tag type="green">Success</Tag>
                                                    }

                                                    <button disabled={item.status === "INPROGRESS"}
                                                        onClick={() => this.toggleDetail(item.id)}>
                                                        {this.state.openIds.has(item.id) ? <ChevronDown/> :
                                                            <ChevronRight/>}
                                                    </button>
                                                </td>
                                                <td>
                                                    {this.dateDisplay(item.startDate)}
                                                </td>
                                                <td>
                                                    {this.dateDisplay(item.endDate)}
                                                </td>
                                            </tr>
                                            {this.state.openIds.has(item.id) &&
                                                (Array.isArray(item.tests) ? item.tests : []).map((test, idx) => (
                                                    <tr key={idx}>
                                                        <td colSpan="5">
                                                            <table style={{width: '100%'}}>
                                                                <tr>
                                                                    <td>
                                                                        <h5>Test: {test.name}</h5>
                                                                    </td>
                                                                    <td>
                                                                        {test.description}
                                                                    </td>
                                                                    <td>
                                                                        ProcessInstance: {test.processInstancesId}
                                                                        Server: {this.state.testresults.serverName}
                                                                    </td>
                                                                    <td style={{textAlign: 'right'}}>
                                                                        {test.result === "SUCCESS" &&
                                                                            <Tag type="green">Success</Tag>
                                                                        }
                                                                        {test.result === "FAIL" &&
                                                                            <Tag type="red">Fail</Tag>
                                                                        }
                                                                    </td>
                                                                </tr>

                                                            </table>


                                                            <table border="1" width="100%">
                                                                <thead>
                                                                <tr>
                                                                    <th></th>
                                                                    <th>Info</th>
                                                                    <th>Message</th>
                                                                    <th>Result</th>
                                                                </tr>
                                                                </thead>
                                                                <tbody>
                                                                {(Array.isArray(test.detail) ? test.detail : []).map((d, i) => (
                                                                    <tr key={i}>
                                                                        <td style={{paddingRight: "20px"}}>
                                                                            {d.typeVerification === "GOBYTASK" &&
                                                                                <IbmKnowledgeCatalogStandard /> }
                                                                            {d.typeVerification === "VARIABLE" &&
                                                                                <DataCheck />
                                                                            }
                                                                            {d.typeVerification === "PERFORMANCE" &&
                                                                                <Timer />
                                                                            }
                                                                        </td>
                                                                        <td>{d.info}</td>
                                                                        <td>{d.message}</td>
                                                                        <td>
                                                                            {d.result === "SUCCESS" &&
                                                                                <Tag type="green">Success</Tag>
                                                                            }
                                                                            {d.result === "FAIL" &&
                                                                                <Tag type="red">Fail</Tag>
                                                                            }
                                                                        </td>
                                                                    </tr>
                                                                ))}
                                                                </tbody>
                                                            </table>
                                                        </td>
                                                    </tr>
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
                                disabled
                                onClick={() => {
                                    this.startAll()
                                }}
                                disabled={true}>
                            Start All tests
                        </Button>
                    </div>
                    <div className="col-md-2">
                        <Button className="btn btn-danger btn-sm"
                                onClick={() => {
                                    this.clearAll()
                                }}
                                disabled={this.state.display.loading}>
                            <TrashCan /> Clear All test
                        </Button>
                    </div>

                </div>
            </div>
        )

    }


    refreshTestResult = () => {
        let uri = 'pea/api/unittest/list?details=true';
        console.log("TestResult.refreshTestResult http[" + uri + "]");

        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson(uri, this, this.refreshTestResultCallback);
    }

    refreshTestResultCallback = (httpPayload) => {
        console.log("DashBoard.refreshTestResultCallback");

        this.setDisplayProperty("loading", false);
        if (httpPayload.isError()) {
            console.log("TestResult.refreshTestResultCallback: error " + httpPayload.getError());
            this.setState({status: "Error"});
        } else {
            this.setState({testresults: httpPayload.getData()});

        }
    }
    startAll() {
        console.log("Definition.refreshList http[/pea/api/unittest/runall]");
        this.setState({runners: [], status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson('/pea/api/unittest/runall?wait=false&server=Camunda8Ruby', this, this.refreshListCallback);
    }

    clearAll ()  {
        let uri = '/pea/api/unittest/clearall?';
        console.log("TestResult.clearAll http[" + uri + "]");

        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.putJson(uri, {}, this, this.clearAllCallback);
    }

    clearAllCallback  (httpPayload) {
        console.log("DashBoard.refreshTestResultCallback");

        this.setDisplayProperty("loading", false);
        if (httpPayload.isError()) {
            console.log("TestResult.clearAllCallback: error " + httpPayload.getError());
            this.setState({status: "Error"});
        } else {
            this.setState({testresults: []});

        }
    }

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

    schedule() {
        let uri = 'pea/api/unittest/list?details=true';
        console.log("DashBoard.schedule Schedule http[" + uri + "]");

        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson(uri, this, this.refreshTestResultCallback);


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


    dateDisplay = (isoDate) => {
        if (isoDate===null || isoDate==="")
            return "";
        return new Date(isoDate).toLocaleString(); // local timezone
    };
}

export default TestResult;
