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
import {
    InlineNotification,
    Table,
    TableBody,
    TableHead,
    TableHeader,
    TableRow, Tag,
    TextInput
} from "carbon-components-react";
import { ArrowRepeat } from "react-bootstrap-icons";
import { BookmarkPlusFill } from "react-bootstrap-icons";


import {ChevronDown, ChevronRight} from '@carbon/icons-react';


class Server extends React.Component {


    constructor(_props) {
        super();

        this.state = {
            servers: [],
            preferateServer: "",
            openIds: new Set(),
            testconnection: {
                engine: {status: "", AnalyseHuman: []},
                admin: {status: "", AnalyseHuman: []},
                tasklist: {status: "", AnalyseHuman: []},
            },
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
                        <h1 className="title">Server</h1>
                        <InlineNotification kind="info" hideCloseButton="true" lowContrast="false">
                            Server
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
                            <Card.Header style={{backgroundColor: "rgba(0,0,0,.03)"}}>Test connection</Card.Header>
                            <Card.Body>

                                <TextInput
                                    id="preferate-server"
                                    labelText="Preferred Server"
                                    value={this.state.preferateServer}
                                    readOnly={true}
                                    style={{ backgroundColor: '#f4f4f4' }}
                                />

                                <Button className="btn btn-info btn-sm" style={{marginTop: "10px"}}
                                        onClick={() => {
                                            this.testConnection()
                                        }}
                                        disabled={this.state.display.loading}>
                                    Test connection
                                </Button>
                                <p/>
                                <table>
                                    <tr>
                                        <td>Engine (Zeebe,Camunda 7 Engine)</td>
                                        <td>
                                            {this.state.testconnection?.engine?.status === "OK" && (
                                                <Tag type="green">OK</Tag>
                                            )}
                                            <p></p>
                                            {this.state.testconnection?.engine?.status === "FAIL" && (
                                                <Tag type="red">FAIL</Tag>
                                            )}

                                            {this.state.testconnection?.engine?.AnalyseHuman?.map((name, index) => (
                                                <p key={index}>{name}</p>
                                            ))}
                                            </td>
                                    </tr>
                                    <tr>
                                        <td>Admin (Operate, Cockpit)</td>
                                        <td>
                                            {this.state.testconnection?.admin?.status === "OK" && (
                                                <Tag type="green">OK</Tag>
                                            )}
                                            <p></p>
                                            {this.state.testconnection?.admin?.status === "FAIL" && (
                                                <Tag type="red">FAIL</Tag>
                                            )}
                                            {this.state.testconnection?.admin?.status === "NOT_NEEDED" && (
                                                <Tag type="default">Not configured</Tag>
                                            )}

                                            {this.state.testconnection?.admin?.AnalyseHuman?.map((name, index) => (
                                                <p key={index}>{name}</p>
                                            ))}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>TaskList</td>
                                        <td>
                                            {this.state.testconnection?.tasklist?.status === "OK" && (
                                                <Tag type="green">OK</Tag>
                                            )}
                                            <p></p>
                                            {this.state.testconnection?.tasklist?.status === "FAIL" && (
                                                <Tag type="red">FAIL</Tag>
                                            )}
                                            {this.state.testconnection?.tasklist?.status === "NOT_NEEDED" && (
                                                <Tag type="default">Not configured</Tag>
                                            )}

                                            {this.state.testconnection?.tasklist?.AnalyseHuman?.map((name, index) => (
                                                <p key={index}>{name}</p>
                                            ))}
                                        </td>
                                    </tr>
                                </table>
                            </Card.Body>
                        </Card>
                    </div>
                </div>


                <div className="row" style={{width: "100%", marginTop: "10px"}}>
                    <div className="col-md-12">
                        <Card>
                            <Card.Header style={{backgroundColor: "rgba(0,0,0,.03)"}}>Server</Card.Header>
                            <Card.Body>
                                <Table>
                                    <TableHead>
                                        <TableRow>
                                            <TableHeader></TableHeader>
                                            <TableHeader>Server Name</TableHeader>
                                            <TableHeader>Type</TableHeader>
                                            <TableHeader>Address</TableHeader>
                                            <TableHeader>Description</TableHeader>
                                            <TableHeader></TableHeader>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {this.state.servers ? this.state.servers.map((item, index) =>
                                                <React.Fragment key={index}>
                                                    <TableRow style={{
                                                        backgroundColor: index % 2 === 1 ? '#f4f4f4' : 'white'
                                                    }}>
                                                        <td>{ item.name === this.state.preferateServer ? <BookmarkPlusFill style={{ color: '#24a148' }} size={24}/>:<div/>}
                                                        </td>
                                                        <td>{item.name}</td>
                                                        <td>{item.type}</td>
                                                        <td>{item.zeebeGrpcAddress}
                                                            {item.camunda7ServerUrl}
                                                        </td>
                                                        <td>{item.description}</td>
                                                        <td>

                                                            <button onClick={() => this.toggleDetail(item.name)}>
                                                                {this.state.openIds.has(item.name) ? <ChevronDown/> :
                                                                    <ChevronRight/>}
                                                            </button>
                                                        </td>
                                                    </TableRow>
                                                    {this.state.openIds.has(item.name) &&
                                                        <TableRow style={{
                                                            backgroundColor: index % 2 === 1 ? '#f4f4f4' : 'white'
                                                        }}>
                                                            <td colSpan="4">
                                                                <div style={{
                                                                    border: "2px solid #3498db",
                                                                    borderRadius: "8px",
                                                                    padding: "16px",
                                                                    width: '100%'
                                                                }}>
                                                                    <h6>Configuration</h6>
                                                                    <table style={{width: '100%'}}
                                                                           className="table is-hoverable is-fullwidth">
                                                                        <tr>
                                                                            <th>Attribut</th>
                                                                            <th>Value</th>
                                                                        </tr>
                                                                        {item.type === "CAMUNDA_7" && (
                                                                            <tr>
                                                                                <td>Camunda 7 UserName</td>
                                                                                <td>{item.userName}</td>
                                                                            </tr>
                                                                        )}
                                                                        {item.type === "CAMUNDA_8" && (
                                                                            <tr>
                                                                                <td>zeebeGrpcAddress</td>
                                                                                <td>{item.zeebeGrpcAddress}</td>
                                                                            </tr>
                                                                        )}
                                                                        {item.type === "CAMUNDA_8" && (
                                                                            <tr>
                                                                                <td>zeebeRestAddress</td>
                                                                                <td>{item.zeebeRestAddress}</td>
                                                                            </tr>
                                                                        )}
                                                                        {(item.type === "CAMUNDA_8" || item.type === "CAMUNDA_8_SAAS") && (
                                                                            <tr>
                                                                                <td> zeebeClientId< /td>
                                                                                <td>{item.zeebeClientId}
                                                                                </td>
                                                                            </tr>
                                                                        )}
                                                                        {(item.type === "CAMUNDA_8" || item.type === "CAMUNDA_8_SAAS") && (
                                                                            <tr>
                                                                                <td> zeebeClientSecret< /td>
                                                                                <td>{item.zeebeClientSecret}
                                                                                </td>
                                                                            </tr>
                                                                        )}
                                                                        {(item.type === "CAMUNDA_8" || item.type === "CAMUNDA_8_SAAS") && (
                                                                            <tr>
                                                                                <td>Identity URL</td>
                                                                                <td>{item.identityUrl}</td>
                                                                            </tr>)}
                                                                        {item.type === "CAMUNDA_8_SAAS" && (<tr>
                                                                                <td>Cluster ID</td>
                                                                                <td>{item.clusterId}</td>
                                                                            </tr>
                                                                        )}
                                                                        {item.type === "CAMUNDA_8_SAAS" && (<tr>
                                                                                <td>SaaS Region</td>
                                                                                <td>{item.zeebeSaasRegion}</td>
                                                                            </tr>
                                                                        )}
                                                                        {(item.type === "CAMUNDA_8" || item.type === "CAMUNDA_8_SAAS") && (
                                                                            <tr>
                                                                                <td>Authentication URL</td>
                                                                                <td>{item.authenticationUrl}</td>
                                                                            </tr>
                                                                        )}
                                                                        {(item.type === "CAMUNDA_8" || item.type === "CAMUNDA_8_SAAS") && (
                                                                            <tr>
                                                                                <td>Operate URL</td>
                                                                                <td>{item.operateUrl}</td>
                                                                            </tr>)}
                                                                        {(item.type === "CAMUNDA_8" || item.type === "CAMUNDA_8_SAAS") && (
                                                                            <tr>
                                                                                <td>OperteClientId< /td>
                                                                                <td>{item.operateClientId}
                                                                                </td>
                                                                            </tr>
                                                                        )}
                                                                        {(item.type === "CAMUNDA_8" || item.type === "CAMUNDA_8_SAAS") && (
                                                                            <tr>
                                                                                <td>OperateClientSecret< /td>
                                                                                <td>{item.operateClientSecret}
                                                                                </td>
                                                                            </tr>
                                                                        )}{(item.type === "CAMUNDA_8" || item.type === "CAMUNDA_8_SAAS") && (
                                                                            <tr>
                                                                                <td>Operate user name</td>
                                                                                <td>{item.operateUserName}</td>
                                                                            </tr>)}

                                                                        {(item.type === "CAMUNDA_8" || item.type === "CAMUNDA_8_SAAS") && (
                                                                            <tr>
                                                                                <td>TaskList URL</td>
                                                                                <td>{item.taskListUrl}</td>
                                                                            </tr>)}
                                                                        {(item.type === "CAMUNDA_8" || item.type === "CAMUNDA_8_SAAS") && (
                                                                            <tr>
                                                                                <td>TaskList ClientId< /td>
                                                                                <td>{item.taskListClientId}
                                                                                </td>
                                                                            </tr>
                                                                        )}
                                                                        {(item.type === "CAMUNDA_8" || item.type === "CAMUNDA_8_SAAS") && (
                                                                            <tr>
                                                                                <td>TaskList ClientSecret< /td>
                                                                                <td>{item.taskListClientSecret}
                                                                                </td>
                                                                            </tr>
                                                                        )}
                                                                        {(item.type === "CAMUNDA_8" || item.type === "CAMUNDA_8_SAAS") && (
                                                                            <tr>
                                                                                <td>TaskList UserName</td>
                                                                                <td>{item.taskListUserName}</td>
                                                                            </tr>)}
                                                                        {(item.type === "CAMUNDA_8" || item.type === "CAMUNDA_8_SAAS") && (
                                                                            <tr>
                                                                                <td>TaskList Keycloack URL</td>
                                                                                <td>{item.taskListKeycloakUrl}</td>
                                                                            </tr>)}
                                                                        {(item.type === "CAMUNDA_8" || item.type === "CAMUNDA_8_SAAS") && (
                                                                            <tr>
                                                                                <td>Worker Execution Thread</td>
                                                                                <td>{item.workerExecutionThreads}</td>
                                                                            </tr>)}
                                                                        <tr>
                                                                            <td>Worker Max Jobs</td>
                                                                            <td>{item.workerMaxJobsActive}</td>
                                                                        </tr>

                                                                    </table>
                                                                </div>
                                                            </td>
                                                        </TableRow>
                                                    }
                                                </React.Fragment>
                                            ) :
                                            <div/>
                                        }
                                    </TableBody>
                                </Table>


                            </Card.Body>
                        </Card>
                    </div>
                </div>

            </div>
        )
            ;
    }


    refreshList() {
        console.log("Definition.refreshList http[pea/api/server/list]");
        this.setState({servers: [], preferateServer: "",testconnection: {}, status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson('pea/api/server/list?details=true', this, this.refreshListCallback);
    }

    refreshListCallback(httpPayload) {
        if (httpPayload.isError()) {
            this.setState({status: "Error"});
        } else {
            this.setState({
                servers: httpPayload.getData().servers,
                preferateServer: httpPayload.getData().preferateServer
            });

        }
    }

    testConnection() {
        console.log("Server.testConnection http[pea/api/server/testconnection?serverName=" + this.state.preferateServer + "]");
        this.setState({testconnection: {}, status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson('pea/api/server/testconnection?serverName=' + this.state.preferateServer, this, this.testConnectionCallback);
    }

    testConnectionCallback(httpPayload) {
        if (httpPayload.isError()) {
            this.setState({status: "Error"});
        } else {
            this.setState({
                testconnection: httpPayload.getData()
            });

        }
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

export default Server;
