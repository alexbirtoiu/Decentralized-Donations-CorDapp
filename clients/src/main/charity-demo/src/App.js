import './App.css';
import React, {useEffect, useState} from "react";
import InputLabel from '@material-ui/core/InputLabel';
import { makeStyles } from '@material-ui/core/styles';
import MenuItem from '@material-ui/core/MenuItem';
import Select from '@material-ui/core/Select';
import axios from 'axios'
import Popup from './Popup'
import Money from './Money'
import {
    Button,
    Card,
    CardMedia,
    colors,
    Container,
    createTheme,
    Label,
    Paper,
    TextField,
    Typography
} from "@material-ui/core";
import Carousel from 'react-material-ui-carousel'
import MoneyFormModal from "./MoneyFormModal";
import CauseFormModal from "./CauseFormModal";
import DonateFormModal from "./DonateFormModal";
import Countdown from "react-countdown";
import ProofModal from "./ProofModal";
import {green, red} from "@material-ui/core/colors";
const BACKEND_URL = "http://localhost:10050"

const theme = createTheme({
    palette: {
        secondary: {
            main: red[500],
        },
    },
});

const useStyles = makeStyles((theme) => ({
    paper: {
        marginTop: theme.spacing(8),
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
    },
    selector : {
        color: "red",
    },
    media : {
        height: 250
    },
    form: {
        width: '100%',
        margin: theme.spacing(3),
    },
    button: {
        color: "red",
        margin: theme.spacing(5, 1, 0, 0),
    },
    buttonColor: {
        borderColor: "red"
    },

    formItem: {
        margin: theme.spacing(3, 1, 2, 0),
        background: "white"
    },
    description: {
        margin: theme.spacing(3, 1, 0, 0),
    },
    carouselLabel : {
        margin: theme.spacing(5, 0, 0, 0)
    },
    text: {
        marginTop: 5,
    },
    warning: {
        marginTop : 5,
        color : "red"
    }
}));

const currencies = [
    {
        value: 'GBP',
        label: '£'
    },
    {
        value: 'EUR',
        label: '€',
    },
    {
        value: 'USD',
        label: '$',
    }

]

const isNotNumber = (field, event) => {
    return field.some(item => event.target.name === item)
        && event.target.value !== ''
        && !Number(event.target.value)
}

const areNotFutureDates = (field, event) => {
    let date_now = new Date();
    let date_input = new Date(event.target.value);

    return field.some(item => event.target.name === item)
    && event.target.value !== ''
    && date_now.getTime() > date_input.getTime()
}


