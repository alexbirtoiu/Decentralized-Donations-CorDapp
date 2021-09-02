import {Grid, Modal} from "@material-ui/core";
import FormControl from "@material-ui/core/FormControl";
import React from "react";
import {ThemeProvider} from "react-bootstrap";

export default function ProofModal(props) {

return (
    <div>
        <Modal
            className="modal"
            open={props.proofModalOpen}
            onClose={props.proofModalClose}
        >
            <form
                onSubmit={props.settleIouToken}
            >
                <ThemeProvider theme={props.theme}>
                    <Grid className="modal-background">
                        <FormControl>
                                    <label className="party-selector-label">
                                        Select File:
                                    </label>
                                    <input
                                        className={props.classes.formItem}
                                        color="secondary"
                                        type="file"
                                        name="file"
                                        required
                                        onChange={props.proofFormChange} />

                                    <button
                                        className="form-button"
                                        type="submit"
                                    >
                                        Send Proof!
                                    </button>
                        </FormControl>
                    </Grid>
                </ThemeProvider>
            </form>
        </Modal>
    </div>
    )
}