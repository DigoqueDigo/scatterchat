#!/bin/bash

# Debugging
#set -x

# set -e

set -u

YELLOW=$(tput setaf 3)
NORMAL=$(tput sgr0)

msg() {
  printf "%s%s%s\n" "$YELLOW" "$1" "$NORMAL"
}

BUILD_DIR="_build/default/lib/my_server"

compile() {
    msg "Compiling..."
    rebar3 compile
    mkdir -p $BUILD_DIR
    if ! erlc -o $BUILD_DIR *.erl
    then
        msg "Compilation failed."
        exit 1
    fi
}

check_processes() { # To kill every process: pgrep erl | xargs kill -9
  local server_pid tree child_pids
  server_pid=$(pgrep -f "search_server")
  tree=$(pstree -p | grep "beam.smp($server_pid)")
  child_pids=$(echo "$tree" | grep -oP '\(\d+\)' | tr -d '()' | grep -v "^$server_pid$")
  
  if [ -n "$server_pid" ] || [ -n "$child_pids" ]; then
    msg "Server is still running with PID: $server_pid"
    msg "Children PIDs: [$child_pids]"
  fi
}

MODULE="server"
NODE_NAME="server_node"

start() {
#   -s "${MODULE}" start "${number}" \
#   local number="$1"

    #   -sname "${NODE_NAME}" \ # not needed since I only use tcp for comms between servers

  msg "Starting server..."
  erl -pa $BUILD_DIR -pa _build/default/lib/*/ebin \
      -s "${MODULE}" start \
      -setcookie sdoge \
      -noshell -noinput
    #  &> server.log &
  msg "OK"
}

stop() {
  msg "Stoping server..."
#   erl -sname stop_node \
    #   -setcookie sdoge \
    #   -eval "rpc:call(${NODE_NAME}@$(uname -n), ${MODULE}, stop, []), init:stop()." \
    #   -noshell -noinput
  pkill -f beam
  msg "OK"
}

help() {
  msg "Usage: $0 <command>"
  msg "  - compile    : compile"
  msg "  - start    : Start Search Server"
  msg "  - stop     : Stop Search Server"
  msg "  - help     : Display this help message"
}

run_with_terminal() {
    alacritty --working-directory=$(pwd) --command ./build.sh start & disown
}

node_write() {
    if [ $# -ne 2 ]; then
        echo "Error: Function requires exactly two strings"
        return 1
    fi

    # Assign arguments to variables
    local room="$1"
    local ips="$2"

    local json="{\"code\": 1, \"room\": \"$room\", \"ips\": $ips}"

    # exit after sending message
    echo $json | nc localhost 12345 -q 0
}

node_read() {
    if [ $# -ne 1 ]; then
        echo "Error: Function requires exactly one"
        return 1
    fi

    # Assign arguments to variables
    local room="$1"

    local json="{\"code\": 0, \"room\": \"$room\"}"

    # exit after reading 1 packet
    echo $json | nc localhost 12345 -W 1
}

test_nodes() {
    echo "launch node 0"
    run_with_terminal
    sleep 1.5

    # both writes should be on node 0
    echo "add ips to room 0"
    node_write "Room0" "[\"This is a room0 IP\", \"This is also a room0 IP\"]"
    echo "add ips to room 3"
    node_write "Room3" "[\"This is a room3 IP\", \"This is also a room3 IP\"]"
    sleep 1.5

    # reading room 0 should come from node 0
    echo "read room 0"
    node_read "Room0" | jq

    echo "launch node 1"
    run_with_terminal
    sleep 1.5

    # reading node 3 should come from node 1, after values were moved from node 0
    echo "read room 3"
    node_read "Room3" | jq

    # writing room 5 should be on node 0
    echo "add ips to room 5"
    node_write "Room3" "[\"This is a room5 IP\", \"This is also a room5 IP\"]"
    sleep 1.5

    echo "launch node 2, 3 and 4"
    run_with_terminal
    run_with_terminal
    run_with_terminal
    sleep 1.5

    # reading room 0 should come from node 4, after being moved from node 1, which was originally on node 0
    echo "read room 0"
    node_read "Room0" | jq

    # expected result:
    # launch node 0
    # add ips to room 0
    # add ips to room 3
    # read room 0
    # {
    #   "ips": [
    #     "This is a room0 IP",
    #     "This is also a room0 IP"
    #   ]
    # }
    # launch node 1
    # read room 3
    # {
    #   "ips": [
    #     "This is a room3 IP",
    #     "This is also a room3 IP"
    #   ]
    # }
    # add ips to room 5
    # launch node 2, 3 and 4
    # read room 0
    # {
    #   "ips": [
    #     "This is a room0 IP",
    #     "This is also a room0 IP"
    #   ]
    # }
}

case "${1:-help}" in
  "compile")
    compile
    ;;
  "start")
    compile
    # if [ -z "${2:-}" ]; then
    #   msg "You must provide a number argument for 'start'"
    #   exit 1
    # fi
    # start "$2"
    start
    ;;
  "stop")
    stop
    check_processes
    ;;
  "test")
    compile
    test_nodes
    ;;
  "help"|*)
    help
    ;;
esac
