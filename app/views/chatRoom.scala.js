@(username: String)

$(function() {
    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var chatSocket = new WS("@routes.Application.chat(username).webSocketURL(request)")
    $("#onChat").show()

    var sendMessage = function() {
        chatSocket.send(JSON.stringify(
            {text: $("#talk").val()}
        ))
        $("#talk").val('')
    }

    var receiveEvent = function(event) {
        var data = JSON.parse(event.data)
        
        // Create the message element
        if (data.type == "members") {
            // Update the members list
            $("#members").html('')
            $(data.members).each(function() {
                var li = document.createElement('li');
                li.textContent = this;
                $("#members").append(li);
            })
        }
        else if (data.type == "talk") {
            var el = $('<div class="message"><span></span><p></p></div>')
            $("span", el).text(data.username)
            $("p", el).text(data.message)
            $(el).addClass(data.kind)
            if(data.username == '@username') $(el).addClass('me')
            $('#messages').append(el)
        }
    }

    var handleReturnKey = function(e) {
        if(e.charCode == 13 || e.keyCode == 13) {
            e.preventDefault()
            sendMessage()
        }
    }

    $("#talk").keypress(handleReturnKey)

    chatSocket.onmessage = receiveEvent

})
