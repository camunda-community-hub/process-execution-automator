// -----------------------------------------------------------
//
// Definition
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';
import {Button, Modal} from "react-bootstrap";

import RestCallService from "../services/RestCallService";
import {InlineNotification, CodeSnippet} from "carbon-components-react";


class Scenario extends React.Component {


  constructor(_props) {
    super();
    this.state = {runners: [], isOpen: false};
  }

  componentDidMount() {
    this.refreshList();
  }

  /*           {JSON.stringify(this.state.runners, null, 2) } */
  render() {
    return (<div>
      <div className="container">
        <div className="row" style={{width: "100%"}}>
          <div className="col-md-10">
            <h1 className="title">Restore</h1>
            <InlineNotification kind="info" hideCloseButton="true" lowContrast="false">
              The BlueBerry runtime can be configured to execute a backup at intervalle.
            </InlineNotification>

          </div>
        </div>


        This Blueberry runtime did not enable a scheduler to execute backups regularly.

        <div className="alert alert-info" style={{margin: "10px 10px 10px 10px"}}>
          Configure the scheduler in the parameters or in the values.yaml file.
          The variable to set are<p/>
          <code>ENABLE_SCHEDULER=true</code><br/>
          <code>SCHEDULER_CRON="0 0 * * *"</code><br/>

          Blueberry use the Cron expression to calculated the next execution.
        </div>


      </div>


    </div>);
  }


  refreshList() {
    console.log("Definition.refreshList http[blueberry/api/runner/list]");
    this.setState({runners: [], status: ""});
    var restCallService = RestCallService.getInstance();
    restCallService.getJson('blueberry/api/runner/list?', this, this.refreshListCallback);
  }

  refreshListCallback(httpPayload) {
    if (httpPayload.isError()) {
      this.setState({status: "Error"});
    } else {
      this.setState({runners: httpPayload.getData()});

    }
  }


}

export default Scenario;
