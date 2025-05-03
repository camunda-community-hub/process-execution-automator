// -----------------------------------------------------------
//
// Parameters
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';
import {Button, InlineNotification, TextInput} from "carbon-components-react";
import {ArrowRepeat} from "react-bootstrap-icons";
import RestCallService from "../services/RestCallService";

class Parameters extends React.Component {


    constructor(_props) {
        super();
        this.state = {
            parameters: [],
            display: {loading: false}
        };
        this.headers = [
            {key: "key", header: "Parameter"},
            {key: "value", header: "Value"}
        ];
    }

    componentDidMount(prevProps) {
        this.refresh();
    }

    render() {
        const rows = Object.entries(this.state.parameters).map(([key, value], index) => ({
            id: index.toString(),
            key,
            value
        }));

        return (
            <div className={"container"}>
                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-10">
                        <h1 className="title">Parameters</h1>

                    </div>
                </div>
                <div className="row" style={{marginTop: "10px"}}>
                    <div className="col-md-2">
                        <Button className="btn btn-success btn-sm"
                                onClick={() => {
                                    this.refresh()
                                }}
                                disabled={this.state.display.loading}>
                            <ArrowRepeat/> Refresh
                        </Button>
                    </div>
                </div>

                <div className="row" style={{marginTop: "10px"}}>

                </div>



            </div>
        )
    }

    refresh() {
        let uri = '/blueberry/api/parameters/getall?';
        console.log("platform.checkup http[" + uri + "]");

        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson(uri, this, this.refreshParametersCallback);
    }

    refreshParametersCallback(httpPayload) {
        this.setDisplayProperty("loading", false);
        if (httpPayload.isError()) {
            console.log("Configuration.startBackupCallback: error " + httpPayload.getError());
            this.setState({status: "Error"});
        } else {
            this.setState({parameters: httpPayload.getData()})
        }
    }

    /* Set the display property
    * @param propertyName name of the property
    * @param propertyValue the value
    */
    setDisplayProperty(propertyName, propertyValue) {
        let displayObject = this.state.display;
        displayObject[propertyName] = propertyValue;
        this.setState({display: displayObject});
    }

}

export default Parameters;