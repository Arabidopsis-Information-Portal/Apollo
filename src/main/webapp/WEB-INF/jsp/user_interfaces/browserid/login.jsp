<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Login</title>
<!--
<script src="jslib/jquery-1.7.1.min.js" type="text/javascript"></script>
-->
<script src="https://browserid.org/include.js" type="text/javascript"></script>  
<script>
	var context;
	$(document).ready(function() {
		var pathname = location.pathname;
		context = /^\/([^\/]+)\//.exec(pathname)[1];
		$("#login_button").click(function() {
			login();
		});
		$("#sign_in_image").attr("src", "/" + context + "/images/sign_in_green.png");
	});
	
	function login() {
		navigator.id.get(function(assertion) {
		    if (assertion) {
		    	var json = new Object();
		    	json.assertion = assertion;
		    	$.ajax({
		    		type: "post",
		    		url: "/" + context + "/Login?operation=login",
		    		processData: false,
		    		dataType: "json",
		    		contentType: "application/json",
		    		data: JSON.stringify(json),
		    		success: function(data) {
		    			//window.location = data.url;
		    			window.location.reload();
		    		},
		    		error: function(jqXHR, textStatus, errorThrown) {
		    			var error = $.parseJSON(jqXHR.responseText);
		    			setMessage(error.error);
		    		}
		    	});
		    }
		    else {
		    	setMessage("Error logging in");
		    }
		});
	};
	
	function setMessage(message) {
    	$("#message").text(message);
	};
	
</script>
</head>
<body>
<div>
<button id="login_button"><img id="sign_in_image" /></button>
<div id="message">
</div>
</div>
</body>
</html>