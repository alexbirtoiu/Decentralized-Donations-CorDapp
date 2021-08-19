import MuiAlert from "@material-ui/lab/Alert";
import Snackbar from "@material-ui/core/Snackbar";
import React from "react";


export default function Popup(props) {
    function Alert(props) {
        return <MuiAlert elevation={6} variant="filled" {...props} />;
    }

    return (
        <Snackbar open={props.popup} autoHideDuration={3000} onClose={props.handlePopupClose}>
            <Alert onClose={props.handlePopupClose} severity={props.severity}>
                {props.message}
            </Alert>
        </Snackbar>
    )
}