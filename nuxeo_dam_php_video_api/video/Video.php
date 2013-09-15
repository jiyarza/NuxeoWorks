<?php

include_once ("NuxeoAutomationApi.php");

/**
 * Nuxeo DAM Video wrapper.
 *
 * @author jiyarza
 */
class Video {
	
	/**
	 * @var NuxeoDocument;
	 */
	private $nuxeoDocument;
	/** 
	 * @var VideoManager
	 */
	private $manager;
	
	/** 
	 * Available video formats
	 */
	const WEBM = 0;
	const OGG = 1;
	const MP4 = 2;
	
	/**
	 * @param VideoManager $manager
	 * @param NuxeoDocument $nuxeoDocument
	 */
	public function __construct($manager, $nuxeoDocument) {		
		if (!isset($nuxeoDocument)) {
			throw new Exception ("nuxeoDocument cannot be null.");
		}
		if (!isset($manager)) {
			$this->manager = new VideoManager();
		} else {
			$this->manager = $manager;
		}	
		$this->nuxeoDocument = $nuxeoDocument;
	}

	/**
	 * @return string - video Uid in Nuxeo
	 */
	public function getUid() {
		return $this->nuxeoDocument->getUid();
	}	
	
	/**
	 * @return string - video path in Nuxeo
	 */
	public function getPath() {
		return $this->nuxeoDocument->getPath();
	}
	
	/**
	 * @return string - video title
	 */
	public function getTitle() {
		return $this->nuxeoDocument->getTitle();
	}
	
	/**
	 * @return string - video last modified date
	 */
	public function getLastModifiedDate() {
		return $this->nuxeoDocument->getLastModified();
	}
	
	/**
	 * @return string - video description
	 */
	public function getDescription() {		
		return $this->nuxeoDocument->getProperty('dc:description');
	}
	
	/**  
	 * @return string - thumbnail url (shortcut to use it as an html img src)
	 */
	public function getThumbnailUrl() {
		return $this->manager->getNuxeoUrl()."/nxpicsfile/default/".$this->getUid()."/Thumbnail:content/";
	}
	
	/**
	 * @param number $index - storyboard image index(0-8)
	 * @return string - storyboard image url
	 */
	public function getStoryboardUrl($index = 0) {
		if ($index<0) throw Exception("storyboard index cannot be negative.");
		return ($this->manager->getNuxeoUrl())."/nxbigfile/default/".$this->getUid()."/vid:storyboard/".$index."/content/storyboard-000.jpeg";
	}
	
	/**
	 * @return array - returns an array with the video subjects (dublincore field)
	 */
	public function getSubjects() {
		return $this->nuxeoDocument->getProperty("dc:subjects");
	}
	
	
	/**
	 * @return string - returns a string with the video subjects separated by comas.
	 */
	public function getSubjectsAsString() {
		$s = "";
		$i = 0;
		foreach ($this->nuxeoDocument->getProperty("dc:subjects") as $value) {
			if ($i>0) $s .= ', ';
			$s .= $value;
			$i++;
		}
		return $s;
	}
	
	public function containsSubject($subject) {
		if (array_search($subject, $this->nuxeoDocument->getProperty("dc:subjects")) >= 0) return TRUE;
		return FALSE;
	}
	
	/**
	 * @param $format - video format desired, WEBM recommended.
	 * @return string - returns the video url, use this as <source> in a <video> element to play it.
	 */
	public function getTranscodedVideoUrl($format = self::WEBM) {
		return $this->manager->getNuxeoUrl()."/nxbigfile/default/".$this->getUid()."/vid:transcodedVideos/".$format."/content/video.webm";		
	}
	
	/**
	 * @return string - original video file url, use it for downloading the video in the format it was uploaded, not recommended for playing it.
	 */
	public function getFileUrl() {
		return $this->manager->getNuxeoUrl()."/nxbigfile/default/".$this->getUid()."/file:content/";
	}
	
	
	
}