function App() {
    const classes = useStyles()
    const [peers, setPeers] =useState([])
    const [party, setParty] = useState('PartyA');
    const [open, setOpen] = useState(false);
    const [popup, setPopup] = useState(false)
    const [message, setMessage] = useState('')
    const [severity, setSeverity] = useState('success')

    const [myCauses, setMyCauses] = useState([])
    const [activeCauses, setActiveCauses] = useState([])
    const [owedTokens, setOwedTokens] = useState([])
    const [rewardTokens, setRewardTokens] = useState([])
    const [incomingTokens, setIncomingTokens] = useState([])
    const [iouMoney, setIouMoney] = useState([])

    const [images, setImages] = useState([])

    const [money, setMoney] = useState({
        GBP: 0.00,
        EUR: 0.00,
        USD: 0.00
    })

    const initialMoneyForm = {
        amount: '',
        currency: 'GBP',
        party: ''
    }
    const [moneyModalOpen, showMoneyModal] = useState(false)
    const [moneyForm, setMoneyForm] = useState(initialMoneyForm)

    const initialCauseForm = {
        name: '',
        description: '',
        neededAmount: '',
        currency: 'GBP',
        totalTokens: '',
        tokenName: '',
        timeLimit: ''
    }
    const [causeModalOpen, showCauseModal] = useState(false)
    const [causeForm, setCauseForm] = useState(initialCauseForm)

    const initialDonateForm = {
        cause: '',
        amount: ''
    }
    const [donateModalOpen, showDonateModal] = useState(false)
    const [donateForm, setDonateForm] = useState(initialDonateForm)

    const initialProofForm = {
        causeId: '',
        file: null
    }
    const [proofModalOpen , showProofModal] = useState(false)
    const [proofForm, setProofForm] = useState(initialProofForm)

    useEffect(() => {
        updateParty()
    })

    const updateAll = () => {
        updateParty()
        updateMoney()
        updatePeers()
        updateMyCauses()
        updateOwedTokens()
        updateActiveCauses()
        updateRewardTokens()
        updateIncomingTokens()
        updateIOUMoney()
    }

    // Update data from backend server functions
    const updateMyCauses = () => {
        axios.get(BACKEND_URL + '/mycauses').then(res => {
            setMyCauses(res.data)
        })
    }

    const updateActiveCauses = () => {
        axios.get(BACKEND_URL + '/activecauses').then(res => {
            setActiveCauses(res.data)
        })
    }

    const updateOwedTokens = () => {
        axios.get(BACKEND_URL + '/owedtokens').then(res => {
            setOwedTokens(res.data)
        })
    }

    const updateRewardTokens = () => {
        axios.get(BACKEND_URL + '/rewardtokens').then(res => {
            setRewardTokens(res.data)
        })
    }

    const updateIncomingTokens = () => {
        axios.get(BACKEND_URL + '/incomingtokens').then(res => {
            setIncomingTokens(res.data)
        })
    }

    const updateIOUMoney = () => {
        axios.get(BACKEND_URL + '/ioumoney').then(res => {
            setIouMoney(res.data)
        })
    }

    const updateMoney = () => {
        axios.get(BACKEND_URL + '/money').then(res => {
            setMoney(res.data)
        })
    }

    const updatePeers = () => {
        axios.get(BACKEND_URL + '/peers').then(res => {
            setPeers(res.data)
        })
    }

    const updateParty = () => {
        axios.get(BACKEND_URL + '/node').then(res => {
            setParty(res.data)
        })
    }


    // Popup functions
    const handlePopupClose = (event, reason) => {
        if(reason === 'clickaway') {
            return;
        }
        setPopup(false)
    }

    // Select party functions
    const partyHandleClose = () => {
        setOpen(false);
    };

    const partyHandleOpen = () => {
        setOpen(true);
    };

    const chooseParty = (event) => {
        const data = {
            name: event.target.value.trim(),
        }
        console.log(data)

        post('/party', data)
    }

    // Issue money form functions
    const openMoneyModal = () => {
        showMoneyModal(true)
    }

    const moneyModalClose = () => {
        showMoneyModal(false)
    }

    const moneyFormChange = (event) => {
        if(isNotNumber(['amount'], event))
            return;

        setMoneyForm({
            ...moneyForm,
            [event.target.name]: event.target.value
        })
    }

    const submitMoney = (event) => {
        event.preventDefault();

        console.log(moneyForm)

        showMoneyModal(false)

        setSeverity('success')
        setMessage("Issue Money form sent")
        setPopup(true)

        post('/issuemoney', moneyForm)
        setMoneyForm(initialMoneyForm)
    }

    // Issue cause form functions
    const openCauseModal = () => {
        showCauseModal(true)
    }

    const causeModalClose = () => {
        showCauseModal(false)
    }

    const causeFormChange = (event) => {
        if(areNotFutureDates(['timeLimit'], event) || isNotNumber(['neededAmount', "totalTokens"], event))
            return

        setCauseForm({
            ...causeForm,
            [event.target.name]: event.target.value
        })
    }

    const submitCause = (event) => {
        event.preventDefault();

        console.log(causeForm)

        showCauseModal(false)

        setSeverity('success')
        setMessage("Issue Cause form sent")
        setPopup(true)

        post('/issuecause', causeForm)
        setCauseForm(initialCauseForm)
    }

    // Donate form functions
    const openDonateModal = (event, cause) => {
        event.preventDefault()
        showDonateModal(true)

        setDonateForm({
            ...donateForm,
            ["cause"]: cause
        })

        console.log(donateForm.cause)
    }

    const donateModalClose = () => {
        showDonateModal(false)
    }

    const donateFormChange = (event) => {
        if(isNotNumber(['amount'], event))
            return;
        setDonateForm({
            ...donateForm,
            [event.target.name]: event.target.value
        })
    }

    const donate = (event) => {
        event.preventDefault()

        console.log(donateForm)

        showDonateModal(false)

        setSeverity('success')
        setMessage("Donate form sent")
        setPopup(true)

        post('/donate', donateForm)
        setDonateForm(initialDonateForm)
    }

    // Settle cause functions

    const settleCause = (event, cause) => {
        event.preventDefault()

        console.log(cause.linearId)

        const data = {
            linearId: cause.linearId
        }

        setSeverity('success')
        setMessage("Settle cause request sent")
        setPopup(true)

        post('/settlecause', data)
    }

    // Request money functions

    const requestMoney = (event, causeId) => {
        event.preventDefault()

        const data = {
            linearId: causeId
        }

        console.log(data)

        setSeverity('success')
        setMessage("Request for money has been sent")
        setPopup(true)

        post('/requestmoney', data)
    }

    // Settle IOUMoney functions

    const repay = (event, linearId) => {
        event.preventDefault()

        const data = {
            linearId: linearId
        }

        console.log(data)

        setSeverity('success')
        setMessage("Money repayment request has been sent")
        setPopup(true)

        post('/repay', data)
    }


    // Settle IOUToken functions
    const openProofModal = (event, causeId) => {
        event.preventDefault()
        showProofModal(true)
        console.log(causeId)

        setProofForm({
            ...proofForm,
            ["causeId"]: causeId
        })

    }

    const proofModalClose = () => {
        showProofModal(false)
    }

    const proofFormChange = (event) => {
        setProofForm({
            ...proofForm,
            [event.target.name]: event.target.files[0]
        })
    }

    const settleIouToken = (event) => {
        event.preventDefault()

        console.log(proofForm)

        showProofModal(false)

        setSeverity('success')
        setMessage("Proof sent")
        setPopup(true)

        const imageData = new FormData()

        imageData.append("causeId", proofForm.causeId)
        imageData.append('file', proofForm.file)

        post('/settletokens', imageData)
    }

    // Post to backend server function
    const post = (urlExtension, data) => {
        axios.post(
            BACKEND_URL + urlExtension,
            data,
            {
                headers: { 'Content-Type': 'application/json' }
            }
        ).then(res => {
            setMessage(res.data)
            setPopup(true)
            setSeverity("success")
            updateAll()
        })
            .catch(err => {
                if(err.response) {
                    setMessage(err.response.data)
                }
                else {
                    setMessage("Connection Failed " + urlExtension)
                }
                setPopup(true)
                setSeverity('error')
            });
    }

    return (
        <div className="app">
            <nav className="navbar">
                    <img
                        className="corda-logo"
                        src = "corda-logo.png">
                    </img>
                    <h2 className="demo-title">
                        Decentralized Donations Demo
                    </h2>
                    <Money
                        classname="moneycards"
                        money = {money}
                        currencies = {currencies}>
                    </Money>
                    <button
                        className="navbar-button"
                        type="button"
                        onClick={openCauseModal}>
                        Issue Cause
                    </button>
                    <button
                        className="navbar-button"
                        type="button"
                        onClick={openMoneyModal}>
                        Issue Money
                    </button>

                    <div className="party-selector-part">
                    <h5
                        className="party-selector-label"
                    >
                    Current Party
                    </h5>
                    <Select
                        className={classes.selector}
                        labelId="select-label"
                        id="select"
                        fullWidth
                        open={open}
                        onClose={partyHandleClose}
                        onOpen={partyHandleOpen}
                        value={party}
                        onChange={chooseParty}
                    >
                        <MenuItem value={'PartyA'}>PartyA</MenuItem>
                        <MenuItem value={'PartyB'}>PartyB</MenuItem>
                        <MenuItem value={'PartyC'}>PartyC</MenuItem>
                        <MenuItem value={'Bank'}>Bank</MenuItem>
                    </Select>
                    </div>
            </nav>

        <Popup
            popup = {popup}
            message = {message}
            handlePopupClose = {handlePopupClose}
            severity = {severity}>
        </Popup>

        <MoneyFormModal
            moneyModalOpen = {moneyModalOpen}
            moneyModalClose = {moneyModalClose}
            submitMoney = {submitMoney}
            classes = {classes}
            theme = {theme}
            moneyForm = {moneyForm}
            moneyFormChange = {moneyFormChange}
            currencies = {currencies}
            peers = {peers} />

        <CauseFormModal
            causeModalOpen = {causeModalOpen}
            causeModalClose = {causeModalClose}
            submitCause = {submitCause}
            theme = {theme}
            classes = {classes}
            causeForm = {causeForm}
            causeFormChange = {causeFormChange}
            currencies = {currencies} />

        <DonateFormModal
            donateModalOpen = {donateModalOpen}
            donateModalClose = {donateModalClose}
            donate = {donate}
            classes = {classes}
            theme = {theme}
            donateForm = {donateForm}
            donateFormChange = {donateFormChange} />

        <ProofModal
            proofModalOpen = {proofModalOpen}
            classes = {classes}
            theme = {theme}
            proofModalClose = {proofModalClose}
            settleIouToken = {settleIouToken}
            proofFormChange = {proofFormChange}
        />

        <div className='cards'>
            <div className='cards_container'>
                <ul className='cards-row'>
                    <div className='big_card'>

                    <h5 className='card-header'>
                        My Causes
                    </h5>

                    <Carousel>
                        {myCauses.map((cause, i) => (
                            <Card
                                className='card'
                                key = {i}>
                                <h2 className={classes.text}>
                                    {cause.name}
                                </h2>

                                <h6 className={classes.text}>
                                    {cause.description}
                                </h6>

                                <Typography className={classes.text}>
                                    Amount needed: {cause.neededAmount.toFixed(2)} {cause.currency}
                                </Typography>

                                <Typography className={classes.text}>
                                    Amount gathered: {cause.gatheredAmount.toFixed(2)} {cause.currency}
                                </Typography>

                                <Countdown
                                    className='countdown'
                                    date={Date.parse(cause.timeLimit)}
                                />

                                <Button
                                    classname={classes.cardButton}
                                    theme = {theme}
                                    color = "secondary"
                                    type="button"
                                    variant="outlined"
                                    disabled = {!(cause.gatheredAmount === cause.neededAmount)}
                                    onClick = {(e) => settleCause(e, cause)}
                                    >
                                    Settle cause
                                </Button>
                            </Card>
                        ))}
                    </Carousel>
                    </div>

                    <div className='big_card'>
                        <h5 className='card-header'>
                            Active Causes
                        </h5>

                        <Carousel>
                            {activeCauses.map((cause, i) => (
                                <Card
                                    className='card'
                                    key = {i}>
                                    <h2 className={classes.text}>
                                        {cause.name}
                                    </h2>

                                    <h6 className={classes.text}>
                                       {cause.description}
                                    </h6>

                                    <Typography className={classes.text}>
                                        Amount needed: {cause.neededAmount.toFixed(2)} {cause.currency}
                                    </Typography>

                                    <Typography className={classes.text}>
                                        Amount gathered: {cause.gatheredAmount.toFixed(2)} {cause.currency}
                                    </Typography>

                                    <Typography
                                        className={"party"+cause.reliability}>
                                        Issued by {cause.party}
                                    </Typography>

                                    <Countdown
                                        className='countdown'
                                        date={Date.parse(cause.timeLimit)}
                                    />

                                    <Button className={classes.cardButton}
                                            theme = {theme}
                                            color = "secondary"
                                            type="button"
                                            variant="outlined"
                                            disabled = {(cause.gatheredAmount === cause.neededAmount)}
                                            onClick = {(e) => openDonateModal(e, cause)} >
                                        Donate To cause
                                    </Button>
                                </Card>
                            ))}
                        </Carousel>
                    </div>

                    <div className='big_card'>
                        <h5 className='card-header'>
                            IOUMoney
                        </h5>
                        <Carousel>
                            {iouMoney.map((it, i) => (
                                <Card
                                    className='card'
                                    key = {i}>

                                    <h4 className={classes.warning}>
                                        You owe {it.amount} {it.currency} to {it.lender}
                                    </h4>

                                    <Button className={classes.cardButton}
                                            theme = {theme}
                                            color = "secondary"
                                            type="button"
                                            variant="outlined"
                                            onClick = {(e) => repay(e, it.linearId)} >
                                        Return money
                                    </Button>
                                </Card>
                            ))}
                        </Carousel>
                    </div>
                </ul>
                <ul className='cards-row'>
                    <div className='big_card'>
                        <h5 className='card-header'>
                            Owed Tokens
                        </h5>
                        <Carousel>
                            {owedTokens.map((owedToken, i) => (
                                <Card
                                    className='card'
                                    key = {i}>

                                    <h2 className={classes.text}>
                                        {owedToken.name}
                                    </h2>

                                    <h6 className={classes.text}>
                                        {owedToken.description}
                                    </h6>

                                    <Typography className={classes.text}>
                                        Amount of owed tokens: {owedToken.amount.toFixed(2)} {owedToken.token}
                                    </Typography>

                                    <Countdown
                                        className='countdown'
                                        date={Date.parse(owedToken.expirationDate)}
                                    />

                                    <Button className={classes.cardButton}
                                            theme = {theme}
                                            color = "secondary"
                                            type="button"
                                            variant="outlined"
                                            disabled = {owedToken.show}
                                            onClick = {(e) => openProofModal(e, owedToken.causeId)} >
                                        Upload proof
                                    </Button>
                                </Card>
                            ))}
                        </Carousel>
                    </div>

                    <div className='big_card'>
                        <h5 className='card-header'>
                            Incoming Tokens
                        </h5>
                        <Carousel>
                            {incomingTokens.map((incomingToken, i) => (
                                <Card
                                    className='card'
                                    key = {i}>

                                    <Typography className={classes.text}>
                                        Amount of incoming tokens: {incomingToken.amount.toFixed(2)} {incomingToken.token}
                                    </Typography>

                                    <Typography className={classes.text}>
                                        Donated amount: {incomingToken.donatedAmount.toFixed(2)} {incomingToken.currency}
                                    </Typography>

                                    <Countdown
                                        className='countdown'
                                        date = {Date.parse(incomingToken.expirationDate)}
                                    />

                                    <Button className={classes.cardButton}
                                            theme = {theme}
                                            color = "secondary"
                                            type="button"
                                            variant="outlined"
                                            disabled = {
                                                new Date(Date.parse(incomingToken.expirationDate)).getTime()
                                                > (new Date()).getTime()
                                            }
                                            onClick = {(e) => requestMoney(e, incomingToken.causeId)} >
                                        Request Money
                                    </Button>
                                </Card>
                            ))}
                        </Carousel>
                    </div>

                    <div className='big_card'>
                        <h5 className='card-header'>
                            Reward Tokens
                        </h5>
                        <Carousel>
                            {rewardTokens.map((rewardToken, i) => (
                                <Card
                                    className='card'
                                    key = {i}>

                                    <CardMedia
                                        className = {classes.media}
                                        image = {process.env.PUBLIC_URL + "/images/" + rewardToken.image}>
                                    </CardMedia>

                                    <h6 className={classes.text}>
                                        {rewardToken.amount.toFixed(2)} {rewardToken.token}
                                    </h6>

                                </Card>
                            ))}
                        </Carousel>

                    </div>
                </ul>
            </div>
        </div>
        </div>
    );
}


export default App;
