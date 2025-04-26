// -----------------------------------------------------------
//
// ControllerPage
//
// Manage the control on the page: display "loading in progress", "error"
//
// -----------------------------------------------------------

import React from 'react';
import {InlineLoading} from "carbon-components-react";

class ControllerPage extends React.Component {


    constructor(_props) {
        super();
        this.state = {
            errorMessage: "",
            loading: false
        };
    }

    componentDidUpdate(prevProps) {
        if (prevProps.error !== this.props.error) {
            console.log("Controller.componentDidUpdate: Change error["+this.props.error+"]");
            this.setState({
                error: this.props.error
            });
        }
        if (prevProps.errorMessage !== this.props.errorMessage) {
            // console.log("Controller.componentDidUpdate: Change");
            this.setState({
                errorMessage: this.props.errorMessage
            });
        }
        if (prevProps.loading !== this.props.loading) {
            this.setState({
                loading: this.props.loading
            });
        }

    }

    /*           {JSON.stringify(this.state.runners, null, 2) } */
    render() {
        return (
            <div>
                <div style={{height: "40px"}}>
                    {this.state.loading &&
                        <table>
                            <tr>
                                <td><InlineLoading></InlineLoading></td>
                                <td>Loading</td>
                            </tr>
                        </table>}
                </div>
                {this.state.errorMessage &&
                    <div className="alert alert-danger" style={{margin: "10px 10px 10px 10px"}}>
                        <b>{this.state.error}</b><br/>
                        {this.state.errorMessage}
                    </div>}
            </div>
    )
    }

}

export default ControllerPage;