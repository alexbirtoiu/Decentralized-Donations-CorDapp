const PROTOCOL = 'http://'
const PORT = '10050' // note that this is the port of the santaServer! (from clients build.gradle: '--server.port=10056')
const HOSTNAME = 'localhost'

const BACKEND_URL = PROTOCOL + HOSTNAME + ':' + PORT;

export { BACKEND_URL };
