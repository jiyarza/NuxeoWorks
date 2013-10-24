NuxeoWorks
==========

This repo holds Nuxeo platform related crafts, such as plugins, add-ons or clients.

These are the current projects and their description:

javafx13-picture-gallery. This is a JavaFX 1.3 script that implements the GUI for a picture gallery. It adds rotation, animation and zooming to a set of photos accesible through http, improving the user experience. It was developed to be integrated in a Nuxeo plugin as a client side jar file (see nuxeo-picture-gallery in this repo).

nuxeo-picture-gallery. This is a Nuxeo plugin that contributes an alternative GUI for the Nuxeo picture gallery. It uses the above JavaFX picture gallery compiled in a jar. The user browser executes this jar using the Java plugin, which communicates with the server through http REST petitions.

nuxeo_dam_php_video_api. This project is a PHP client api to integrate Nuxeo DAM Videos in a PHP based site. It uses the PHP automation client and the Projekktor javascript video player for the video player service example. Offers basic operations such as search, blob and metadata extraction.

nxdroid-sync. This project is an Android sync client for Nuxeo. It is meant to allow the user to synchronize her media (image, audio, video, and documents) with a Nuxeo server, as well as publishing photos in Facebook. It has some dependency with custom document types from a custom server plugin, however removing this dependency is rather easy if needed, and it may be useful as example nonetheless. It is targeted for the Android api level 8 (Android 2.2) and Nuxeo DM 5.x.

