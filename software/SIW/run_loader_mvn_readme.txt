											Instructions to run UML loader using Maven

1.       Go to Admin tool, create a new account. You can find similar accounts in DEV tier, make the new account has same groups, privilege as the old accounts. 
2.       There should be an existing UML LOADER account in DEV, ask system team what is the password.
3.       Use Toad (or other tools) to login to DEV DB. Find table UML_LOADER_DEFAULTS Under SBREXT. Each row represents one model/CS loading.
         Insert a new row at the end of the table, make the ID = the current biggest ID +1, give it a project name (e.g. UMLLoader_TEST, version 1). context Name "NCIP" or any other context name to load model into selected context. Leave PACKAGE_FILTER empty to include the whole model or package names.
4.       Check out the latest UML Loader code
5.       Under properties folder, there is db.properties file. Put correct tier(dev/qa/stage/prod etc.) DB info into the file.
		 changed to
		 db.url.dev = jdbc:oracle:thin:@url:port:SID
6.       Update the build.properties file by changing dir, projectName & projectVersion.
		 dir= {SIW_root_folder}/data
7.       Put the model xmi file into {SIW_root_folder}/data. 
8.		 Navigate to {SIW_root_folder} and run the following command to launch UML loader

		 mvn install exec:exec -Ddb.url=<DB Hostname>:<Port>:<SID> -Ddb.passwd=<Password> -Ddb.user=<Username>

9.		 Type the account name that you created through Admin Tool as first step in both fields in the dialog that pops up. 
		 The Loader will ask if you want to continue loading despite the errors, type 'y' in the dialog to continue loading and 'N' to stop loading. 
		 [Typed options have to be the exact case as shown in the dialog]
		 
		 At then end of loading, should see log like this
		 [java] INFO CadsrLoader.run(179) | refreshing database views
		 [java] INFO CadsrLoader.run(201) | refreshed databased views
           
		 Wait for minutes (depending on model size, might be much longer when load big model) until see
		 BUILD SUCCESSFUL
		 Total time: xx minutes xx second
		 
10.   	 Login to Admin Tool > Classification Schema, search Fields "Long Name", Search for the project name (in step 3). In the search result, click "Modify" icon, change Workflow status from "Draft New" to "RELEASED".
11.  	 Open CDEBrowser, refresh tree. In the tree view, expend NCIP to find the newly loaded CS "UMLLoader_TEST". Newly created CDEs are under this CS.
12.      Best way to test whether the loading is done properly, launch the SIW and validate the model that loaded before. If it shows any validation errors after loading, then the loading is not successful.