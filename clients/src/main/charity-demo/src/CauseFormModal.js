import FormControl from "@material-ui/core/FormControl";
import {Button, Grid, Modal, TextField} from "@material-ui/core";
import MenuItem from "@material-ui/core/MenuItem";
import React from "react";
import {ThemeProvider} from "react-bootstrap";

export default function CauseFormModal(props) {
    return (
        <div>
            <Modal
                className="modal"
                open={props.causeModalOpen}
                onClose={props.causeModalClose}
            >
                <form
                    onSubmit={props.submitCause}
                >
                    <ThemeProvider theme={props.theme}>
                    <Grid className="modal-background">
                    <FormControl>
                        <Grid container spacing ={2}>

                            <Grid item xs={12} sm={6}>
                                <TextField
                                    className={props.classes.formItem}
                                    name="name"
                                    color="secondary"
                                    label="Enter Name"
                                    variant="outlined"
                                    required
                                    fullWidth
                                    autoFocus
                                    value={props.causeForm.name}
                                    onChange={props.causeFormChange}
                                />
                            </Grid>

                            <Grid item xs={12} sm={6}>
                                <TextField
                                    className={props.classes.formItem}
                                    multiline
                                    minRows={2}
                                    maxRows={4}
                                    name="description"
                                    color="secondary"
                                    label="Provide a brief description"
                                    variant="outlined"
                                    required
                                    fullWidth
                                    value={props.causeForm.description}
                                    onChange={props.causeFormChange}
                                />
                            </Grid>

                            <Grid item xs={12} sm={6}>
                                <TextField
                                    className={props.classes.formItem}
                                    name="neededAmount"
                                    color="secondary"
                                    label="Total needed money"
                                    variant="outlined"
                                    required
                                    fullWidth
                                    value={props.causeForm.neededAmount}
                                    onChange={props.causeFormChange}
                                />
                            </Grid>

                            <Grid item xs={12} sm={6}>
                                <TextField
                                    className={props.classes.formItem}
                                    name="currency"
                                    color="secondary"
                                    select
                                    label="Select Currency"
                                    variant="outlined"
                                    required
                                    fullWidth
                                    value={props.causeForm.currency}
                                    onChange={props.causeFormChange}
                                >
                                    {props.currencies.map((option) => (
                                        <MenuItem key={option.value} value={option.value}>
                                            {option.value + " " + option.label}
                                        </MenuItem>
                                    ))}
                                </TextField>
                            </Grid>

                            <Grid item xs={12} sm={6}>
                                <TextField
                                    className={props.classes.formItem}
                                    name="totalTokens"
                                    color="secondary"
                                    label="Total tokens given back"
                                    variant="outlined"
                                    required
                                    fullWidth
                                    value={props.causeForm.totalTokens}
                                    onChange={props.causeFormChange}
                                />
                            </Grid>

                            <Grid item xs={12} sm={6}>
                                <TextField
                                    className={props.classes.formItem}
                                    name="tokenName"
                                    color="secondary"
                                    label="Token's Name"
                                    variant="outlined"
                                    required
                                    fullWidth
                                    value={props.causeForm.tokenName}
                                    onChange={props.causeFormChange}
                                />
                            </Grid>

                            <Grid item xs={12} sm={6}>
                                <TextField
                                    className={props.classes.formItem}
                                    required
                                    name="timeLimit"
                                    color="secondary"
                                    label="Time limit for donating"
                                    type="datetime-local"
                                    InputLabelProps={{
                                        shrink: true,
                                    }}
                                    value={props.causeForm.timeLimit}
                                    onChange={props.causeFormChange}
                                />
                            </Grid>

                            <Grid item xs={12} sm={6}>
                                <button
                                    className="form-button"
                                    type="submit"
                                >
                                    Issue Cause!
                                </button>
                            </Grid>

                        </Grid>
                    </FormControl>
                    </Grid>
                    </ThemeProvider>
                </form>
            </Modal>
        </div>
    )
}