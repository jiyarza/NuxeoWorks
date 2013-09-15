<?php

include_once ('NuxeoAutomationAPI.php');
include_once ('Video.php');

/*
 * @author jiyarza
 */
class VideoManager {
	
	private $host, $port, $username, $password;	
	private $client;
	private $test;
	
	const ORDER_BY_DATE_DESC = "dc:created desc";
	const ORDER_BY_NAME = "dc:title";
	
	const FIELDS = "uid, ecm:path, dc:title, dc:description, dc:created, ecm:currentLifeCycleState, ecm:primaryType";
	
	
	public function __construct($host = 'localhost', $port = '8080', $username = 'Administrator', $password = 'Administrator') {
		$this->host = $host;
		$this->port = $port;
		$this->username = $username;
		$this->password = $password;
		$this->client = new NuxeoPhpAutomationClient($this->getRestUrl());		
	}
	
	public function getNuxeoUrl() {
		return "http://".$this->host.":".$this->port."/nuxeo";
	}
	
	public function getRestUrl() {
		return $this->getNuxeoUrl()."/site/automation";
	}
		
	public function _getVideos($orderBy = self::ORDER_BY_DATE_DESC) {

		$session = $this->client->getSession($this->username, $this->password);
		$answer = $session->newRequest("Document.Query")->set('params', 'query', "SELECT * FROM Video where ecm:currentLifeCycleState!='deleted' order by ".$orderBy)->setSchema("dublincore, file")->sendRequest();
		
		$documentsArray = $answer->getDocumentList();
		$value = sizeof($documentsArray);
		$videos = array($value);	
		
		for ($i = 0; $i < $value; $i++) {
			$video = new Video($this, current($documentsArray));
			$videos[$i] = $video;
			next($documentsArray);
		}

		return $videos;
	}
	
	public function getVideos($filter = null, $orderBy = self::ORDER_BY_DATE_DESC) {
	
		$query = "SELECT * FROM Video where ecm:currentLifeCycleState!='deleted'";		
		if ($filter != null) {			
			$query .= " and (dc:title like '%".$filter."%' or dc:description like '%".$filter."%' or dc:subjects/* like '%".$filter."%')";
		}		
		$query .= " order by ".$orderBy;
		
		$session = $this->client->getSession($this->username, $this->password);
		$answer = $session->newRequest("Document.Query")->set('params', 'query', $query)->setSchema("dublincore, file")->sendRequest();
	
		$documentsArray = $answer->getDocumentList();
		$value = sizeof($documentsArray);
		$videos = array($value);
	
		for ($i = 0; $i < $value; $i++) {
			$video = new Video($this, current($documentsArray));
			$videos[$i] = $video;
			next($documentsArray);
		}
	
		return $videos;
	}
	
	
	public function getVideoByUid($uid) {
		if (!isset($uid)) throw new Exception("uid cannot be null");		
		$session = $this->client->getSession($this->username, $this->password);		
		$answer = $session->newRequest("Document.Query")->set('params', 'query', "SELECT * FROM Document where ecm:uuid = '".$uid."'")->setSchema("dublincore, file")->sendRequest();
		
		if (!isset($answer)) {
			//echo "<p>answer not set</p>";
		} else {
			$documentsArray = $answer->getDocumentList();
			$value = sizeof($documentsArray);
			//echo "<p>size=".$value."</p>";
		}
		
		if (0 < sizeof($answer)) {
			return new Video($this, current($documentsArray));
		} else {
			return null;
		}
	}
}