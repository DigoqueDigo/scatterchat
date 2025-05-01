-module(server).
-export([start/0, start/1, stop/1]).

% DATA

% ID == port

% info about this node
% list of the IDs of other nodes
% the ID for a given hash is stored on the ets
-record(info,
    {
        id, my_hash, succ, hashes
    }
).

% stored IPs for a given room
% use ets(room_ips). returns list of strings with IPs

% PID of the handler to another node
% use ets(node_handler). returns PID

% what nodes are responsible for which rooms
% use get_room_id

% DEFAULTS, I assume these values
default_port() -> 12345.

% debug() ->
%     io:format("0 ~p~n", [sha1(0)]),
%     io:format("1 ~p~n", [sha1(1)]),
%     io:format("2 ~p~n", [sha1(2)]),
%     io:format("3 ~p~n", [sha1(3)]),
%     io:format("4 ~p~n", [sha1(4)]),
%     io:format("Room0 ~p~n", [sha1("Room0")]),
%     io:format("Room1 ~p~n", [sha1("Room1")]),
%     io:format("Room2 ~p~n", [sha1("Room2")]),
%     io:format("Room3 ~p~n", [sha1("Room3")]),
%     io:format("Room4 ~p~n", [sha1("Room4")]),
%     io:format("Room5 ~p~n", [sha1("Room5")]),
%     io:format("Room6 ~p~n", [sha1("Room6")]),
%     io:format("Room7 ~p~n", [sha1("Room7")]),
%     io:format("Room8 ~p~n", [sha1("Room8")]),
%     io:format("Room9 ~p~n", [sha1("Room9")]).

start() -> start(0).
start(ID) ->
    % debug(),
    % io:format("starting~n"),
    Port = default_port() + ID,
    % table room name -> list of strings
    case gen_tcp:listen(Port, [binary, {packet, line}, {reuseaddr, true}, {active, true}]) of
        {ok, SSock} ->
            io:format("Server started, listening on port ~p~n", [Port]),

            ets:new(node_handler, [named_table, public, {keypos, 1}]),

            ets:new(node_hashes, [named_table, public, {keypos, 1}]),

            % ets:new(node_rooms, [named_table, public, {keypos, 1}]),
            % ets:insert(node_rooms, {"Room0", 0}),
            % ets:insert(node_rooms, {"Room1", 1}),
            % ets:insert(node_rooms, {"Room2", 2}),
            % ets:insert(node_rooms, {"Room3", 3}),
            % ets:insert(node_rooms, {"Room4", 4}),

            ets:new(room_ips, [named_table, public, {keypos, 1}]),
            % for testing
            % ets:insert(room_ips, {"Room0", ["192.168.1.1:12345", "192.168.1.2:12345"]}),
            % ets:insert(room_ips, {"Room1", ["10.0.0.1:12345"]}),
            % ets:insert(room_ips, {"Room2", ["blabla"]}),
            % ets:insert(room_ips, {"Room3", ["sadafadsfas"]}),
            % ets:insert(room_ips, {"Room4", ["test"]}),

            InfoManager = spawn(fun() -> manage_info(ID) end),
            case ID of
                0 ->
                    io:format("Node 0, not contacting anyone~n"),
                    acceptor(SSock, InfoManager);
                _ ->
                    io:format("Node ~p~n", [ID]),
                    % ask some other node who he thinks my succ is
                    % FIXME: for now I assume this is correct and up to date, and I always contact node 0
                    Succ = ask_for_succ(ID, 0),
                    io:format("My successor is ~p~n", [Succ]),
                    spawn(fun() -> contact_succ(Succ, ID, InfoManager) end),
                    acceptor(SSock, InfoManager)
            end,
            % {ok, SSock};
            ok;
        {error, _} ->
            io:format("Port ~p is in use, trying port ~p~n", [Port, Port + 1]),
            start(ID + 1)
    end,
    % manhosice por causa de usar tcp como active, faz com que quem trata de pedidos sao os processos 'principais' e nao os filhos,
    % por isso quando por ex os clientes fecham a conexao o processo 'pai' fecha e esta funcao sai, e a socket principal fecha
    receive
        _ -> io:format("Exiting~n")
    end.

