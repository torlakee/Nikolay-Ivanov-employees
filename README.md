# Nikolay-Ivanov-employees

Exercise analysis/summary:
  Position: Senior Fullstack developer

  It is mentioned to use only Java and Angular. I assume I should not use larger libraries or packages like Spring Boot.
  The position is for a senior engineer so I assume they require the following from the solution:
    - class describing the input data
	- input validation
	- error handling/reporting
	- create user-friendly UI
	- readme file describing 
		- how to set up a development and deployment environment
	    - how to run
        - how to test
    
  
  
  Tasks:
    Back-end:
		Set up a java project with an entrypoint class
		Create an entity/model which describes the input data
		Create an entity/model which describes the output message/result
		Set up a web server using JDK's built-in HTTP server
		Set a controller/handler which has provides a HTTP POST method
		Create a class which provides the following methods:
		   - method to validate if the HTTP method is called via POST request
		   - method to validate if the uploaded file has the correct structure
		   - method to validate of the uploaded file is csv // soft by filename extension
		   - method to read the file contents
		   - method to map the contents to an entity/model class
		   - method to find the solution:
		      - group by project
			  - count the days
    Front-end:
		Set up angular project	
		
        

    Create repository "Nikolay-Ivanov-employees"

    
	Requirements:
	   jdk 17+
	   angular 17+
