# restez
An ORM style api made for Rest Api which allows fast, easy and efficient access to the Rest APIs

### Dependencies:-
* [reflection]( https://mvnrepository.com/artifact/org.reflections/reflections )
* [commons-io]( https://mvnrepository.com/artifact/commons-io/commons-io )
* [json]( https://mvnrepository.com/artifact/org.json/json/20160810 )

### Getting Started:-
 
 **Note:- For this example, we are going to use Rest API provided by jsonplaceholder.typicode.com that is made for
 practice purposes. The data that is taken from the API is the property of jsonplaceholder.typicode.com and, here, 
 it is used for demonstration purposes only.**
 
 For the first step, we need to initilize ***RestEz*** and this can be done by either using an 
 initialization config file, or the config method. 
  
  **Using the config file**
  
  In you project root, create a new file named "ezconfig.xml", and give it the following structure:-
  
  ```xml
  
  <?xml version="1.0" encoding="UTF-8" standalone="no" ?>
  <EzConfig>
	  <EzName>
		  DataFetcher
	  </EzName>
	  <EzUrl>
		  <auth>jsonplaceholder.typicode.com</auth>
		  <protocol>https</protocol>
	  </EzUrl>
	  <EzModels>
		  <model>
			  <name>DTOTest</name>
			  <class>model.DTOTest</class>
		  </model>
	  </EzModels>
  </EzConfig>
  
  ```
 
  **Explanation**
  ***EzName*** :- For this version, "EzName" is not important ( in later versions, handling data from 
                  multiple API's will be introduction and then the EzName attribute will come into play.)
  ***EzUrl*** :- The url of the API.
  ***EzModels*** :- The model classes ( mostly the same DTO objects that are made for ORM are used by adding annotations )
  
  **Using the config method ( Required when developing for Android )** :-
  ```java
  RestEz.configure(
					"DataFetcher", // EzName
					"https://jsonplaceholder.typicode.com", // EzUrl
					DTOObj1.class, DTOObj2.class); // EzModels
  ```
  
  The fields are same as above.
  
  **Making/Annotating the DTO Objects**:-
  
  ```java
  @EzModel ( name = "test" , extension = "posts/#" )
public class DTOTest {
	
	@EzItem ( name = "userId" )
	private Integer userId;
	
	@EzItem ( name = "id" )
	private Integer id;
	
	@EzItem ( name = "title" )
	private String title;
	
	@EzItem ( name = "body" )
	private String body;

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
	
}
```

***Note :- this object must reflect the JSON response***
**Example Response**:-
```JSON
{
  "userId": 1,
  "id": 1,
  "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
  "body": "quia et suscipit\nsuscipit recusandae consequuntur expedita et cum\nreprehenderit molestiae ut ut quas totam\nnostrum rerum est autem sunt rem eveniet architecto"
}
```
Where each field in the response is denoted by an object's fields.

** Calling the RestApi ( single object response ) **
*** Getting single post ***
```Java
      RestEz.Query query = RestEz.getQuery(RestEz.getConfiguration());
			DTOTest rs = query.execQuery(DTOTest.class, 1);
			
			System.out.println("userID : "+rs.getUserId()
					+" , id : "+rs.getId()
					+" , title : "+rs.getTitle()
					+" , body : "+rs.getBody()
					);
```
***output***:-
```
userID : 1 , id : 1 , title : sunt aut facere repellat provident occaecati excepturi optio reprehenderit , body : quia et suscipit
suscipit recusandae consequuntur expedita et cum
reprehenderit molestiae ut ut quas totam
nostrum rerum est autem sunt rem eveniet architecto
```

** Calling the RestApi ( Multiple object response ) **
*** Getting all the posts ***
```java
      RestEz.Query query = RestEz.getQuery(RestEz.getConfiguration());
			ArrayList<DTOTest> rs = query.execQuery(DTOTest.class, "");
			
      for ( DTOTest test : rs ) {
			  System.out.println("userID : "+test.getUserId()
				  	+" , id : "+test.getId()
					  +" , title : "+test.getTitle()
					  +" , body : "+test.getBody()
					  );
      }
```