ask_for_succ(MY_ID, OtherID) ->
    io:format("asking node ~p for my successor~n", [OtherID]),
    case gen_tcp:connect("127.0.0.1", default_port() + OtherID, [
        binary,
            {packet, line},
            {reuseaddr, true},
            {active, true} % TODO: mudar para 1??
        ])
    of
        {ok, Socket} ->
            Request = #{code => 3, id => MY_ID},
            RequestJson = jsone:encode(Request),
            gen_tcp:send(Socket, [RequestJson, "\n"]),
            io:format("request sent~n"),
            receive
                {tcp, Socket, JsonData} ->
                    TrimmedData = string:trim(binary_to_list(JsonData)),
                    Json = jsone:decode(list_to_binary(TrimmedData), [{object_format, proplist}]),

                    Succ = proplists:get_value(<<"succ">>, Json),
                    gen_tcp:close(Socket),
                    Succ;
                _ -> error
            end;
        {error, Reason} ->
            {error, Reason}
    end.

% add all the nodes in this list to the info manager
% whenever a new node gets added, the successor ID gets updated automatically
% add_other_nodes_to_info(_, []) -> ok;
% add_other_nodes_to_info(InfoManager, [Node | Tail]) ->
%     InfoManager ! {insert_node, Node},
%     add_other_nodes_to_info(InfoManager, Tail).


% get_my_succ(InfoManager) ->
%     InfoManager ! {get_succ, self()},
%     receive
%         {Succ} -> Succ
%     end.

% This is just like contacting another node, except a special request is sent to hand over all the data that now belongs to me
contact_succ(Succ, MY_ID, InfoManager) ->
    case gen_tcp:connect("127.0.0.1", default_port() + Succ, [
        binary,
            {packet, line},
            {reuseaddr, true},
            {active, true} % TODO: mudar para 1??
        ])
    of
        {ok, Socket} ->
            io:format("reached successor ~p. sending sync request~n", [Succ]),
            % have to send a special message that says I am a node, my ID, and that I need whatever data belongs to me, as well as a list of all other nodes
            Request = #{code => 4, id => MY_ID},
            RequestJson = jsone:encode(Request),
            gen_tcp:send(Socket, [RequestJson, "\n"]),
            receive
                {tcp, Socket, JsonData} ->
                    TrimmedData = string:trim(binary_to_list(JsonData)),
                    Json = jsone:decode(list_to_binary(TrimmedData), [{object_format, proplist}]),

                    Data = proplists:get_value(<<"data">>, Json),
                    io:format("received sync data:~p~n", [Data]),
                    % add all of the data
                    integrate_received_data(Data),
                    io:format("data sync finished~n"),

                    Nodes = proplists:get_value(<<"nodes">>, Json),
                    io:format("other nodes are~p, contacting them~n", [Nodes]),
                    contact_nodes(MY_ID, Nodes, InfoManager),

                    % keep handling this node
                    handle_node(Succ, Socket, InfoManager)
            end;
        {error, Reason} ->
            io:format("failed to contact successor ~p~n", [Reason]),
            {error, Reason}
    end.

integrate_received_data([]) -> ok;
integrate_received_data([Head | Tail]) ->
    % io:format("integrating ~p~n", [Head]),
    TrimmedData = string:trim(binary_to_list(Head)),
    Json = jsone:decode(list_to_binary(TrimmedData), [{object_format, proplist}]),
    RoomName = proplists:get_value(<<"room_name">>, Json),
    % io:format("Room is ~p~n", [RoomName]),
    {ok, RoomAsString} = bytes_to_str(RoomName),
    IPs = proplists:get_value(<<"ips">>, Json),
    IPsAsStrings = bytes_list_to_strings(IPs),
    local_set_ips(RoomAsString, IPsAsStrings),
    integrate_received_data(Tail).

stop(Server) -> Server ! stop.

acceptor(SSock, InfoManager) ->
    case gen_tcp:accept(SSock) of
    {ok, Sock} ->
        io:format("New connection accepted~n"),
        spawn(fun() -> acceptor(SSock, InfoManager) end),
        % acceptor(SSock, InfoManager),
        % spawn(fun() -> main_handler(SSock, Sock, InfoManager) end)
        main_handler(SSock, Sock, InfoManager)
        ;
    {error, Reason} -> 
        io:format("error accepting connection: ~p~n", [Reason])
    end.

