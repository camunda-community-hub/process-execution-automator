// -----------------------------------------------------------
//
// TestResult
//
// Manage the dashboard. Root component
//
// -----------------------------------------------------------

import React from 'react';

import {Button, InlineNotification, Tag} from "carbon-components-react";
import {ChevronDown, ChevronRight} from '@carbon/icons-react';


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
                loading: true
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
                                        <th>Result</th>
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
                                            <td>{item.status}</td>
                                            <td>
                                                {item.result === "INPROGRESS" &&
                                                    <div>
                                                        <Tag type="blue">In progress</Tag><br/>
                                                    </div>
                                                }
                                                {item.result === "FAILED" &&
                                                    <div>
                                                        <Tag type="red">Failed</Tag><br/>

                                                    </div>
                                                }
                                                {item.result === "SUCCESS" &&
                                                    <Tag type="green">Success</Tag>}

                                                <button onClick={() => this.toggleDetail(item.id)}>
                                                    {this.state.openIds.has(item.id) ? <ChevronDown/> : <ChevronRight/>}
                                                </button>
                                            </td>
                                            <td>
                                                {this.dateDisplay(item.startDate)}
                                            </td>
                                            <td>
                                                {this.dateDisplay(item.endDate)}
                                            </td>
                                        </tr>
                                            {this.state.openIds.has(item.id) && ( <tr>
                                                <td></td>
                                            </tr>)}
                                        </React.Fragment>
                                    ) : <div/>}

                                    </tbody>
                                </table>


                            </Card.Body>
                        </Card>
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
        return new Date(isoDate).toLocaleString(); // local timezone
    };
}

export default TestResult;
