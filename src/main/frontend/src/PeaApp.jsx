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

const FRAME_NAME = {
  TESTRESULT: "TestResult",
  SCENARIO: "Scenario",
  PARAMETERS: "Parameters"

}

class PeaApp extends React.Component {


  constructor(_props) {
    super();
    this.state = {frameContent: FRAME_NAME.TESTRESULT};
    this.clickMenu = this.clickMenu.bind(this);
  }


  render() {
    return (
      <div>

        <Navbar bg="light" variant="light">
          <Container>
            <Nav className="mr-auto">
              <Navbar.Brand href="#home">
                <img src="/img/pea.jpeg" width="28" height="28" alt="Pea"/>
                Process Execution Automator
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
        {this.state.frameContent === FRAME_NAME.TESTRESULT && <TestResult/>}
        {this.state.frameContent === FRAME_NAME.SCENARIO && <Scenario/>}
        {this.state.frameContent === FRAME_NAME.PARAMETERS && <Parameters/>}


      </div>);
  }


  clickMenu(menu) {
    console.log("ClickMenu " + menu);
    this.setState({frameContent: menu});

  }

}

export default PeaApp;