% answers first-contact requests
main_handler(SSock, Socket, InfoManager) ->
    receive
        {tcp, Socket, JsonData} ->
            % try
                % Trim newline and decode JSON
                TrimmedData = string:trim(binary_to_list(JsonData)),
                Json = jsone:decode(list_to_binary(TrimmedData), [{object_format, proplist}]),

                Code = proplists:get_value(<<"code">>, Json),

                case Code of
                    0 ->
                        % get IPs
                        Room = proplists:get_value(<<"room">>, Json),
                        {ok, RoomAsString} = bytes_to_str(Room),
                        {ok, IPs} = get_ips(RoomAsString, InfoManager),
                        % Response = #{code => 0, room => RoomAsString, ips => [list_to_binary(IP) || IP <- IPs]},
                        Response = #{ips => [list_to_binary(IP) || IP <- IPs]},
                        ResponseJson = jsone:encode(Response),
                        gen_tcp:send(Socket, [ResponseJson, "\n"]),
                        % keep accepting requests in this process
                        main_handler(SSock, Socket, InfoManager);
                    1 ->
                        % set IPs
                        Room = proplists:get_value(<<"room">>, Json),
                        IPs = proplists:get_value(<<"ips">>, Json),
                        {ok, RoomAsString} = bytes_to_str(Room),
                        IPsAsStrings = bytes_list_to_strings(IPs),
                        set_ips(RoomAsString, IPsAsStrings, InfoManager),
                        % keep accepting requests in this process
                        main_handler(SSock, Socket, InfoManager);
                    2 ->
                        % open a handler to keep contact with the node
                        Other_ID = proplists:get_value(<<"id">>, Json),
                        io:format("being contacted by a node: ~p~n", [Other_ID]),
                        % keep accepting requests in a new process
                        spawn(fun() -> acceptor(SSock, InfoManager) end),
                        handle_node(Other_ID, Socket, InfoManager);
                    3 ->
                        OtherID = proplists:get_value(<<"id">>, Json),
                        io:format("Successor request received from ~p~n", [OtherID]),
                        Hashes = get_hash_list(InfoManager),
                        Succ = calculate_succ(Hashes, OtherID),
                        Response = #{succ => Succ},
                        ResponseJson = jsone:encode(Response),
                        gen_tcp:send(Socket, [ResponseJson, "\n"]),
                        % keep accepting requests in this process
                        main_handler(SSock, Socket, InfoManager);
                    4 ->
                        Other_ID = proplists:get_value(<<"id">>, Json),

                        % manually register now instead of calling handle_node, so it shows up on the ets, will be needed
                        InfoManager ! {insert_node, Other_ID},
                        ets:insert(node_handler, {Other_ID, self()}),

                        io:format("received sync request from node ~p~n", [Other_ID]),
                        MY_ID = get_id(InfoManager),
                        Nodes = get_sync_nodes(Other_ID, MY_ID),
                        Data = get_sync_data(MY_ID, InfoManager),

                        % io:format("data: ~p~n", [Data]),

                        Response = #{nodes => Nodes, data => Data},
                        ResponseJson = jsone:encode(Response),
                        gen_tcp:send(Socket, [ResponseJson, "\n"]),
                        io:format("response sent~n"),

                        % do not keep accepting requests
                        % spawn(fun() -> acceptor(SSock, InfoManager) end),
                        handle_node_loop(Other_ID, Socket, InfoManager); % WARN: using this since it was already added above
                    _ ->
                        io:format("Unknown code received: ~p~n", [Code]),
                        % keep accepting requests in this process
                        main_handler(SSock, Socket, InfoManager)
                end,
                gen_tcp:close(Socket);
                % handle_client(Socket)
            % catch
            %     error:Reason ->
            %         io:format("Error processing data: ~p~n", [Reason]),
            %         gen_tcp:close(Socket)
            % end;
        {tcp_closed, Socket} ->
            io:format("Client disconnected~n"),
            gen_tcp:close(Socket);
        {tcp_error, Socket, Reason} ->
            io:format("TCP error: ~p~n", [Reason]),
            gen_tcp:close(Socket)
    end.

