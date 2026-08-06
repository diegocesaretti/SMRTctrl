package com.bwa3d.homehublegacy;

final class HubState {
    static volatile boolean screenOn = true;
    static volatile boolean motion = false;
    static volatile String mediaState = "idle";
    static volatile String mediaUrl = "";
    static volatile String mediaTitle = "";
    static volatile int volume = 50;

    private HubState() {
    }
}
