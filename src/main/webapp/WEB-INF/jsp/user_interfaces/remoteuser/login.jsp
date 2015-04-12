<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Login</title>
<!--
<link rel="stylesheet" type="text/css" href="../styles/login.css" />
-->
<!--
<script src="jslib/jquery-1.7.1.min.js" type="text/javascript"></script>
<script type="text/javascript" src="jslib/jquery-ui-1.8.9.custom/jquery-ui-1.8.9.custom.min.js"></script>
-->
<script>
	var context;
	$(document).ready(function() {
		var pathname = location.pathname;
		context = /^\/([^\/]+)\//.exec(pathname)[1];
        // Execute login ASAP.
        login();
	});
	
	function login() {
		var json = new Object();
    	$.ajax({
    		type: "post",
    		url: "/" + context + "/Login?operation=login",
    		processData: false,
    		dataType: "json",
    		contentType: "application/json",
    		data: JSON.stringify(json),
    		success: function(data) {
    			window.location.reload();
    		},
    		error: function(jqXHR, textStatus, errorThrown) {
    			var error = $.parseJSON(jqXHR.responseText);
    			setMessage(error.error);
    		}
    	});
	};
	
	function setMessage(message) {
    	$("#message").text(message);
	};

</script>
</head>
<body>
<div id="message"></div>	
</body>
</html>
