import instance from "./axios";
import type { ProfileData } from "../customTypes";

export async function pullProfiles(): Promise<ProfileData> {
    const res = await instance.get<ProfileData>('/auth/pullProfiles');

    if (res.data === null) {
        throw new Error('No response from server');
    }

    return res.data;
}

export async function updateProfile(data: Record<string, unknown>): Promise<unknown> {
    if (!data) {
        throw new Error('Invalid data');
    }

    const res = await instance.post('/auth/pushProfile', data);

    if (res.data === null) {
        throw new Error('No response from server');
    }

    return res.data;
}