% this list of nodes cannot include:
% the node contacting me
% myself
get_sync_nodes(Other_ID, MY_ID) ->
    All_IDs = ets:foldl(fun({_, ID}, Acc) -> [ID | Acc] end, [], node_hashes),
    WithoutOther = lists:delete(Other_ID, All_IDs),
    Final = lists:delete(MY_ID, WithoutOther),
    Final.

get_hash_list(InfoManager) ->
    InfoManager ! {get_hashes, self()},
    receive
        {ok, Hashes} -> Hashes
    end.

% will also delete data that is no longer mine to handle
% get_sync_data(MY_ID, InfoManager) ->
%     HashList = get_hash_list(InfoManager),
%     Entries = ets:tab2list(room_ips),
%     lists:foldl(
%         fun(Entry = {Room, IPs}, Acc) ->
%             case hash_belongs_to_me(MY_ID, sha1(Room), HashList) of
%                 true -> Acc;
%                 false ->
%                     ets:delete(room_ips, Room),
%                     [Entry | Acc]
%             end
%         end, [], Entries).

get_sync_data(MY_ID, InfoManager) ->
    HashList = get_hash_list(InfoManager),
    Entries = ets:tab2list(room_ips),
    lists:foldl(
        fun({Room, IPs}, Acc) ->
            case hash_belongs_to_me(MY_ID, sha1(Room), HashList) of
                true ->
                    % do nothing
                    Acc;
                false ->
                    % Delete entry, add to accumulator
                    ets:delete(room_ips, Room),
                    % [#{<<"room_name">> => list_to_binary(Room), <<"ips">> => iplist_to_binary(IPs, [])} | Acc]
                    Json = #{room_name => list_to_binary(Room), ips => [list_to_binary(IP) || IP <- IPs]},
                    [jsone:encode(Json) | Acc]
                    % [#{list_to_binary(Room) => iplist_to_binary(IPs, [])} | Acc]
            end
        end, [], Entries).

hash_belongs_to_me(MY_ID, Hash, HashList) ->
    {ok, ResID} = get_id_from_hash_with_default(HashList, Hash),
    Bool = MY_ID =:= ResID,
    io:format("I am ~p, result is ~p. returning ~p~n", [MY_ID, ResID, Bool]),
    Bool.

% iplist_to_binary([], Acc) -> Acc;
% iplist_to_binary([Head | Tail], Acc) ->
%     iplist_to_binary(Tail, [list_to_binary(Head) | Acc]).


% contact_other_nodes(MyInfo) ->
%     io:format("Contacting other nodes~n"),
%     spawn(fun() -> contact_other_nodes_aux(MyInfo, MyInfo#info.other_ids) end).


% contact_other_nodes_aux(_, []) -> ok;
% contact_other_nodes_aux(MyInfo, [Other_ID | Tail]) ->
%     MY_ID = MyInfo#info.id,
%     spawn(fun() -> contact_other_nodes_aux(MyInfo, Tail) end),
%     % io:format("Node ~p~n", [Other_ID]),
%     case gen_tcp:connect("127.0.0.1", default_port() + Other_ID, [
%         binary,
%             {packet, line},
%             {reuseaddr, true},
%             {active, true} % TODO: mudar para 1??
%         ])
%     of
%         {ok, Socket} ->
%             io:format("reached node ~p~n", [Other_ID]),
%             ok = contact_node(MY_ID, Other_ID, Socket),
%             handle_node(Other_ID, Socket, MyInfo),
%             ok;
%         {error, Reason} ->
%             io:format("failed to reached node ~p~n", [Other_ID]),
%             {error, Reason}
%     end.

contact_node(MY_ID, OtherID, InfoManager) ->
    % open a connection
    case gen_tcp:connect("127.0.0.1", default_port() + OtherID, [
        binary,
            {packet, line},
            {reuseaddr, true},
            {active, true} % TODO: mudar para 1??
        ])
    of
        {ok, Socket} ->
            % send a connect message
            Json = jsone:encode(#{code => 2, id => MY_ID}),
            ok = gen_tcp:send(Socket, [Json, "\n"]),
            % handle this node. this function creates the handler etc
            io:format("contacted node ~p~n", [OtherID]),
            handle_node(OtherID, Socket, InfoManager);
        {error, Reason} ->
            {error, Reason}
    end.

contact_nodes(_, [], _) -> ok;
contact_nodes(MY_ID, [Node | Tail], InfoManager) ->
    spawn(fun() -> contact_node(MY_ID, Node, InfoManager) end),
    contact_nodes(MY_ID, Tail, InfoManager).


% checks if node is already being handled or not, safe to call whenever
% TODO: might be missing a lock between the lookup and the insert
handle_node(Other_ID, Socket, InfoManager) ->
    case ets:lookup(node_handler, Other_ID) of
        [{_, _}] ->
            io:format("Node already being handled, closing socket~n"),
            gen_tcp:close(Socket);
        [] ->
            ets:insert(node_handler, {Other_ID, self()}),
            io:format("Node not already being handled, added to ets~n"),
            InfoManager ! {insert_node, Other_ID},
            handle_node_loop(Other_ID, Socket, InfoManager)
    end.

% process that handle connections to other nodes run in this loop
% will also process requests and answer back to the requester
handle_node_loop(Other_ID, Socket, InfoManager) ->
    % simulate a "GET IPS" request
    % timer:sleep(2000),
    % io:format("simulating GET IPS request~n"),
    % Json = jsone:encode(#{code => 0, room => "Room1"}),
    % ok = gen_tcp:send(Socket, [Json, "\n"]),

    receive
        % TCP message received from another node, can either be a request for IPs or a reply to a previous request sent by me
        {tcp, Socket, JsonData} ->
            TrimmedData = string:trim(binary_to_list(JsonData)),
            Json = jsone:decode(list_to_binary(TrimmedData), [{object_format, proplist}]),
            Code = proplists:get_value(<<"code">>, Json),
            case Code of
                0 ->
                    io:format("got IPs request from a node~n"),
                    Room = proplists:get_value(<<"room">>, Json),
                    {ok, RoomAsStr} = bytes_to_str(Room),
                    RequesterBin = proplists:get_value(<<"pid">>, Json),
                    {ok, RequesterStr} = bytes_to_str(RequesterBin),
                    {ok, IPs} = get_ips(RoomAsStr, InfoManager),
                    Response = #{code => 1, pid => RequesterStr, ips => [list_to_binary(IP) || IP <- IPs]},
                    ResponseJson = jsone:encode(Response),
                    gen_tcp:send(Socket, [ResponseJson, "\n"]);
                1 ->
                    io:format("got IPs response from a node~n"),
                    % get the information from the response
                    RequesterBin = proplists:get_value(<<"pid">>, Json),
                    {ok, RequesterStr} = bytes_to_str(RequesterBin),
                    RequesterPID = list_to_pid(RequesterStr),
                    IPs = proplists:get_value(<<"ips">>, Json),
                    IPsAsStrings = bytes_list_to_strings(IPs),
                    % send the response back to who requested it
                    RequesterPID ! {ips_result, IPsAsStrings};
                2 ->
                    io:format("got PUT request from a node~n"),
                    Room = proplists:get_value(<<"room">>, Json),
                    IPs = proplists:get_value(<<"ips">>, Json),
                    {ok, RoomAsString} = bytes_to_str(Room),
                    IPsAsStrings = bytes_list_to_strings(IPs),
                    set_ips(RoomAsString, IPsAsStrings, InfoManager);
                _ ->
                    io:format("Unknown code received: ~p~n", [Code])
            end;
        % request_ips message received from another process
        % need to send a get ips request to the node
        % I send the PID in the message so the requester process can be contacted when the reply comes
        {request_ips, {PID, RoomNameAsStr}} ->
            io:format("Request to send GET to node received~n"),
            PidStr = pid_to_list(PID),
            Response = #{code => 0, room => RoomNameAsStr, pid => PidStr},
            ResponseJson = jsone:encode(Response),
            gen_tcp:send(Socket, [ResponseJson, "\n"]);
        % requested to set IPs
        {set_ips, {RoomNameAsStr, IPs}} ->
            io:format("Request to send PUT to node received~n"),
            Req = #{code => 2, room => RoomNameAsStr, ips => [list_to_binary(IP) || IP <- IPs]},
            ReqJson = jsone:encode(Req),
            gen_tcp:send(Socket, [ReqJson, "\n"])
    end,
    handle_node_loop(Other_ID, Socket, InfoManager).

get_id(InfoManager) ->
    InfoManager ! {get_id, self()},
    receive
        {ok, ID} -> ID
    end.

% {ok, IPs} pr {error, Reason}
get_ips(RoomName, InfoManager) ->
    io:format("Requested room ~p~n", [RoomName]),
    MY_ID = get_id(InfoManager),
    Responsible_ID = get_room_id(RoomName, InfoManager),
    case MY_ID =:= Responsible_ID of
        true ->
            io:format("I am responsible for the requested room~n"),
            {ok, IPs} = local_ip_lookup(RoomName),
            {ok, IPs};
        false ->
            io:format("I (~p) am NOT responsible for the requested room, contacting node ~p~n", [MY_ID, Responsible_ID]),
            {ok, IPs} = remote_ip_lookup(RoomName, Responsible_ID),
            {ok, IPs}
    end.

local_ip_lookup(RoomName) ->
    case ets:lookup(room_ips, RoomName) of
        [{_, IPs}] ->
            io:format("Requested IPs for ~s: ~p~n", [RoomName, IPs]),
            {ok, IPs};
        [] ->
            io:format("Requested IPs for ~s: [] (room not found)~n", [RoomName]),
            {ok, []}
    end.

remote_ip_lookup(RoomName, Responsible_ID) ->
    case ets:lookup(node_handler, Responsible_ID) of
        [{_, HandlerPID}] ->
            % send message to node handler and wait for response
            HandlerPID ! {request_ips, {self(), RoomName}},
            receive
                {ips_result, IPs} -> {ok, IPs};
                {_} -> {error, incorrect_response}
            end;
        [] ->
            {error, handler_unavailable}
    end.

% handle_set_ips(RoomName, IPs) ->
%     io:format("Setting IPs for ~p~n", [RoomName]),
%     ets:insert(room_ips, {RoomName, IPs}).

set_ips(RoomName, IPs, InfoManager) ->
    io:format("Setting IPs for ~p~n", [RoomName]),
    MY_ID = get_id(InfoManager),
    Responsible_ID = get_room_id(RoomName, InfoManager),

    case MY_ID =:= Responsible_ID of
        true ->
            io:format("I am responsible for the requested room~n"),
            local_set_ips(RoomName, IPs);
        false ->
            io:format("I (~p) am NOT responsible for the requested room, contacting node ~p~n", [MY_ID, Responsible_ID]),
            remote_set_ips(RoomName, Responsible_ID, IPs)
    end.

local_set_ips(RoomName, IPs) ->
    io:format("Setting IPs for ~p~n", [RoomName]),
    ets:insert(room_ips, {RoomName, IPs}).

remote_set_ips(RoomName, Responsible_ID, IPs) ->
    case ets:lookup(node_handler, Responsible_ID) of
        [{_, HandlerPID}] ->
            HandlerPID ! {set_ips, {RoomName, IPs}};
        [] ->
            {error, handler_unavailable}
    end.



get_room_id(RoomName, InfoManager) ->
    RoomHash = sha1(RoomName),
    InfoManager ! {get_id_from_hash, {self(), RoomHash}},
    receive
        {id_result, ID} ->
            ID;
        % {id_result, {error, Reason}} ->
        %     io:format("error in get_room_id: ~p~n", [Reason]),
        %     {error, Reason};
        {_} ->
            io:format("error, invalid respose in get_room_id~n"),
            error
    end.

% manages a list of hashes, and updates ets as well
manage_info(MY_ID) ->
    MyHash = sha1(MY_ID),
    MyInfo = #info{id = MY_ID, my_hash = MyHash, succ = 0, hashes = [MyHash]},
    ets:insert(node_hashes, {MyHash, MY_ID}),
    manage_info_loop(MyInfo).

manage_info_loop(MyInfo) ->
    receive
        % adds a new node and its hash
        {insert_node, ID} ->
            List = MyInfo#info.hashes,
            Hash = sha1(ID),
            ListWithHash = [Hash | List],
            SortedList = lists:sort(ListWithHash),
            ets:insert(node_hashes, {Hash, ID}),
            Succ = calculate_succ(SortedList, MyInfo#info.id),
            NewInfo = #info{id = MyInfo#info.id, succ = Succ, hashes = SortedList},
            io:format("info manager: ~p associated to ~p. Succ is now ~p~n", [ID, Hash, Succ]),
            manage_info_loop(NewInfo);
        {get_id_from_hash, {RequesterPID, Hash}} ->
            io:format("info manager: requested hash ~p~n", [Hash]),
            List = MyInfo#info.hashes,
            % MY_ID = MyInfo#info.id,
            {ok, Res} = get_id_from_hash_with_default(List, Hash),
            RequesterPID ! {id_result, Res},
            manage_info_loop(MyInfo);
        {change_succ, Succ} ->
            NewInfo = #info{id = MyInfo#info.id, succ = Succ, hashes = MyInfo#info.hashes},
            manage_info_loop(NewInfo);
        {get_succ, ReqPID} ->
            ReqPID ! {MyInfo#info.succ},
            manage_info_loop(MyInfo);
        {get_hashes, ReqPID} ->
            ReqPID ! {ok, MyInfo#info.hashes},
            manage_info_loop(MyInfo);
        {get_id, ReqPID} ->
            ReqPID ! {ok, MyInfo#info.id},
            manage_info_loop(MyInfo)
    end.

% hash list has to be sorted
calculate_succ(HashList, MY_ID) ->
    MyHash = sha1(MY_ID),
    % Convert hex string to integer
    {ok, [Number], _} = io_lib:fread("~16u", MyHash),
    % Add 1
    NewNumber = Number + 1,
    % Convert back to hex string (uppercase, no leading zeros)
    NewHexString = io_lib:format("~40.16.0B", [NewNumber]),
    % Flatten to string
    lists:flatten(NewHexString),
    {ok, ID} = get_id_from_hash_with_default(HashList, NewHexString),
    ID.

% returns the ID of node that is responsible for this hash
% use this over get_id_from_hash to correctly handle edge cases
get_id_from_hash_with_default([], _) ->
    io:format("Error: requested ID from hash but I have no hashes~n"),
    {error, no_hashes};
get_id_from_hash_with_default(HashList, Hash) ->
    % MyHash = sha1(MY_ID),
    % Smallest_diff = get_smallest_diff(HashList, MyHash),
    Smallest = hd(HashList),
    get_id_from_hash_aux(HashList, Hash, Smallest).

% % get the smallest value
% get_smallest_diff([], MyHash) -> MyHash;
% get_smallest_diff([Head | Tail], MyHash) ->
%     case Head =/= MyHash of
%         true ->
%             Head;
%         false ->
%             get_smallest_diff(Tail, MyHash)
%     end.


% usually default will be the smallest hash
% returns the ID of node that is responsible for this hash
get_id_from_hash_aux([], _, Default) ->
io:format("id from hash: no entries, returning default hash~p~n", [Default]), 
    case ets:lookup(node_hashes, Default) of
        [{_, ID}] ->
            io:format("id from hash: returning ID ~p~n", [ID]),
            {ok, ID};
        [] ->
            io:format("id from hash: there is no node for ~p!!~n", [Default]),
            {error, no_node}
    end;
get_id_from_hash_aux([Head | Tail], Hash, Default) ->
    case Head >= Hash of
        true ->
            case ets:lookup(node_hashes, Head) of
                [{_, ID}] ->
                    io:format("id from hash: returning ID ~p~n", [ID]),
                    {ok, ID};
                [] ->
                    io:format("id from hash: there is no node for ~p!!~n", [Head]),
                    {error, no_node}
            end;
        false ->
            get_id_from_hash_aux(Tail, Hash, Default)
    end.

sha1(Data) ->
    DataBin = case Data of
        B when is_binary(B) -> B;
        L when is_list(L)   -> list_to_binary(L);
        Other               -> term_to_binary(Other)
    end,
    HashBin = crypto:hash(sha, DataBin),
    BinList = binary:bin_to_list(HashBin),
    lists:flatten([ io_lib:format("~2.16.0B", [Byte]) || Byte <- BinList ]).

bytes_list_to_strings(Binaries) when is_list(Binaries) ->
    lists:map(
        fun(Binary) ->
            case bytes_to_str(Binary) of
                {ok, String} -> String;
                {error, invalid_utf8} -> "invalid_utf8"
            end
        end,
        Binaries
    ).

bytes_to_str(Bytes) ->
    case unicode:characters_to_list(Bytes, utf8) of
        String when is_list(String) -> {ok, String};
        {error, _, _} -> {error, invalid_utf8}
    end.
