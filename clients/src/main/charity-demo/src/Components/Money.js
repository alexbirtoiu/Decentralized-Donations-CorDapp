import React from "react";
import '../App.css';
import {Grid} from "@material-ui/core";

export default function Money(props)  {

        return (
            <>
                {props.currencies.map((option) => (
                    <Grid className="money-card">
                        <h6 className="money-text">
                        {props.money[option.value].toFixed(2) + " " + option.label}
                        </h6>
                    </Grid>
                ))}
            </>
        )
}
