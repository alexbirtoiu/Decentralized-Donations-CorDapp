import React from "react";
import FormControl from "@material-ui/core/FormControl";
import {Button, Grid, Modal, TextField} from "@material-ui/core";
import MenuItem from "@material-ui/core/MenuItem";
import {makeStyles} from "@material-ui/core/styles";
import {ThemeProvider} from "react-bootstrap";

export default function MoneyFormModal(props) {
    return (
        <div>
            <Modal
                className="modal"
                open={props.moneyModalOpen}
                onClose={props.moneyModalClose}
            >
                <form
                    onSubmit={props.submitMoney}
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
                                value={props.moneyForm.amount}
                                onChange={props.moneyFormChange}
                            />
                            <TextField
                                className={props.classes.formItem}
                                name="currency"
                                color="secondary"
                                select
                                label="Select Currency"
                                variant="outlined"
                                required
                                value={props.moneyForm.currency}
                                onChange={props.moneyFormChange}
                            >
                                {props.currencies.map((option) => (
                                    <MenuItem key={option.value} value={option.value}>
                                        {option.value + " " + option.label}
                                    </MenuItem>
                                ))}
                            </TextField>
                            <TextField
                                className={props.classes.formItem}
                                name="party"
                                color="secondary"
                                select
                                label="Choose Party"
                                variant="outlined"
                                required
                                value={props.moneyForm.party}
                                onChange={props.moneyFormChange}
                            >
                                {props.peers.map((option) => (
                                    <MenuItem key={option} value={option}>
                                        {option}
                                    </MenuItem>
                                ))}
                            </TextField>
                            <button
                                className="form-button"
                                type="submit"
                                variant="outlined"
                                color="primary"
                            >
                                Issue Money!
                            </button>
                        </FormControl>
                        </Grid>
                    </ThemeProvider>
                </form>
            </Modal>
        </div>
    )
}