import FormControl from "@material-ui/core/FormControl";
import { Grid, Modal, TextField} from "@material-ui/core";
import React from "react";
import '../App.css';
import {ThemeProvider} from "react-bootstrap";



export default function DonateFormModal(props) {
    return (

            <Modal
                className="modal"
                open={props.donateModalOpen}
                onClose={props.donateModalClose}
            >
                <form
                    onSubmit={props.donate}
                >
                    <ThemeProvider theme={props.theme}>
                    <Grid className="modal-background">
                    <FormControl>
                        <TextField
                            className={props.classes.formItem}
                            color="secondary"
                            name="amount"
                            label="Enter Amount"
                            variant="outlined"
                            required
                            autoFocus
                            value={props.donateForm.amount}
                            onChange={props.donateFormChange}
                        />

                        <button
                            className="form-button"
                            type="submit"
                        >
                            Donate!
                        </button>
                    </FormControl>
                    </Grid>
                </ThemeProvider>
                </form>
            </Modal>

    )
}