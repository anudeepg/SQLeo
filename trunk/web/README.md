Steps to adapt SQLeo website content hosted https://sqleo.sourceforge.net/index.html

1. Unzip htdocs.zip file in this folder
2. Modify the content if needed 
3. SFTP or Use Filezilla to connect to sourceforge server
   Go to directory  - /home/project-web/sqleo/htdocs
   Upload the changes in sourceforge server
4. Zip the local folder htdocs back to htdocs.zip
5. Remove the local folder htdocs and commit only htdocs.zip into SVN

NOTE : Incase if SVN htdocs.zip is outdated, SFTP to sourceforge to get the htdocs folder and update the SVN.
