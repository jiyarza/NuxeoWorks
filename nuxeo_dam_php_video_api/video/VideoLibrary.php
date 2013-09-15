<?php

/**
 * Sample code for a video player using Nuxeo DAM API
 */

include_once ("Video.php");	
include_once ("VideoManager.php");


// Nuxeo connection parameters. Typically retrieved from a configuration file
$host = "localhost";
$port = "8080";
$username = "Administrator";
$password = "Administrator";

// Create one VideoManager instance
$man = new VideoManager($host, $port, $username, $password);

// $filter contains the string for the search filter
$filter = "";
if (isset($_GET) && array_key_exists('filter', $_GET)) {
	// Filtered video list
	$filter = $_GET['filter'];
	$videos = $man->getVideos($filter, VideoManager::ORDER_BY_DATE_DESC);
} else {
	// Get all the videos ordered by modified date
	$videos = $man->getVideos(null, VideoManager::ORDER_BY_DATE_DESC);
}
// Get the selected video that will be shown in the video player.
// It should come defined in the URL (parameter uid). If this param is not set,
// the first video in the list is selected by default.
if (isset($_GET) && array_key_exists('uid', $_GET)) {
	$uid = $_GET['uid'];
	$selectedVideo = $man->getVideoByUid($uid);
} else {
	$selectedVideo = $videos[0];
	$uid = $selectedVideo->getUid();
}


// Builds the html template with the list of videos that is shown to the side of the player
// For the thumbnail it is used getStoryboardUrl(3) instead getThumbnailUrl() since the image 
// is smaller and less heavy.
$videoTemplate = "";
foreach ($videos as $v) {
	$videoTemplate .= "<li><span>"
				 ."<img style='float:left' src='".$v->getStoryboardUrl(3)."' />"				 
				 ."<a href='videoteca.php?uid=".$v->getUid()."'><b>".$v->getTitle()."</b></a><br/>"
				 ."<i>".$v->getDescription()."</i><br/>"
				 .$v->getSubjectsAsString()."<br/>"
				 ."</span><br/></li>";	
}

?>

<!DOCTYPE HTML>
<html>

<head>
	<title>Video Library</title>
	
	<!-- Required by Projekktor -->
    <script type="text/javascript" src="/projekktor/jquery.min.js"></script>
    <script type="text/javascript" src="/projekktor/projekktor.min.js"></script>
    <link rel="stylesheet" href="/projekktor/theme/style.css" type="text/css" media="screen" />    

	<script type="text/javascript">
		$(document).ready(function() {
			projekktor('video', {
			     /* path to the MP4 Flash-player fallback component */
			     playerFlashMP4:        '/projekktor/jarisplayer.swf',			    
			     /* path to the MP3 Flash-player fallback component */
			     playerFlashMP3:        '/projekktor/jarisplayer.swf',			     
			});
		});
	</script>    
    <!-- End required by Projekktor -->
</head>

<body>

	<h1>Video Library</h1>
	<!-- Render video player - the selected video is the source -->
	<div style="float:left">
		<video id="player_a" class="projekktor" poster="intro.png" title="Video Library" width="480" height="320" controls>
			<source src="<?php echo $selectedVideo->getTranscodedVideoUrl(Video::WEBM); ?>"></source>
		</video>
	</div>

	<!-- Render video list -->
	<div style="float:left; width:600px">		
		<form action="videoteca.php">
			Filter:<input type="text" id="filter" name="filter" value="<?php echo $filter?>"/>
			<input type="submit" id="button" value="Buscar"/>			
		</form>
		<ul>
			<?php echo $videoTemplate ?>
		</ul>
	</div>

</body>
</html>