var socket = new SockJS('/secured/room');
var stompClient = Stomp.over(socket);
var sessionId = "";

stompClient.connect({}, function(frame) {
	var url = stompClient.ws._transport.url;
	url = url.replace(
		"ws://localhost:8080/spring-security-mvc-socket/secured/room/", "");
	url = url.replace("/websocket", "");
	url = url.replace(/^[0-9]+\//, "");
	console.log("Your current session is: " + url);
	sessionId = url;
	console.log('SessionId: ' + sessionId);
	// Definisci la tua stringa
	var urlString = sessionId;

	// Usa un'espressione regolare per trovare l'ultima occorrenza di "/"
	var regex = /\/([^/]+)\/([^/]+)$/; // Questa regex cattura gli ultimi due elementi separati da "/"
	var match = urlString.match(regex);
	// Controlla se c'è una corrispondenza
	if (match) {
		var penultimateSubstring = match[1]; // "/426"
		var lastSubstring = match[2]; // "gsxt0rwk"
		console.log("Penultimate Substring:", penultimateSubstring);
		console.log("Last Substring:", lastSubstring);
	} else {
		console.log("Nessuna corrispondenza trovata.");
	}
	var stringa = '/secured/user/queue/notifications' + '-user' + lastSubstring;
	console.log('Stinga: ' + stringa);
	stompClient.subscribe(stringa
		, function(messageOutput) {
			console.log('Received!!!!!!');
		});
});