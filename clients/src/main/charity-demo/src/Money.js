import React, {useState} from "react";
import axios from "axios";

const BACKEND_URL = "http://localhost:10050"

export default function Money(props)  {
        return (
            <div>
                {props.money}
            </div>
        )
}
