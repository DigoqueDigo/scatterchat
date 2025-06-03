#!/bin/bash

set -u

spawn_dht() {
    cd src/main/erlang/scatterchat/dht/
    ./build.sh start
}

spawn_sa() {
    local id="$1"
    mvn exec:java -Dexec.mainClass=scatterchat.aggrserver.AggrServer -Dexec.args="config/config.json $id"
}

spawn_sc() {
    local id="$1"
    mvn exec:java -Dexec.mainClass=scatterchat.chatserver.ChatServer -Dexec.args="config/config.json $id"
}

client() {
    local id="$1"
    mvn exec:java -Dexec.mainClass=scatterchat.client.Client -Dexec.args="config/config.json $id"
}

main() {
    mvn clean compile
    sleep 0.1
    mvn clean compile

    # dht
    swaymsg split vertical

    # alacritty --working-directory=$(pwd) --command ./present.sh dht > /dev/null 2>&1 & disown
    # sleep 0.5;
    alacritty --working-directory=$(pwd) --command ./present.sh dht > /dev/null 2>&1 & disown
    sleep 0.5;
    alacritty --working-directory=$(pwd) --command ./present.sh dht > /dev/null 2>&1 & disown
    sleep 0.5;

    # sa
    swaymsg focus up
    swaymsg focus up

    swaymsg split horizontal
    alacritty --working-directory=$(pwd) --command ./present.sh sa sa1 > /dev/null 2>&1 & disown
    sleep 0.5;
    swaymsg focus down
    swaymsg split horizontal
    alacritty --working-directory=$(pwd) --command ./present.sh sa sa2 > /dev/null 2>&1 & disown
    sleep 0.5;
    swaymsg focus down
    swaymsg split horizontal
    alacritty --working-directory=$(pwd) --command ./present.sh sa sa3 > /dev/null 2>&1 & disown
    sleep 0.5;

    # sc
    swaymsg focus up
    swaymsg focus up

    alacritty --working-directory=$(pwd) --command ./present.sh sc sc1 > /dev/null 2>&1 & disown
    sleep 0.5;
    swaymsg focus down
    alacritty --working-directory=$(pwd) --command ./present.sh sc sc2 > /dev/null 2>&1 & disown
    sleep 0.5;
    swaymsg focus down
    alacritty --working-directory=$(pwd) --command ./present.sh sc sc3 > /dev/null 2>&1 & disown
    sleep 0.5;

    ./present.sh dht
}

case "$1" in
    "sa")
        spawn_sa $2
        ;;
    "sc")
        spawn_sc $2
        ;;
    "dht")
        spawn_dht
        ;;
    "main")
        main
        ;;
    "client")
        client $2
        ;;
esac
