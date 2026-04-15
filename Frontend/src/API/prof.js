import axios from "./axios";


export async function pullProfiles() {
    const res = await axios.get('/auth/pullProfiles');

    if (res.data === null) {
        throw new Error('No response from server');
    }

    return res.data;
}

export async function updateProfile(data) {
    if (!data) {
        throw new Error('Invalid data');
    }

    const res = await axios.post('/auth/pushProfile', data);

    if (res.data === null) {
        throw new Error('No response from server');
    }

    return res.data;
}