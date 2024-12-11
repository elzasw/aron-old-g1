Transformacni agent
==========================

Vyzaduje Java11

Pro spusteni testu
---------------------

Prerequisities
------------------

Library javax.imageio.ImageIO is used to read images and transform them. 
Library  imageio-jnr (https://github.com/dbmdz/imageio-jnr) is used to read 
Jp2 files. 

Linux (Debian):
   sudo apt install libturbojpeg0 libopenjp2-7

Windows:
  OpenJPEG: https://github.com/uclouvain/openjpeg/releases/tag/v2.5.3


Eclipse
=========

To run test in Eclipse set environment variable:
  PATH to the value: C:\Pyta\toolkits\openjpeg\openjpeg-v2.5.3-windows-x64\bin;${env_var:PATH}

To run tests in Windows from Command line:

  SET PATH=%PATH%;C:\install\openjpeg-v2.5.0-windows-x64\bin
  mvn11.bat %*
