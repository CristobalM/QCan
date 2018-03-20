PREFIX ex:  <http://example.org/>

DELETE { ?b0 ex:arg ?b1 .
		?b1 ?p ?o }
INSERT { ?b0 ex:arg ?q }
WHERE
  { 
  	?b0 ex:type ex:join .
  	{
	  	?b1 ex:type ex:join .
	  	?b1 ex:arg ?q .
	  	?q ex:type ex:TP .
	  	?b1 ?p ?o
  	}
  	FILTER NOT EXISTS{
  		?b1 ex:arg ?q .
  		?q ex:type ?type .
  		FILTER(?type != ex:TP)
  	}
  	?b0 ex:arg ?b1 .
  } 