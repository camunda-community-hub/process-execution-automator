// -----------------------------------------------------------
//
// PeaApps
//
// Manage the main application
//
// -----------------------------------------------------------

import React from 'react';
import './index.scss';

import 'bootstrap/dist/css/bootstrap.min.css';

import {Container, Nav, Navbar} from 'react-bootstrap';
import TestResult from "./testresult/TestResult";
import Scenario from "./scenario/Scenario";
import Parameters from "./parameters/Parameters";
import HeaderMessage from "./HeaderMessage/HeaderMessage";
import RestCallService from "./services/RestCallService";

const FRAME_NAME = {
    TESTRESULT: "TestResult",
    SCENARIO: "Scenario",
    PARAMETERS: "Parameters"

}

class PeaApp extends React.Component {


    constructor(_props) {
        super();
        this.state = {
            frameContent: FRAME_NAME.TESTRESULT,
            info: {
                version: ""
            }
        };
        this.clickMenu = this.clickMenu.bind(this);
    }

    componentDidMount() {
        this.refreshInfo();
    }

    render() {
        return (
            <div>
                <Navbar bg="light" variant="light">
                    <Container>
                        <Nav className="mr-auto">
                            <Navbar.Brand href="#home">
                                <div style={{display: 'flex', alignItems: 'center'}}>
                                    <img src="/img/pea.png" width="28" height="28" alt="Pea"
                                         style={{marginRight: '8px'}}/>
                                    <div>
                                        Process Execution Automator <span
                                        style={{fontSize: "0.5em"}}>v{this.state.info.version}</span>
                                    </div>
                                </div>
                            </Navbar.Brand>

                            <Nav.Link onClick={() => {
                                this.clickMenu(FRAME_NAME.TESTRESULT)
                            }}>{FRAME_NAME.TESTRESULT}</Nav.Link>

                            <Nav.Link onClick={() => {
                                this.clickMenu(FRAME_NAME.SCENARIO)
                            }}>{FRAME_NAME.SCENARIO}</Nav.Link>


                            <Nav.Link onClick={() => {
                                this.clickMenu(FRAME_NAME.PARAMETERS)
                            }}>{FRAME_NAME.PARAMETERS}</Nav.Link>

                        </Nav>
                    </Container>
                </Navbar>
                <HeaderMessage/>
                {
                    this.state.frameContent === FRAME_NAME.TESTRESULT && <TestResult/>
                }
                {
                    this.state.frameContent === FRAME_NAME.SCENARIO && <Scenario/>
                }
                {
                    this.state.frameContent === FRAME_NAME.PARAMETERS && <Parameters/>
                }


            </div>)
            ;
    }


    clickMenu(menu) {
        console.log("ClickMenu " + menu);
        this.setState({frameContent: menu});

    }

    refreshInfo() {
        let uri = 'pea/api/ping?';
        console.log("Pea.ping http[" + uri + "]");

        this.setState({status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson(uri, this, this.refreshInfoCallback);
    }

    refreshInfoCallback(httpPayload) {
        console.log("Pea.ping");

        if (httpPayload.isError()) {
            console.log("Pea.ping: error " + httpPayload.getError());
            this.setState({status: "Error"});
        } else {
            this.setState({info: httpPayload.getData()});

        }
    }
}

export default PeaApp;


