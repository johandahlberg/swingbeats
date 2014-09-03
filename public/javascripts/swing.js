/* Utility stuff */
function scrollTo(id) {
	$('html,body').animate({
		scrollTop : $("#" + id).offset().top
	}, 'slow');
}

/* Progress indications */

Ladda.bind('input[type=submit]');

$(function() {
	$('#swing-button').click(function(e) {
		e.preventDefault();
		var spinner = Ladda.create(this);
		spinner.start();
		getPlaylist(spinner);
		return false;
	});
});

/* Tools for making working with the Spotify and Echo Nest APIs easier */

/**
 * @param fid
 */
function fidToSpid(fid) {
	var fields = fid.split(':');
	return fields[fields.length - 1];
}

/**
 * 
 * @param title
 * @param playlist
 * @returns
 */
function getSpotifyPlayButtonForPlaylist(title, playlist) {
	var embed = '<iframe src="https://embed.spotify.com/'
			+ '?uri=spotify:trackset:PREFEREDTITLE:TRACKS&theme=white"'
			+ 'style="width:640px; height:520px;" frameborder="0" '
			+ 'allowtransparency="true"></iframe>';
	var tids = [];
	playlist.forEach(function(entry) {
		var tid = fidToSpid(entry.spotify_id);
		tids.push(tid);
	});
	var tracks = tids.join(',');
	var tembed = embed.replace('TRACKS', tracks);
	tembed = tembed.replace('PREFEREDTITLE', title);
	var li = $("<span>").html(tembed);
	return $("<span>").html(tembed);
}

/**
 * 
 * @param spinner
 */
function getPlaylist(spinner) {
	var bmpValues = $('#bpm-slider').slider('getValue');
	var minDancability = $('#danceability-slider').slider('getValue');

	var minBpm = bmpValues[0];
	var maxBpm = bmpValues[1];

	var url = '/playlist?minbeat=' + minBpm + '&maxbeat=' + maxBpm
			+ "&minDancability=" + minDancability;

	$("#playlist_result").empty();
	console.log("Creating the playlist for range: " + minBpm + "-" + maxBpm);
	$.getJSON(url).done(
			function(data) {
				if (data.length < 1) {
					console.log("Can't find any songs!");
					console.log(data);
				} else {
					var title = "Awesome swing list!";
					var spotifyPlayButton = getSpotifyPlayButtonForPlaylist(
							title, data);
					$("#playlist_result").append(spotifyPlayButton);
					$("#playlist").show();
					spinner.stop();
					scrollTo("playlist");
				}
			}).error(function() {
		console.log("Whoops, had some trouble getting that playlist.");
		spinner.stop();
	});
}