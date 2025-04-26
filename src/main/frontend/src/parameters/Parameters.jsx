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
                    <h1>Connection</h1>
                    <InlineNotification kind="info" hideCloseButton="true" lowContrast="false">
                        Different URL and parameter to connect the different component in the Zeebe Cluster<br/>
                    </InlineNotification>
                </div>
                <div className="row" style={{marginTop: "10px"}}>
                    <div className="col-md-2">
                        <TextInput
                            id="operateActuatorUrl"
                            labelText="operateActuatorUrl"
                            value={this.state.parameters.operateActuatorUrl}
                            readOnly
                        />
                    </div>
                    <div className="col-md-2">
                        <TextInput
                            id="tasklistActuatorUrl"
                            labelText="tasklistActuatorUrl"
                            value={this.state.parameters.tasklistActuatorUrl}
                            readOnly
                        />
                    </div>
                    <div className="col-md-2">
                        <TextInput
                            id="elasticsearchUrl"
                            labelText="elasticsearchUrl"
                            value={this.state.parameters.elasticsearchUrl}
                            readOnly
                        />
                    </div>
                    <div className="col-md-2">
                        <TextInput
                            id="zeebeActuatorUrl"
                            labelText="zeebeActuatorUrl"
                            value={this.state.parameters.zeebeActuatorUrl}
                            readOnly
                        />
                    </div>
                    <div className="col-md-2">
                        <TextInput
                            id="optimizeActuatorUrl"
                            labelText="optimizeActuatorUrl"
                            value={this.state.parameters.optimizeActuatorUrl}
                            readOnly
                        />
                    </div>
                </div>
                <div className="row" style={{marginTop: "10px"}}>
                    <div className="col-md-2">
                        <TextInput
                            id="zeebeIsConnected"
                            labelText="zeebeIsConnected"
                            value={this.state.parameters.zeebeIsConnected}
                            readOnly
                        />
                    </div>
                    <div className="col-md-2">
                        <TextInput
                            id="grpcAddress"
                            labelText="GrpcAddress"
                            value={this.state.parameters.grpcAddress}
                            readOnly
                        />
                    </div>
                </div>
                <div className="row" style={{marginTop: "10px"}}>
                    <div className="col-md-2">
                        <TextInput
                            id="kubeConfig"
                            labelText="kubeConfig"
                            value={this.state.parameters.kubeConfig}
                            readOnly
                        />
                    </div>
                    <div className="col-md-2">
                        <TextInput
                            id="namespace"
                            labelText="namespace"
                            value={this.state.parameters.namespace}
                            readOnly
                        />
                    </div>

                </div>
                <div className="row" style={{marginTop: "10px"}}>
                    <h1>Repositories</h1>
                    <InlineNotification kind="info" hideCloseButton="true" lowContrast="false">
                        Zeebe component declare different repository, to store information.
                        The Repository name come from the component (Operate, TaskList, Optimize),
                        the BasePath come from PEA (or was created in Elasticseach), the container name come from
                        PEA (or was created in Elasticseach)
                    </InlineNotification>
                </div>
                <div className="row" style={{marginTop: "10px"}}>
                    <div className="col-md-2">
                        <TextInput
                            id="operateRepository"
                            labelText="operateRepository"
                            value={this.state.parameters.operateRepository}
                            readOnly
                        />
                    </div>
                    <div className="col-md-2">
                        <TextInput
                            id="operateContainerBasePath"
                            labelText="operateContainerBasePath"
                            value={this.state.parameters.operateContainerBasePath}
                            readOnly
                        />
                    </div>
                </div>
                <div className="row" style={{marginTop: "10px"}}>
                    <div className="col-md-2">
                        <TextInput
                            id="tasklistRepository"
                            labelText="tasklistRepository"
                            value={this.state.parameters.tasklistRepository}
                            readOnly
                        />
                    </div>
                    <div className="col-md-2">
                        <TextInput
                            id="tasklistContainerBasePath"
                            labelText="tasklistContainerBasePath"
                            value={this.state.parameters.tasklistContainerBasePath}
                            readOnly
                        />
                    </div>
                </div>
                <div className="row" style={{marginTop: "10px"}}>
                    <div className="col-md-2">
                        <TextInput
                            id="optimizeRepository"
                            labelText="optimizeRepository"
                            value={this.state.parameters.optimizeRepository}
                            readOnly
                        />
                    </div>
                    <div className="col-md-2">
                        <TextInput
                            id="optimizeContainerBasePath"
                            labelText="optimizeContainerBasePath"
                            value={this.state.parameters.optimizeContainerBasePath}
                            readOnly
                        />
                    </div>
                </div>
                <div className="row" style={{marginTop: "10px"}}>
                    <div className="col-md-2">
                        <TextInput
                            id="zeebeRecordRepository"
                            labelText="zeebeRecordRepository"
                            value={this.state.parameters.zeebeRecordRepository}
                            readOnly
                        />
                    </div>
                    <div className="col-md-2">
                        <TextInput
                            id="zeebeRecordContainerBasePath"
                            labelText="zeebeRecordContainerBasePath"
                            value={this.state.parameters.zeebeRecordContainerBasePath}
                            readOnly
                        />
                    </div>

                </div>

                <div className="row" style={{marginTop: "10px"}}>
                    <h1>Container</h1>
                    <InlineNotification kind="info" hideCloseButton="true" lowContrast="false">
                        Elasticsearch connect a repository to a container. A container may be Azure, S3, or different
                        container. The container defined in Blueberry is used to create repositories.
                    </InlineNotification>
                </div>
                <div className="row" style={{marginTop: "10px"}}>
                    <div className="col-md-2">
                        <TextInput
                            id="elasticsearchContainerType"
                            labelText="elasticsearchContainerType"
                            value={this.state.parameters.elasticsearchContainerType}
                            readOnly
                        />
                    </div>
                </div>
                <div className="row" style={{marginTop: "10px"}}>
                    <div className="col-md-2">
                        <TextInput
                            id="azureContainerName"
                            labelText="azureContainerName"
                            value={this.state.parameters.azureContainerName}
                            readOnly
                        />
                    </div>
                </div>
                <div className="row" style={{marginTop: "10px"}}>
                    <div className="col-md-2">
                        <TextInput
                            id="s3Bucket"
                            labelText="s3Bucket"
                            value={this.state.parameters.s3Bucket}
                            readOnly
                        />
                    </div>

                    <div className="col-md-2">
                        <TextInput
                            id="s3Region"
                            labelText="s3Region"
                            value={this.state.parameters.s3Region}
                            readOnly
                        />
                    </div>
                    <div className="col-md-2">
                        <TextInput
                            id="s3BasePath"
                            labelText="s3BasePath"
                            value={this.state.parameters.s3BasePath}
                            readOnly
                        />
                    </div>

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